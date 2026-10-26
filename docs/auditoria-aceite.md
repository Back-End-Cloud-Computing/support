# Auditoria do aceite — 25 e 26/10/2026

Foram encontradas 35 issues atribuídas: as 32 da lista inicial e support #16/#17/#18 já fechadas. Resultado após a auditoria: **28 concluídas, 7 abertas**. Na lista original: 26/32 concluídas. Issue fechada e CI verde, sozinhos, não comprovam todos os critérios.

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
| [support #11](https://github.com/Back-End-Cloud-Computing/support/issues/11) | Aberta | Consumidor pronto; order ainda não publica order.created. Broker/teste de consumidor não substituem teste do publisher real. |
| [support #12](https://github.com/Back-End-Cloud-Computing/support/issues/12) | Aberta | Integração Twilio preparada; faltam conta, remetentes/template e envios reais. |
| [support #13](https://github.com/Back-End-Cloud-Computing/support/issues/13) | Concluída | order-support.json + acceptance-audit.json: email único, duplicata e pedido com suporte desligado |
| [support #14](https://github.com/Back-End-Cloud-Computing/support/issues/14) | Aberta | Ensaio real com o grupo, com tempos anotados. |
| [support #16](https://github.com/Back-End-Cloud-Computing/support/issues/16) | Concluída | acceptance-audit.json: health e ready 200 pela rede do cluster; probes versionadas |
| [support #17](https://github.com/Back-End-Cloud-Computing/support/issues/17) | Aberta | Payload compatível e Secrets iguais no laboratório. Falta acordo entre responsáveis e caminho documental correspondente no order (atual INTEGRATION-CONTRACTS.md §8). |
| [support #18](https://github.com/Back-End-Cloud-Computing/support/issues/18) | Concluída | acceptance-audit.json: gateway acessou DNS support:8090 dentro do cluster |

## Limites da prova

O checkout usa order, SQL Server, gateway, auth, client e support reais; Cart e Product são fixtures declaradas. Promotion/JWT também foi testado com MySQL real. Não foi demonstrado checkout com todos os serviços reais. Consumidor RabbitMQ foi testado com broker real, sem publisher real no order. Twilio foi testado com HTTP simulado, sem envio externo. Material do professor disponível compara Keycloak/Istio ao contrato implementado; não foi identificada uma página datada especificamente 20/10. Roteiros preparados não são ensaio em grupo.

O contrato no order usa docs/INTEGRATION-CONTRACTS.md §8, e não docs/contrato-api.md. Telefone é opcional; order atual o omite, apesar do plano anterior prever consulta ao client. Os valores dos Secrets do laboratório foram comparados sem imprimir seus conteúdos. O compartilhamento com outros responsáveis depende de canal privado.

O ambiente original ganjj-n2 permanece na versão 1.0.1 com exchange topic. A 1.0.2 é validada em namespace novo ganjj-acceptance-audit com exchange direct, sem remover bindings do ambiente anterior. Migração do exchange antigo foi bloqueada pela revisão automática por risco de remover bindings; não é necessária para esta validação isolada.
