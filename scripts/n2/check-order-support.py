#!/usr/bin/env python3
"""Order e SQL Server reais, com fixtures explícitas de Cart/Product."""
from pathlib import Path
import json,urllib.request,urllib.error,time,uuid,os
r=Path(os.environ.get('GANJJ_SECRET_DIR','/tmp/ganjj-n2-secrets'));state=json.loads((r/'test-identity.json').read_text());token=state['customer']['accessToken']
support=os.environ.get('SUPPORT_URL','http://localhost:18190')
def call(base,path,method='GET',body=None,headers=None):
 h={'Content-Type':'application/json',**(headers or {})};req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),headers=h,method=method)
 try:
  with urllib.request.urlopen(req,timeout=25) as x:return x.status,json.loads(x.read())
 except urllib.error.HTTPError as x:return x.code,x.read().decode()
event=(r/'support-event-id').read_text();status,data=call(support,'/internal/notifications/'+event,headers={'x-internal-secret':(r/'internal').read_text()});assert status==200 and len(data['deliveries'])==3,(status,data)
print('PASS: histórico no PVC após recriar o pod support.')
body={'endereco_entrega':{'rua':'Rua de teste','numero':'1','bairro':'Centro','cidade':'Sao Paulo','estado':'SP','cep':'01310-100','pais':'BR'}}
idem='n2-'+str(uuid.uuid4());headers={'Authorization':'Bearer '+token,'Idempotency-Key':idem,'X-Correlation-ID':idem}
status,order=call('http://localhost:18000','/api/pedidos','POST',body,headers);assert status==201,(status,order)
repeat_status,repeat=call('http://localhost:18000','/api/pedidos','POST',body,headers);assert repeat_status==201 and repeat['id']==order['id'],(repeat_status,repeat)
print('PASS: order real criou pedido via gateway; Idempotency-Key preservou o mesmo pedido.')
for _ in range(80):
 with urllib.request.urlopen('http://localhost:18025/api/v1/messages',timeout=10) as x:messages=json.load(x)
 if any('Pedido '+order['codigo']+' confirmado'==m['Subject'] for m in messages['messages']):break
 time.sleep(.25)
else:raise AssertionError('E-mail do pedido real não chegou ao Mailpit')
print('PASS: pedido real gerou e-mail no Mailpit via support.')
result={'date':'2026-10-25','real_services':['authorization','client','api-gateway','order','SQL Server 2022','support','Mailpit'],'fixtures':['shopping-cart','product-api'],'checks':['support PVC survived pod recreation','POST order 201 through gateway','idempotent repeat preserved order id','real order email received in Mailpit'],'order_id':order['id'],'order_code':order['codigo']}
(Path(__file__).parents[2]/'docs/evidence/2026-10-25/order-support.json').write_text(json.dumps(result,indent=2)+'\n')
