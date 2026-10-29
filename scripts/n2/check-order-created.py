#!/usr/bin/env python3
"""Prova order real -> outbox SQL -> RabbitMQ -> support -> Mailpit, com HTTP fallback indisponível.
Requer a release do laboratório e port-forwards descritos na documentação.
"""
from pathlib import Path
from datetime import datetime,timezone
import base64,hashlib,json,os,secrets,subprocess,time,uuid,urllib.request,urllib.error
private=Path(os.environ.get('GANJJ_SECRET_DIR','/tmp/ganjj-n2-secrets'))
k=os.environ.get('GANJJ_KUBECTL','/tmp/ganjj-k');ns='ganjj-acceptance-audit'
def call(base,path,body=None,headers=None):
 request=urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**(headers or {})})
 try:
  with urllib.request.urlopen(request,timeout=30) as r:return r.status,r.read().decode()
 except urllib.error.HTTPError as r:return r.code,r.read().decode()
def kubectl(*args):return subprocess.check_output([k,'-n',ns,*args],text=True)
secret=lambda name:json.loads(kubectl('get','secret',name,'-o','json'))['data']['internal-secret']
assert base64.b64decode(secret('order-secrets'))==base64.b64decode(secret('audit-support-credentials'))
deployment=json.loads(kubectl('get','deployment','order','-o','json'))
env={x['name']:x.get('value') for x in deployment['spec']['template']['spec']['containers'][0]['env']}
assert env['Services__Support__BaseUrl']=='http://127.0.0.1:1','Este teste exige fallback HTTP indisponível'
assert env['RabbitMq__Enabled']=='true'
state={'email':'order-amqp-'+uuid.uuid4().hex+'@example.com','password':secrets.token_urlsafe(32)+'!'}
status,_=call('http://localhost:18381','/auth/register',state);assert status==201,('customer registration',status)
identity=private/'order-created-identity.json';identity.write_text(json.dumps(state));identity.chmod(0o600)
status,raw=call('http://localhost:18381','/auth/login',{'email':state['email'],'password':state['password']});assert status==200,('customer login',status)
token=json.loads(raw)['accessToken']
status,pem=call('http://localhost:18381','/auth/public-key');assert status==200
assert pem.strip()==(private/'public.pem').read_text().strip()
status,raw=call('http://localhost:18381','/auth/login',{'email':'admin@ganjj.com','password':(private/'auth-admin').read_text()});assert status==200,('ADMIN login',status)
assert json.loads(base64.urlsafe_b64decode(json.loads(raw)['accessToken'].split('.')[1]+'=='))['role']=='ADMIN'
body={'endereco_entrega':{'rua':'Rua de teste','numero':'1','bairro':'Centro','cidade':'Sao Paulo','estado':'SP','cep':'01310-100','pais':'BR'}}
key='amqp-real-'+str(uuid.uuid4());headers={'Authorization':'Bearer '+token,'Idempotency-Key':key,'X-Correlation-ID':key}
status,raw=call('http://localhost:18384','/api/pedidos',body,headers);assert status==201,(status,raw)
order=json.loads(raw);order_id=str(uuid.UUID(order['id']))
status,raw=call('http://localhost:18384','/api/pedidos',body,headers);assert status==201 and json.loads(raw)['id']==order_id
query="SET NOCOUNT ON; SELECT Id,EventType,CASE WHEN PublishedAt IS NULL THEN 0 ELSE 1 END AS Published FROM OutboxMessages WHERE JSON_VALUE(Payload,'$.order_id')='"+order_id+"' FOR JSON PATH;"
command='SQLCMDPASSWORD="$MSSQL_SA_PASSWORD" /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -d GanjjSupportOutbox20261028 -h -1 -y 4000 -w 65535 -Q '+__import__('shlex').quote(query)
for _ in range(100):
 output=subprocess.check_output(['docker','exec','ganjj-n2-sqlserver','bash','-lc',command],text=True)
 rows=json.loads(output.strip());created=[r for r in rows if r['EventType']=='OrderCreated']
 if created and created[0]['Published']==1:break
 time.sleep(.25)
else:raise AssertionError('OrderCreated ainda não publicado pelo outbox')
event=str(uuid.UUID(created[0]['Id']));secret_headers={'x-internal-secret':(private/'internal').read_text()}
for _ in range(100):
 status,raw=call('http://localhost:18391','/internal/notifications/'+event,headers=secret_headers)
 if status==200:
  deliveries=json.loads(raw)['deliveries']
  if len(deliveries)==3:break
 time.sleep(.25)
else:raise AssertionError('Evento AMQP não persistido/entregue no support')
assert any(x['channel']=='email' and x['provider']=='smtp' and x['status']=='sent' for x in deliveries)
time.sleep(3)
status,raw=call('http://localhost:18325','/api/v1/messages');assert status==200
count=sum(m['Subject']=='Pedido '+order['codigo']+' confirmado' for m in json.loads(raw)['messages']);assert count==1,count
contract=Path(__file__).parents[3]/'order/docs/contrato-api.md'
# Sibling checkout contracts must agree byte-for-byte in this workspace.
assert contract.read_bytes()==(Path(__file__).parents[2]/'docs/contrato-api.md').read_bytes()
result={'date':datetime.now(timezone.utc).date().isoformat(),'namespace':ns,'real_services':['authorization','order','SQL Server 2022','RabbitMQ','support Java','Mailpit'],'fixtures':['Cart','Product'],'http_fallback':'http://127.0.0.1:1 (unavailable)','checks':{'order_status':201,'repeat_preserved_order_id':True,'order_created_published_in_sql_outbox':True,'support_received_same_event_id':event,'matching_emails':count,'contracts_identical':True,'internal_secrets_equal':True,'public_key_endpoint':200,'ADMIN_login_and_role':True},'public_key_sha256':hashlib.sha256(pem.encode()).hexdigest(),'order_id':order_id,'deliveries':deliveries}
(Path(__file__).parents[2]/'docs/evidence/2026-10-28/order-created.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS: order real -> SQL outbox -> RabbitMQ -> support -> SMTP; fallback HTTP indisponível; exatamente um email; Secrets e contratos iguais; ADMIN/chave pública confirmados.')
