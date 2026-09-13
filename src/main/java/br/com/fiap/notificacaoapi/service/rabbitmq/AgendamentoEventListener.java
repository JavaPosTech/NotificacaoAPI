package br.com.fiap.notificacaoapi.service.rabbitmq;

import br.com.fiap.notificacaoapi.config.RabbitMQConfig;
import br.com.fiap.notificacaoapi.model.entity.notificacao.Notificacao;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoAtualizadoEvent;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoCriadoEvent;
import br.com.fiap.notificacaoapi.service.notificacao.NotificacaoSender;
import br.com.fiap.notificacaoapi.service.notificacao.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitMQConfig.NOTIFICACAO_AGENDAMENTO_QUEUE)
public class AgendamentoEventListener {

    private final NotificacaoService notificacaoService;

    private final NotificacaoSender notificacaoSender;

    @RabbitHandler
    public void receber(AgendamentoCriadoEvent evento) {
        log.info("Evento de consulta criada recebido - Agendamento: [ID: {}] - Event ID: [{}]",
                evento.agendamentoId(), evento.eventId());

        notificacaoService.registrarPendente(evento)
                .ifPresentOrElse(this::processar, () -> log.info("Evento [{}] não gerou notificação nova.", evento.eventId()));
    }

    @RabbitHandler
    public void receber(AgendamentoAtualizadoEvent evento) {
        log.info("Evento de consulta atualizada recebido - Agendamento: [ID: {}] - Event ID: [{}]",
                evento.agendamentoId(), evento.eventId());

        notificacaoService.registrarPendente(evento)
                .ifPresentOrElse(this::processar, () -> log.info("Evento [{}] não gerou notificação nova.", evento.eventId()));
    }

    private void processar(Notificacao notificacao) {
        try {
            notificacaoSender.enviar(notificacao);
            notificacaoService.marcarComoEnviada(notificacao.getId());
        } catch (Exception exception) {
            log.error("Erro ao enviar notificação! - Agendamento: [ID: {}]", notificacao.getAgendamentoId(), exception);
            notificacaoService.marcarComoFalha(notificacao.getId(), exception.getMessage());
        }
    }
}
