# Canais reais — plano para SMS e WhatsApp

Na N2, SMS e WhatsApp são **simulados** (adapter `fake`: grava em log e conta na métrica). Este documento
deixa o caminho para trocar por APIs reais sem mexer no resto do serviço.

## O que já está pronto

- Interface `Notifier` ([arquitetura](arquitetura.md#canais-e-adapters)): um adapter por canal e provedor.
- Seleção por variável de ambiente (`SMS_PROVIDER`, `WHATSAPP_PROVIDER`).
- Retry, registro em `deliveries` com o campo `provider`, métricas por canal e status.
- Telefone em E.164 validado na entrada.

## Passos para ligar um provedor real

1. Escolher o provedor (candidatos abaixo) e abrir conta de teste/sandbox.
2. Criar o adapter em `internal/notify/<provedor>_sms.go` (e `_whatsapp.go`) implementando `Notifier`.
3. Registrar o nome do provedor na lista aceita pela configuração (`internal/config`).
4. Colocar credenciais em **Secret** do Kubernetes (nunca em ConfigMap, nunca no repo) e documentar as
   variáveis no `.env.example` sem valores.
5. Teste do adapter contra um servidor HTTP falso (sem chamar o provedor); depois um envio manual em sandbox.
6. Trocar `SMS_PROVIDER` / `WHATSAPP_PROVIDER` no ConfigMap.

## Candidatos

| Provedor | SMS | WhatsApp | Observações |
|---|---|---|---|
| Twilio | sim | sim (sandbox gratuito) | API REST simples, bom para demonstração; número de teste e saldo de avaliação |
| Zenvia | sim | sim | Provedor brasileiro, mais próximo do uso real no Brasil |
| Meta WhatsApp Cloud API | não | sim | Exige conta Business e template aprovado para mensagem iniciada pela empresa |

Variáveis previstas (exemplo Twilio): `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` (Secret), `TWILIO_SMS_FROM`,
`TWILIO_WHATSAPP_FROM` (ConfigMap).

## Cuidados

- Custo: provedor real cobra por mensagem; manter `fake` em desenvolvimento e CI.
- WhatsApp: mensagem iniciada pela empresa só com template aprovado; o texto do `OrderCreated` precisará virar template.
- LGPD: telefone e e-mail são dado pessoal; não registrar o telefone completo em log (mascarar) e não guardar além do necessário.
- Limites do provedor: respeitar taxa de envio; o retry com espera crescente já ajuda.
