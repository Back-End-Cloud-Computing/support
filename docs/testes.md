# Testes — Serviço de Suporte

Teste primeiro: estes casos existem antes do código. Ferramenta provisória: `go test` com testes de tabela,
`httptest` para a API, SQLite em arquivo temporário (`t.TempDir()`), SMTP falso (servidor TCP local mínimo ou
interface `Notifier` com dublê) e adapters fake que registram chamadas. Sem rede externa.

## Casos

### API (`internal/httpapi`)

| # | Dado | Então |
|---|---|---|
| H1 | `POST /internal/notifications` válido com segredo | 202, `duplicate:false` |
| H2 | Sem `x-internal-secret` | 403, nada gravado |
| H3 | Segredo errado | 403 |
| H4 | Mesmo `event_id` duas vezes | segunda 202 com `duplicate:true`; um único envio por canal |
| H5 | `customer_email` inválido | 400 `campo inválido: customer_email` |
| H6 | Falta `event_id` | 400 |
| H7 | `event_type` diferente de `OrderCreated` | 400 |
| H8 | `customer_phone` fora de E.164 | 400 |
| H9 | `total` negativo | 400 |
| H10 | `GET /internal/notifications/{id}` de evento existente | lista as entregas |
| H11 | `GET /internal/notifications/{id}` desconhecido | 404 |
| H12 | `GET /health` e `GET /health/ready` | 200; `ready` com banco indisponível: 503 |

### Despacho (`internal/notify`)

| # | Dado | Então |
|---|---|---|
| D1 | Evento com e-mail e telefone | três entregas `sent` (e-mail smtp, sms fake, whatsapp fake) |
| D2 | Evento sem `customer_phone` | e-mail `sent`; sms e whatsapp `skipped` |
| D3 | SMTP falha nas 3 tentativas | e-mail `failed` com `attempts=3` e erro; SMS e WhatsApp seguem; serviço continua de pé |
| D4 | SMTP falha na 1ª e funciona na 2ª | `sent` com `attempts=2` |
| D5 | Texto do e-mail | assunto `Pedido GANJJ-000007 confirmado`; total `R$ 1.234,56` |
| D6 | Texto do SMS | até 160 caracteres, mesmo com código de pedido longo |

### Configuração (`internal/config`)

| # | Dado | Então |
|---|---|---|
| C1 | `INTERNAL_SECRET` ausente | erro ao iniciar |
| C2 | `SMS_PROVIDER=desconhecido` | erro ao iniciar, nomeando a variável |
| C3 | `SMS_PROVIDER=fake` | adapter fake selecionado |

### Armazenamento (`internal/store`)

| # | Dado | Então |
|---|---|---|
| S1 | Inserir `event_id` repetido | retorna "já existe", sem erro de SQL |
| S2 | Reabrir o arquivo | eventos e entregas continuam lá |

### Métricas

| # | Dado | Então |
|---|---|---|
| M1 | Evento aceito, duplicado e inválido | `support_events_total{result}` incrementa cada um |
| M2 | Envio `sent`, `failed` e `skipped` | `support_notifications_total{channel,status}` |

## Ponta a ponta (manual)

Com Mailpit rodando: `curl` do exemplo do [contrato](contrato-api.md), e a mensagem aparece em `http://localhost:8025`.
No cluster: mesma chamada com `kubectl port-forward svc/support 8090`. Checklist em [integration-issues.md](integration-issues.md) (SUP-001).

## Como rodar

`go test ./...` localmente e no CI. Cobertura não é meta; os casos acima são.
