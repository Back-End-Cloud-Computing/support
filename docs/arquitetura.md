# Arquitetura — Serviço de Suporte

## Posição no sistema

```
order ─POST /internal/notifications─► support:8090 ─► SMTP (mailpit:1025)
        x-internal-secret                  │
                                           ├─► adapter SMS       (fake | provedor real)
                                           ├─► adapter WhatsApp  (fake | provedor real)
                                           └─► SQLite (/data/support.db): events + deliveries
```

Sem Ingress e fora do API Gateway: só o `order`, de dentro do cluster, chama o Service `support:8090`.
Futuro: o mesmo tratamento chega por RabbitMQ ([rabbitmq.md](rabbitmq.md)).

## Fluxo de um evento

1. Valida `x-internal-secret` (comparação em tempo constante). Falhou: 403.
2. Valida o payload (campos obrigatórios, e-mail, `event_type` suportado, telefone em formato E.164 se vier). Falhou: 400.
3. Tenta inserir `event_id` em `events` (chave primária). Já existe: responde 202 com `duplicate: true`, sem enviar nada.
4. Responde **202** e processa em goroutine, para o `order` não esperar o SMTP.
5. Para cada canal aplicável, monta a mensagem, envia com até 3 tentativas (espera 1 s, 2 s, 4 s) e grava em `deliveries`
   `sent`, `failed` (com erro) ou `skipped` (sem dado para o canal).
6. Atualiza as métricas e o log JSON (com `correlation_id`).

Regras de canal:

| Canal | Enviado quando |
|---|---|
| E-mail | `customer_email` presente |
| SMS | `customer_phone` presente |
| WhatsApp | `customer_phone` presente |

## Modelo de dados (SQLite)

```sql
CREATE TABLE events (
  event_id     TEXT PRIMARY KEY,
  event_type   TEXT NOT NULL,
  order_id     TEXT NOT NULL,
  received_at  TEXT NOT NULL,
  payload      TEXT NOT NULL
);
CREATE TABLE deliveries (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  event_id     TEXT NOT NULL REFERENCES events(event_id),
  channel      TEXT NOT NULL,      -- email | sms | whatsapp
  status       TEXT NOT NULL,      -- sent | failed | skipped
  attempts     INTEGER NOT NULL DEFAULT 0,
  error        TEXT,
  provider     TEXT NOT NULL,      -- smtp | fake | nome do provedor real
  updated_at   TEXT NOT NULL,
  UNIQUE (event_id, channel)
);
```

Arquivo em volume (`/data`); no K8s, PVC de 100 Mi. Migrations simples executadas no boot (`CREATE TABLE IF NOT EXISTS`).

## Canais e adapters

```go
type Notifier interface {
    Channel() string                               // "email" | "sms" | "whatsapp"
    Provider() string                              // "smtp" | "fake" | ...
    Send(ctx context.Context, msg Message) error
}
```

`Message` carrega destinatário, assunto (e-mail) e texto. A escolha vem do `.env`:
`EMAIL_PROVIDER=smtp|fake`, `SMS_PROVIDER=fake`, `WHATSAPP_PROVIDER=fake`. Provedor desconhecido faz o serviço
recusar subir (falha rápida, mensagem clara). Plano de provedores reais em [canais-reais.md](canais-reais.md).

Textos (pt-BR), para `OrderCreated`:

- E-mail: assunto `Pedido {order_code} confirmado`; corpo com código, total formatado (`R$ 1.234,56`) e data.
- SMS: `GANJJ: pedido {order_code} confirmado. Total R$ 1.234,56.` (até 160 caracteres).
- WhatsApp: mesmo texto do SMS.

## Configuração

| Variável | Padrão | Uso |
|---|---|---|
| `PORT` | `8090` | Porta HTTP |
| `INTERNAL_SECRET` | obrigatória | Valor esperado em `x-internal-secret` (Secret no K8s) |
| `DB_PATH` | `/data/support.db` | Arquivo SQLite |
| `EMAIL_PROVIDER` | `smtp` | `smtp` ou `fake` |
| `SMTP_HOST` / `SMTP_PORT` | `mailpit` / `1025` | Servidor SMTP |
| `SMTP_FROM` | `no-reply@ganjj.local` | Remetente |
| `SMS_PROVIDER` | `fake` | `fake` agora; provedor real depois |
| `WHATSAPP_PROVIDER` | `fake` | `fake` agora; provedor real depois |
| `LOG_LEVEL` | `info` | Nível do log |

## Métricas (Prometheus, em `/metrics` na mesma porta; o Service é interno)

| Métrica | Tipo | Rótulos |
|---|---|---|
| `support_events_total` | counter | `result` (`accepted`, `duplicate`, `invalid`) |
| `support_notifications_total` | counter | `channel`, `status` (`sent`, `failed`, `skipped`) |
| `support_send_duration_seconds` | histogram | `channel` |

Painel do Grafana: eventos por resultado, envios por canal e status, p95 de envio. Exportado em JSON em
`k8s/observability/`.

## Estrutura de código (provisória)

```
cmd/support/main.go        # monta config, banco, adapters, servidor
internal/config/           # lê e valida o ambiente
internal/httpapi/          # handlers, middleware de segredo, métricas
internal/notify/           # Notifier, Message, email.go, sms_fake.go, whatsapp_fake.go, dispatcher.go
internal/store/            # SQLite: events, deliveries
```
