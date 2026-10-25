# ADR 0006 — Java com Spring Boot

Status: Aceito por Eduardo em 24/10/2026. Substitui ADR 0001.

## Decisão

Implementar o suporte em Java 21 e Spring Boot, alinhado ao authorization. Spring MVC recebe as notificações; JDBC SQLite persiste eventos e entregas; Spring Mail envia SMTP e Micrometer/Actuator expõe Prometheus.

O contrato HTTP, o SQLite, as métricas e as variáveis de ambiente permanecem. SMS e WhatsApp usam adapters fake; canais reais e RabbitMQ continuam planejados e dependem de credenciais/broker externos.

## Consequências

O build usa Maven Wrapper, testes JUnit e Docker com JRE 21. Uma réplica com estratégia Recreate mantém um worker por SQLite. Eventos aceitos são persistidos antes da resposta 202; o worker retoma eventos incompletos após reinício. Uma interrupção entre envio externo e gravação da entrega pode repetir a mensagem: a idempotência por event_id impede novos eventos duplicados, mas não garante exatamente um envio diante de falhas nesse intervalo. Essa garantia exige suporte de idempotência no provedor ou outra coordenação de entrega.
