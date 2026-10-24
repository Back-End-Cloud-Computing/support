# Documentação — Serviço de Suporte (GANJJ)

Escrita para ser lida na ordem abaixo. Fluxo de trabalho: pergunta, decisão, documento, só então código.

## Índice

| Documento | O que responde |
|---|---|
| [PRD](prd.md) | Problema, escopo, fora de escopo, critérios de aceite |
| [Arquitetura](arquitetura.md) | Fluxo, modelo de dados, canais, configuração, métricas |
| [Contrato](contrato-api.md) | Endpoint, payload, respostas, quem chama |
| [Testes](testes.md) | Casos de teste definidos antes da implementação |
| [Canais reais](canais-reais.md) | Plano para SMS e WhatsApp reais |
| [RabbitMQ](rabbitmq.md) | Plano da fila e do consumidor |
| [Fases](fases.md) | Entregas e datas |
| [Integração](integration-issues.md) | Dependências dos outros serviços e riscos |
| [Convenções](convencoes.md) | Commits, branches, PRs, autoria, idioma |
| [Roteiro N2](apresentacao-n2.md) | Demonstração, decisões para defender, perguntas prováveis |
| ADRs | [0001 Go provisório (substituído)](adr/0001-go-provisorio.md) · [0002 HTTP antes de RabbitMQ](adr/0002-http-antes-de-rabbitmq.md) · [0003 SQLite](adr/0003-sqlite.md) · [0004 Canais por adapter](adr/0004-canais-por-adapter.md) · [0005 Destinatário no payload](adr/0005-destinatario-no-payload.md) · [0006 Java/Spring Boot](adr/0006-java-spring-boot.md) |

## Datas que não se movem

| Data | Evento |
|---|---|
| 27/10 | Aula de Helm |
| 03/11 | Aula de Observabilidade (Prometheus e Grafana) |
| **10/11/2026** | **N2 — apresentação em equipe, 40% da nota** |

Rubrica da N2: R1 arquitetura distribuída em Kubernetes (4,0) · R2 segurança, distribuição e
observabilidade (3,0) · R3 apresentação técnica (3,0).

## Pendências fora do código

- [x] Stack Java 21/Spring Boot confirmada por Eduardo em 24/10 (ADR 0006).
- [ ] Rodrigo (`order`) combinar o payload e o segredo interno ([SUP-001](integration-issues.md)).
- [ ] Conta `bascratki` no Docker Hub com o repositório `support` público.
