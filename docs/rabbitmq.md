# RabbitMQ — plano

Objetivo da equipe: todos os serviços usarem RabbitMQ. Hoje nenhum repo implementa; por isso o transporte
inicial é HTTP interno ([ADR 0002](adr/0002-http-antes-de-rabbitmq.md)). Este documento fixa o desenho para que a
troca seja mecânica quando os demais repos terminarem.

## Desenho

| Item | Valor |
|---|---|
| Exchange | `ganjj.orders` (tipo `topic`, durável) |
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
4. Falha de infraestrutura (banco indisponível): `nack` com reenfileirar.
5. Duplicata (`event_id` já visto): `ack` sem reenviar.

Em K8s: variável `AMQP_URL` em Secret; se vazia, o consumidor não sobe e o serviço funciona só por HTTP.

## Por que só depois

- O `order` e os demais ainda não têm publisher nem consumer; subir o broker sem ninguém usando é custo sem prova.
- O contrato do corpo já é o final, então a mudança de transporte não altera o payload.
- O endpoint HTTP permanece, para teste e depuração.

## Checklist para ativar

- [ ] Broker RabbitMQ no cluster (um Deployment e um Service, definido pela equipe).
- [ ] `order` publicando `order.created` com o payload do contrato.
- [ ] Consumidor implementado e testado com o broker (teste de integração com container).
- [ ] Métrica `support_events_total{result}` igual nos dois transportes.
