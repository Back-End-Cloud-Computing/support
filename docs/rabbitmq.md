# RabbitMQ — transporte principal implementado

O `order` grava `OrderCreated` no outbox na mesma transação do pedido e publica no RabbitMQ quando `RabbitMq:Enabled=true`. O Support Java 21/Spring Boot consome `order.created`; o HTTP interno permanece como alternativa para fallback, teste e depuração. A decisão de começar por HTTP foi [superada em 28/10](adr/0002-http-antes-de-rabbitmq.md).

## Contrato e topologia

| Item | Valor |
|---|---|
| Exchange | `ganjj.orders`, tipo `direct`, durável |
| Routing key | `order.created` |
| Fila | `support.order-created`, durável e vinculada à routing key |
| Dead letter | `support.order-created.dlq`, durável, pelo exchange padrão |
| Corpo | JSON `OrderCreated`, igual ao [contrato HTTP](contrato-api.md) |
| Publicador | `order`, por outbox transacional |
| Consumidor | Support Java/Spring Boot, no mesmo processo da API |
| Ativação | `AMQP_URL` não vazia, fornecida por Secret no Kubernetes |

O Support declara exchange, filas e binding por `RabbitAdmin`. Sem `AMQP_URL`, o consumidor não é criado; o endpoint HTTP continua disponível. A implementação está em [RabbitConfiguration.java](../src/main/java/com/ganjj/support/RabbitConfiguration.java).

## Aceitação, ack e DLQ

1. O listener usa ack manual e prefetch 1. Valida o JSON e o contrato antes da aceitação.
2. HTTP e AMQP chamam `NotificationAcceptance`, com persistência e idempotência por `event_id`.
3. O `ack` ocorre depois de persistir o evento no SQLite. Os envios aos canais são assíncronos; ack não comprova recebimento de e-mail, SMS ou WhatsApp.
4. JSON/payload inválido recebe `basicReject` sem reenfileirar e vai diretamente para a DLQ.
5. Falha SQL recebe até três tentativas locais, com esperas de 100 ms e 200 ms. Esgotadas as tentativas, a mensagem é rejeitada sem reenfileirar e vai para a DLQ.
6. Duplicata já persistida recebe ack sem reenviar. Se a conexão cair antes do ack, pode haver reentrega: a idempotência protege a aceitação duplicada.

## Se o broker ou o consumidor ficar indisponível

O container Spring AMQP tenta restabelecer o consumo quando a conexão volta; a recuperação automática do cliente RabbitMQ está desativada para deixar esse controle com o Spring. `RabbitAdmin` declara novamente a topologia quando necessário e o listener permite iniciar sem a fila disponível (`missingQueuesFatal=false`). A API HTTP e o dispatcher de eventos já persistidos não dependem de uma conexão AMQP para trabalhar.

Com o broker ativo e a fila existente, mensagens aguardam o consumidor. Durante indisponibilidade do broker, o publisher depende do outbox/retry do Order e de sua configuração de fallback HTTP. O Support não publica nem recupera o outbox do Order. A prova de 28/10 valida o fluxo AMQP, não uma simulação de queda e recuperação completa do broker.

Filas/exchange duráveis não garantem sobrevivência à perda do armazenamento do broker. O broker opcional de laboratório em `k8s/rabbitmq.yaml` tem armazenamento efêmero; o chart o ativa por `rabbitmq.enabled=true`. O kit EKS compartilhado também transforma PVCs em `emptyDir`: não apresentar isso como persistência após substituição do Pod. As provas de persistência SQLite com PVC pertencem ao laboratório Minikube.

## Evidências e validação

- O [teste de consumidor](../src/test/java/com/ganjj/support/RabbitConsumerTest.java) usa RabbitMQ real e cobre persistência, duplicata, invalidez, falha de armazenamento e recriação da fila. A CI fornece o broker para esse teste.
- Em 28/10, o publisher real do Order foi integrado pelo [PR order #22](https://github.com/Back-End-Cloud-Computing/order/pull/22). O fallback HTTP foi apontado para uma porta indisponível; mesmo assim o pedido gerou um único e-mail via RabbitMQ. [Evidência](evidence/2026-10-28/order-created.json). Cart/Product eram fixtures declaradas.
- Em 29/10, a suíte completa passou com 12 testes, zero falhas/erros/skips, incluindo RabbitMQ real. [Resultado](evidence/2026-10-29/full-validation.json).
- Essas evidências são históricas do laboratório; não comprovam uma nova execução no EKS nem o ensaio humano.

## Compatibilidade do exchange

A versão 1.0.2 corrigiu `topic` para `direct`, conforme o contrato compartilhado. O teste declara primeiro o exchange `direct`, evitando incompatibilidade silenciosa. Em laboratório antigo com exchange `topic`, planeje a migração com o grupo: drene filas e preserve dados antes de trocar a topologia. Não apague exchange com tráfego real. A integração da issue #11 já foi concluída; não é uma implementação futura.
