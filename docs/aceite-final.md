# Aceite final — support

Conferência de 29/10/2026 dos critérios das 17 issues atribuídas a Eduardo. A troca do esqueleto provisório Go por Java foi autorizada pelo responsável e registrada no ADR 0006.

## Escopo e três exceções aceitas

A entrega técnica está atendida no escopo local com trial. Permanecem explicitamente fora da conclusão: (1) ensaio humano com o grupo, (2) revisão/merge do PR e (3) envio WhatsApp iniciado pela aplicação não comprovado nesta configuração trial. Não marcar essas etapas como realizadas.

| Issue | Resultado | Evidência |
|---|---|---|
| #1 — Scaffold, CI e imagem | Atendido em Java autorizado | Maven/CI/gitleaks/Dockerfile; [CI main](https://github.com/Back-End-Cloud-Computing/support/actions/runs/40803612505), PR #23 e ADR 0006 |
| #2 — Endpoint interno e validação | Atendido | [12 testes completos](evidence/2026-10-29/full-validation.json), H1–H9/C1; [checks HTTP](evidence/2026-10-29/local-runtime.json) |
| #3 — SQLite/idempotência | Atendido | Testes de concorrência/reabertura, PVC e [SMS/duplicata](evidence/2026-10-29/sms-api-test.json) |
| #4 — SMTP/retry | Atendido | Testes de retry e [pedido real → Mailpit](evidence/2026-10-28/order-created.json) |
| #5 — SMS/WhatsApp fake por configuração | Atendido | Testes de seleção, telefone ausente e limite de texto; defaults fake preservados |
| #6 — Métricas e consulta de entregas | Atendido | Casos H10/H11/M1/M2, instrumentação e runtime/metrics 200 |
| #7 — Imagem pública multiarch | Atendido | [eduardofabri/support:1.0.3, AMD64/ARM64](evidence/2026-10-29/support-1.0.3-image.json) |
| #8 — Kubernetes/PVC/Mailpit | Atendido | [Aceite K8s/SMTP/PVC](evidence/2026-10-25/acceptance-audit.json); imagem 1.0.3 instalada posteriormente |
| #9 — Helm | Atendido | Instalação anterior e [release 1.0.3 deployed](evidence/2026-10-29/support-1.0.3-runtime.json) |
| #10 — Grafana | Atendido | [Dashboard JSON](../k8s/observability/dashboard.json) e [captura](evidence/2026-10-25/grafana-support.jpg) |
| #11 — RabbitMQ/publisher | Atendido | Outbox real, DLQ/reentrega/idempotência testadas e [prova sem fallback HTTP](evidence/2026-10-28/order-created.json) |
| #12 — Providers reais | Atendido no escopo trial, pendente de revisão e com exceção WhatsApp declarada | SMS recebido e Secret validado; ver seção abaixo |
| #13 — Ponta a ponta com order | Atendido | [Pedido real, e-mail único e falha isolada](evidence/2026-10-25/order-support.json), complementado por outbox em 28/10 |
| #14 — Ensaio | Exceção temporária aceita; issue aberta | Ensaio do grupo ainda não realizado |
| #16 — Health/ready | Atendido | Probes e [seis checks da imagem 1.0.3](evidence/2026-10-29/support-1.0.3-runtime.json) |
| #17 — Contrato/Secret do order | Atendido | Contrato idêntico nos repos e Secrets iguais comprovados em [order-created.json](evidence/2026-10-28/order-created.json) |
| #18 — Nome/porta internos | Atendido | Service support:8090 e acesso de dentro do cluster; [aceite](evidence/2026-10-25/acceptance-audit.json) |

## Aceite trial da issue #12

- Adapters SMS/WhatsApp implementados, com configuração separada e credenciais fora do Git.
- Secret Twilio aplicado no cluster; pod descartável SMS=twilio ficou Ready carregando os SecretRefs, sem criar eventos. A imagem pública 1.0.3 também passou nos checks HTTP da release integrada.
- SMS enviado pelo support, aceito pela API, recebido no celular (confirmação do usuário) e evento repetido reconhecido como duplicata. [Evidência](evidence/2026-10-29/sms-api-test.json).
- WhatsApp no Console: Received e resposta automática Delivered. **Não é prova de envio WhatsApp iniciado pelo support.** Esse envio fica como limitação aceita desta configuração trial por falta de ContentSid permitido disponível no fluxo observado. Não houve pagamento/upgrade nem se afirma que todo teste WhatsApp trial exige pagamento.
- A issue #12 foi atualizada com esse escopo e permanece aberta durante a revisão do [PR #23](https://github.com/Back-End-Cloud-Computing/support/pull/23). Não foi fechada para simular revisão concluída.

## Validação e operação

Maven verify: 12 testes, zero falhas/erros/skips, com RabbitMQ real. Carregador privado: 6 testes offline aprovados. A primeira consulta extra ao pod sob carga expirou e está registrada como tal; após estabilização, os seis checks da release integrada passaram.

O workflow adicional de publicação Docker Hub não tem os secrets de Actions configurados. A imagem exigida pelas issues está pública e validada, publicada com a autenticação Docker local existente. Automatizar próximas publicações nesse registry não é critério pendente das issues auditadas; CI build/test e publicação GHCR na main já estão implementados. Não compartilhar credenciais para resolver uma exigência que não está no escopo.

Não houve merge na main. Cart/Product na prova de checkout eram fixtures declaradas. Roteiros e testes técnicos não substituem ensaio com o grupo. Guia operacional: [entrega-local.md](entrega-local.md).

## Contexto da apresentação

A validação local descrita nesta matriz é de desenvolvimento/testes. A implantação compartilhada da N2 segue o [kit EKS do api-gateway](https://github.com/Back-End-Cloud-Computing/api-gateway/tree/main/eks), sem tornar cada serviço público. Esta revisão documental não executa infraestrutura AWS nem afirma que uma nova implantação EKS foi realizada pelo agente.
