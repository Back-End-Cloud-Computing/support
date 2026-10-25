#!/usr/bin/env bash
# Senhas entram pelo ambiente e por arquivos temporários privados, nunca pelo Git.
set -euo pipefail
umask 077
: "${INTERNAL_SECRET:?Defina INTERNAL_SECRET no ambiente antes de executar}"
secret_dir="$(mktemp -d)"
trap 'rm -rf "$secret_dir"' EXIT
printf '%s' "$INTERNAL_SECRET" > "$secret_dir/internal-secret"
kubectl create secret generic support-credentials \
  --from-file=internal-secret="$secret_dir/internal-secret" \
  --dry-run=client -o yaml | kubectl apply -f -
