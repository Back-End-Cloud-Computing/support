# ADR 0002 — HTTP interno primeiro, RabbitMQ depois

Status: Superado em 28/10/2026 pela integração RabbitMQ descrita em [rabbitmq.md](../rabbitmq.md)
Data: 2026-10-19

## Decisão vigente

O caminho principal é `order` → outbox → exchange `ganjj.orders` (`direct`) → routing key `order.created` → fila `support.order-created` → Support Java/Spring Boot. O publisher real e o consumidor foram validados em 28/10 ([evidência](../evidence/2026-10-28/order-created.json)). O HTTP interno com `x-internal-secret` permanece como alternativa para fallback, teste e depuração, com o mesmo contrato e idempotência.

A decisão original abaixo é histórica: não se aplica mais a premissa de ausência de broker/publisher. Recuperação, ack e limites de armazenamento estão em [rabbitmq.md](../rabbitmq.md).

## Contexto histórico

O `order/docs/INTEGRATION-CONTRACTS.md` §8 define o evento `OrderCreated` para o suporte e deixa o transporte em
aberto. O objetivo da equipe é todos usarem RabbitMQ, mas nenhum repo o implementa hoje e não há broker no
cluster. O `promotion` tem a mesma pendência (PROMO-001).

## Decisão original

1. Agora: `POST /internal/notifications` com `x-internal-secret`, mesmo corpo do `OrderCreated`.
2. Depois, quando os demais repos terminarem as próprias implementações: consumidor RabbitMQ no mesmo processo, com o
   **mesmo corpo** e o mesmo tratamento ([rabbitmq.md](../rabbitmq.md)).
3. O endpoint HTTP permanece para teste e depuração.

## Consequências da decisão original

- Sem broker no caminho crítico da N2: menos peças no cluster e no ensaio.
- O payload já é o definitivo; trocar o transporte não obriga o `order` a mudar o que envia, só como envia.
- Sem fila, se o suporte estiver fora quando o `order` chama, aquela notificação se perde. Aceito para a N2; a fila resolve com reentrega.
- Dois pontos de entrada exigem a mesma idempotência por `event_id` (já é regra).
