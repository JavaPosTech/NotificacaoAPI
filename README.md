<div align="center"> <br> 
  <img align="center" alt="guru-java" height="150" width="150" src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/spring/spring-original.svg" />
</div> 

<br> 

<div align="center">
  Turma 12ADJT – Projeto desenvolvido na pós-graduação em Arquitetura e Desenvolvimento em Java da FIAP. O objetivo é desenvolver um serviço responsável por consumir os eventos de agendamento publicados pela AgendamentoAPI, notificar os pacientes por e-mail e enviar lembretes das consultas agendadas.
</div> 

<br>

<div align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white" />
  <img alt="Spring Boot 4.0.5" src="https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?style=flat-square&logo=springboot&logoColor=white" />
  <img alt="PostgreSQL 18" src="https://img.shields.io/badge/PostgreSQL-18-4169E1?style=flat-square&logo=postgresql&logoColor=white" />
  <img alt="RabbitMQ 4" src="https://img.shields.io/badge/RabbitMQ-4-FF6600?style=flat-square&logo=rabbitmq&logoColor=white" />
</div>

 <br> <br> 

## 🧰 Ferramentas Utilizadas

* 📝 Log4j2

* 🦅 Flyway

* ☕️ Java 21

* 🐘 PostgreSQL 18

* 🗄️ Spring Data JPA

* 🟢 Spring Boot 4.0.5

* 🔄 MapStruct + Lombok

* 🐇 RabbitMQ (Spring AMQP)

* 📧 Spring Mail + Spring Retry

* 🧪 JUnit 5 + Mockito + JaCoCo

* 🛠️ Gradle 9.7 (Kotlin DSL)

* 🐳 Docker / Docker Compose

<br> 

## 📁 Estrutura do Projeto

O código é organizado **por camada e, dentro de cada camada, por domínio**:

```
src/main/java/br/com/fiap/notificacaoapi/
├── config/           # DataBaseConfig (perfis dev e prod) e RabbitMQConfig
├── enums/            # StatusNotificacao, TipoNotificacao
├── exceptions/       # NotificacaoNaoEncontradaException
├── model/
│   ├── entity/       # Notificacao (entidade JPA)
│   ├── mapper/       # NotificacaoMapper (MapStruct)
│   └── rabbitmq/     # Contrato dos eventos publicados pela AgendamentoAPI (records)
├── repository/       # NotificacaoRepository
└── service/
    ├── notificacao/  # NotificacaoService, LembreteConsultaJob, NotificacaoSender e EmailNotificacaoSender
    └── rabbitmq/     # AgendamentoEventListener — consumidor da fila

src/main/resources/
├── application.yaml  # Perfis dev, prod e test
├── log4j2.xml        # Console em dev; arquivo rotativo em prod
└── db/migration/     # Migrations Flyway da tabela notificacao
```

> ℹ️ Esta aplicação **não expõe nenhuma rota HTTP**: o `application.yaml` define `spring.main.web-application-type: none`. Toda a entrada acontece pela fila do RabbitMQ e pelo job agendado, por isso não há Swagger, context path nem porta publicada.

> ℹ️ O projeto **não utiliza Javadoc nem comentários explicativos** — nem no código Java, nem nos arquivos de build e de infraestrutura (`build.gradle.kts`, Compose, `Dockerfile`). Todo o contexto de arquitetura, execução e infraestrutura fica neste `README.md`.

<br> 

## ⚙️ Comandos Disponíveis

Os comandos de build, testes e execução estão disponíveis via Gradle Wrapper:

```bash
# Build completo (com testes, relatório e verificação de cobertura)
./gradlew build

# Build sem testes
./gradlew clean build -x test

# Executar a suíte de testes
./gradlew test

# Executar o serviço no perfil desejado
./gradlew bootRun --args="--spring.profiles.active=dev"
```

> ℹ️ No Windows, utilize `.\gradlew.bat` no lugar de `./gradlew`.

<br> 

## 🔄 Fluxo de Notificação

```
AgendamentoAPI ──▶ exchange agendamento.events (direct)
                        │  agendamento.criado
                        │  agendamento.atualizado
                        ▼
                  fila notificacao.email.agendamento
                        │
                        ▼
                  AgendamentoEventListener ──▶ tabela notificacao ──▶ e-mail ao paciente
                                                     ▲
                  LembreteConsultaJob ───────────────┘
```

Toda notificação passa pelo mesmo ciclo, em três passos independentes:

1. `NotificacaoService.registrarPendente(...)` grava o registro com status `PENDENTE`, em uma transação própria.
2. `NotificacaoSender.enviar(...)` tenta enviar o e-mail, **fora** de qualquer transação de banco.
3. Conforme o resultado, `marcarComoEnviada(...)` grava `ENVIADA` e a `dataEnvio`, ou `marcarComoFalha(...)` grava `FALHA` e a mensagem de erro — cada um também na sua própria transação.

Como o envio não participa da transação que criou o registro, uma falha no e-mail **nunca desfaz** a notificação que já foi gravada. A tabela `notificacao` funciona como auditoria completa: todo evento recebido fica registrado junto com o seu desfecho.

<br> 

### 🎯 Responsabilidades

A entidade `Notificacao` contém apenas o mapeamento JPA (`@Getter`/`@Setter`), sem regra de negócio. As decisões — idempotência, transição de status e tratamento de falha — ficam no `NotificacaoService`.

O envio fica atrás da interface `NotificacaoSender`, e hoje `EmailNotificacaoSender` é a única implementação. Um novo canal, como SMS ou push, entra como uma nova implementação, sem alterar o `NotificacaoService` nem o listener.

<br> 

### 📧 Tipos de Notificação

| Tipo | Origem | Assunto do e-mail |
| --- | --- | --- |
| `CONSULTA_CRIADA` | Evento `agendamento.criado` | Consulta agendada com sucesso |
| `CONSULTA_ATUALIZADA` | Evento `agendamento.atualizado` | Sua consulta foi remarcada |
| `LEMBRETE_CONSULTA` | `LembreteConsultaJob` | Lembrete: sua consulta é amanhã |

O remetente vem de `MAIL_FROM` (padrão `naoresponda@hospital.fiap.br`) e os assuntos ficam em `app.mail.*`, no `application.yaml`. O e-mail de remarcação traz o horário anterior e o novo.

<br> 

## 🐇 Contrato de Eventos (RabbitMQ)

A AgendamentoAPI publica na exchange `agendamento.events` (direct) com duas routing keys, ambas ligadas à mesma fila consumida por este serviço:

| Routing key | Type ID | Classe |
| --- | --- | --- |
| `agendamento.criado` | `agendamento.criado.v1` | `AgendamentoCriadoEvent` |
| `agendamento.atualizado` | `agendamento.atualizado.v1` | `AgendamentoAtualizadoEvent` |

Fila consumida: **`notificacao.email.agendamento`** (durável).

O `RabbitMQConfig` declara a exchange, a fila e os bindings **com os mesmos nomes e argumentos** da AgendamentoAPI, de modo que tanto faz qual dos dois serviços sobe primeiro. Ele também registra um `DefaultClassMapper` com os mesmos Type IDs, apontando para as classes locais. Esse mapeamento é obrigatório: a AgendamentoAPI grava o Type ID, e não o nome completo da classe, no header `__TypeId__` da mensagem — sem ele, a desserialização falha.

> ⚠️ **O contrato é estrito.** O conversor de mensagens do `RabbitMQConfig` usa um `JsonMapper` com `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` habilitado, então um campo novo no evento publicado pela AgendamentoAPI faz a desserialização falhar e a mensagem é descartada, sem gerar notificação. O `JsonMapper` é montado explicitamente porque o conversor padrão do Spring AMQP desliga essa verificação, e a propriedade `spring.jackson` do `application.yaml` não chega até ele. Ao alterar os records de evento lá, altere também os de `model/rabbitmq/` aqui.

> ⚠️ **Não há Dead Letter Queue.** O RabbitMQ exige que uma fila já existente seja redeclarada com argumentos **idênticos**; caso contrário, recusa a conexão com `406 PRECONDITION_FAILED`. Como a fila também é declarada, sem DLQ, pela AgendamentoAPI, adicionar `x-dead-letter-exchange` só aqui quebraria a integração. Para ter DLQ, os argumentos da fila precisam mudar **nos dois projetos ao mesmo tempo**.

<br> 

### 🛡️ Resiliência

Sem DLQ, a resiliência é tratada em duas camadas:

1. **Retry local:** `EmailNotificacaoSender.enviar(...)` possui `@Retryable` para `MailException` — 3 tentativas, com espera inicial de 2 segundos que dobra a cada nova tentativa — absorvendo falhas transitórias de SMTP sem devolver a mensagem à fila. Cada tentativa tem limite de 5 segundos para conectar, ler e escrever (`mail.smtp.connectiontimeout`, `mail.smtp.timeout` e `mail.smtp.writetimeout`), então um servidor de e-mail que não responde gera `FALHA` em cerca de 20 segundos, em vez de prender o único consumidor da fila no timeout do sistema operacional.
2. **Persistência como fonte da verdade:** se todas as tentativas falharem, o listener captura a exceção, grava o status `FALHA` com a mensagem de erro e confirma a mensagem normalmente. Isso evita um loop infinito de reentrega causado por uma mensagem que nunca será processada.

> ℹ️ O container do listener usa `setDefaultRequeueRejected(false)`. Uma exceção que aconteça **antes** do registro da notificação — por exemplo, erro de desserialização ou banco indisponível — faz a mensagem ser rejeitada e descartada, sem voltar para a fila.

<br> 

### ♻️ Idempotência

Antes de registrar, o `NotificacaoService` verifica se o `eventId` já existe. Se existir, o evento é ignorado e nenhum e-mail é enviado de novo. A constraint `uk_notificacao_event_id` garante o mesmo no nível do banco: mesmo numa corrida entre duas entregas do mesmo evento, apenas uma é persistida.

<br> 

## ⏰ Lembrete de Consultas

O `LembreteConsultaJob` busca as consultas marcadas para **o dia seguinte** e envia um e-mail de lembrete a cada paciente.

- A busca considera apenas o **último estado de cada agendamento** — o registro mais recente de `CONSULTA_CRIADA` ou `CONSULTA_ATUALIZADA`. Uma consulta remarcada para outro dia não gera lembrete pela data antiga.
- O lembrete é gravado com o `eventId` `lembrete-<eventId do último estado>`, então rodar o job de novo **não duplica** o envio.
- O envio segue o mesmo ciclo `PENDENTE` → `ENVIADA`/`FALHA` das demais notificações.

> ℹ️ O job roda **diariamente às 8h**, com `@Scheduled(cron = "0 0 8 * * *")`, no fuso `America/Sao_Paulo` definido pela variável `TZ` dos composes.

> ℹ️ Como o serviço não lê as tabelas da AgendamentoAPI, o lembrete depende do que já está na tabela `notificacao`. Um cancelamento de consulta não gera evento para esta fila, então uma consulta cancelada ainda pode receber lembrete.

<br> 

## 🐳 Banco Compartilhado e Docker Compose

Os microsserviços da Fase 3 compartilham **um único PostgreSQL** e **um único RabbitMQ**. A infraestrutura sobe de forma independente e cria a rede `shared-net`, e cada serviço se conecta a ela como rede externa:

```
                        ┌──────────────────────────────────┐
                        │         rede: shared-net         │
                        │                                  │
   host:8745 ──────────▶│  postgres:5432                   │
   host:5672/15672 ────▶│  rabbitmq:5672  (UI 15672)       │
                        │                                  │
                        │  AgendamentoAPI   host:9027      │
                        │  HistoricoAPI     host:9028      │
                        │  NotificacaoAPI   sem porta      │
                        └──────────────────────────────────┘
```

Dentro da rede, o banco é alcançado pelo hostname **`postgres`** na porta **`5432`**, e o broker pelo hostname **`rabbitmq`** na porta **`5672`**.

O projeto disponibiliza três arquivos Compose:

| Arquivo | Finalidade |
| --- | --- |
| `docker-compose-postgres-dev.yml` | Infraestrutura de desenvolvimento, com credenciais fixas e sem `.env`: PostgreSQL, RabbitMQ e o container `notificacao-database-fase3`, que cria o banco `notificacao` caso ainda não exista. Cria a rede `shared-net`. |
| `docker-compose-postgres-prod.yml` | Infraestrutura de produção: PostgreSQL e RabbitMQ com painel de gerenciamento. Lê o `.env`, possui *healthcheck* e cria a rede `shared-net`. |
| `docker-compose-notificacaoapi.yml` | Este serviço no perfil `prod`, conectado à `shared-net` já existente. Antes da aplicação, o container `NotificacaoAPI-Database` cria o banco `notificacao` caso ainda não exista. |

> ℹ️ Os serviços `postgres` e `rabbitmq`, os volumes e a rede são **idênticos aos dos composes da AgendamentoAPI**, com nome de projeto, container e volume fixos. Tanto faz de qual repositório a infraestrutura é iniciada: os containers e os dados serão sempre os mesmos. Suba-a **uma vez**.

> ℹ️ Os containers `notificacao-database-fase3` e `NotificacaoAPI-Database` servem apenas para criar o banco. Eles aguardam o PostgreSQL responder, executam `CREATE DATABASE` quando o banco ainda não existe e seguem em execução sem fazer nada, marcados como *healthy*. No compose de produção, a aplicação só sobe depois disso. Executar o Compose novamente não recria nem apaga o banco.

> ⚠️ **Este serviço usa um banco de dados próprio.** A tabela `notificacao` é criada pelas migrations Flyway deste projeto (`V1.0__CreateTables.sql` e `V1.1__AlterEventIdLength.sql`). A AgendamentoAPI também tem migrations `V1.0` e `V1.1`, e as duas usam a tabela padrão `flyway_schema_history`. No mesmo banco, o Flyway encontraria versões com checksum diferente e interromperia a inicialização. Por isso, a aplicação usa o banco `notificacao`, separado do banco `postgres` da AgendamentoAPI, dentro do mesmo PostgreSQL.

<br> 

## 🛠️ Desenvolvimento 

Para o ambiente de desenvolvimento, utilize o `docker-compose-postgres-dev.yml` deste repositório. Ele sobe o PostgreSQL e o RabbitMQ com credenciais fixas (porta `8745` para o banco, `5672` e `15672` para o broker) e cria o banco `notificacao`:

```bash
docker compose -f docker-compose-postgres-dev.yml up -d --wait
```

Em seguida, exporte as variáveis de ambiente, com `DATABASE_NAME=notificacao`, e execute o serviço no perfil `dev`. Na primeira execução, o Flyway cria a tabela `notificacao`.

| Variável | Obrigatória | Padrão |
| --- | --- | --- |
| `DATABASE_IP`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASSWORD` | Sim | — |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | Sim | — |
| `RABBITMQ_HOST` | Não | `localhost` |
| `RABBITMQ_PORT` | Não | `5672` |
| `RABBITMQ_VHOST` | Não | `/` |
| `RABBITMQ_SSL` | Não | `false` |
| `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Sim | — |
| `MAIL_PORT` | Não | `587` |
| `MAIL_FROM` | Não | `naoresponda@hospital.fiap.br` |

> ℹ️ A conexão com o banco é montada em `DataBaseConfig` a partir das variáveis `DATABASE_*`. Ao rodar localmente, a aplicação acessa o banco em `localhost:8745`; o valor `5432` só é utilizado pelos containers, que enxergam o PostgreSQL pela rede interna do Docker.

> ℹ️ O SMTP é configurado com autenticação e STARTTLS. Para desenvolvimento, um *sandbox* de e-mail evita envios reais aos pacientes.

<br> 

## 🚀 Produção

Para execução em ambiente de produção, o projeto disponibiliza os arquivos `docker-compose-postgres-prod.yml` e `docker-compose-notificacaoapi.yml`. Antes de iniciar o serviço, é necessário configurar o arquivo `.env` na raiz do projeto:

```bash
# DATABASE_NAME (opcional)
$ Exemplo: postgres

# DATABASE_USER (opcional)
$ Exemplo: postgres

# DATABASE_PASSWORD
$ Exemplo: postgres@2026

# NOTIFICACAO_DATABASE_NAME (opcional)
$ Exemplo: notificacao

# RABBITMQ_USERNAME (opcional)
$ Exemplo: fiap

# RABBITMQ_PASSWORD
$ Exemplo: fiap@2026

# MAIL_HOST
$ Exemplo: smtp.seuprovedor.com

# MAIL_USERNAME
$ Exemplo: usuario-smtp

# MAIL_PASSWORD
$ Exemplo: senha-smtp

# MAIL_PORT (opcional)
$ Exemplo: 587

# MAIL_FROM (opcional)
$ Exemplo: naoresponda@hospital.fiap.br
```

Se `DATABASE_PASSWORD`, `RABBITMQ_PASSWORD`, `MAIL_HOST`, `MAIL_USERNAME` ou `MAIL_PASSWORD` não estiverem preenchidas, o Compose interrompe a execução com uma mensagem explícita, em vez de subir com valores em branco. As senhas do banco e do broker precisam ser as mesmas usadas na criação da infraestrutura.

> ℹ️ `DATABASE_NAME` é o banco padrão do PostgreSQL da fase: cria o container do banco e serve de conexão inicial para o container que cria o banco deste serviço. A aplicação usa `NOTIFICACAO_DATABASE_NAME` (padrão `notificacao`), então não há risco de o Flyway rodar no banco da AgendamentoAPI.

> ℹ️ Não é necessário configurar a porta do banco nem do broker: dentro da rede `shared-net` as conexões são sempre feitas em `postgres:5432` e `rabbitmq:5672`, valores já fixados no Compose.

<br> 

Após configurar o arquivo `.env`, inicie a infraestrutura e, em seguida, o serviço:

```bash
# 1. PostgreSQL e RabbitMQ, que também criam a rede shared-net (execute apenas uma vez, daqui ou da AgendamentoAPI)
docker compose -f docker-compose-postgres-prod.yml up -d --wait

# 2. NotificacaoAPI: cria o banco notificacao, aplica as migrations Flyway e começa a consumir a fila
docker compose -f docker-compose-notificacaoapi.yml up -d
```

> ℹ️ O container não publica nenhuma porta no host, pois o serviço não recebe requisições HTTP. Para acompanhar o processamento, utilize os logs ou o painel do RabbitMQ em `http://localhost:15672`.

> ℹ️ Quando o serviço é executado em produção, é criada automaticamente uma pasta chamada `logs` no diretório onde a aplicação está sendo executada. Essa pasta é responsável por armazenar todos os logs gerados pelo serviço, sendo organizados de forma diária, ou seja, a cada novo dia é gerado um arquivo específico contendo a data correspondente, facilitando a rastreabilidade e análise das execuções. Além disso, a aplicação possui uma política de limpeza automática, na qual os arquivos de `logs` são mantidos por um período de 30 dias. Após esse prazo, os `logs` mais antigos são excluídos automaticamente, garantindo melhor gerenciamento de armazenamento.

<br> 

## 🗄️ Banco de Dados

O serviço é dono apenas da tabela `notificacao`, criada pelas próprias migrations:

| Migration | Descrição |
| --- | --- |
| `V1.0__CreateTables.sql` | Cria a tabela `notificacao`, com a constraint única `uk_notificacao_event_id`. |
| `V1.1__AlterEventIdLength.sql` | Amplia `event_id` para 60 caracteres, comportando o prefixo `lembrete-` somado ao UUID do evento. |

Cada registro guarda os dados necessários para montar o e-mail — paciente, médico, especialidade e horários — o que permite ao job de lembrete trabalhar sem consultar a AgendamentoAPI.

| Status | Significado |
| --- | --- |
| `PENDENTE` | Notificação registrada, envio ainda não concluído. |
| `ENVIADA` | E-mail enviado; `data_envio` preenchida. |
| `FALHA` | Todas as tentativas falharam; `mensagem_erro` preenchida. |

<br> 

## 🧪 Testes

A suíte é composta principalmente por **testes de integração reais**: eles sobem o contexto do Spring e se conectam a um PostgreSQL de verdade, dentro de transações revertidas ao final de cada caso. Por isso, **o banco precisa estar no ar antes de executar os testes**, assim como o RabbitMQ:

```bash
./gradlew test
```

Os testes utilizam o perfil `test`, que carrega a configuração de banco de `TestDataBaseConfig`. Os valores padrão apontam para o PostgreSQL da fase (`localhost:8745`, banco `postgres`), os mesmos usados pelo CI. Contra o banco compartilhado, exporte `DATABASE_NAME=notificacao` antes de rodar a suíte, para que o Flyway não esbarre nas migrations da AgendamentoAPI. O RabbitMQ e o SMTP também possuem valores padrão no perfil `test`.

O único mock nos testes de integração é o `NotificacaoSender`, para não depender de um servidor SMTP real. O `EmailNotificacaoSenderTest` é um teste unitário, com o `JavaMailSender` mockado.

<br> 

### 📊 Cobertura

A suíte conta atualmente com **16 testes distribuídos em 4 classes**:

| Classe | O que cobre |
| --- | --- |
| `NotificacaoServiceTest` | Envio com sucesso marca `ENVIADA`; falha no envio preserva o registro com `FALHA`; evento duplicado não gera segundo envio; evento de atualização gera `CONSULTA_ATUALIZADA`; lembrete duplicado não gera novo registro. |
| `LembreteConsultaJobTest` | Lembrete enviado para consulta de amanhã; job rodando duas vezes não duplica; apenas o último estado do agendamento é considerado; consulta em outra data não gera lembrete; falha no envio grava `FALHA`. |
| `EmailNotificacaoSenderTest` | Assunto, destinatário e corpo corretos para cada um dos três tipos de notificação. |
| `ContratoEventosTest` | Eventos de consulta criada e atualizada dentro do contrato são convertidos para os records. Evento com campo fora do contrato é recusado pelo conversor. |

Ao final da execução, o **JaCoCo** gera o relatório completo em:

```bash
build/reports/jacoco/test/html/index.html
```

> ⚠️ O `./gradlew build` executa também a verificação de cobertura (`jacocoTestCoverageVerification`), que exige **no mínimo 80%**. Abaixo disso, o build falha.

> ℹ️ Os pacotes `config`, `enums`, `exceptions` e `model`, além da classe `NotificacaoAPIApplication`, são intencionalmente excluídos do cálculo de cobertura, por serem estruturais e não conterem regra de negócio.

<br> 

## ⚠️ Observação

O serviço depende da AgendamentoAPI para receber eventos: sem ela publicando na exchange `agendamento.events`, nenhuma notificação de agendamento ou remarcação é gerada, e o job de lembrete não encontra consultas para processar.
