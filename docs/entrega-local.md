# Entrega local — support

Atualizado em 29/10/2026. Este guia pertence ao support. Visão geral no [README](../README.md), contrato em [contrato-api.md](contrato-api.md) e limitações do provedor em [canais-reais.md](canais-reais.md).

## Desenvolvimento local e apresentação N2

Este guia descreve o laboratório de desenvolvimento e validação local usado nesta etapa. A decisão temporária de trabalhar localmente não define o destino final da apresentação. A implantação compartilhada da N2 segue o [kit Amazon EKS do api-gateway](https://github.com/Back-End-Cloud-Computing/api-gateway/tree/main/eks), coordenado pelo grupo. O Gateway/Ingress é a entrada externa única; client, authorization e support continuam serviços internos, sem necessidade de uma URL pública por serviço.

O kit aplica os manifests no namespace do contexto kubectl, default no fluxo padrão. DNS curto como authorization:8081 funciona para consumidores no mesmo namespace; authorization.default.svc:8081 identifica explicitamente o namespace default. O DNS com ganjj-n2 só vale no laboratório onde esse namespace foi criado/aplicado. Esta revisão é documental: não cria nem modifica infraestrutura AWS.

## Responsabilidade e dependências

Serviço Java 21/Spring Boot com SQLite: recebe OrderCreated, persiste eventos/entregas e notifica por e-mail, SMS e WhatsApp. event_id fornece idempotência; o worker reprocessa eventos pendentes. Com SQLite, usar uma réplica.

No laboratório integrado, order grava o evento no outbox e publica no RabbitMQ; o suporte consome pela fila. O HTTP com x-internal-secret permanece como fallback. O e-mail usa SMTP/Mailpit. A prova de 28/10 usou order/SQL Server/RabbitMQ reais, com Cart/Product explicitamente simulados.

## Ambientes locais distintos

| Ambiente | Porta e comportamento |
|---|---|
| Serviço padrão / Kubernetes | 8090, chamado internamente pelo order |
| Laboratório isolado Twilio | 127.0.0.1:8095; banco separado, AMQP desligado, SMS Twilio e e-mail/WhatsApp fake |

O laboratório Twilio isolado não substitui automaticamente a release Kubernetes nem recebe a fila antiga. Esse laboratório permanece local para desenvolvimento; a apresentação compartilhada usa o kit EKS do api-gateway, com RabbitMQ/Mailpit e os canais definidos pela configuração do cluster. Não enviar tokens JWT ao support para autenticar o contrato interno: suas rotas internas usam o segredo x-internal-secret.

## Rodar o serviço padrão

```bash
cp .env.example .env
./mvnw package
./mvnw spring-boot:run
```

Configurar INTERNAL_SECRET privado e DB_PATH no .env. O Spring importa o .env; o diretório do banco deve ser gravável. SMTP precisa de Mailpit ou outro servidor configurado. Kubernetes/Helm: [manifests](../k8s/README.md), [chart](../helm/support/README.md) e [Grafana](../k8s/observability/README.md).

## Rodar o laboratório Twilio

O usuário preenche um JSON privado com credenciais, remetentes e destinatários. Não versionar esse arquivo. O carregador valida sem rede por padrão:

```bash
python3 scripts/twilio-local.py --config /caminho/privado/TWILIO-CONFIGURAR.json --channel sms
```

Acrescentar --run inicia o JAR compilado na porta 8095. O carregador cria banco/segredo privados em support-twilio-local ao lado do JSON. Ele não cria eventos nem envia mensagens por conta própria. O POST posterior de um evento com telefone pode enviar pelo canal real selecionado. Não copiar o segredo desse laboratório para outra release automaticamente.

Para WhatsApp trial, --channel whatsapp/both exige ContentSid permitido iniciado em HX. Account SID iniciado em AC não serve nesse campo. A conta atual não disponibilizou esse identificador no fluxo observado; a biblioteca ofereceu upgrade para templates próprios. Nenhum upgrade foi contratado. Nem todo teste WhatsApp trial exige pagamento; o bloqueio desta configuração está descrito no aceite proposto da issue #12.

## Rotas e verificação

| Rota | Resultado |
|---|---|
| GET /health | Processo responde |
| GET /health/ready | Prontidão do banco |
| GET /metrics | Métricas Prometheus |
| POST /internal/notifications | Aceita evento: 202, duplicate:false/true |
| GET /internal/notifications/{event_id} | Entregas por canal; 404 para evento desconhecido |

Rotas internas exigem x-internal-secret e retornam 403 sem segredo/segredo errado. O corpo válido está no contrato. Telefone é opcional: sem ele, SMS/WhatsApp ficam skipped. Não confundir sent de um adapter fake com entrega externa, nem sent do Twilio com recebimento no aparelho.

## O que foi comprovado

- Seis checks do runtime local: health/readiness/metrics 200; rotas protegidas 403 sem segredo/segredo errado e 404 com segredo correto para evento inexistente. [Evidência](evidence/2026-10-29/local-runtime.json).
- SMS enviado pelo support com modelo fixo do trial, aceito pela API e recebido no celular, conforme confirmação do usuário. O mesmo evento retornou duplicate:true. Isso comprova transporte; não detalhes personalizados do pedido. [Evidência](evidence/2026-10-29/sms-api-test.json).
- WhatsApp no Console: mensagem Received e resposta automática Delivered; não foi comprovado envio WhatsApp iniciado pela aplicação.
- Integração de 28/10: outbox/RabbitMQ real com fallback HTTP indisponível e um único e-mail no Mailpit. [Evidência](evidence/2026-10-28/order-created.json).

## Testes e publicação

./mvnw verify executa a suíte Java; testes HTTP abrem sockets e testes AMQP exigem broker. O carregador local tem seis testes offline: python3 -m unittest discover -s scripts -p test_twilio_local.py -v. Eles não substituem os testes da aplicação ou entregas reais.

Após autorização de acesso ampliado em 29/10, ./mvnw verify passou com 12 testes, zero falhas/erros/skips e RabbitMQ real descartável. [Evidência](evidence/2026-10-29/full-validation.json). Antes disso, o usuário executou o runtime e o teste SMS no próprio Terminal; os resultados sanitizados foram conferidos.

Imagem pública atual: eduardofabri/support:1.0.3, amd64/arm64, manifesto verificado sem login. [Evidência](evidence/2026-10-29/support-1.0.3-image.json). Código e documentação estão enviados à branch própria codex/twilio-trial, aguardando revisão para integrar na main.

## Pendências

Revisar o PR #23 sem merge automático e fazer o ensaio real do grupo (#14), por último. A issue #12 já registra o escopo trial e permanece aberta durante a revisão; WhatsApp pela aplicação não foi marcado como aprovado. Textos de issues em [atualizacao-issues-trial.md](atualizacao-issues-trial.md). Não repetir o SMS já recebido sem necessidade.

A imagem 1.0.3 também foi instalada por Helm no namespace isolado ganjj-acceptance-audit, com seis checks HTTP aprovados e Secret validado em pod descartável. Detalhes em [support-1.0.3-runtime.json](evidence/2026-10-29/support-1.0.3-runtime.json). A release integrada mantém SMS/WhatsApp fake para não disparar eventos externos automaticamente.

Matriz completa de aceite das issues deste serviço: [aceite-final.md](aceite-final.md).
