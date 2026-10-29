#!/usr/bin/env bash
# Recebe credenciais pelo ambiente; nunca por flags, manifest ou saída de terminal.
set +x
set -euo pipefail
umask 077
: "${TWILIO_ACCOUNT_SID:?Defina TWILIO_ACCOUNT_SID no ambiente privado}"
: "${TWILIO_AUTH_TOKEN:?Defina TWILIO_AUTH_TOKEN no ambiente privado}"
if [[ ! "$TWILIO_ACCOUNT_SID" =~ ^AC[0-9a-fA-F]{32}$ ]]; then
  echo 'TWILIO_ACCOUNT_SID inválido (esperado AC + 32 caracteres hexadecimais)' >&2
  exit 1
fi
private_dir="$(mktemp -d)"
trap 'rm -f "$private_dir/account-sid" "$private_dir/auth-token"; rmdir "$private_dir"' EXIT
printf '%s' "$TWILIO_ACCOUNT_SID" > "$private_dir/account-sid"
printf '%s' "$TWILIO_AUTH_TOKEN" > "$private_dir/auth-token"
"${KUBECTL:-kubectl}" "$@" create secret generic support-twilio \
  --from-file=account-sid="$private_dir/account-sid" \
  --from-file=auth-token="$private_dir/auth-token" \
  --dry-run=client -o yaml | "${KUBECTL:-kubectl}" "$@" apply -f -
