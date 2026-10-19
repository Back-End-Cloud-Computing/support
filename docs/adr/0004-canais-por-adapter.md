# ADR 0004 — Canais por adapter, escolhidos por variável de ambiente

Status: Aceito
Data: 2026-10-19

## Contexto

O enunciado pede e-mail, SMS e WhatsApp. SMS e WhatsApp reais exigem conta paga ou sandbox e credenciais, e a
N2 avalia arquitetura, segurança e observabilidade, não a integração com provedores.

## Decisão

Cada canal é um `Notifier` ([arquitetura](../arquitetura.md#canais-e-adapters)). E-mail é real (SMTP, Mailpit no cluster).
SMS e WhatsApp têm adapter `fake`, que registra a mensagem em log e conta a métrica como qualquer envio. O provedor
vem de `EMAIL_PROVIDER`, `SMS_PROVIDER`, `WHATSAPP_PROVIDER`; nome desconhecido impede o boot.

## Consequências

- Demo mostra e-mail de verdade e os demais pelo log e pela API de status, com o campo `provider: fake` visível (sem fingir que enviou).
- Ligar um provedor real é criar um adapter e trocar a variável ([canais-reais.md](../canais-reais.md)).
- CI e desenvolvimento não gastam crédito nem precisam de segredo.
