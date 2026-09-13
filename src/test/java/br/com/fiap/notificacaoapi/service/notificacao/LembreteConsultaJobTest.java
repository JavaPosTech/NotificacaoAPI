package br.com.fiap.notificacaoapi.service.notificacao;

import br.com.fiap.notificacaoapi.config.AbstractTest;
import br.com.fiap.notificacaoapi.enums.StatusNotificacao;
import br.com.fiap.notificacaoapi.enums.TipoNotificacao;
import br.com.fiap.notificacaoapi.model.entity.notificacao.Notificacao;
import br.com.fiap.notificacaoapi.repository.notificacao.NotificacaoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
class LembreteConsultaJobTest extends AbstractTest {

    @Autowired
    private LembreteConsultaJob lembreteConsultaJob;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @MockitoBean
    private NotificacaoSender notificacaoSender;

    @Test
    void lembreteEnviadoParaConsultaDeAmanhaTest() {
        Mockito.doNothing().when(notificacaoSender).enviar(Mockito.any());

        var consulta = salvarConsulta(TipoNotificacao.CONSULTA_CRIADA, LocalDate.now().plusDays(1).atTime(10, 0));

        lembreteConsultaJob.enviarLembretes();

        var lembrete = notificacaoRepository.findByEventId("lembrete-" + consulta.getEventId()).orElseThrow();
        Assertions.assertEquals(TipoNotificacao.LEMBRETE_CONSULTA, lembrete.getTipoNotificacao());
        Assertions.assertEquals(StatusNotificacao.ENVIADA, lembrete.getStatus());
        Assertions.assertNotNull(lembrete.getDataEnvio());
    }

    @Test
    void lembreteNaoEDuplicadoAoRodarDeNovoTest() {
        Mockito.doNothing().when(notificacaoSender).enviar(Mockito.any());
        salvarConsulta(TipoNotificacao.CONSULTA_CRIADA, LocalDate.now().plusDays(1).atTime(10, 0));

        lembreteConsultaJob.enviarLembretes();
        lembreteConsultaJob.enviarLembretes();

        Mockito.verify(notificacaoSender, Mockito.times(1)).enviar(Mockito.any());
    }

    @Test
    void lembreteConsideraApenasUltimoEstadoDoAgendamentoTest() {
        Mockito.doNothing().when(notificacaoSender).enviar(Mockito.any());

        var agendamentoId = 999;
        salvarConsulta(agendamentoId, TipoNotificacao.CONSULTA_CRIADA, LocalDate.now().plusDays(10).atTime(9, 0));
        var atualizada = salvarConsulta(agendamentoId, TipoNotificacao.CONSULTA_ATUALIZADA, LocalDate.now().plusDays(1).atTime(15, 0));

        lembreteConsultaJob.enviarLembretes();

        var lembrete = notificacaoRepository.findByEventId("lembrete-" + atualizada.getEventId()).orElseThrow();
        Assertions.assertEquals(agendamentoId, lembrete.getAgendamentoId());
        Assertions.assertEquals(atualizada.getDataHoraConsulta(), lembrete.getDataHoraConsulta());
    }

    @Test
    void consultaQueNaoEAmanhaNaoGeraLembreteTest() {
        salvarConsulta(TipoNotificacao.CONSULTA_CRIADA, LocalDate.now().plusDays(5).atTime(10, 0));

        lembreteConsultaJob.enviarLembretes();

        Mockito.verify(notificacaoSender, Mockito.never()).enviar(Mockito.any());
    }

    @Test
    void falhaNoEnvioDoLembreteRegistraStatusFalhaTest() {
        Mockito.doThrow(new MailSendException("Servidor SMTP indisponível"))
                .when(notificacaoSender).enviar(Mockito.any());

        var consulta = salvarConsulta(TipoNotificacao.CONSULTA_CRIADA, LocalDate.now().plusDays(1).atTime(10, 0));

        lembreteConsultaJob.enviarLembretes();

        var lembrete = notificacaoRepository.findByEventId("lembrete-" + consulta.getEventId()).orElseThrow();
        Assertions.assertEquals(StatusNotificacao.FALHA, lembrete.getStatus());
        Assertions.assertNotNull(lembrete.getMensagemErro());
    }

    private Notificacao salvarConsulta(TipoNotificacao tipo, LocalDateTime dataHoraConsulta) {
        return salvarConsulta(1, tipo, dataHoraConsulta);
    }

    private Notificacao salvarConsulta(Integer agendamentoId, TipoNotificacao tipo, LocalDateTime dataHoraConsulta) {
        var notificacao = new Notificacao();
        notificacao.setEventId(UUID.randomUUID().toString());
        notificacao.setTipoNotificacao(tipo);
        notificacao.setAgendamentoId(agendamentoId);
        notificacao.setPacienteNome("Paciente Teste");
        notificacao.setPacienteEmail("paciente.teste@fiap.com");
        notificacao.setMedicoNome("Médico Teste");
        notificacao.setEspecialidade("Cardiologia");
        notificacao.setDataHoraConsulta(dataHoraConsulta);
        notificacao.setStatus(StatusNotificacao.ENVIADA);
        notificacao.setDataCadastro(LocalDateTime.now());
        return notificacaoRepository.save(notificacao);
    }
}