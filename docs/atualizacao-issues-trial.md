# Atualizações de issues preparadas em 29/10

Os comentários foram registrados no GitHub em 29/10. Authorization #8 foi encerrada após conferir os critérios. Support #12 registra SMS comprovado e a limitação WhatsApp trial; permanece vinculada à revisão da entrega, sem afirmar envio WhatsApp pela aplicação.

## authorization #8 — registrar compartilhamento e encerrar a pendência

Texto preparado:

> O endpoint de chave pública e o login ADMIN foram validados no laboratório. Eduardo confirmou em 29/10 que compartilhou as credenciais de teste por canal privado com os responsáveis por promotion e gateway. O ambiente continuará local: o DNS do laboratório não é uma URL pública. Nenhuma credencial deve constar nesta issue.
>
> A pendência de compartilhamento registrada na auditoria foi atendida.

A evidência técnica anterior e a confirmação do usuário atendem à pendência registrada. O corpo vigente foi conferido: endpoint no cluster e credenciais combinadas fora do repositório. Ambos foram atendidos; [issue #8 encerrada](https://github.com/Back-End-Cloud-Computing/authorization/issues/8).

## support #12 — atualizar escopo para trial

Texto preparado:

> Entrega local com Twilio trial, sem upgrade pago, conforme decisão de Eduardo.
>
> - SMS enviado pelo support, aceito pela API e recebido no celular (confirmação do usuário). Reenvio do mesmo evento reconhecido como duplicata.
> - WhatsApp: mensagem recebida e resposta automática entregue no ambiente Try out WhatsApp do Console.
> - Adapter WhatsApp e modo de template trial implementados. Envio WhatsApp iniciado pelo support NÃO foi comprovado: falta ContentSid permitido disponível para esta conta/configuração.
> - Nesta entrega trial, a prova de envio WhatsApp pelo support fica explicitamente fora do aceite concluído; não deve ser marcada como teste aprovado.
> - Credenciais ficam privadas e o Secret local foi aplicado. Runtime local validado; imagem 1.0.3 publicada e branch enviada, sem merge automático na main.
>
> Evidências locais na branch codex/twilio-trial: docs/evidence/2026-10-29/sms-api-test.json e local-runtime.json.

Alteração necessária no aceite: separar SMS real comprovado de WhatsApp preparado/limitado no trial. Se a issue mantiver o requisito original de ambos os canais reais pela aplicação, ela continua parcialmente pendente. Só encerrar conforme o escopo revisado registrado; não substituir o teste WhatsApp pela resposta automática sem declarar a mudança.

## Ensaios

client #7, authorization #7 e support #14 permanecem pendentes de ensaio real com o grupo, por último. Roteiros e validações técnicas não substituem essa atividade.
