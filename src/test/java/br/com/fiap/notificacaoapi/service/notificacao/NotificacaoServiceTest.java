package br.com.fiap.notificacaoapi.service.notificacao;

import br.com.fiap.notificacaoapi.config.AbstractTest;
import br.com.fiap.notificacaoapi.enums.StatusNotificacao;
import br.com.fiap.notificacaoapi.enums.TipoNotificacao;
import br.com.fiap.notificacaoapi.model.entity.notificacao.Notificacao;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoAtualizadoEvent;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoCriadoEvent;
import br.com.fiap.notificacaoapi.repository.notificacao.NotificacaoRepository;
import br.com.fiap.notificacaoapi.service.rabbitmq.AgendamentoEventListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
class NotificacaoServiceTest extends AbstractTest {

    @Autowired
    private AgendamentoEventListener agendamentoEventListener;

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @MockitoBean
    private NotificacaoSender notificacaoSender;

    @Test
    void envioComSucessoMarcaComoEnviadaTest() {
        var eventId = UUID.randomUUID().toString();
        var evento = criarEventoCriado(eventId);

        Mockito.doNothing().when(notificacaoSender).enviar(Mockito.any());

        Assertions.assertDoesNotThrow(() -> agendamentoEventListener.receber(evento));

        var notificacao = notificacaoRepository.findByEventId(eventId).orElseThrow();
        Assertions.assertEquals(StatusNotificacao.ENVIADA, notificacao.getStatus());
        Assertions.assertNotNull(notificacao.getDataEnvio());
        Assertions.assertNull(notificacao.getMensagemErro());
    }

    @Test
    void falhaNoEnvioNaoPerdeORegistroDaNotificacaoTest() {
        var eventId = UUID.randomUUID().toString();
        var evento = criarEventoCriado(eventId);

        Mockito.doThrow(new MailSendException("Servidor SMTP indisponível"))
                .when(notificacaoSender).enviar(Mockito.any());

        Assertions.assertDoesNotThrow(() -> agendamentoEventListener.receber(evento));

        var notificacao = notificacaoRepository.findByEventId(eventId).orElseThrow();
        Assertions.assertEquals(StatusNotificacao.FALHA, notificacao.getStatus());
        Assertions.assertNotNull(notificacao.getMensagemErro());
        Assertions.assertNull(notificacao.getDataEnvio());
    }

    @Test
    void eventoDuplicadoNaoGeraNovoEnvioTest() {
        var eventId = UUID.randomUUID().toString();
        var evento = criarEventoCriado(eventId);

        Mockito.doNothing().when(notificacaoSender).enviar(Mockito.any());

        agendamentoEventListener.receber(evento);
        agendamentoEventListener.receber(evento);

        Mockito.verify(notificacaoSender, Mockito.times(1)).enviar(Mockito.any());
    }

    @Test
    void atualizacaoDeConsultaGeraNotificacaoComTipoAtualizadaTest() {
        var eventId = UUID.randomUUID().toString();
        var evento = criarEventoAtualizado(eventId);

        Mockito.doNothing().when(notificacaoSender).enviar(Mockito.any());

        Assertions.assertDoesNotThrow(() -> agendamentoEventListener.receber(evento));

        var notificacao = notificacaoRepository.findByEventId(eventId).orElseThrow();
        Assertions.assertEquals(TipoNotificacao.CONSULTA_ATUALIZADA, notificacao.getTipoNotificacao());
        Assertions.assertNotNull(notificacao.getDataHoraAnterior());
        Assertions.assertEquals(StatusNotificacao.ENVIADA, notificacao.getStatus());
    }

    @Test
    void lembreteDuplicadoNaoGeraNovoRegistroTest() {
        var consultaAtual = new Notificacao();
        consultaAtual.setEventId(UUID.randomUUID().toString());
        consultaAtual.setAgendamentoId(1);
        consultaAtual.setPacienteNome("Paciente Teste");
        consultaAtual.setPacienteEmail("paciente.teste@fiap.com");
        consultaAtual.setMedicoNome("Médico Teste");
        consultaAtual.setEspecialidade("Cardiologia");
        consultaAtual.setDataHoraConsulta(LocalDateTime.now().plusDays(1));

        var primeiro = notificacaoService.registrarLembretePendente(consultaAtual);
        var segundo = notificacaoService.registrarLembretePendente(consultaAtual);

        Assertions.assertTrue(primeiro.isPresent());
        Assertions.assertTrue(segundo.isEmpty());
    }

    private AgendamentoAtualizadoEvent criarEventoAtualizado(String eventId) {
        return new AgendamentoAtualizadoEvent(
                eventId,
                1,
                1,
                "Paciente Teste",
                "paciente.teste@fiap.com",
                1,
                "Médico Teste",
                "Cardiologia",
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(1),
                null,
                LocalDateTime.now()
        );
    }

    private AgendamentoCriadoEvent criarEventoCriado(String eventId) {
        return new AgendamentoCriadoEvent(
                eventId,
                1,
                1,
                "Paciente Teste",
                "paciente.teste@fiap.com",
                1,
                "Médico Teste",
                "Cardiologia",
                LocalDateTime.now().plusDays(1),
                null,
                LocalDateTime.now()
        );
    }
}