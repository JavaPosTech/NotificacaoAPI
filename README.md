<div align="center"> <br>
  <img align="center" alt="guru-java" height="150" width="150" src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/spring/spring-original.svg" />
</div>

<br>

<div align="center">
    Turma 12ADJT – Projeto desenvolvido na pós-graduação em Arquitetura e Desenvolvimento em Java da FIAP. Serviço responsável por consumir os eventos de agendamento publicados pela AgendamentoAPI e notificar os pacientes por e-mail.
</div>

<br> <br>

## 🧰 Ferramentas Utilizadas

* ☕️ Java 21
* 🦅 Flyway
* 📝 Log4j2
* 🐘 PostgreSQL 18
* 🐇 RabbitMQ (Spring AMQP)
* 📧 Spring Mail + Spring Retry
* 🧪 JUnit 5 + Mockito + JaCoCo
* 🟢 Spring Boot 4.0.5
* 🔄 MapStruct + Lombok
* 🛠️ Gradle 9.7 (Kotlin DSL)
* 🐳 Docker / Docker Compose

<br>

## 📁 Estrutura do Projeto

O código segue o mesmo padrão da AgendamentoAPI — organizado **por camada e, dentro de cada camada, por domínio**:

```
src/main/java/br/com/fiap/notificacaoapi/
├── config/           # DataBaseConfig, RabbitMQConfig
├── enums/            # StatusNotificacao, TipoNotificacao
├── exceptions/       # NotificacaoNaoEncontradaException
├── model/
│   ├── entity/       # Notificacao (entidade JPA, sem regra de negócio)
│   ├── rabbitmq/      # Contrato dos eventos (idêntico ao publicado pela AgendamentoAPI)
│   └── mapper/        # NotificacaoMapper (MapStruct)
├── repository/        # NotificacaoRepository
└── service/
    ├── rabbitmq/       # AgendamentoEventListener (consumidor da fila)
    └── notificacao/    # NotificacaoService, NotificacaoSender (porta) e EmailNotificacaoSender (adapter)
```

> ℹ️ Assim como na AgendamentoAPI, **não há Javadoc nem comentários explicativos** no código, no `build.gradle.kts`, no Compose ou no Dockerfile. O contexto de arquitetura vive neste `README.md`.

<br>

## 🎯 Por que a entidade `Notificacao` não tem regra de negócio

Isso resolve diretamente o problema apontado na primeira versão do serviço: a entidade `Notification` acumulava, ao mesmo tempo, mapeamento JPA e regra de negócio (`markSent`, `markFailed`).

Aqui a `Notificacao` é uma entidade anêmica — só `@Getter`/`@Setter`, igual a `Agendamento` e `Paciente` na AgendamentoAPI. Toda decisão de negócio (idempotência, transição de status, tratamento de falha) vive no `NotificacaoService`, e o envio em si fica atrás de uma interface (`NotificacaoSender`), com `EmailNotificacaoSender` como única implementação hoje. Isso permite adicionar SMS ou push no futuro criando um novo adapter, sem tocar no `NotificacaoService` (Open/Closed).

<br>

## 🔄 Contrato de Eventos (RabbitMQ)

A `AgendamentoAPI` publica na exchange `agendamento.events` (direct) com duas routing keys, ambas roteadas para a mesma fila que este serviço consome:

| Routing key | Type ID | Classe |
| --- | --- | --- |
| `agendamento.criado` | `agendamento.criado.v1` | `AgendamentoCriadoEvent` |
| `agendamento.atualizado` | `agendamento.atualizado.v1` | `AgendamentoAtualizadoEvent` |

Fila consumida: **`notificacao.email.agendamento`**.

`RabbitMQConfig` aqui **replica exatamente** a exchange, a fila e os bindings declarados na AgendamentoAPI (mesmos nomes, mesmos argumentos) e registra um `DefaultClassMapper` com os mesmos Type IDs, apontando para as classes de evento locais. Isso é obrigatório: o `JacksonJsonMessageConverter` da AgendamentoAPI grava o Type ID (não o nome completo da classe) no header `__TypeId__` da mensagem — sem esse mesmo mapeamento aqui, a desserialização falha.

> ⚠️ **Por que não há Dead Letter Queue aqui.** O RabbitMQ exige que uma fila já existente seja redeclarada com argumentos **idênticos** — do contrário, o broker recusa a conexão com `406 PRECONDITION_FAILED`. Como a fila `notificacao.email.agendamento` já é declarada (sem DLQ) pela AgendamentoAPI, adicionar `x-dead-letter-exchange` somente aqui quebraria a integração sempre que um dos dois serviços subir primeiro. Se quiserem DLQ no futuro, os argumentos da fila precisam ser alterados **nos dois projetos ao mesmo tempo**.

Na ausência de DLQ, a resiliência foi resolvida em duas camadas:

1. **Retry local**: `EmailNotificacaoSender.enviar(...)` tem `@Retryable` (3 tentativas, backoff de 2s dobrando) para absorver falhas transitórias de SMTP sem tocar na fila.
2. **Persistência como fonte da verdade**: se todas as tentativas falharem, o `AgendamentoEventListener` captura a exceção, registra o status `FALHA` com a mensagem de erro no banco, e a mensagem é confirmada (ack) normalmente — evitando um loop de redelivery infinito por uma mensagem "envenenada". A tabela `notificacao` fica como auditoria completa de todo evento recebido e seu desfecho.

<br>

## 🐛 O bug corrigido em relação à primeira versão

Na primeira versão, um único método `@Transactional` fazia: salvar `PENDING` → tentar enviar e-mail → salvar `FAILED` → `throw ex`. Como a exceção propagava para fora do método transacional, o Spring desfazia **a transação inteira**, apagando também o registro de falha que acabara de ser salvo — ou seja, um envio malsucedido não deixava rastro nenhum no banco.

Aqui a responsabilidade foi dividida em três métodos independentes do `NotificacaoService` (`registrarPendente`, `marcarComoEnviada`, `marcarComoFalha`), cada um com sua própria transação, chamados pelo `AgendamentoEventListener` — que não é transacional. Como a tentativa de envio acontece **fora** de qualquer transação de banco, uma falha no e-mail nunca desfaz o registro que já foi commitado. O teste `falhaNoEnvioNaoPerdeORegistroDaNotificacaoTest` comprova exatamente isso.

<br>

## 🗄️ Banco de Dados

Reaproveita o **mesmo PostgreSQL compartilhado da Fase 3** (a `AgendamentoAPI` continua sendo a dona do schema principal), mas a tabela `notificacao` é criada por uma migration própria deste serviço — é um dado que só interessa a ele:

```sql
V1.0__CreateTables.sql -- cria a tabela notificacao com constraint única em event_id
```

A constraint `uk_notificacao_event_id` é a garantia de idempotência a nível de banco: mesmo numa corrida entre duas entregas do mesmo evento, apenas uma é persistida.

Assim como na AgendamentoAPI, o `DataSource` é montado manualmente em `DataBaseConfig` a partir de `DATABASE_IP`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER` e `DATABASE_PASSWORD`.

<br>

## 🐳 Docker Compose

| Arquivo | Finalidade |
| --- | --- |
| `docker-compose-notificacaoapi.yml` | Apenas este serviço, perfil `prod`, conectando-se à `shared-net` já existente. |
| `docker-compose-rabbitmq-prod.yml` | RabbitMQ de produção (management UI incluída), lê o `.env`, cria a `shared-net`. **Não existia no repositório da AgendamentoAPI** — foi adicionado aqui para fechar o ambiente de produção, já que o `docker-compose-postgres-prod.yml` cobre só o banco. |

> ⚠️ **Ponto de atenção no `docker-compose-agendamentoapi.yml` original**: o bloco `environment` não define `RABBITMQ_HOST`/`RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD`. Sem esses valores, o container sobe com `RABBITMQ_HOST=localhost` (não enxerga o RabbitMQ da rede Docker) e falha ao resolver `RABBITMQ_USERNAME`/`RABBITMQ_PASSWORD` (sem valor default). Vale replicar neste arquivo o mesmo bloco usado aqui no `docker-compose-notificacaoapi.yml` para os dois serviços conversarem em produção.

Para desenvolvimento local, o RabbitMQ já está no `docker-compose-postgres-dev.yml` da AgendamentoAPI (usuário `fiap` / senha `fiap@2026`, portas `5672` e `15672`) — não é necessário nenhum compose adicional em dev, os dois serviços apontam para a mesma instância.

<br>

## 🛠️ Comandos

```bash
# Build completo (compila + roda testes + gera relatório JaCoCo)
./gradlew build

# Build sem testes
./gradlew clean build -x test

# Somente os testes (exige PostgreSQL e RabbitMQ no ar)
./gradlew test

# Executar em desenvolvimento
./gradlew bootRun --args="--spring.profiles.active=dev"
```

> ℹ️ No Windows/PowerShell use `.\gradlew.bat` no lugar de `./gradlew`.

<br>

## 🧪 Testes

Segue a mesma filosofia da AgendamentoAPI: **testes de integração de verdade**, sem Testcontainers e sem mocks de infraestrutura — suba o Postgres e o RabbitMQ (`docker-compose-postgres-dev.yml`) antes de rodar `./gradlew test`. O único mock é o `NotificacaoSender` (a própria interface criada para isso), para não depender de um servidor SMTP real nos testes.

`NotificacaoServiceTest` cobre os três comportamentos centrais:

* envio com sucesso marca `ENVIADA` e preenche `dataEnvio`;
* falha no envio preserva o registro com status `FALHA` e a mensagem de erro (regressão do bug corrigido);
* o mesmo `eventId` processado duas vezes não gera um segundo envio.

Para chegar ao padrão de cobertura da AgendamentoAPI (100% em `service` e `repository`), vale acrescentar casos de borda adicionais seguindo o mesmo `AbstractTest`.
