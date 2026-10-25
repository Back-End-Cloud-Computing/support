# Fases — Serviço de Suporte

Cada fase é entregável sozinha e ancorada numa data da disciplina. Data deste plano: 19/10/2026.

| Fase | Quando | Entrega |
|---|---|---|
| 0 Fundação | 19/10 | Repo, docs, issues, scaffold com CI, lefthook e Dockerfile |
| 1 Núcleo | até 24/10 | Endpoint com segredo, validação, idempotência em SQLite, adapters fake, testes verdes (H, D, C, S) |
| 2 E-mail, Docker e Kubernetes | até 26/10 | SMTP real com Mailpit, imagem `eduardofabri/support` multi-arch, manifests com volume, 1 réplica com probes |
| 3 Distribuição | 28/10 a 31/10 | Chart Helm (após a aula de 27/10) |
| 4 Observabilidade | 04/11 a 07/11 | `/metrics`, scrape pelo Prometheus, painel Grafana em JSON (após 03/11) |
| 5 Ensaio | 08/11 a 09/11 | Demonstração completa com o `order` chamando |
| N2 | **10/11/2026** | Apresentação |
| Depois | após os demais repos | RabbitMQ ([rabbitmq.md](rabbitmq.md)) e canais reais ([canais-reais.md](canais-reais.md)) |

## Por fase: pronto quando

- **1:** `./mvnw verify` verde; `curl` local do exemplo do contrato retorna 202 e o log mostra os três canais.
- **2:** e-mail aparece na UI do Mailpit no cluster; pod reiniciado mantém o histórico (volume).
- **3:** `helm install` sobe o mesmo estado que `kubectl apply -k k8s/`.
- **4:** painel mostra envios por canal e status durante a demo.
- **5:** `order` real cria um pedido e o e-mail chega, ou, se o `order` não estiver pronto, `curl` imita a chamada.

## Dependências de outras pessoas

Ver [integration-issues.md](integration-issues.md). As fases 1 a 4 não dependem de ninguém; a fase 5 depende do
`order` (SUP-001).
