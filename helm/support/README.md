# Helm

Crie `support-credentials` com `k8s/criar-credentials.sh` no mesmo namespace antes de instalar.

```bash
helm lint helm/support
helm upgrade --install support helm/support --wait --timeout 5m
```

A imagem deve estar publicada previamente. Use `image.repository` e `image.tag` para outra imagem. O estado corresponde ao Kustomize: uma réplica, PVC, Mailpit e Services internos. Os nomes dos Services seguem o contrato, então use uma release por namespace. Para SMTP externo, use `mailpit.enabled=false` e configure `smtpHost`. O chart não inclui segredos nem Ingress.
