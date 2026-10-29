# Validação N2 — support Java/Spring Boot

Atualizado em 25/10/2026. Evidências sanitizadas em `docs/evidence/2026-10-25/`.

## Executado

- Build, análise e testes locais; CI do PR de auditoria #20 verde; a revisão final também é verificada antes de integrar.
- Imagem pública Docker Hub `eduardofabri/support:1.0.2`, com linux/amd64 e linux/arm64. Manifest inspecionado e acesso anônimo validado.
- Instalação real por Helm em Minikube limpo (perfil `ganjj-n2`, Kubernetes 1.37.0), credenciais externas ao Git.
- Prometheus coletando o serviço e dashboard Grafana importado por provisionamento. Captura versionada.
- 12 testes sem falhas nem skips com RabbitMQ real: contrato HTTP, concorrência, SQLite, retry, Twilio com HTTP simulado e consumidor/DLQ/recriação de fila.
- SMTP real no Mailpit em Minikube; histórico preservado após recriar o pod usando PVC.
- Instalação Kustomize em namespace isolado, além da instalação Helm.
- Gateway → order real (.NET) → SQL Server real → support → Mailpit: pedido criado, repetição idempotente preservou o mesmo pedido e e-mail do pedido confirmado. Carrinho e estoque foram fixtures explícitas; isso não é um checkout com todos os serviços reais.
- Twilio preparado e testado sem contato com a API externa; o padrão continua fake. Eduardo informou que a equipe ainda não tem conta.
- Consumidor RabbitMQ opcional pronto; o order continua publicando `OrderCreated` por HTTP.

## Pendências humanas / externas

- Ensaio da apresentação com o grupo, com tempo por pessoa; roteiro pronto, mas não foi declarado um ensaio humano que não ocorreu.
- Conta, remetentes e template/consentimento no Twilio para um teste de envio real. Integração AMQP do publisher do order permanece trabalho coordenado com o responsável pelo serviço.

Auditoria de aceite: [acceptance-audit.json](evidence/2026-10-25/acceptance-audit.json). A issue #11 foi reaberta: consumidor testado não prova publisher no order. A versão 1.0.2 alinha o exchange `direct` ao contrato do grupo. Destino Docker Hub autorizado por Eduardo: `eduardofabri/support`, requisito da issue #7 atualizado.

Imagem 1.0.2 validada em namespace separado `ganjj-acceptance-audit`; `ganjj-n2` anterior preservado. Evidência: [support-1.0.2-runtime.json](evidence/2026-10-25/support-1.0.2-runtime.json).

## Atualização de 28/10/2026

Publisher order.created integrado ao outbox real do order, contrato espelhado no caminho exigido e Secrets iguais configurados fora do Git. Teste real AMQP → SQLite → SMTP passou com fallback HTTP indisponível e um único e-mail, além de 55 testes .NET locais/CI. Ver docs/evidence/2026-10-28/order-created.json e order-tests.json. Isso resolve as pendências técnicas anteriores de #11/#17; permanece a necessidade de conta Twilio, compartilhamento privado das credenciais ADMIN pelo Eduardo e ensaio posterior com o grupo.
