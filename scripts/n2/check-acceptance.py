#!/usr/bin/env python3
"""Aceite adicional no laboratório isolado. Interrompe support e restaura em finally."""
import base64,json,os,subprocess,time,uuid,urllib.request,urllib.error
from pathlib import Path
secret_dir=Path(os.environ.get('GANJJ_SECRET_DIR','/tmp/ganjj-n2-secrets'))
k=os.environ.get('GANJJ_KUBECTL','/tmp/ganjj-k')
namespace=os.environ.get('GANJJ_NAMESPACE','ganjj-n2')
support=os.environ.get('SUPPORT_URL','http://localhost:18191')
def kubectl(*args):
 return subprocess.check_output([k,'-n',namespace,*args],text=True)
def call(base,path,body=None,headers=None):
 req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json',**(headers or {})})
 try:
  with urllib.request.urlopen(req,timeout=30) as r:return r.status,json.load(r)
 except urllib.error.HTTPError as e:return e.code, None
secret=lambda name:base64.b64decode(json.loads(kubectl('get','secret',name,'-o','json'))['data']['internal-secret'])
assert secret('order-secrets')==secret('support-credentials')
print('PASS: Secrets de order/support contêm o mesmo segredo (valores omitidos).')
node="Promise.all(['/health','/health/ready'].map(async p=>{let r=await fetch('http://support:8090'+p);if(r.status!==200)throw Error(p+': '+r.status);return {path:p,status:r.status}})).then(x=>console.log(JSON.stringify(x))).catch(e=>{console.error(e.message);process.exit(1)})"
health=json.loads(kubectl('exec','deployment/api-gateway','--','node','-e',node))
print('PASS: DNS support:8090 e as duas probes respondem 200 de dentro do cluster.')
event_id=str(uuid.uuid4());code='AUDIT-'+event_id
payload={'event_id':event_id,'event_type':'OrderCreated','occurred_at':'2026-10-25T12:00:00Z','order_id':event_id,'order_code':code,'customer_id':'audit','customer_email':'audit@example.com','total':0,'currency':'BRL'}
headers={'x-internal-secret':secret('support-credentials').decode()}
def count():
 status,data=call('http://localhost:18025','/api/v1/messages');assert status==200
 return sum(m['Subject']=='Pedido '+code+' confirmado' for m in data['messages'])
status,data=call(support,'/internal/notifications',payload,headers);assert status==202 and data['duplicate'] is False
for _ in range(80):
 if count()==1:break
 time.sleep(.25)
else:raise AssertionError('Primeiro email não recebido')
status,data=call(support,'/internal/notifications',payload,headers);assert status==202 and data['duplicate'] is True
time.sleep(3)
assert count()==1,'Duplicata produziu outro email'
print('PASS: mesmo event_id enviado duas vezes, exatamente um email no Mailpit; total zero aceito.')
identity=json.loads((secret_dir/'test-identity.json').read_text())
status,auth=call('http://localhost:18181','/auth/login',{'email':identity['email'],'password':identity['password']});assert status==200
replicas=json.loads(kubectl('get','deployment','support','-o','json'))['spec']['replicas']
assert replicas==1,'Executar apenas com a réplica de laboratório esperada'
try:
 kubectl('scale','deployment/support','--replicas=0')
 kubectl('wait','--for=delete','pod','-l','app=support','--timeout=60s')
 status,order=call('http://localhost:18000','/api/pedidos',{'endereco_entrega':{'rua':'Rua teste','numero':'1','bairro':'Centro','cidade':'Sao Paulo','estado':'SP','cep':'01310-100','pais':'BR'}},{'Authorization':'Bearer '+auth['accessToken'],'Idempotency-Key':'audit-offline-'+str(uuid.uuid4())})
 assert status==201,(status,order)
finally:
 kubectl('scale','deployment/support','--replicas='+str(replicas))
 kubectl('rollout','status','deployment/support','--timeout=120s')
print('PASS: order real retorna 201 com support desligado; réplica restaurada e pronta.')
result={'date':'2026-10-25','cluster':namespace,'checks':{'shared_secret_equal':True,'internal_dns_health':health,'zero_total_accepted':True,'same_event_submissions':2,'matching_emails_after_duplicate':1,'order_created_while_support_offline':status,'support_restored':True},'offline_order_id':order['id'],'fixtures':['shopping-cart','product-api'],'limits':['HTTP integration; order does not publish order.created','No real Twilio send','This is a technical lab, not a group rehearsal']}
(Path(__file__).parents[2]/'docs/evidence/2026-10-25/acceptance-audit.json').write_text(json.dumps(result,indent=2)+'\n')
