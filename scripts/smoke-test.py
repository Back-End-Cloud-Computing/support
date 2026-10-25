#!/usr/bin/env python3
"""SMTP real + idempotência. Rode com suporte e Mailpit acessíveis localmente."""
import json, os, time, uuid, urllib.request
base = os.environ.get('SUPPORT_URL', 'http://localhost:8090')
mailpit = os.environ.get('MAILPIT_URL', 'http://localhost:8025')
secret = os.environ['INTERNAL_SECRET']
event_id = str(uuid.uuid4())
payload = dict(event_id=event_id, event_type='OrderCreated', occurred_at='2026-10-24T12:00:00Z',
               order_id='7', order_code='GANJJ-' + event_id[:8], customer_id='42',
               customer_email='smoke@example.com', customer_phone='+5541999999999', total=1234.56, currency='BRL')
def call(path, body=None):
    req = urllib.request.Request(base+path, data=None if body is None else json.dumps(body).encode(),
                                 headers={'x-internal-secret':secret, 'Content-Type':'application/json'})
    with urllib.request.urlopen(req, timeout=10) as response:
        return response.status, json.load(response)
status, result = call('/internal/notifications', payload)
assert status == 202 and result['duplicate'] is False
for _ in range(100):
    _, result = call('/internal/notifications/'+event_id)
    if len(result['deliveries']) == 3: break
    time.sleep(.1)
assert len(result['deliveries']) == 3 and all(d['status']=='sent' for d in result['deliveries']), result
_, duplicate = call('/internal/notifications', payload)
assert duplicate['duplicate'] is True
with urllib.request.urlopen(mailpit+'/api/v1/messages',timeout=10) as response:
    messages=json.load(response)
assert any(m['Subject']=='Pedido '+payload['order_code']+' confirmado' for m in messages['messages']), messages
print('PASS: SMTP real no Mailpit; três entregas; evento duplicado não reenviado.')
print('EVENT_ID='+event_id)
