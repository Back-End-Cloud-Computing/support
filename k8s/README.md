# Kubernetes

Defina `INTERNAL_SECRET` por um canal seguro e execute `./k8s/criar-credentials.sh` no namespace de teste. Publique a imagem `eduardofabri/support:1.0.1` (ou ajuste a referência para uma imagem disponível).

```bash
kubectl apply -k k8s/
kubectl rollout status deployment/support
kubectl port-forward svc/support 8090:8090
kubectl port-forward svc/mailpit 8025:8025
```

O serviço não tem Ingress. Acesse a UI do Mailpit em http://localhost:8025 e envie o payload de `docs/contrato-api.md` com o mesmo segredo. Depois de confirmar três entregas, reinicie o pod e consulte o mesmo `event_id`: o histórico deve permanecer. O Secret deve ser compartilhado com o `order` fora do Git.

O PVC de 100Mi precisa de uma StorageClass padrão. Uma réplica com `Recreate` evita workers concorrentes sobre o SQLite. Não escale este Deployment sem mudar a estratégia de coordenação. E-mails do Mailpit são de teste e ficam no armazenamento efêmero do Mailpit; o histórico de entregas do support fica no PVC.
