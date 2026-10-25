#!/usr/bin/env python3
"""Valida authorization/client reais; grava evidência pública sem tokens."""
import base64, hashlib, json, os, pathlib, subprocess, time, urllib.request, urllib.error
secret_dir=pathlib.Path(os.environ.get('GANJJ_SECRET_DIR','/tmp/ganjj-n2-secrets'))
auth=os.environ.get('AUTH_URL','http://localhost:18081');client=os.environ.get('CLIENT_URL','http://localhost:18082')
def call(base,path,method='GET',body=None,token=None,extra=None):
 headers={'Content-Type':'application/json',**(extra or {})}
 if token:headers['Authorization']='Bearer '+token
 req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
 try:
  with urllib.request.urlopen(req,timeout=20) as r:return r.status,r.read().decode()
 except urllib.error.HTTPError as e:return e.code,e.read().decode()
def require(status,wanted,label):
 assert status==wanted,f'{label}: esperado {wanted}, recebido {status}'
 print('PASS:',label,status)
status,pem=call(auth,'/auth/public-key');require(status,200,'chave pública no cluster')
assert pem.strip()==(secret_dir/'public.pem').read_text().strip()
admin_pass=(secret_dir/'auth-admin').read_text()
status,body=call(auth,'/auth/login','POST',{'email':'admin@ganjj.com','password':admin_pass});require(status,200,'login ADMIN')
admin=json.loads(body)
email='n2-'+str(int(time.time()))+'@example.com';password='LocalN2-'+os.urandom(16).hex()+'!'
status,_=call(auth,'/auth/register','POST',{'email':email,'password':password});require(status,201,'cadastro CLIENTE')
status,body=call(auth,'/auth/login','POST',{'email':email,'password':password});require(status,200,'login CLIENTE')
customer=json.loads(body)
payload=json.loads(base64.urlsafe_b64decode(admin['accessToken'].split('.')[1]+'=='))
assert payload['iss']=='ganjj-authorization' and payload['typ']=='access' and payload['role']=='ADMIN'
header,data,signature=admin['accessToken'].split('.')
(secret_dir/'jwt-data').write_text(header+'.'+data);(secret_dir/'jwt-signature').write_bytes(base64.urlsafe_b64decode(signature+'=='))
subprocess.run(['openssl','dgst','-sha256','-verify',str(secret_dir/'public.pem'),'-signature',str(secret_dir/'jwt-signature'),str(secret_dir/'jwt-data')],check=True,stdout=subprocess.DEVNULL)
require(call(client,'/clients/me')[0],401,'perfil sem token')
profile={'nome':'Cliente N2','telefone':'(11) 98765-4321','endereco':{'logradouro':'Rua de teste','numero':'1','bairro':'Centro','cidade':'Sao Paulo','estado':'SP','cep':'01310-100'}}
require(call(client,'/clients','POST',profile,customer['accessToken'])[0],201,'criar perfil com JWT real')
require(call(client,'/clients/me',token=customer['accessToken'])[0],200,'consultar perfil')
require(call(client,'/clients',token=customer['accessToken'])[0],403,'CLIENTE sem acesso administrativo')
require(call(client,'/clients',token=admin['accessToken'])[0],200,'ADMIN com acesso administrativo')
require(call(client,'/clients/me',token=customer['refreshToken'])[0],401,'refresh não é access token')
require(call(client,'/clients/me',token=customer['accessToken'][:-8]+'invalid!')[0],401,'assinatura adulterada')
for base,name in [(auth,'authorization'),(client,'client')]:
 status,metrics=call(base,'/metrics');require(status,200,'métricas '+name)
 assert 'http_' in metrics
state={'admin':admin,'customer':customer,'email':email,'password':password}
p=secret_dir/'test-identity.json';p.write_text(json.dumps(state));p.chmod(0o600)
evidence={'date':'2026-10-25','cluster':'ganjj-n2','public_key_sha256':hashlib.sha256(pem.encode()).hexdigest(),'checks':['public-key','RS256 signature','ADMIN login','CLIENTE registration/login','create/get profile','401 without token','403 CLIENTE admin route','200 ADMIN admin route','401 refresh as access','401 invalid signature','Prometheus endpoints']}
path=pathlib.Path(__file__).parents[2]/'docs/evidence/2026-10-25/auth-client.json';path.write_text(json.dumps(evidence,indent=2)+'\n')
