# Canais reais — plano para SMS e WhatsApp

Na N2, SMS e WhatsApp são **simulados** (adapter `fake`: grava em log e conta na métrica). Este documento
deixa o caminho para trocar por APIs reais sem mexer no resto do serviço.

## O que já está pronto

- Interface `Notifier` ([arquitetura](arquitetura.md#canais-e-adapters)): um adapter por canal e provedor.
- Seleção por variável de ambiente (`SMS_PROVIDER`, `WHATSAPP_PROVIDER`).
- Retry, registro em `deliveries` com o campo `provider`, métricas por canal e status.
- Telefone em E.164 validado na entrada.

## Ativação do Twilio

A implementação Java já está pronta; não é necessário criar outro adapter. Usar o [passo a passo de ativação e aceite](ativacao-twilio.md), com helper para criar o Secret e conferir a entrega real. A conta trial foi criada em 28/10; o teste real e a vinculação de WhatsApp ainda precisam ser comprovados. A issue #12 permanece aberta.

## Cuidados

- Custo: provedor real cobra por mensagem; manter `fake` em desenvolvimento e CI.
- WhatsApp: mensagem iniciada pela empresa só com template aprovado; o texto do `OrderCreated` precisará virar template.
- LGPD: telefone e e-mail são dado pessoal; não registrar o telefone completo em log (mascarar) e não guardar além do necessário.
- Limites do provedor: respeitar taxa de envio; o retry com espera crescente já ajuda.

## Implementação Twilio preparada em 25/10/2026

`TwilioNotifier` usa a [Messages API oficial](https://www.twilio.com/docs/messaging/api/message-resource), com HTTPS, autenticação Basic, formulário URL encoded, timeout e redirecionamentos desativados. Os testes usam servidor HTTP local e não contatam o provedor.

Para selecionar `twilio`, definir `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` e o remetente do canal. A aplicação recusa iniciar se faltarem as configurações necessárias. As credenciais entram no Secret `support-twilio` (chaves `account-sid` e `auth-token`) e os remetentes em `values.yaml`/ambiente. Os defaults continuam `fake`.

WhatsApp suporta `TWILIO_WHATSAPP_CONTENT_SID` com variáveis `1=assunto` e `2=texto` para um template aprovado. Sem template usa Body, destinado ao sandbox/janela permitida da conversa. A conta, remetentes, template e autorização dos destinatários precisam ser configurados antes do teste real. Nenhuma mensagem real foi enviada: Eduardo informou que a equipe ainda não tem conta.

O status `sent` de um adapter Twilio significa que a API aceitou a solicitação; não equivale a uma confirmação de entrega no aparelho. Confirmar entrega final exige callbacks/status do provedor.

O modo SMS trial da versão 1.0.3 usa o modelo permitido sms_order_confirmation quando configurado; detalhes e limitações em [ativacao-twilio.md](ativacao-twilio.md). A criação da conta não comprova entrega real e não encerra a issue.
