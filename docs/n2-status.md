# Estado da entrega — 24/10/2026

Implementado: Spring Boot/Java 21, CI e Dockerfile (#1); endpoint interno com segredo e validação (#2); SQLite e idempotência (#3); SMTP e retry (#4); adapters fake (#5); métricas e consulta de entregas (#6); Kustomize com Mailpit/PVC (#8); Helm (#9); dashboard JSON (#10).

Esses itens ainda exigem CI remoto e/ou validação no cluster para o aceite final. O ambiente local não tem contexto Kubernetes configurado. O dashboard precisa de captura no Grafana instalado e os recursos devem ser conferidos com as aulas de 27/10 e 03/11.

Pendentes: publicação Docker Hub multi-arch (#7), integração com o order real (#13), ensaio com o grupo (#14). O workflow de publicação exige secrets `DOCKERHUB_USERNAME` e `DOCKERHUB_TOKEN` com acesso a `bascratki/support`.

Planejados após dependências externas: RabbitMQ (#11, depende do broker e do order publicar eventos) e provedores reais SMS/WhatsApp (#12, exige escolha de provedor e credenciais). As interfaces de Notifier permitem adicionar esses adapters sem mudar o contrato.

A issue #1 ainda descreve Go provisório; a implementação segue a decisão explícita de Eduardo em ADR 0006. A matriz de testes e os aceites funcionais continuam válidos.

## Evidência local

`./mvnw verify`: 8 testes de comportamento verdes; build Docker passou. Smoke test com Mailpit em containers: e-mail SMTP recebido, três entregas `sent`, repetição do evento com `duplicate=true`, histórico preservado após reiniciar o container support. Helm lint e renderização Helm/Kustomize passaram. Esse teste usa payload fictício e não substitui a integração com o order real.
