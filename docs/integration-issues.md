# Integração — Serviço de Suporte

Atualizado em 2026-10-28. IDs `SUP-*` são locais deste repo; `INT-*` vêm do `order`.
Alterações interserviços são entregues por PR, com contrato, CI e evidência. O publisher de order foi incluído no PR #22 a pedido de Eduardo para concluir a integração.

## Resumo

| ID | Tema | Serviços | Situação | Bloqueia agora? |
|---|---|---|---|---|
| SUP-001 | `order` chamar `POST /internal/notifications` ao criar o pedido, com `customer_email` e `customer_phone` | `order`, `client` | HTTP e AMQP validados com order real; publisher no PR #22 | Só a fase 5 |
| SUP-002 | Segredo interno compartilhado entre `order` e `support` | `order` | Secrets iguais configurados fora do Git e comprovados no laboratório | Só a fase 5 |
| SUP-003 | Telefone do cliente | `client`, `order` | Planejado: ler `GET /clients/me`; implementação atual do order omite o telefone opcional ([ADR 0005](adr/0005-destinatario-no-payload.md)) | Não |
| SUP-004 | RabbitMQ com publisher e consumidor reais | todos | Outbox order.created → support testado; HTTP mantido como fallback | Não |
| SUP-005 | SMS e WhatsApp reais | provedor externo | Planejado em [canais-reais.md](canais-reais.md) | Não |
| SUP-006 | Suporte fora do API Gateway | `api-gateway` | Decidido: o `order` chama `support:8090` direto | Não |
| SUP-007 | SQLite limita a 1 réplica | este repo | Aceito no [ADR 0003](adr/0003-sqlite.md) | Não |
| SUP-008 | Stack provisória | Eduardo | Java 21/Spring Boot confirmado por Eduardo em 24/10 (ADR 0006) | Não |

## Detalhes

### SUP-001 — O que o `order` precisa fazer

- Ao criar o pedido (`POST /api/pedidos`), montar o `OrderCreated` do [contrato](contrato-api.md) e chamar
  `http://support:8090/internal/notifications` com `x-internal-secret`.
- `customer_email` vem do claim `email` do JWT; `customer_phone` de `GET /clients/me` (opcional: sem telefone, só e-mail).
- Falha do suporte **não** pode falhar o pedido (log e segue).
- `event_id` deve ser estável para o mesmo pedido (UUID gravado com o pedido), para o reenvio ser idempotente.

### SUP-001 — Checklist de ponta a ponta

- [x] Pedido criado no `order` gera um e-mail na UI do Mailpit.
- [x] Reenviar o mesmo `event_id` não gera segundo e-mail.
- [x] Suporte desligado: o pedido ainda é criado.

### SUP-008 — Se o Eduardo trocar a stack

Manter: porta 8090, rotas, payload, códigos de status, nomes de métricas e variáveis de ambiente do
[arquitetura.md](arquitetura.md#configuração). O resto (Dockerfile, CI, testes) acompanha a nova stack.

## Pedidos por repo (issues abertas por Lucas)

| Repo | Pedido |
|---|---|
| `order` | SUP-001 e SUP-002 |

Evidência: [aceite executado](evidence/2026-10-25/acceptance-audit.json) e [checkout real](evidence/2026-10-25/order-support.json). Cart/Product são fixtures explícitas; order e SQL Server são reais. O reenvio do mesmo evento foi contado no Mailpit (um e-mail); o pedido com suporte desligado retornou 201 e a réplica foi restaurada.

O contrato HTTP/AMQP foi espelhado byte a byte em `docs/contrato-api.md` dos dois repos no PR order #22. Secrets do order/support foram configurados privadamente com o mesmo valor e comparados sem exibir conteúdo. Evidência de 28/10: [order-created.json](evidence/2026-10-28/order-created.json).
