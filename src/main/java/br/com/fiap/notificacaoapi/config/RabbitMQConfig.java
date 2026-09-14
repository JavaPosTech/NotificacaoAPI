package br.com.fiap.notificacaoapi.config;

import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoAtualizadoEvent;
import br.com.fiap.notificacaoapi.model.rabbitmq.AgendamentoCriadoEvent;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String AGENDAMENTO_EXCHANGE = "agendamento.events";
    public static final String AGENDAMENTO_CRIADO_TYPE_ID = "agendamento.criado.v1";
    public static final String AGENDAMENTO_CRIADO_ROUTING_KEY = "agendamento.criado";
    public static final String AGENDAMENTO_ATUALIZADO_TYPE_ID = "agendamento.atualizado.v1";
    public static final String AGENDAMENTO_ATUALIZADO_ROUTING_KEY = "agendamento.atualizado";
    public static final String NOTIFICACAO_AGENDAMENTO_QUEUE = "notificacao.email.agendamento";

    @Bean
    public DirectExchange agendamentoExchange() {
        return new DirectExchange(AGENDAMENTO_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificacaoAgendamentoQueue() {
        return QueueBuilder
                .durable(NOTIFICACAO_AGENDAMENTO_QUEUE)
                .build();
    }

    @Bean
    public Binding agendamentoCriadoBinding(Queue notificacaoAgendamentoQueue, DirectExchange agendamentoExchange) {
        return BindingBuilder
                .bind(notificacaoAgendamentoQueue)
                .to(agendamentoExchange)
                .with(AGENDAMENTO_CRIADO_ROUTING_KEY);
    }

    @Bean
    public Binding agendamentoAtualizadoBinding(Queue notificacaoAgendamentoQueue, DirectExchange agendamentoExchange) {
        return BindingBuilder
                .bind(notificacaoAgendamentoQueue)
                .to(agendamentoExchange)
                .with(AGENDAMENTO_ATUALIZADO_ROUTING_KEY);
    }

    @Bean
    public DefaultClassMapper rabbitClassMapper() {
        var classMapper = new DefaultClassMapper();
        classMapper.setIdClassMapping(
                Map.of(
                        AGENDAMENTO_CRIADO_TYPE_ID,
                        AgendamentoCriadoEvent.class,

                        AGENDAMENTO_ATUALIZADO_TYPE_ID,
                        AgendamentoAtualizadoEvent.class
                )
        );

        return classMapper;
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter(DefaultClassMapper rabbitClassMapper) {
        var jsonMapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        var jsonMessageConverter = new JacksonJsonMessageConverter(jsonMapper);
        jsonMessageConverter.setClassMapper(rabbitClassMapper);

        return jsonMessageConverter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter jsonMessageConverter) {

        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
