# Evidências — 25/10/2026

- `*-image.json`: imagem, digest, plataformas e teste de acesso público anônimo.
- `cluster.json`: versões, réplicas e imagens efetivas no Minikube.
- `auth-client.json`: chave, assinatura, papéis, perfil e rejeição de tokens.
- `gateway.json` e `promotion-jwt.json`: integrações JWT com serviços reais.
- `order-support.json`: pedido real com SQL Server, idempotência e e-mail recebido no Mailpit; Cart/Product eram fixtures explícitas.
- `kustomize.json`: instalação com kubectl apply -k, SMTP e histórico preservado após recriar o pod.
- `*-tests.json`: resumos JUnit sem logs sensíveis.
- `grafana-*.jpg`: capturas dos dashboards realmente consultados.

Nenhuma senha, token de acesso, segredo interno ou chave privada está incluída. Os arquivos de credenciais de teste ficam fora do Git, em diretório local privado e Secrets do namespace. Scripts em `scripts/n2/` documentam os testes e os endpoints de validação.
