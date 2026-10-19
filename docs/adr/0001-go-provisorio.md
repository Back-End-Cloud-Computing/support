# ADR 0001 — Go como stack provisória

Status: Aceito (provisório, Eduardo pode trocar)
Data: 2026-10-19

## Contexto

O suporte é do Eduardo, que já mantém o `client` (Go) e o `authorization` (Java). O professor aceita qualquer
stack. O Lucas precisava deixar estrutura e testes definidos antes de o Eduardo decidir.

## Decisão

Go com `net/http` da biblioteca padrão, `database/sql` com SQLite (`modernc.org/sqlite`, sem CGO), `net/smtp` para
e-mail e `prometheus/client_golang` para métricas. Go foi escolhido por: mesma stack do `client` do Eduardo, binário
pequeno (imagem enxuta), concorrência simples para o envio assíncrono e cliente RabbitMQ maduro (`amqp091-go`) para a fase futura.

## Consequências

- O **contrato** (rotas, payload, status, métricas, variáveis) é a parte estável; a linguagem não é.
- Se o Eduardo preferir Java/Spring (Actuator já expõe Prometheus) ou outra, só `cmd/`, `internal/`, Dockerfile e CI mudam.
- Sem CGO no SQLite, a imagem multi-arch (amd64 e arm64) compila sem toolchain extra.
