# Auditoria do aceite — 25 e 26/10/2026

Foram encontradas 35 issues atribuídas: as 32 da lista inicial e support #16/#17/#18 já fechadas. Atualização de 28/10: **30 concluídas, 5 abertas** após concluir publisher real e contrato/Secrets. Na lista original: 28/32 concluídas. Issue fechada e CI verde, sozinhos, não comprovam todos os critérios.

## Correções desta auditoria

- Reenvio de evento contado no Mailpit: exatamente um e-mail; pedido real com support desligado retornou 201. Réplica restaurada em finally.
- Ausência de event_id agora tem teste HTTP explícito, incluindo ausência de gravação.
- Exchange corrigido de topic para direct, compatível com o contrato order/promotion. Teste real pré-declara direct antes do bootstrap. Versão 1.0.2.
- Issue #7 atualizada para eduardofabri/support por autorização de Eduardo.
- #11 e #17 reabertas por critérios ainda incompletos. O fechamento anterior foi prematuro.

## Matriz

Os nomes de evidência abaixo se referem a docs/evidence/2026-10-25 neste repo, salvo indicação de outro serviço. Não contêm tokens ou senhas.

| Issue | Estado auditado | Evidência ou pendência |
|---|---|---|
| [client #1](https://github.com/Back-End-Cloud-Computing/client/issues/1) | Concluída | CI main + auth-client.json + prometheus-targets.json |
| [client #2](https://github.com/Back-End-Cloud-Computing/client/issues/2) | Concluída | CI main run 40204237676 |
| [client #3](https://github.com/Back-End-Cloud-Computing/client/issues/3) | Concluída | prometheus-targets.json; manifests Ingress e pod scrape |
| [client #4](https://github.com/Back-End-Cloud-Computing/client/issues/4) | Concluída | client-image.json |
| [client #5](https://github.com/Back-End-Cloud-Computing/client/issues/5) | Concluída | Helm lint/install no Minikube; cluster.json |
| [client #6](https://github.com/Back-End-Cloud-Computing/client/issues/6) | Concluída | grafana-client.jpg; dashboard JSON provisionado |
| [client #7](https://github.com/Back-End-Cloud-Computing/client/issues/7) | Aberta | Ensaio real com o grupo, com tempos anotados. |
| [client #8](https://github.com/Back-End-Cloud-Computing/client/issues/8) | Concluída | k8s/criar-credentials.sh; manifests sem valores |
| [authorization #1](https://github.com/Back-End-Cloud-Computing/authorization/issues/1) | Concluída | CI main + auth-client.json + jwt-n2.md + Helm Minikube |
| [authorization #2](https://github.com/Back-End-Cloud-Computing/authorization/issues/2) | Concluída | CI main run 40204272768 |
| [authorization #3](https://github.com/Back-End-Cloud-Computing/authorization/issues/3) | Concluída | prometheus-targets.json; auth-client.json; Ingress exclui metrics |
| [authorization #4](https://github.com/Back-End-Cloud-Computing/authorization/issues/4) | Concluída | authorization-image.json |
| [authorization #5](https://github.com/Back-End-Cloud-Computing/authorization/issues/5) | Concluída | Helm lint/install Minikube; cluster.json |
| [authorization #6](https://github.com/Back-End-Cloud-Computing/authorization/issues/6) | Concluída | grafana-authorization.jpg; dashboard JSON provisionado |
| [authorization #7](https://github.com/Back-End-Cloud-Computing/authorization/issues/7) | Aberta | Ensaio real com o grupo, com tempos anotados. |
| [authorization #8](https://github.com/Back-End-Cloud-Computing/authorization/issues/8) | Aberta | Acordo e compartilhamento privado das credenciais com os responsáveis; endpoints/ADMIN tecnicamente testados. |
| [authorization #9](https://github.com/Back-End-Cloud-Computing/authorization/issues/9) | Concluída | authorization/docs/jwt-n2.md: diferenças registradas como permitido no aceite |
| [authorization #10](https://github.com/Back-End-Cloud-Computing/authorization/issues/10) | Concluída | k8s/criar-credentials.sh; manifests sem valores |
| [support #1](https://github.com/Back-End-Cloud-Computing/support/issues/1) | Concluída | Java autorizado; CI Maven, Dockerfile não root, lefthook |
| [support #2](https://github.com/Back-End-Cloud-Computing/support/issues/2) | Concluída | SupportTest: rejeição sem event_id incluída; support-tests.json |
| [support #3](https://github.com/Back-End-Cloud-Computing/support/issues/3) | Concluída | SupportTest SQLite/concorrência/reabertura; order-support.json PVC |
| [support #4](https://github.com/Back-End-Cloud-Computing/support/issues/4) | Concluída | SMTP real no Mailpit + testes retry/backoff/continuidade |
| [support #5](https://github.com/Back-End-Cloud-Computing/support/issues/5) | Concluída | SupportTest fake/skipped/formatação; Twilio opcional |
| [support #6](https://github.com/Back-End-Cloud-Computing/support/issues/6) | Concluída | SupportTest; prometheus-targets.json |
| [support #7](https://github.com/Back-End-Cloud-Computing/support/issues/7) | Concluída | support-image.json; destino eduardofabri autorizado e issue atualizada |
| [support #8](https://github.com/Back-End-Cloud-Computing/support/issues/8) | Concluída | kustomize.json; PVC e SMTP validados |
| [support #9](https://github.com/Back-End-Cloud-Computing/support/issues/9) | Concluída | Helm lint/install; mesma configuração de PVC/SMTP/Secret |
| [support #10](https://github.com/Back-End-Cloud-Computing/support/issues/10) | Concluída | grafana-support.jpg; dashboard JSON provisionado |
| [support #11](https://github.com/Back-End-Cloud-Computing/support/issues/11) | Concluída | order PR #22; order-created.json: pedido real → outbox → RabbitMQ → support → SMTP, fallback HTTP indisponível. |
| [support #12](https://github.com/Back-End-Cloud-Computing/support/issues/12) | Aberta | Integração Twilio preparada; faltam conta, remetentes/template e envios reais. |
| [support #13](https://github.com/Back-End-Cloud-Computing/support/issues/13) | Concluída | order-support.json + acceptance-audit.json: email único, duplicata e pedido com suporte desligado |
| [support #14](https://github.com/Back-End-Cloud-Computing/support/issues/14) | Aberta | Ensaio real com o grupo, com tempos anotados. |
| [support #16](https://github.com/Back-End-Cloud-Computing/support/issues/16) | Concluída | acceptance-audit.json: health e ready 200 pela rede do cluster; probes versionadas |
| [support #17](https://github.com/Back-End-Cloud-Computing/support/issues/17) | Concluída | docs/contrato-api.md idênticos; mesmo Secret interno configurado privadamente nos dois serviços; prova em order-created.json. |
| [support #18](https://github.com/Back-End-Cloud-Computing/support/issues/18) | Concluída | acceptance-audit.json: gateway acessou DNS support:8090 dentro do cluster |

## Limites da prova em 25/26 de outubro

O checkout usa order, SQL Server, gateway, auth, client e support reais; Cart e Product são fixtures declaradas. Promotion/JWT também foi testado com MySQL real. Não foi demonstrado checkout com todos os serviços reais. Consumidor RabbitMQ foi testado com broker real, sem publisher real no order. Twilio foi testado com HTTP simulado, sem envio externo. Material do professor disponível compara Keycloak/Istio ao contrato implementado; não foi identificada uma página datada especificamente 20/10. Roteiros preparados não são ensaio em grupo.

O contrato no order usa docs/INTEGRATION-CONTRACTS.md §8, e não docs/contrato-api.md. Telefone é opcional; order atual o omite, apesar do plano anterior prever consulta ao client. Os valores dos Secrets do laboratório foram comparados sem imprimir seus conteúdos. O compartilhamento com outros responsáveis depende de canal privado.

O ambiente original ganjj-n2 permanece na versão 1.0.1 com exchange topic. A 1.0.2 é validada em namespace novo ganjj-acceptance-audit com exchange direct, sem remover bindings do ambiente anterior. Migração do exchange antigo foi bloqueada pela revisão automática por risco de remover bindings; não é necessária para esta validação isolada.

Validação da imagem pública 1.0.2 em 26/10: [support-1.0.2-runtime.json](evidence/2026-10-25/support-1.0.2-runtime.json). Namespace novo com Secret de nome alternativo, exchange direct, publicação sintética confirmada, persistência SQLite, SMTP real no Mailpit, duplicata HTTP do mesmo evento com um único e-mail, probes 200 e réplica pronta. Isso não representa publisher implementado no order.

## Atualização de 28/10 — pendências técnicas encerradas

O [PR order #22](https://github.com/Back-End-Cloud-Computing/order/pull/22) adiciona OrderCreated ao outbox existente sem remover OrderConfirmed. Os 55 testes locais passaram sem skips, com SQL Server/RabbitMQ reais, e a CI do PR passou. O teste em Kubernetes usou order e SQL Server reais e desabilitou efetivamente o fallback HTTP apontando-o para 127.0.0.1:1. O evento foi publicado, recebido pelo support Java e gerou exatamente um e-mail no Mailpit. A repetição da chave preservou o pedido. Os dois contratos são byte-idênticos e os Secrets internos têm o mesmo valor, criado fora do Git.

Evidência atual: [order-created.json](evidence/2026-10-28/order-created.json) e [order-tests.json](evidence/2026-10-28/order-tests.json). As limitações anteriores de ausência de publisher/caminho documental referem-se ao estado de 25/26 e foram resolvidas nesta atualização. Cart/Product continuam fixtures; não foi alegado checkout completo com todos os serviços reais.

Continuam abertas: os três ensaios (Eduardo pediu para fazê-los por último); authorization #8 (chave/ADMIN confirmados e arquivo privado entregue a Eduardo, que optou por compartilhar com promotion/gateway; aguarda conclusão do compartilhamento); support #12 (conta Twilio ainda inexistente, conforme confirmação de 28/10). O helper de Secret e o roteiro concreto de ativação estão preparados, mas não houve envio externo.
