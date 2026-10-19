# ADR 0005 — Destinatário vem no payload do evento

Status: Aceito
Data: 2026-10-19

## Contexto

O serviço `client` guarda `nome`, `telefone` e `endereco`, mas não o e-mail (credenciais ficam no `authorization`).
`GET /clients/me` devolve o telefone só do próprio usuário; `GET /clients/{id}` é só admin; não existe rota serviço a serviço.
O e-mail está no claim `email` do JWT. O `OrderCreated` do `order` já carrega `customer_email`.

## Decisão

O `order` envia `customer_email` e `customer_phone` (opcional) no evento. O suporte não chama `client` nem `authorization`.
O telefone o `order` obtém de `GET /clients/me` com o **token do usuário que cria o pedido**, quando o pedido é criado
(momento em que o token existe; o suporte, assíncrono, não o teria).

## Consequências

- Zero acoplamento do suporte com outros serviços; teste local só com `curl`.
- O `order` ganha uma chamada e um campo novo ([SUP-001](../integration-issues.md)); sem telefone, o aviso sai só por e-mail.
- Dado pessoal trafega no evento: tráfego interno ao cluster, protegido por `x-internal-secret`; telefone mascarado nos logs do suporte.
- Alternativas descartadas: rota nova no `client` (mais um repo do Eduardo e do Rodrigo mudando) e telefone no JWT (token carrega menos o que muda).
