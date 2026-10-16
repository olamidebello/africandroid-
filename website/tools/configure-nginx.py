#!/usr/bin/env python3
"""Configure only the AfricAndroid vhost, preserving issued TLS directives."""
import argparse,datetime,re,subprocess
from pathlib import Path

def configure(domain,root,available=Path('/etc/nginx/sites-available'),enabled=Path('/etc/nginx/sites-enabled')):
 if not re.fullmatch(r'(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}',domain):raise ValueError('Invalid public domain')
 if not re.fullmatch(r'/var/www/[a-z0-9.-]+',root):raise ValueError('Invalid website root')
 hits=[]
 for link in enabled.iterdir():
  if link.is_file() and re.search(r'\bserver_name\s+[^;]*?(?<![\w.-])'+re.escape(domain)+r'(?=\s|;)',link.read_text()):hits.append(link)
 if len(hits)>1:raise ValueError('More than one enabled Nginx configuration uses this domain; resolve the duplicate first')
 link=hits[0] if hits else enabled/'africandroid-public'
 if hits and not re.fullmatch(r'(?:africandroid-public|olamide-static-[0-9]+)',link.name):raise ValueError('Existing domain vhost is not a recognized managed site; review it before migration')
 target=link.resolve() if hits else available/'africandroid-public'
 previous=target.read_text() if target.exists() else None
 if previous:
  names=re.findall(r'\bserver_name\s+([^;]+);',previous)
  if any(set(value.split())!={domain} for value in names) or 'proxy_pass' in previous:raise ValueError('Refusing to replace a shared/proxied vhost')
  if not re.search(r'\broot\s+[^;]+;',previous):raise ValueError('Existing managed vhost has no document root')
  content=re.sub(r'\broot\s+[^;]+;', 'root '+root+';',previous)
  content=re.sub(r'\n\s*include /etc/nginx/snippets/africandroid-public.conf;','',content)
  content=re.sub(r'(\broot\s+[^;]+;)',r'\1\n include /etc/nginx/snippets/africandroid-public.conf;',content)
 else:content='server {\n listen 80;\n server_name '+domain+';\n root '+root+';\n include /etc/nginx/snippets/africandroid-public.conf;\n index index.html;\n location / { try_files $uri $uri/ =404; }\n location ~ /\\. { deny all; }\n}\n'
 if content==previous:return {'changed':False,'vhost':str(target)}
 if previous:target.with_name(target.name+'.backup-'+datetime.datetime.now().strftime('%Y%m%d%H%M%S')).write_text(previous)
 target.write_text(content);target.chmod(0o644)
 if not hits:link.symlink_to(target)
 try:subprocess.run(['nginx','-t'],check=True,capture_output=True,text=True)
 except subprocess.SubprocessError:
  if previous is not None:target.write_text(previous)
  else:link.unlink(missing_ok=True);target.unlink(missing_ok=True)
  raise
 return {'changed':True,'vhost':str(target)}
if __name__=='__main__':
 import json
 p=argparse.ArgumentParser();p.add_argument('--domain',required=True);p.add_argument('--root',required=True);a=p.parse_args()
 print(json.dumps(configure(a.domain,a.root)))
