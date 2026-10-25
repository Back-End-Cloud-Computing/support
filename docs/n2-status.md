# Validação N2 — support Java/Spring Boot

Atualizado em 25/10/2026. Evidências sanitizadas em `docs/evidence/2026-10-25/`.

## Executado

- Build, análise e testes locais; CI do PR anterior verde (nova revisão será validada novamente).
- Imagem pública Docker Hub `eduardofabri/support:1.0.1`, com linux/amd64 e linux/arm64. Manifest inspecionado e acesso anônimo validado.
- Instalação real por Helm em Minikube limpo (perfil `ganjj-n2`, Kubernetes 1.37.0), credenciais externas ao Git.
- Prometheus coletando o serviço e dashboard Grafana importado por provisionamento. Captura versionada.
- 11 testes sem falhas nem skips com RabbitMQ real: contrato HTTP, concorrência, SQLite, retry, Twilio com HTTP simulado e consumidor/DLQ/recriação de fila.
- SMTP real no Mailpit em Minikube; histórico preservado após recriar o pod usando PVC.
- Instalação Kustomize em namespace isolado, além da instalação Helm.
- Gateway → order real (.NET) → SQL Server real → support → Mailpit: pedido criado, repetição idempotente preservou o mesmo pedido e e-mail do pedido confirmado. Carrinho e estoque foram fixtures explícitas; isso não é um checkout com todos os serviços reais.
- Twilio preparado e testado sem contato com a API externa; o padrão continua fake. Eduardo informou que a equipe ainda não tem conta.
- Consumidor RabbitMQ opcional pronto; o order continua publicando `OrderCreated` por HTTP.

## Pendências humanas / externas

- Ensaio da apresentação com o grupo, com tempo por pessoa; roteiro pronto, mas não foi declarado um ensaio humano que não ocorreu.
- Conta, remetentes e template/consentimento no Twilio para um teste de envio real. Integração AMQP do publisher do order permanece trabalho coordenado com o responsável pelo serviço.
