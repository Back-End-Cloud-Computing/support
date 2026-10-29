# Atualizações de issues preparadas em 29/10

Estes textos estão preparados localmente. Nenhuma issue remota foi editada por esta atualização.

## authorization #8 — registrar compartilhamento e encerrar a pendência

Texto preparado:

> O endpoint de chave pública e o login ADMIN foram validados no laboratório. Eduardo confirmou em 29/10 que compartilhou as credenciais de teste por canal privado com os responsáveis por promotion e gateway. O ambiente continuará local: o DNS do laboratório não é uma URL pública. Nenhuma credencial deve constar nesta issue.
>
> A pendência de compartilhamento registrada na auditoria foi atendida.

A evidência técnica anterior e a confirmação do usuário atendem à pendência registrada. Publicar o registro/atualizar o estado remoto quando o GitHub estiver acessível, conferindo o corpo vigente da issue para não substituir requisitos desconhecidos.

## support #12 — atualizar escopo para trial

Texto preparado:

> Entrega local com Twilio trial, sem upgrade pago, conforme decisão de Eduardo.
>
> - SMS enviado pelo support, aceito pela API e recebido no celular (confirmação do usuário). Reenvio do mesmo evento reconhecido como duplicata.
> - WhatsApp: mensagem recebida e resposta automática entregue no ambiente Try out WhatsApp do Console.
> - Adapter WhatsApp e modo de template trial implementados. Envio WhatsApp iniciado pelo support NÃO foi comprovado: falta ContentSid permitido disponível para esta conta/configuração.
> - Nesta entrega trial, a prova de envio WhatsApp pelo support fica explicitamente fora do aceite concluído; não deve ser marcada como teste aprovado.
> - Credenciais ficam privadas. Runtime local validado. Ajustes 1.0.3 ainda precisam ser enviados/publicados; a imagem pública anterior é 1.0.2.
>
> Evidências locais na branch codex/twilio-trial: docs/evidence/2026-10-29/sms-api-test.json e local-runtime.json.

Alteração necessária no aceite: separar SMS real comprovado de WhatsApp preparado/limitado no trial. Se a issue mantiver o requisito original de ambos os canais reais pela aplicação, ela continua parcialmente pendente. Só encerrar conforme o escopo revisado registrado; não substituir o teste WhatsApp pela resposta automática sem declarar a mudança.

## Ensaios

client #7, authorization #7 e support #14 permanecem pendentes de ensaio real com o grupo, por último. Roteiros e validações técnicas não substituem essa atividade.
