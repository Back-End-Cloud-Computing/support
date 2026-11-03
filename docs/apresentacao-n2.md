# Roteiro de apresentação N2 — Support

Responsável: Eduardo. Apresentação: **10/11/2026**. Registro do ensaio até **07/11**, ligado à issue #14.

Roteiro de preparação, ainda sem ensaio humano registrado. O desenvolvimento/Twilio trial permanece local; a apresentação compartilhada segue o kit EKS do grupo, com Gateway/Ingress como entrada externa única.

## 1. O que falar e em quanto tempo

Sugestão: **3 minutos**. Combinar com Rodrigo a criação do pedido para a sua fala começar no evento recebido.

| Janela planejada | Fala sugerida | O que mostrar |
|---|---|---|
| 0:00–0:30 | “O Support cuida das notificações. É Java/Spring Boot com SQLite próprio e uma réplica; o Order não precisa conhecer SMTP ou Twilio.” | Responsabilidade e banco por serviço |
| 0:30–1:00 | “O pedido grava OrderCreated no outbox. O publisher entrega em ganjj.orders com a chave order.created; o Support consome a fila support.order-created.” | Pods/Services, fila e consumidor |
| 1:00–2:10 | “O pedido entra pelo Gateway. O processamento segue pelo RabbitMQ e o e-mail aparece no Mailpit.” | Pedido real criado por Rodrigo e e-mail correspondente |
| 2:10–2:40 | “O mesmo event_id não pode gerar outro envio. Ao repetir o evento, o serviço reconhece a duplicata.” | duplicate:true e um único e-mail |
| 2:40–3:00 | “SMS real foi validado antes. No trial, WhatsApp pela aplicação não foi comprovado; a resposta automática do Console é uma prova diferente.” | Limitação explícita e encerramento |

## 2. Preparar com o grupo

Confirmar com o responsável pelo EKS o contexto e namespace. Preparar dados fictícios de Product/Cart e a conta CLIENTE antes da fala, conforme o runbook do Order. A implantação AWS é coordenada pelo responsável pelo cluster; este roteiro não a executa.

Os comandos desta seção usam os nomes dos manifests k8s/ do kit EKS; o chart do laboratório local pode usar nomes diferentes. Confirmar o contexto e ajustar o namespace antes de executar. Variáveis exportadas valem somente na aba de Terminal em que foram definidas.

```bash
export GANJJ_NAMESPACE=default
kubectl config current-context
kubectl get deployment support rabbitmq mailpit -n "$GANJJ_NAMESPACE"
kubectl get service support rabbitmq mailpit -n "$GANJJ_NAMESPACE"
kubectl exec deployment/rabbitmq -n "$GANJJ_NAMESPACE" -- rabbitmqctl list_queues name messages consumers
```

Esperado: support pronto e fila support.order-created com consumidor. Fila vazia não significa falha: uma mensagem pode ter sido consumida imediatamente. Observar também os logs após criar o pedido.

## 3. Demonstração principal: pedido real

Rodrigo executa o pedido pelo Gateway conforme o [runbook do Order](https://github.com/Back-End-Cloud-Computing/order/blob/main/docs/PRESENTATION-RUNBOOK.md), com Product/Cart preparados. Combinar o código do pedido e um identificador de correlação para reconhecer a mensagem certa.

```bash
export GANJJ_NAMESPACE=default
kubectl config current-context
kubectl logs deployment/order -n "$GANJJ_NAMESPACE" --since=5m
kubectl logs deployment/support -n "$GANJJ_NAMESPACE" --since=5m
kubectl port-forward service/mailpit 8025:8025 -n "$GANJJ_NAMESPACE" --address 127.0.0.1
```

Abrir http://127.0.0.1:8025 e localizar o e-mail desse pedido. Deixar o port-forward em outra aba do Terminal; encerrar com Ctrl+C ao terminar. Ele serve para inspeção, enquanto o pedido entra pelo Ingress do Gateway.

Mostrar que a troca é entre dois serviços: Order publica e Support consome/processa. Só ter o RabbitMQ de pé não comprova a integração. OrderConfirmed/order.confirmed é o fluxo do Promotion; o Support atual usa **OrderCreated/order.created**.

### Repetição idempotente

No fluxo real, Rodrigo repete a criação com a mesma Idempotency-Key e deve obter o mesmo pedido sem novo evento/e-mail. Para provar diretamente a deduplicação do Support, reenviar o **mesmo payload OrderCreated, com o mesmo event_id**, conforme o [contrato](contrato-api.md): esperado 202 e duplicate:true. Um UUID novo significa outro evento e não testa duplicata.

## 4. Plano B: endpoint interno com evento sintético

Usar apenas se o fluxo do pedido estiver indisponível. Dizer na apresentação: **este passo testa o Support via HTTP, não comprova o publisher do Order/RabbitMQ**.

Confirmar antes que o SMTP da aplicação aponta para Mailpit. Usar payload sem customer_phone, para os canais telefônicos ficarem skipped; não repetir testes reais Twilio na apresentação.

Em outra aba, abrir inspeção interna:

```bash
export GANJJ_NAMESPACE=default
kubectl config current-context
kubectl port-forward service/support 18090:8090 -n "$GANJJ_NAMESPACE" --address 127.0.0.1
```

Na aba da demonstração, carregar o segredo sem mostrá-lo. O nome abaixo corresponde aos manifests padrão; ajustar somente se o responsável configurou outro Secret.

```bash
export GANJJ_SUPPORT_SECRET_NAME=support-credentials
export GANJJ_INTERNAL_SECRET="$(kubectl get secret "$GANJJ_SUPPORT_SECRET_NAME" -n "$GANJJ_NAMESPACE" -o jsonpath='{.data.internal-secret}' | python3 -c 'import base64,sys; print(base64.b64decode(sys.stdin.read()).decode())')"
test -n "$GANJJ_INTERNAL_SECRET" && printf 'Segredo interno carregado\n'
export GANJJ_SUPPORT_URL=http://127.0.0.1:18090
umask 077
export GANJJ_ENSAIO_DIR="$(mktemp -d)"
python3 - <<'PY'
import datetime,json,os,uuid
from pathlib import Path
event_id=str(uuid.uuid4())
body=dict(event_id=event_id,event_type='OrderCreated',occurred_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),order_id='ensaio-http',order_code='ENSAIO-'+event_id[:8],customer_id='cliente-ensaio',customer_email='ensaio@example.invalid',total=1,currency='BRL')
Path(os.environ['GANJJ_ENSAIO_DIR'],'evento.json').write_text(json.dumps(body))
print('Payload sintético preparado; event_id='+event_id)
PY
curl -i -X POST "$GANJJ_SUPPORT_URL/internal/notifications" -H "x-internal-secret: $GANJJ_INTERNAL_SECRET" -H 'Content-Type: application/json' --data-binary "@$GANJJ_ENSAIO_DIR/evento.json"
```

Se o carregamento do segredo falhar, resolver antes do POST. Esperado: 202, duplicate:false; aguardar o e-mail no Mailpit. Repetir **a última chamada curl, sem recriar o arquivo**: esperado duplicate:true e nenhum novo e-mail. O arquivo usa e-mail fictício, valor de teste e não contém telefone.

Para mostrar a proteção sem expor segredo:

```bash
curl -sS -o /dev/null -w 'HTTP %{http_code}\n' "$GANJJ_SUPPORT_URL/internal/notifications/00000000-0000-0000-0000-000000000000"
```

Esperado: 403. Não mostrar o valor de GANJJ_INTERNAL_SECRET nem um manifesto Secret na tela. Ao terminar, encerrar os port-forwards e limpar as variáveis privadas do Terminal.

## 5. Decisões e perguntas para defender

| Pergunta | Resposta curta |
|---|---|
| Por que um serviço de suporte separado? | Centraliza canais, retry e histórico; o Order não conhece os provedores. |
| Por que SQLite e uma réplica? | Histórico/idempotência simples num banco próprio; este desenho não foi preparado para múltiplos writers/Pods. |
| Como evitar perder o evento entre banco e broker? | O Order grava pedido e outbox na mesma transação; o worker tenta publicar depois. |
| Se chegar duas vezes? | A chave event_id é única; a segunda aceitação vira duplicate e não dispara canais de novo. |
| Se o provedor falhar? | Há até três tentativas com espera crescente; falha é registrada, os outros canais continuam. |
| Por que não mostrar WhatsApp real agora? | O modo trial foi preparado, mas falta ContentSid permitido disponível nesta configuração; o limite foi aceito e documentado. |
| O histórico sobrevive à recriação do Pod no EKS atual? | Não prometer isso: o filtro do kit troca PVC por emptyDir. A prova com PVC foi feita no Minikube. Não apagar o Pod nesta demo para fingir persistência. |

## 6. Evidências e extras

- [Pedido real/outbox/RabbitMQ → Mailpit no laboratório](evidence/2026-10-28/order-created.json).
- [SMS real recebido e duplicata](evidence/2026-10-29/sms-api-test.json). É evidência anterior, sem necessidade de novo disparo.
- [Imagem 1.0.3/Secret/checks no laboratório](evidence/2026-10-29/support-1.0.3-runtime.json).
- [Grafana histórico, extra](evidence/2026-10-25/grafana-support.jpg).
- [Guia operacional](entrega-local.md), [limites dos canais](canais-reais.md) e [aceite](aceite-final.md).

EKS, responsabilidade dos serviços e fluxo REST/RabbitMQ são o foco do roteiro consolidado. Grafana e Helm podem ser mostrados se houver tempo. Prints antigos devem ser identificados como antigos, não como nova execução no EKS.

## 7. Checklist e registro

- [ ] Grupo confirmou contexto, namespace e estado do ambiente EKS.
- [ ] Rodrigo preparou Product/Cart/cliente e o pedido da demonstração.
- [ ] Mailpit aberto e código do pedido reconhecível.
- [ ] Fluxo real e plano B claramente separados na fala.
- [ ] Repetição com a mesma chave/event_id sem novo e-mail.
- [ ] Nenhum token, senha ou segredo exposto na tela.
- [ ] Ensaio humano cronometrado e [registro-ensaio.md](registro-ensaio.md) preenchido.

A preparação do roteiro não encerra a issue #14. Não há registro de ensaio realizado neste arquivo.

## Modelos usados

A estrutura segue os roteiros dos colegas: demonstração com comando/resultado, decisões para defender, perguntas e checklist.

- [Runbook do Rodrigo — Order](https://github.com/Back-End-Cloud-Computing/order/blob/main/docs/PRESENTATION-RUNBOOK.md).
- [Roteiro do Promotion](https://github.com/Back-End-Cloud-Computing/promotion/blob/main/docs/apresentacao-n2.md).
- [Rubrica atual transcrita pelo grupo](https://github.com/Back-End-Cloud-Computing/promotion/blob/main/docs/rubrica-n2.md). O documento registra a fonte Canvas e a dúvida sobre a soma dos pontos; este guia não redefine a avaliação.
- [Kit compartilhado EKS](https://github.com/Back-End-Cloud-Computing/api-gateway/tree/main/eks).

Preparação escrita em 29/10/2026. Tempos abaixo são sugestão; tempos reais só entram no [registro de ensaio](registro-ensaio.md) depois da atividade com o grupo.
