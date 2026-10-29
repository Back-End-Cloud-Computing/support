# Ativar Twilio e comprovar um envio real

Os adapters estão implementados e testados contra HTTP simulado. A equipe ainda não possui conta (confirmado por Eduardo em 28/10/2026), portanto a issue #12 permanece aberta. Não registrar envio real antes de haver conta, remetentes habilitados, destinatários autorizados e recebimento confirmado.

## Configuração da conta

1. Criar a conta no [Twilio Console](https://console.twilio.com/) e concluir a verificação da conta e do telefone.
2. Habilitar SMS e o ambiente de teste WhatsApp oferecido à conta. Seguir os passos do Console para vincular o telefone destinatário; usar o remetente exibido, sem copiar números de tutoriais antigos.
3. Conferir as restrições da conta: trial pode limitar destinatários e conteúdo. A integração usa texto de pedido próprio e, para WhatsApp com ContentSid, um template com duas variáveis: 1=assunto e 2=texto. Esse template precisa ser permitido/aprovado para a conta. Credenciais de teste da API e HTTP simulado não comprovam entrega real.
4. Guardar Account SID e Auth Token em ambiente privado. Não enviar pelo chat, issue, PR ou commit.

Referências oficiais consultadas em 28/10/2026: [trial](https://www.twilio.com/docs/usage/tutorials/how-to-use-your-free-trial-account), [WhatsApp quickstart](https://www.twilio.com/docs/whatsapp/quickstart), [teste WhatsApp da conta trial](https://www.twilio.com/docs/usage/trials/try-out-whatsapp).

## Secret no namespace de teste

Em terminal privado, ler as duas credenciais sem colocá-las no histórico:

```bash
read -r -p 'Account SID: ' TWILIO_ACCOUNT_SID
read -r -s -p 'Auth Token: ' TWILIO_AUTH_TOKEN; printf '\n'
export TWILIO_ACCOUNT_SID TWILIO_AUTH_TOKEN
KUBECTL=/tmp/ganjj-k ./k8s/criar-twilio-credentials.sh -n ganjj-acceptance-audit
unset TWILIO_ACCOUNT_SID TWILIO_AUTH_TOKEN
```

O helper escreve arquivos temporários privados e aplica o Secret support-twilio. O chart já referencia account-sid/auth-token por secretKeyRef.

## Selecionar o provedor

Na release de teste, manter os valores já usados para banco, Secret interno e RabbitMQ e configurar:

```yaml
channels:
  sms: twilio
  whatsapp: twilio
twilio:
  existingSecret: support-twilio
  smsFrom: "<remetente SMS habilitado em E.164>"
  whatsappFrom: "whatsapp:<remetente WhatsApp exibido no Console>"
  whatsappContentSid: "<ContentSid do template permitido, se necessário>"
```

Esse trecho é um arquivo de override; as credenciais continuam somente no Secret. Sem ContentSid, o adapter usa Body: confirmar que a conta e a janela de conversa permitem esse texto antes de enviar. Não alterar o padrão fake do desenvolvimento/CI.

## Prova para fechar a issue

- Enviar um OrderCreated com event_id novo, customer_phone E.164 de destinatário autorizado e x-internal-secret pelo endpoint interno ou pelo publisher real do order.
- Consultar GET /internal/notifications/{event_id}; SMS e WhatsApp devem mostrar provider=twilio e status=sent.
- Conferir no Console o estado final e confirmar recebimento no aparelho. sent no support significa API aceitou a requisição, não entrega final.
- Repetir o mesmo event_id e confirmar duplicate:true, sem nova mensagem.
- Registrar data, ambiente e resultados sanitizados; omitir tokens e números completos. Só então fechar #12.

Para voltar ao laboratório sem envios externos, restaurar channels.sms=fake e channels.whatsapp=fake na release.
