package br.com.fiap.notificacaoapi.model.mapper.notificacao;

import br.com.fiap.notificacaoapi.model.entity.notificacao.Notificacao;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoAtualizadoEvent;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoCriadoEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificacaoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoNotificacao", expression = "java(br.com.fiap.notificacaoapi.enums.TipoNotificacao.CONSULTA_CRIADA)")
    @Mapping(target = "dataHoraConsulta", source = "dataHoraConsulta")
    @Mapping(target = "dataHoraAnterior", ignore = true)
    @Mapping(target = "status", expression = "java(br.com.fiap.notificacaoapi.enums.StatusNotificacao.PENDENTE)")
    @Mapping(target = "mensagemErro", ignore = true)
    @Mapping(target = "dataCadastro", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "dataEnvio", ignore = true)
    Notificacao toNotificacao(AgendamentoCriadoEvent evento);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoNotificacao", expression = "java(br.com.fiap.notificacaoapi.enums.TipoNotificacao.CONSULTA_ATUALIZADA)")
    @Mapping(target = "dataHoraConsulta", source = "dataHoraAtual")
    @Mapping(target = "dataHoraAnterior", source = "dataHoraAnterior")
    @Mapping(target = "status", expression = "java(br.com.fiap.notificacaoapi.enums.StatusNotificacao.PENDENTE)")
    @Mapping(target = "mensagemErro", ignore = true)
    @Mapping(target = "dataCadastro", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "dataEnvio", ignore = true)
    Notificacao toNotificacao(AgendamentoAtualizadoEvent evento);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "tipoNotificacao", expression = "java(br.com.fiap.notificacaoapi.enums.TipoNotificacao.LEMBRETE_CONSULTA)")
    @Mapping(target = "agendamentoId", source = "consultaAtual.agendamentoId")
    @Mapping(target = "pacienteNome", source = "consultaAtual.pacienteNome")
    @Mapping(target = "pacienteEmail", source = "consultaAtual.pacienteEmail")
    @Mapping(target = "medicoNome", source = "consultaAtual.medicoNome")
    @Mapping(target = "especialidade", source = "consultaAtual.especialidade")
    @Mapping(target = "dataHoraConsulta", source = "consultaAtual.dataHoraConsulta")
    @Mapping(target = "dataHoraAnterior", ignore = true)
    @Mapping(target = "observacao", source = "consultaAtual.observacao")
    @Mapping(target = "status", expression = "java(br.com.fiap.notificacaoapi.enums.StatusNotificacao.PENDENTE)")
    @Mapping(target = "mensagemErro", ignore = true)
    @Mapping(target = "dataCadastro", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "dataEnvio", ignore = true)
    Notificacao toLembrete(Notificacao consultaAtual, String eventId);

}
