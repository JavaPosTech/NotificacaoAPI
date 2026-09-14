package br.com.fiap.notificacaoapi.service.rabbitmq;

import br.com.fiap.notificacaoapi.config.RabbitMQConfig;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoAtualizadoEvent;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoCriadoEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

class ContratoEventosTest {

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    private final JacksonJsonMessageConverter jsonMessageConverter =
            rabbitMQConfig.jsonMessageConverter(rabbitMQConfig.rabbitClassMapper());

    @Test
    void eventoCriadoDentroDoContratoEConvertidoTest() {
        var mensagem = criarMensagem(RabbitMQConfig.AGENDAMENTO_CRIADO_TYPE_ID, eventoCriadoJson(""));

        var evento = Assertions.assertInstanceOf(AgendamentoCriadoEvent.class, jsonMessageConverter.fromMessage(mensagem));

        Assertions.assertEquals("a9ab74d4-6cd2-4f20-8b4d-5a7aaee589ea", evento.eventId());
        Assertions.assertEquals(10, evento.agendamentoId());
        Assertions.assertEquals("pedro.almeida@email.com", evento.pacienteEmail());
        Assertions.assertEquals(LocalDateTime.of(2027, 8, 25, 14, 0), evento.dataHoraConsulta());
    }

    @Test
    void eventoAtualizadoDentroDoContratoEConvertidoTest() {
        var mensagem = criarMensagem(RabbitMQConfig.AGENDAMENTO_ATUALIZADO_TYPE_ID, """
                {
                  "eventId": "37cd45d1-6991-4c42-9350-2c4d71b84696",
                  "agendamentoId": 10,
                  "pacienteId": 1,
                  "pacienteNome": "PEDRO ALMEIDA",
                  "pacienteEmail": "pedro.almeida@email.com",
                  "medicoId": 1,
                  "medicoNome": "JOAO SILVA",
                  "especialidade": "CARDIOLOGIA",
                  "dataHoraAnterior": "2027-08-25T14:00:00",
                  "dataHoraAtual": "2027-08-25T16:30:00",
                  "observacao": null,
                  "ocorridoEm": "2026-09-12T15:40:00"
                }
                """);

        var evento = Assertions.assertInstanceOf(AgendamentoAtualizadoEvent.class, jsonMessageConverter.fromMessage(mensagem));

        Assertions.assertEquals(LocalDateTime.of(2027, 8, 25, 14, 0), evento.dataHoraAnterior());
        Assertions.assertEquals(LocalDateTime.of(2027, 8, 25, 16, 30), evento.dataHoraAtual());
    }

    @Test
    void eventoComCampoForaDoContratoERecusadoTest() {
        var mensagem = criarMensagem(RabbitMQConfig.AGENDAMENTO_CRIADO_TYPE_ID,
                eventoCriadoJson(",\n  \"campoNovo\": \"fora do contrato\""));

        Assertions.assertThrows(MessageConversionException.class, () -> jsonMessageConverter.fromMessage(mensagem));
    }

    private String eventoCriadoJson(String campoExtra) {
        return """
                {
                  "eventId": "a9ab74d4-6cd2-4f20-8b4d-5a7aaee589ea",
                  "agendamentoId": 10,
                  "pacienteId": 1,
                  "pacienteNome": "PEDRO ALMEIDA",
                  "pacienteEmail": "pedro.almeida@email.com",
                  "medicoId": 1,
                  "medicoNome": "JOAO SILVA",
                  "especialidade": "CARDIOLOGIA",
                  "dataHoraConsulta": "2027-08-25T14:00:00",
                  "observacao": "Primeira consulta cardiológica.",
                  "ocorridoEm": "2026-09-12T15:32:58"%s
                }
                """.formatted(campoExtra);
    }

    private Message criarMensagem(String typeId, String json) {
        var propriedades = new MessageProperties();
        propriedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propriedades.setContentEncoding(StandardCharsets.UTF_8.name());
        propriedades.setHeader("__TypeId__", typeId);

        return new Message(json.getBytes(StandardCharsets.UTF_8), propriedades);
    }
}
