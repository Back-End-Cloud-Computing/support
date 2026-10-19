# ADR 0002 — HTTP interno primeiro, RabbitMQ depois

Status: Aceito
Data: 2026-10-19

## Contexto

O `order/docs/INTEGRATION-CONTRACTS.md` §8 define o evento `OrderCreated` para o suporte e deixa o transporte em
aberto. O objetivo da equipe é todos usarem RabbitMQ, mas nenhum repo o implementa hoje e não há broker no
cluster. O `promotion` tem a mesma pendência (PROMO-001).

## Decisão

1. Agora: `POST /internal/notifications` com `x-internal-secret`, mesmo corpo do `OrderCreated`.
2. Depois, quando os demais repos terminarem as próprias implementações: consumidor RabbitMQ no mesmo processo, com o
   **mesmo corpo** e o mesmo tratamento ([rabbitmq.md](../rabbitmq.md)).
3. O endpoint HTTP permanece para teste e depuração.

## Consequências

- Sem broker no caminho crítico da N2: menos peças no cluster e no ensaio.
- O payload já é o definitivo; trocar o transporte não obriga o `order` a mudar o que envia, só como envia.
- Sem fila, se o suporte estiver fora quando o `order` chama, aquela notificação se perde. Aceito para a N2; a fila resolve com reentrega.
- Dois pontos de entrada exigem a mesma idempotência por `event_id` (já é regra).
