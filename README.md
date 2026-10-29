# Serviço de Suporte (GANJJ)

Microsserviço de notificações: recebe um evento de negócio (hoje, pedido criado) e avisa o cliente por
e-mail, SMS e WhatsApp. É o "microsserviço de suporte" exigido pelo enunciado do professor.

- **Dono:** Eduardo (`eduardofabrii`)
- **Stack:** Java 21 + Spring Boot + SQLite, escolhida por Eduardo ([ADR 0006](docs/adr/0006-java-spring-boot.md)).
- **Porta:** 8090 · **Sem Ingress** (só Service interno, chamado direto pelo `order`)
- **Prazo:** apresentação N2 em 10/11/2026

## Como funciona

```
order → outbox → RabbitMQ (order.created) → support → e-mail SMTP
        └──── fallback HTTP com x-internal-secret ───┤→ SMS (fake ou Twilio)
                                                    └→ WhatsApp (fake ou Twilio)
```

O publisher real do `order` grava `OrderCreated` no outbox na mesma transação do pedido e publica no RabbitMQ. O support consome com idempotência por `event_id`; o HTTP interno permanece como fallback. O fluxo foi validado em 28/10 com order, SQL Server e RabbitMQ reais e um único e-mail no Mailpit ([evidência](docs/evidence/2026-10-28/order-created.json)). Cart/Product eram fixtures declaradas.

## Estado da entrega — 29/10/2026

O ambiente continuará **local**, com **Twilio trial e sem upgrade pago**, por decisão de Eduardo.

| Parte | Resultado comprovado |
|---|---|
| Support local | health, readiness e metrics: HTTP 200; rotas internas protegidas: 403 sem segredo/segredo errado e 404 com segredo correto para evento inexistente |
| SMS pelo support | API Twilio aceitou o envio, Eduardo confirmou recebimento no celular e a repetição do evento retornou `duplicate:true` |
| WhatsApp no Console | Mensagem `Received` e resposta automática `Delivered` no Try out WhatsApp |
| WhatsApp pelo support | **Não comprovado**: falta ContentSid permitido disponível no fluxo trial desta conta/configuração |
| Publicação | Imagem pública anterior `eduardofabri/support:1.0.2`; alterações 1.0.3 estão nesta branch e ainda não foram publicadas/deployadas |

Evidências: [runtime local](docs/evidence/2026-10-29/local-runtime.json) e [SMS real](docs/evidence/2026-10-29/sms-api-test.json). As execuções foram feitas por Eduardo no Terminal e os resultados sanitizados foram conferidos. `sent` no adapter significa aceitação pela API; neste teste, o recebimento do SMS foi confirmado separadamente pelo usuário.

O trial fornece modelos de teste e exige upgrade para criar templates próprios. Isso não significa que todo teste WhatsApp em trial precise pagar. A resposta automática do Console não comprova envio WhatsApp iniciado pela aplicação. A [proposta de atualização da issue #12](docs/atualizacao-issues-trial.md) explicita esse limite; não marcar o critério original como aprovado sem revisar o aceite. Os três ensaios do grupo continuam pendentes.

## Documentação

Comece por [`docs/README.md`](docs/README.md).

| Documento | O que responde |
|---|---|
| [PRD](docs/prd.md) | Problema, escopo, fora de escopo, critérios de aceite |
| [Arquitetura](docs/arquitetura.md) | Fluxo, modelo de dados, canais, configuração, métricas |
| [Contrato](docs/contrato-api.md) | Endpoint e payload que o `order` precisa respeitar |
| [Testes](docs/testes.md) | Casos definidos antes do código (TDD) |
| [Canais reais](docs/canais-reais.md) | Como trocar SMS e WhatsApp simulados por APIs reais |
| [RabbitMQ](docs/rabbitmq.md) | Fila, consumidor e configuração |
| [Fases](docs/fases.md) | Entregas com datas |
| [Integração](docs/integration-issues.md) | O que depende dos outros serviços |
| [Roteiro N2](docs/apresentacao-n2.md) | O que mostrar na apresentação |

## Rodar localmente

```bash
cp .env.example .env
./mvnw verify
./mvnw spring-boot:run     # http://localhost:8090
```

Mailpit (caixa de e-mail de teste): `docker run -p 8025:8025 -p 1025:1025 axllent/mailpit`, UI em `http://localhost:8025`.

### Carregar a configuração privada Twilio

O JSON privado não é importado automaticamente pelo Spring. O carregador abaixo valida a configuração sem rede e sem imprimir credenciais:

```bash
python3 scripts/twilio-local.py --config /caminho/privado/TWILIO-CONFIGURAR.json --channel sms
```

Acrescentar `--run` inicia o JAR `target/support-1.0.3.jar` (Java 21+, compilar com `./mvnw package`). O laboratório usa `127.0.0.1:8095`, banco separado ao lado do JSON, segredo interno privado e AMQP desligado. No modo SMS, e-mail/WhatsApp ficam em fake. O comando de inicialização não cria eventos; chamadas posteriores ao endpoint podem enviar SMS reais.

O JSON precisa de permissão 0600 e dos campos de credenciais/remetentes descritos em [ativação Twilio](docs/ativacao-twilio.md). `--channel whatsapp`/`both` no modo trial exige `TWILIO_WHATSAPP_CONTENT_SID` válido (`HX...`); o Account SID (`AC...`) não serve nesse campo. Tokens, senhas, números de destinatários e o segredo interno ficam fora do Git e das issues.

### Integração com os colegas no ambiente local

`authorization.ganjj-n2.svc:8081` é DNS interno do namespace Kubernetes do laboratório, não um endereço público. Serviços em outro computador precisam executar authorization no próprio ambiente ou combinar acesso à rede de laboratório; `localhost` sempre aponta para a própria máquina. Não foi criada hospedagem pública.

Eduardo confirmou que compartilhou as credenciais ADMIN de teste por canal privado com os responsáveis por promotion/gateway. Elas não devem ser adicionadas a este README, ao Git ou às issues. A atualização de status remoto está [preparada](docs/atualizacao-issues-trial.md).

## Convenções

Commits em Conventional Commits, PR por mudança, autor único. Ver [`docs/convencoes.md`](docs/convencoes.md).
Depois de clonar: `lefthook install`.

## Entrega e operação

`./mvnw verify` executa build, análise do compilador e testes JUnit. Copie `.env.example` para `.env`; Spring importa esse arquivo local. Não versione `.env`. Para persistência local, use `DB_PATH=./data/support.db`.

O worker consulta eventos persistidos a cada 250 ms e grava o resultado por canal. Notificações aceitas antes de um reinício continuam pendentes no SQLite. Consulte os limites de entrega no ADR 0006.

Veja [Kubernetes](k8s/README.md), [Helm](helm/support/README.md), [dashboard](k8s/observability/README.md) e [estado N2](docs/n2-status.md). O workflow CI publica no GHCR ao entrar em `main`; a release manual Docker Hub requer secrets de Actions com acesso a `eduardofabri/support`.

Com o serviço configurado com e-mail SMTP e **SMS/WhatsApp em fake**, e com Mailpit no ar, execute `INTERNAL_SECRET=... python3 scripts/smoke-test.py`. `SUPPORT_URL` e `MAILPIT_URL` permitem usar portas diferentes. O teste usa destinatário fictício e deve apontar para o Mailpit de teste.

## Histórico da validação de 25/10/2026

Instalações Helm e Kustomize exercitadas em Minikube; imagem pública `eduardofabri/support:1.0.1` com AMD64/ARM64. O pedido do order real chegou ao Mailpit e o PVC preservou o histórico após recriar o pod. Scripts reproduzíveis e fixtures identificadas em `scripts/n2/`; capturas e resultados sem tokens em `docs/evidence/2026-10-25/`.

Consumidor RabbitMQ ativado por `AMQP_URL`. Canais `twilio` disponíveis por configuração, com requisitos e limites em `docs/canais-reais.md`. Naquela data ainda não havia conta nem envio real Twilio. O SMS real foi comprovado em 29/10, conforme a seção de estado atual acima; `fake` continua sendo o padrão do repositório.
