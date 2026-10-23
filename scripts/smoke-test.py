#!/usr/bin/env python3
import json, subprocess, urllib.request, urllib.error
from pathlib import Path
root=Path(__file__).resolve().parent.parent

def request(port,path='',method='GET',data=None):
    req=urllib.request.Request(f'http://localhost:{port}/api/products{path}',
        data=json.dumps(data).encode() if data is not None else None,
        headers={'Content-Type':'application/json'},method=method)
    with urllib.request.urlopen(req,timeout=10) as r:
        body=r.read()
        return r.status,json.loads(body) if body else None

def redis(*args):
    return subprocess.check_output(['/opt/homebrew/bin/redis-cli','-p','6380','--raw',*args],text=True).strip()

_,product=request(8081,method='POST',data={'name':'Shared product','price':12.50})
id=product['id']; key=f'products::{id}'
try:
    assert redis('EXISTS',key)=='0'
    assert request(8081,f'/{id}')[1]==product
    assert redis('EXISTS',key)=='1', 'First GET must populate Redis'
    log=root/'.runtime/app-8082.log'
    before=log.read_text().count(f'Cache miss: loading product {id} from PostgreSQL')
    assert request(8082,f'/{id}')[1]==product
    assert log.read_text().count(f'Cache miss: loading product {id} from PostgreSQL')==before, 'Second instance must use shared cache'
    assert 0<int(redis('TTL',key))<=600
    request(8082,f'/{id}',method='PUT',data={'name':'Updated from instance 2','price':20})
    assert redis('EXISTS',key)=='0', 'Update must evict shared cache'
    assert request(8081,f'/{id}')[1]['name']=='Updated from instance 2'
    assert request(8082)[1] and any(p['id']==id for p in request(8082)[1])
    try:
        request(8081,method='POST',data={'name':'','price':-1})
        raise AssertionError('Invalid data accepted')
    except urllib.error.HTTPError as e: assert e.code==400
finally:
    request(8082,f'/{id}',method='DELETE')
assert redis('EXISTS',key)=='0'
try:
    request(8081,f'/{id}')
    raise AssertionError('Deleted product still available')
except urllib.error.HTTPError as e: assert e.code==404
print('PASS: shared database, shared Redis cache, TTL, update/delete invalidation, validation and 404 across ports 8081/8082')
