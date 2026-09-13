package br.com.fiap.notificacaoapi.service.notificacao;

import br.com.fiap.notificacaoapi.enums.TipoNotificacao;
import br.com.fiap.notificacaoapi.model.entity.notificacao.Notificacao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class EmailNotificacaoSender implements NotificacaoSender {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final JavaMailSender mailSender;
    private final String from;
    private final String assuntoConsultaCriada;
    private final String assuntoConsultaAtualizada;
    private final String assuntoLembreteConsulta;

    public EmailNotificacaoSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.assunto-consulta-criada}") String assuntoConsultaCriada,
            @Value("${app.mail.assunto-consulta-atualizada}") String assuntoConsultaAtualizada,
            @Value("${app.mail.assunto-lembrete-consulta}") String assuntoLembreteConsulta) {
        this.mailSender = mailSender;
        this.from = from;
        this.assuntoConsultaCriada = assuntoConsultaCriada;
        this.assuntoConsultaAtualizada = assuntoConsultaAtualizada;
        this.assuntoLembreteConsulta = assuntoLembreteConsulta;
    }

    @Override
    @Retryable(retryFor = MailException.class, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void enviar(Notificacao notificacao) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notificacao.getPacienteEmail());
        message.setSubject(assuntoPara(notificacao.getTipoNotificacao()));
        message.setText(corpoPara(notificacao));

        mailSender.send(message);
    }

    private String assuntoPara(TipoNotificacao tipoNotificacao) {
        return switch (tipoNotificacao) {
            case CONSULTA_CRIADA -> assuntoConsultaCriada;
            case CONSULTA_ATUALIZADA -> assuntoConsultaAtualizada;
            case LEMBRETE_CONSULTA -> assuntoLembreteConsulta;
        };
    }

    private String corpoPara(Notificacao notificacao) {
        return switch (notificacao.getTipoNotificacao()) {
            case CONSULTA_CRIADA -> corpoConsultaCriada(notificacao);
            case CONSULTA_ATUALIZADA -> corpoConsultaAtualizada(notificacao);
            case LEMBRETE_CONSULTA -> corpoLembreteConsulta(notificacao);
        };
    }

    private String corpoConsultaCriada(Notificacao notificacao) {
        return String.format("""
                Olá, %s!

                Sua consulta com %s (%s) foi agendada com sucesso.

                Data e horário: %s
                Consulta: #%d

                Em caso de necessidade, entre em contato com a unidade responsável.

                Atenciosamente,
                Sistema Hospitalar
                """,
                notificacao.getPacienteNome(),
                notificacao.getMedicoNome(),
                notificacao.getEspecialidade(),
                notificacao.getDataHoraConsulta().format(FORMATTER),
                notificacao.getAgendamentoId());
    }

    private String corpoConsultaAtualizada(Notificacao notificacao) {
        return String.format("""
                Olá, %s!

                Sua consulta com %s (%s) foi remarcada.

                Data e horário anterior: %s
                Novo horário: %s
                Consulta: #%d

                Em caso de necessidade, entre em contato com a unidade responsável.

                Atenciosamente,
                Sistema Hospitalar
                """,
                notificacao.getPacienteNome(),
                notificacao.getMedicoNome(),
                notificacao.getEspecialidade(),
                notificacao.getDataHoraAnterior().format(FORMATTER),
                notificacao.getDataHoraConsulta().format(FORMATTER),
                notificacao.getAgendamentoId());
    }

    private String corpoLembreteConsulta(Notificacao notificacao) {
        return String.format("""
                !!!!!!!!!!!!!!! LEMBRETE !!!!!!!!!!!!!!!

                Olá, %s!

                Passando para lembrar da sua consulta com %s (%s)

                Data e horário: %s
                Consulta: #%d

                Em caso de necessidade, entre em contato com a unidade responsável.

                Atenciosamente,
                Sistema Hospitalar
                """,
                notificacao.getPacienteNome(),
                notificacao.getMedicoNome(),
                notificacao.getEspecialidade(),
                notificacao.getDataHoraConsulta().format(FORMATTER),
                notificacao.getAgendamentoId());
    }
}
