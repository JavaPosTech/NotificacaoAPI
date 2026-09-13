package br.com.fiap.notificacaoapi.model.rabbitmq;

import java.time.LocalDateTime;

public record AgendamentoCriadoEvent(

        String eventId,

        Integer agendamentoId,

        Integer pacienteId,

        String pacienteNome,

        String pacienteEmail,

        Integer medicoId,

        String medicoNome,

        String especialidade,

        LocalDateTime dataHoraConsulta,

        String observacao,

        LocalDateTime ocorridoEm

) {
}
