package br.com.fiap.notificacaoapi.service.notificacao;

import br.com.fiap.notificacaoapi.enums.StatusNotificacao;
import br.com.fiap.notificacaoapi.enums.TipoNotificacao;
import br.com.fiap.notificacaoapi.model.entity.notificacao.Notificacao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

class EmailNotificacaoSenderTest {

    private JavaMailSender mailSender;
    private EmailNotificacaoSender emailNotificacaoSender;

    @BeforeEach
    void setUp() {
        mailSender = Mockito.mock(JavaMailSender.class);
        emailNotificacaoSender = new EmailNotificacaoSender(
                mailSender,
                "naoresponda@hospital.fiap.br",
                "Consulta agendada com sucesso",
                "Sua consulta foi remarcada",
                "Lembrete: sua consulta é amanhã");
    }

    @Test
    void enviaEmailDeConsultaCriadaComAssuntoECorpoCorretosTest() {
        var notificacao = notificacaoBase(TipoNotificacao.CONSULTA_CRIADA);

        emailNotificacaoSender.enviar(notificacao);

        var mensagem = capturarMensagemEnviada();
        Assertions.assertEquals("Consulta agendada com sucesso", mensagem.getSubject());
        Assertions.assertTrue(mensagem.getText().contains("foi agendada com sucesso"));
        Assertions.assertEquals("paciente.teste@fiap.com", mensagem.getTo()[0]);
    }

    @Test
    void enviaEmailDeConsultaAtualizadaComDataAnteriorNoCorpoTest() {
        var notificacao = notificacaoBase(TipoNotificacao.CONSULTA_ATUALIZADA);
        notificacao.setDataHoraAnterior(LocalDateTime.of(2026, 9, 1, 9, 0));

        emailNotificacaoSender.enviar(notificacao);

        var mensagem = capturarMensagemEnviada();
        Assertions.assertEquals("Sua consulta foi remarcada", mensagem.getSubject());
        Assertions.assertTrue(mensagem.getText().contains("foi remarcada"));
        Assertions.assertTrue(mensagem.getText().contains("01/09/2026"));
    }

    @Test
    void enviaEmailDeLembreteComAssuntoECorpoCorretosTest() {
        var notificacao = notificacaoBase(TipoNotificacao.LEMBRETE_CONSULTA);

        emailNotificacaoSender.enviar(notificacao);

        var mensagem = capturarMensagemEnviada();
        Assertions.assertEquals("Lembrete: sua consulta é amanhã", mensagem.getSubject());
        Assertions.assertTrue(mensagem.getText().contains("LEMBRETE"));
        Assertions.assertTrue(mensagem.getText().contains("Passando para lembrar"));
    }

    private SimpleMailMessage capturarMensagemEnviada() {
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private Notificacao notificacaoBase(TipoNotificacao tipo) {
        var notificacao = new Notificacao();
        notificacao.setTipoNotificacao(tipo);
        notificacao.setAgendamentoId(1);
        notificacao.setPacienteNome("Paciente Teste");
        notificacao.setPacienteEmail("paciente.teste@fiap.com");
        notificacao.setMedicoNome("Médico Teste");
        notificacao.setEspecialidade("Cardiologia");
        notificacao.setDataHoraConsulta(LocalDateTime.of(2026, 9, 20, 14, 0));
        notificacao.setStatus(StatusNotificacao.PENDENTE);
        return notificacao;
    }
}