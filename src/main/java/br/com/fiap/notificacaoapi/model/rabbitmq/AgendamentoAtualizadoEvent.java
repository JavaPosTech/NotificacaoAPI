package br.com.fiap.notificacaoapi.model.rabbitmq;

import java.time.LocalDateTime;

public record AgendamentoAtualizadoEvent(

        String eventId,

        Integer agendamentoId,

        Integer pacienteId,

        String pacienteNome,

        String pacienteEmail,

        Integer medicoId,

        String medicoNome,

        String especialidade,

        LocalDateTime dataHoraAnterior,

        LocalDateTime dataHoraAtual,

        String observacao,

        LocalDateTime ocorridoEm

) {
}
