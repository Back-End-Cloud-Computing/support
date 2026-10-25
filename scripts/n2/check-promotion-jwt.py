#!/usr/bin/env python3
from pathlib import Path
import json,base64,time,subprocess,urllib.request,urllib.error
r=Path('/tmp/ganjj-n2-secrets');state=json.loads((r/'test-identity.json').read_text())
def status(token=None):
 h={} if token is None else {'Authorization':'Bearer '+token}
 try:
  with urllib.request.urlopen(urllib.request.Request('http://localhost:18800/api/campaigns',headers=h),timeout=20) as x:return x.status
 except urllib.error.HTTPError as x:return x.code
def signed(claims):
 enc=lambda o:base64.urlsafe_b64encode(json.dumps(o,separators=(',',':')).encode()).decode().rstrip('=')
 data=enc({'alg':'RS256','typ':'JWT'})+'.'+enc(claims)
 signature=subprocess.run(['openssl','dgst','-sha256','-sign',str(r/'private.pem')],input=data.encode(),capture_output=True,check=True).stdout
 return data+'.'+base64.urlsafe_b64encode(signature).decode().rstrip('=')
claims=json.loads(base64.urlsafe_b64decode(state['admin']['accessToken'].split('.')[1]+'=='))
cases={'admin':(state['admin']['accessToken'],200),'client':(state['customer']['accessToken'],403),'missing':(None,401),'refresh':(state['admin']['refreshToken'],401),'malformed':('invalid',401),'invalid-signature':(state['admin']['accessToken'][:-8]+'invalid!',401),'expired':(signed({**claims,'exp':int(time.time())-1000}),401),'wrong-issuer':(signed({**claims,'iss':'wrong-issuer'}),401)}
results={}
for name,(token,wanted) in cases.items():
 actual=status(token);assert actual==wanted,(name,actual,wanted);results[name]=actual;print('PASS:',name,actual)
(Path(__file__).parents[2]/'docs/evidence/2026-10-25/promotion-jwt.json').write_text(json.dumps({'date':'2026-10-25','real_services':['authorization','promotion','MySQL'],'checks':results},indent=2)+'\n')
