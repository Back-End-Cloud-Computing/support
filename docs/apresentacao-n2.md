# Roteiro N2 — Serviço de Suporte (10/11/2026)

Rubrica: R1 arquitetura distribuída em Kubernetes (4,0) · R2 segurança, distribuição e observabilidade (3,0) ·
R3 apresentação técnica (3,0). Este roteiro cobre a parte do suporte; a ordem final depende do grupo.

## Demonstração

| # | O quê | Como mostrar | Rubrica |
|---|---|---|---|
| 1 | Serviço e Mailpit no cluster | `kubectl get pods` (suporte e mailpit `1/1`) | R1 |
| 2 | Pedido dispara aviso | criar pedido no `order` (ou `curl` do contrato) e abrir a UI do Mailpit | R1 |
| 3 | Segredo interno | mesma chamada sem `x-internal-secret`: 403 | R2 |
| 4 | Idempotência | repetir o mesmo `event_id`: `duplicate:true` e nenhum e-mail novo | R1 |
| 5 | Canais simulados | `GET /internal/notifications/{id}` mostra sms e whatsapp `sent` com provider `fake` | R1 |
| 6 | Persistência | `kubectl delete pod` e o histórico continua (volume) | R1 |
| 7 | Distribuição | `helm install` do chart (após 27/10) | R2 |
| 8 | Observabilidade | painel Grafana: eventos por resultado e envios por canal e status | R2 |

## Decisões para defender (R3)

- Serviço de suporte separado: o `order` não conhece SMTP nem provedores ([ADR 0004](adr/0004-canais-por-adapter.md)).
- HTTP interno agora, RabbitMQ planejado com o mesmo payload ([ADR 0002](adr/0002-http-antes-de-rabbitmq.md)).
- Destinatário no payload, sem acoplamento com `client` ([ADR 0005](adr/0005-destinatario-no-payload.md)).
- SQLite para idempotência e histórico; limite de 1 réplica assumido ([ADR 0003](adr/0003-sqlite.md)).
- 202 imediato e envio assíncrono: pedido não espera SMTP; falha de envio não derruba o pedido.
- Canais por variável de ambiente: troca para SMS e WhatsApp reais sem alterar código.

## Perguntas prováveis

- Por que SMS e WhatsApp são simulados? Custo e credenciais; a interface e o plano real existem ([canais-reais.md](canais-reais.md)).
- E se o suporte cair? O `order` continua criando pedidos; sem fila, o aviso daquela chamada se perde (RabbitMQ resolve com reentrega).
- Dá para ter duas réplicas? Não com SQLite; Postgres é o caminho.
- Como garante que não manda e-mail duplicado? `event_id` como chave primária.
- Por que Go? Stack provisória, do mesmo autor do `client`; o contrato independe da linguagem.

## Antes do dia

- [ ] Imagem `bascratki/support` pública e multi-arch (amd64 e arm64).
- [ ] Mailpit exposto por `port-forward` para mostrar a caixa.
- [ ] `order` real chamando, ou `curl` pronto como plano B.
- [ ] Painel Grafana exportado em JSON e importado.
- [ ] Ensaio cronometrado.
