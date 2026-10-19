# Integração — Serviço de Suporte

Atualizado em 2026-10-19. IDs `SUP-*` são locais deste repo; `INT-*` vêm do `order`.
Nos repos dos colegas só se abre issue; nada é editado neles.

## Resumo

| ID | Tema | Serviços | Situação | Bloqueia agora? |
|---|---|---|---|---|
| SUP-001 | `order` chamar `POST /internal/notifications` ao criar o pedido, com `customer_email` e `customer_phone` | `order`, `client` | Issue aberta no `order`; Rodrigo implementa | Só a fase 5 |
| SUP-002 | Segredo interno compartilhado entre `order` e `support` | `order` | Cada lado guarda o valor em Secret próprio; combinar fora do repo | Só a fase 5 |
| SUP-003 | Telefone do cliente | `client`, `order` | Decidido: o `order` lê `GET /clients/me` com o token do usuário na criação do pedido e manda no payload ([ADR 0005](adr/0005-destinatario-no-payload.md)) | Não |
| SUP-004 | RabbitMQ não existe em nenhum repo | todos | Planejado em [rabbitmq.md](rabbitmq.md); HTTP primeiro | Não |
| SUP-005 | SMS e WhatsApp reais | provedor externo | Planejado em [canais-reais.md](canais-reais.md) | Não |
| SUP-006 | Suporte fora do API Gateway | `api-gateway` | Decidido: o `order` chama `support:8090` direto | Não |
| SUP-007 | SQLite limita a 1 réplica | este repo | Aceito no [ADR 0003](adr/0003-sqlite.md) | Não |
| SUP-008 | Stack provisória | Eduardo | Go montado pelo Lucas; Eduardo confirma ou troca | Não |

## Detalhes

### SUP-001 — O que o `order` precisa fazer

- Ao criar o pedido (`POST /api/pedidos`), montar o `OrderCreated` do [contrato](contrato-api.md) e chamar
  `http://support:8090/internal/notifications` com `x-internal-secret`.
- `customer_email` vem do claim `email` do JWT; `customer_phone` de `GET /clients/me` (opcional: sem telefone, só e-mail).
- Falha do suporte **não** pode falhar o pedido (log e segue).
- `event_id` deve ser estável para o mesmo pedido (UUID gravado com o pedido), para o reenvio ser idempotente.

### SUP-001 — Checklist de ponta a ponta

- [ ] Pedido criado no `order` gera um e-mail na UI do Mailpit.
- [ ] Reenviar o mesmo `event_id` não gera segundo e-mail.
- [ ] Suporte desligado: o pedido ainda é criado.

### SUP-008 — Se o Eduardo trocar a stack

Manter: porta 8090, rotas, payload, códigos de status, nomes de métricas e variáveis de ambiente do
[arquitetura.md](arquitetura.md#configuração). O resto (Dockerfile, CI, testes) acompanha a nova stack.

## Pedidos por repo (issues abertas por Lucas)

| Repo | Pedido |
|---|---|
| `order` | SUP-001 e SUP-002 |
