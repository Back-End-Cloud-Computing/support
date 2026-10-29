# Canais reais — entrega com Twilio trial

Estado em 29/10/2026. Eduardo decidiu manter o ambiente local e usar o trial, sem contratar upgrade.

## Resultado comprovado

- SMS: enviado pela aplicação support, aceito pela API Twilio e recebido no celular, conforme confirmação de Eduardo. O mesmo event_id foi reenviado e retornou duplicate:true.
- WhatsApp: o Console registra mensagem Received e resposta automática Delivered após corrigir o formato do destinatário. Não foi comprovado envio WhatsApp pela aplicação.
- Runtime local: health, readiness e metrics retornaram 200; rota interna sem segredo e com segredo errado retornou 403; segredo correto para evento inexistente retornou 404.

Evidências: [SMS real](evidence/2026-10-29/sms-api-test.json) e [runtime local](evidence/2026-10-29/local-runtime.json). As execuções foram feitas por Eduardo no Terminal e os resultados sanitizados foram conferidos pelo agente.

## Configuração implementada

A implementação Java usa um adapter por canal, seleção por SMS_PROVIDER/WHATSAPP_PROVIDER, registro persistente das entregas, retry e métricas. Os defaults continuam fake para desenvolvimento/CI.

TwilioNotifier usa HTTPS, Basic Auth, formulário URL encoded, timeout e redirecionamentos desativados. Os testes do adapter usam HTTP simulado; isso não prova entrega externa. A execução mais recente desses testes foi bloqueada pelo sandbox ao abrir sockets, conforme a auditoria.

O SMS trial da versão 1.0.3 permite TWILIO_SMS_TRIAL_TEMPLATE=sms_order_confirmation. O modelo fixo foi usado para comprovar transporte; ele não comprova recebimento de detalhes personalizados do pedido.

WhatsApp suporta ContentSid. Fora do modo trial, o template personalizado usa as variáveis 1=assunto e 2=texto. TWILIO_WHATSAPP_TRIAL_MODE=true exige ContentSid válido e omite ContentVariables. O trial permite apenas templates fornecidos pelo Twilio; a biblioteca da conta exibiu upgrade para templates próprios e o fluxo Try out WhatsApp não disponibilizou o identificador necessário nesta configuração. Isso não significa que todo teste WhatsApp em trial exija pagamento.

## Limite atual e proposta de aceite

Eduardo decidiu continuar com trial. O envio WhatsApp pela aplicação permanece sem comprovação. A proposta de atualização do requisito da issue #12 deve explicitar: integração preparada e resposta automática comprovada no Console; envio iniciado pelo support e conteúdo personalizado ficam como limitação pendente do canal.

A issue remota ainda precisa receber o escopo atualizado antes de ser tratada como concluída no escopo trial. Não marcar o critério original de entrega real WhatsApp como aprovado. Texto preparado em [atualização das issues](atualizacao-issues-trial.md).

## Execução e credenciais

Seguir [ativação](ativacao-twilio.md). scripts/twilio-local.py lê o JSON privado e permite validar/iniciar SMS separadamente do template WhatsApp. O laboratório isolado não consome a fila antiga. Credenciais, destinatários e segredo interno ficam fora do Git.

A versão 1.0.3 e os ajustes estão na branch própria codex/twilio-trial; não foram publicados/deployados. A imagem pública anterior permanece 1.0.2.

sent no adapter significa aceitação na API, não recebimento no aparelho. No teste SMS desta entrega, Eduardo confirmou também o recebimento. Não repetir o disparo sem necessidade.
