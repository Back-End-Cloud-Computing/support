# PRD — Serviço de Suporte

## Problema

O enunciado do professor exige um microsserviço de suporte para e-mail, SMS e WhatsApp, e nenhum dos cinco
serviços de domínio o cobre. O `order` precisa avisar o cliente quando um pedido é criado, e não deve
conhecer SMTP nem provedores de mensagem.

## Objetivo

Receber um evento de negócio, decidir os canais pelos dados disponíveis e enviar a mensagem, sem duplicar
envio se o evento chegar duas vezes, e registrar o resultado de cada canal.

## Escopo

- Evento `OrderCreated` (payload do `order/docs/INTEGRATION-CONTRACTS.md` §8, mais `customer_phone` opcional).
- Canal e-mail **real** por SMTP (Mailpit no cluster, caixa visível na demo).
- Canais SMS e WhatsApp **simulados** (registram em log e contam na métrica), com interface pronta para
  receber APIs reais; o provedor é escolhido por variável de ambiente.
- Idempotência por `event_id` e registro de entregas em SQLite.
- `/health`, `/health/ready` e `/metrics`.
- Imagem Docker (`eduardofabri/support`), manifests K8s (com Mailpit e volume), chart Helm, painel Grafana.

## Fora de escopo

- SMS e WhatsApp reais (planejado em [canais-reais.md](canais-reais.md)).
- Consumidor RabbitMQ (planejado em [rabbitmq.md](rabbitmq.md); entra quando os demais repos terminarem).
- Outros eventos além de `OrderCreated` (mudança de status do pedido, carrinho abandonado).
- Consultar o serviço `client` ou o `authorization`: tudo vem no payload.
- Ingress público e passagem pelo API Gateway.
- Preferências de notificação do usuário, templates editáveis, fila de reenvio persistente.

## Critérios de aceite

| # | Critério | Como provar |
|---|---|---|
| 1 | Evento válido retorna 202 e envia e-mail | `curl` e mensagem na UI do Mailpit |
| 2 | Sem `x-internal-secret` válido retorna 403 | teste e `curl` |
| 3 | Mesmo `event_id` duas vezes não envia segundo e-mail | teste e Mailpit |
| 4 | Payload inválido retorna 400 com o campo problemático | testes de validação |
| 5 | Sem `customer_phone`, SMS e WhatsApp ficam `skipped` | teste |
| 6 | Trocar `SMS_PROVIDER` muda o adapter sem alterar código | teste de seleção |
| 7 | Falha do SMTP não derruba o serviço nem perde o registro | teste com SMTP falso fora do ar |
| 8 | `/metrics` expõe `support_notifications_total{channel,status}` | `curl` e painel no Grafana |
| 9 | Sobe no minikube com Mailpit e volume | `kubectl get pods` |
| 10 | Instala por Helm | `helm install` (após 27/10) |

## Riscos

- SQLite limita a 1 réplica ([ADR 0003](adr/0003-sqlite.md)).
- Enquanto o transporte é HTTP, se o suporte estiver fora o `order` precisa decidir o que fazer (o contrato diz: não falhar o pedido).
- O `order` ainda precisa ser alterado por Rodrigo para enviar o evento ([SUP-001](integration-issues.md)).
