# RabbitMQ — consumidor e integração pendente

O suporte implementa o consumidor. O order agora publica OrderCreated por outbox quando RabbitMq:Enabled=true; HTTP interno permanece como fallback ([ADR 0002](adr/0002-http-antes-de-rabbitmq.md)). Este documento fixa o desenho para que a
troca seja mecânica quando os demais repos terminarem.

## Desenho

| Item | Valor |
|---|---|
| Exchange | `ganjj.orders` (tipo `direct`, durável; compatível com `order/docs/INTEGRATION-CONTRACTS.md` §9) |
| Routing key | `order.created` |
| Fila do suporte | `support.order-created` (durável), vinculada a `order.created` |
| Dead letter | `support.order-created.dlq`, após 3 falhas de processamento |
| Corpo | o **mesmo JSON** do [contrato HTTP](contrato-api.md) (`OrderCreated`) |
| Publicador | `order` (quando o pedido é criado) |
| Consumidor | `support`, em worker Java próprio dentro do mesmo processo |

## Comportamento do consumidor

1. Recebe a mensagem e chama o **mesmo tratamento** do endpoint HTTP (validação, idempotência por `event_id`, despacho).
2. `ack` depois de gravar o evento em `events` (a entrega aos canais continua assíncrona).
3. Mensagem inválida: `nack` sem reenfileirar (vai para a DLQ).
4. Falha de infraestrutura (banco indisponível): três tentativas locais, depois rejeição para DLQ.
5. Duplicata (`event_id` já visto): `ack` sem reenviar.

Em K8s: variável `AMQP_URL` em Secret; se vazia, o consumidor não sobe e o serviço funciona só por HTTP.

## Por que só depois

- O publisher do order foi implementado no PR #22, reutilizando o outbox transacional existente.
- O contrato do corpo já é o final, então a mudança de transporte não altera o payload.
- O endpoint HTTP permanece, para teste e depuração.

## Checklist para ativar

- [x] Broker RabbitMQ no cluster (um Deployment e um Service, definido pela equipe).
- [x] `order` publicando `order.created` com o payload do contrato.
- [x] Consumidor implementado e testado com o broker (teste de integração com container).
- [x] Métrica `support_events_total{result}` igual nos dois transportes.

## Implementado e testado em 25/10/2026

O consumidor está pronto e é ativado por `AMQP_URL`. HTTP e AMQP usam `NotificationAcceptance`, com o mesmo contrato e idempotência. Mensagens inválidas vão diretamente à DLQ; falhas de armazenamento recebem três tentativas locais (100 ms e 200 ms entre elas) e depois DLQ. O ack ocorre somente após persistir o evento; o dispatcher continua assíncrono.

A fila/exchange são declaradas por RabbitAdmin e recuperadas quando necessário. A suíte com RabbitMQ real prova persistência, duplicata, invalidez, falha de armazenamento e recriação da fila. CI executa esse teste com serviço RabbitMQ. `k8s/rabbitmq.yaml` fornece um broker de laboratório opcional; o chart usa `rabbitmq.enabled=true` quando desejado. O broker de laboratório tem armazenamento efêmero: não é implantação de produção.

Em 28/10, o publisher real do order foi integrado por PR #22: grava OrderCreated na mesma transação SQL e usa a routing key order.created. O fallback HTTP foi apontado para uma porta indisponível no teste isolado; mesmo assim o pedido gerou um único e-mail via RabbitMQ. Evidência: [order-created.json](evidence/2026-10-28/order-created.json). Cart/Product permanecem fixtures explícitas.

## Compatibilidade do exchange (auditoria de aceite)

A versão 1.0.2 corrige `topic` para `direct`, conforme o contrato já publicado por order/promotion. O teste real declara primeiro o exchange `direct`, garantindo que o support possa compartilhar o broker sem `PRECONDITION_FAILED`. Em um laboratório antigo com exchange `topic`, pare os consumidores, drene as filas e migre o exchange antes de atualizar; não apague um exchange com tráfego real. O publisher e o teste de ponta a ponta AMQP foram concluídos em 28/10; a issue #11 atende ao aceite.
