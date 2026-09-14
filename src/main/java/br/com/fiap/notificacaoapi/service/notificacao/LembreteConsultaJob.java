package br.com.fiap.notificacaoapi.service.notificacao;

import br.com.fiap.notificacaoapi.model.entity.notificacao.Notificacao;
import br.com.fiap.notificacaoapi.repository.notificacao.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LembreteConsultaJob {

    private final NotificacaoRepository notificacaoRepository;

    private final NotificacaoService notificacaoService;

    private final NotificacaoSender notificacaoSender;

    @Scheduled(cron = "0 0 8 * * *")
    public void enviarLembretes() {
        var amanha = LocalDate.now().plusDays(1);
        var consultasDeAmanha = notificacaoRepository.findUltimoEstadoPorAgendamentoNaData(amanha);

        log.info("Job de lembrete iniciado - {} consulta(s) encontrada(s) para {}", consultasDeAmanha.size(), amanha);

        consultasDeAmanha.forEach(this::processarLembrete);
    }

    private void processarLembrete(Notificacao consultaAtual) {
        notificacaoService.registrarLembretePendente(consultaAtual)
                .ifPresentOrElse(this::enviar, () ->
                        log.info("Lembrete já enviado anteriormente - Agendamento: [ID: {}]", consultaAtual.getAgendamentoId()));
    }

    private void enviar(Notificacao lembrete) {
        try {
            notificacaoSender.enviar(lembrete);
            notificacaoService.marcarComoEnviada(lembrete.getId());
        } catch (Exception exception) {
            log.error("Erro ao enviar lembrete! - Agendamento: [ID: {}]", lembrete.getAgendamentoId(), exception);
            notificacaoService.marcarComoFalha(lembrete.getId(), exception.getMessage());
        }
    }
}