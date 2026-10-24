# Serviço de Suporte (GANJJ)

Microsserviço de notificações: recebe um evento de negócio (hoje, pedido criado) e avisa o cliente por
e-mail, SMS e WhatsApp. É o "microsserviço de suporte" exigido pelo enunciado do professor.

- **Dono:** Eduardo (`eduardofabrii`)
- **Stack:** Java 21 + Spring Boot + SQLite, escolhida por Eduardo ([ADR 0006](docs/adr/0006-java-spring-boot.md)).
- **Porta:** 8090 · **Sem Ingress** (só Service interno, chamado direto pelo `order`)
- **Prazo:** apresentação N2 em 10/11/2026

## Como funciona

```
order ──POST /internal/notifications──► support ──► e-mail (SMTP, Mailpit no cluster)   real
 (x-internal-secret)                        │──────► SMS                                  simulado, trocável por .env
                                            └──────► WhatsApp                             simulado, trocável por .env
```

Hoje o transporte é HTTP interno, para testar e validar. RabbitMQ está planejado
([docs/rabbitmq.md](docs/rabbitmq.md)) e entra quando os demais repos terminarem as suas implementações.

## Documentação

Comece por [`docs/README.md`](docs/README.md).

| Documento | O que responde |
|---|---|
| [PRD](docs/prd.md) | Problema, escopo, fora de escopo, critérios de aceite |
| [Arquitetura](docs/arquitetura.md) | Fluxo, modelo de dados, canais, configuração, métricas |
| [Contrato](docs/contrato-api.md) | Endpoint e payload que o `order` precisa respeitar |
| [Testes](docs/testes.md) | Casos definidos antes do código (TDD) |
| [Canais reais](docs/canais-reais.md) | Como trocar SMS e WhatsApp simulados por APIs reais |
| [RabbitMQ](docs/rabbitmq.md) | Plano da fila e do consumidor |
| [Fases](docs/fases.md) | Entregas com datas |
| [Integração](docs/integration-issues.md) | O que depende dos outros serviços |
| [Roteiro N2](docs/apresentacao-n2.md) | O que mostrar na apresentação |

## Rodar localmente

```bash
cp .env.example .env
./mvnw verify
./mvnw spring-boot:run     # http://localhost:8090
```

Mailpit (caixa de e-mail de teste): `docker run -p 8025:8025 -p 1025:1025 axllent/mailpit`, UI em `http://localhost:8025`.

## Convenções

Commits em Conventional Commits, PR por mudança, autor único. Ver [`docs/convencoes.md`](docs/convencoes.md).
Depois de clonar: `lefthook install`.

## Entrega e operação

`./mvnw verify` executa build, análise do compilador e testes JUnit. Copie `.env.example` para `.env`; Spring importa esse arquivo local. Não versione `.env`. Para persistência local, use `DB_PATH=./data/support.db`.

O worker consulta eventos persistidos a cada 250 ms e grava o resultado por canal. Notificações aceitas antes de um reinício continuam pendentes no SQLite. Consulte os limites de entrega no ADR 0006.

Veja [Kubernetes](k8s/README.md), [Helm](helm/support/README.md), [dashboard](k8s/observability/README.md) e [estado N2](docs/n2-status.md). O workflow CI publica no GHCR ao entrar em `main`; a release manual Docker Hub requer secrets de Actions com acesso a `bascratki/support`.

Com o serviço e o Mailpit no ar, execute `INTERNAL_SECRET=... python3 scripts/smoke-test.py`. `SUPPORT_URL` e `MAILPIT_URL` permitem usar portas diferentes. O teste usa destinatário fictício e deve apontar para o Mailpit de teste.
