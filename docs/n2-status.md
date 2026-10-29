# Validação N2 — support Java/Spring Boot

Atualizado em 29/10/2026. Guia operacional: [entrega-local.md](entrega-local.md). Os resultados abaixo distinguem as datas de execução e não significam revalidação contínua do ambiente inteiro.

## Comprovado

- Imagem pública eduardofabri/support:1.0.2 amd64/arm64, Helm/Kustomize em Minikube, SQLite/PVC, SMTP Mailpit e Prometheus/Grafana: evidências em evidence/2026-10-25/.
- Publisher OrderCreated integrado ao outbox real do order, contrato espelhado e Secrets compartilhados no laboratório. Prova de 28/10: order e SQL Server reais → RabbitMQ → support → exatamente um e-mail, com fallback HTTP indisponível. Cart/Product eram fixtures. [Evidência](evidence/2026-10-28/order-created.json).
- Runtime Twilio local iniciado por Eduardo: seis verificações HTTP aprovadas. [Evidência](evidence/2026-10-29/local-runtime.json).
- SMS real enviado pela aplicação, aceito pela API, recebido no celular e repetição idempotente. [Evidência](evidence/2026-10-29/sms-api-test.json).
- WhatsApp no Try out WhatsApp: mensagem Received e resposta automática Delivered. Sem prova de envio pela aplicação.
- Carregador local do JSON privado: seis testes offline aprovados; configuração SMS validada sem imprimir valores. [Preflight](evidence/2026-10-29/twilio-local-preflight.json).

## Limites e pendências

- Entrega local com trial, sem contratar upgrade. Falta ContentSid permitido disponível nesta configuração para WhatsApp iniciado pelo support. A resposta automática não substitui esse teste. [Escopo proposto para as issues](atualizacao-issues-trial.md).
- Versão 1.0.3 publicada no Docker Hub com manifesto público AMD64/ARM64. [Evidência](evidence/2026-10-29/support-1.0.3-image.json). Código e documentação enviados à branch própria, aguardando revisão do PR.
- Após autorização de acesso ampliado em 29/10, a suíte Java completa passou: 12 testes, zero falhas/erros/skips, com RabbitMQ real descartável. [Evidência](evidence/2026-10-29/full-validation.json). As validações anteriores de SMS/runtime foram executadas pelo usuário no Terminal, com resultados conferidos.
- Ensaio com o grupo (#14) continua pendente e por último.
- #11 e #17 tiveram critérios técnicos concluídos em 28/10. A necessidade de implementar o publisher no order e a ausência de conta Twilio eram pendências antigas já resolvidas.

Auditoria detalhada: [auditoria-aceite.md](auditoria-aceite.md). Não apresentar Cart/Product como serviços reais na prova de checkout ou o DNS local como endpoint público.
