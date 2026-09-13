CREATE TABLE IF NOT EXISTS public.notificacao (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    tipo_notificacao VARCHAR(30) NOT NULL,
    agendamento_id INTEGER NOT NULL,
    paciente_nome VARCHAR(100) NOT NULL,
    paciente_email VARCHAR(100) NOT NULL,
    medico_nome VARCHAR(100) NOT NULL,
    especialidade VARCHAR(100) NOT NULL,
    datahora_consulta TIMESTAMP NOT NULL,
    datahora_anterior TIMESTAMP,
    observacao TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    mensagem_erro TEXT,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_envio TIMESTAMP,
    CONSTRAINT uk_notificacao_event_id UNIQUE (event_id)
);
