package br.com.fiap.notificacaoapi.model.entity.notificacao;

import br.com.fiap.notificacaoapi.enums.StatusNotificacao;
import br.com.fiap.notificacaoapi.enums.TipoNotificacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "notificacao", schema = "public")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "event_id", nullable = false, unique = true, length = 60)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_notificacao", nullable = false, length = 30)
    private TipoNotificacao tipoNotificacao;

    @Column(name = "agendamento_id", nullable = false)
    private Integer agendamentoId;

    @Column(name = "paciente_nome", nullable = false, length = 100)
    private String pacienteNome;

    @Column(name = "paciente_email", nullable = false, length = 100)
    private String pacienteEmail;

    @Column(name = "medico_nome", nullable = false, length = 100)
    private String medicoNome;

    @Column(nullable = false, length = 100)
    private String especialidade;

    @Column(name = "datahora_consulta", nullable = false)
    private LocalDateTime dataHoraConsulta;

    @Column(name = "datahora_anterior")
    private LocalDateTime dataHoraAnterior;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusNotificacao status;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

}
