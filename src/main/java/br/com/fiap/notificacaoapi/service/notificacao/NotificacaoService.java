package br.com.fiap.notificacaoapi.service.notificacao;

import br.com.fiap.notificacaoapi.enums.StatusNotificacao;
import br.com.fiap.notificacaoapi.exceptions.NotificacaoNaoEncontradaException;
import br.com.fiap.notificacaoapi.model.entity.notificacao.Notificacao;
import br.com.fiap.notificacaoapi.model.mapper.notificacao.NotificacaoMapper;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoAtualizadoEvent;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoCriadoEvent;
import br.com.fiap.notificacaoapi.repository.notificacao.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    private final NotificacaoMapper notificacaoMapper;

    @Transactional
    public Optional<Notificacao> registrarPendente(AgendamentoCriadoEvent evento) {
        if (notificacaoRepository.existsByEventId(evento.eventId())) {
            log.info("Evento [{}] já processado. Ignorando duplicidade.", evento.eventId());
            return Optional.empty();
        }

        log.info("Registrando notificação de consulta criada... - Agendamento: [ID: {}]", evento.agendamentoId());
        var notificacao = notificacaoRepository.save(notificacaoMapper.toNotificacao(evento));
        return Optional.of(notificacao);
    }

    @Transactional
    public Optional<Notificacao> registrarPendente(AgendamentoAtualizadoEvent evento) {
        if (notificacaoRepository.existsByEventId(evento.eventId())) {
            log.info("Evento [{}] já processado. Ignorando duplicidade.", evento.eventId());
            return Optional.empty();
        }

        log.info("Registrando notificação de consulta atualizada... - Agendamento: [ID: {}]", evento.agendamentoId());
        var notificacao = notificacaoRepository.save(notificacaoMapper.toNotificacao(evento));
        return Optional.of(notificacao);
    }

    @Transactional
    public Optional<Notificacao> registrarLembretePendente(Notificacao consultaAtual) {
        var eventId = "lembrete-" + consultaAtual.getEventId();

        if (notificacaoRepository.existsByEventId(eventId)) {
            log.info("Lembrete [{}] já enviado. Ignorando duplicidade.", eventId);
            return Optional.empty();
        }

        log.info("Registrando lembrete de consulta... - Agendamento: [ID: {}]", consultaAtual.getAgendamentoId());
        var lembrete = notificacaoRepository.save(notificacaoMapper.toLembrete(consultaAtual, eventId));
        return Optional.of(lembrete);
    }

    @Transactional
    public void marcarComoEnviada(Integer id) {
        var notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new NotificacaoNaoEncontradaException("Notificação não encontrada!"));

        notificacao.setStatus(StatusNotificacao.ENVIADA);
        notificacao.setDataEnvio(LocalDateTime.now());
        notificacao.setMensagemErro(null);

        log.info("Notificação enviada com sucesso! - Agendamento: [ID: {}]", notificacao.getAgendamentoId());
    }

    @Transactional
    public void marcarComoFalha(Integer id, String mensagemErro) {
        var notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new NotificacaoNaoEncontradaException("Notificação não encontrada!"));

        notificacao.setStatus(StatusNotificacao.FALHA);
        notificacao.setMensagemErro(mensagemErro);

        log.warn("Falha registrada para a notificação! - Agendamento: [ID: {}]", notificacao.getAgendamentoId());
    }
}
