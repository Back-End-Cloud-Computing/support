#!/usr/bin/env python3
import json,ssl,urllib.request,urllib.parse
from pathlib import Path
# Use the OS trust bundle when Python.framework has no bundled certificate setup.
context=ssl.create_default_context(cafile='/etc/ssl/cert.pem' if Path('/etc/ssl/cert.pem').exists() else None)
r=Path(__file__).parents[2]/'docs/evidence/2026-10-25'
for repo,tag in [('client','1.2.0'),('authorization','1.2.1'),('support','1.0.1')]:
 query=urllib.parse.urlencode({'service':'registry.docker.io','scope':f'repository:eduardofabri/{repo}:pull'})
 with urllib.request.urlopen('https://auth.docker.io/token?'+query,context=context,timeout=20) as response:token=json.load(response)['token']
 req=urllib.request.Request(f'https://registry-1.docker.io/v2/eduardofabri/{repo}/manifests/{tag}',headers={'Authorization':'Bearer '+token,'Accept':'application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json'})
 with urllib.request.urlopen(req,context=context,timeout=20) as response:data=json.load(response)
 platforms=[m['platform'] for m in data['manifests'] if m.get('platform',{}).get('architecture') in ['amd64','arm64']]
 assert {p['architecture'] for p in platforms}=={'amd64','arm64'}
 p=r/(repo+'-image.json');existing=json.loads(p.read_text());existing['public_anonymous_pull']=True;p.write_text(json.dumps(existing,indent=2)+'\n')
 print('PASS: imagem pública e multi-arch',repo,tag)
