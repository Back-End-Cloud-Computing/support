# Serviço de Suporte (GANJJ)

Microsserviço de notificações: recebe um evento de negócio (hoje, pedido criado) e avisa o cliente por
e-mail, SMS e WhatsApp. É o "microsserviço de suporte" exigido pelo enunciado do professor.

- **Dono:** Eduardo (`eduardofabrii`)
- **Stack provisória:** Go + SQLite. O Lucas montou a estrutura inicial; o Eduardo pode trocar a stack
  (o professor aceita qualquer uma), desde que o [contrato](docs/contrato-api.md) continue o mesmo.
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
go test ./...
go run ./cmd/support     # http://localhost:8090
```

Mailpit (caixa de e-mail de teste): `docker run -p 8025:8025 -p 1025:1025 axllent/mailpit`, UI em `http://localhost:8025`.

## Convenções

Commits em Conventional Commits, PR por mudança, autor único. Ver [`docs/convencoes.md`](docs/convencoes.md).
Depois de clonar: `lefthook install`.
