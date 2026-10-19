# ADR 0003 — SQLite em volume para histórico e idempotência

Status: Aceito
Data: 2026-10-19

## Contexto

Idempotência por `event_id` exige armazenamento durável: em memória, reiniciar o pod apaga o registro e permite
reenvio. Opções: SQLite em volume, Postgres, Redis, memória.

## Decisão

SQLite em arquivo (`/data/support.db`) num PVC de 100 Mi, 1 réplica. `event_id` é chave primária de `events`;
`deliveries` tem `UNIQUE (event_id, channel)`.

Descartados: Postgres (mais um pod, Secret e migration para um volume baixíssimo); Redis (o `SETNX` serve para
idempotência, mas perde o histórico de entregas e o `shopping-cart` já o usa para outro fim); memória (não é idempotente
após reinício).

## Consequências

- Uma réplica só: SQLite em arquivo não é compartilhado com segurança entre pods. Estratégia de atualização `Recreate`.
- Upgrade para Postgres é local à camada `internal/store`; a interface não muda.
- Backup e retenção fora de escopo na N2.
