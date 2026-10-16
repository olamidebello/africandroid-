#!/usr/bin/env python3
"""Verify a signed APK and publish an immutable download plus release metadata."""
import argparse,datetime,fcntl,hashlib,json,os,re,shutil,subprocess,tempfile
from pathlib import Path

def command(argv):return subprocess.run(argv,check=True,capture_output=True,text=True,timeout=120).stdout

def metadata(apk,aapt,signer,expected,channel):
 output=command([signer,'verify','--verbose','--print-certs',str(apk)])
 certificates=set(re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9:]+)',output))
 certificates={c.replace(':','').lower() for c in certificates}
 expected=expected.replace(':','').lower()
 if not re.fullmatch('[a-f0-9]{64}',expected) or certificates!={expected}:raise ValueError('APK signing certificate does not match the expected fingerprint')
 badging=command([aapt,'dump','badging',str(apk)])
 match=re.search(r"package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']*)'",badging)
 minimum=re.search(r"sdkVersion:'([0-9]+)'",badging)
 if not match or not minimum:raise ValueError('APK package/version/minimum SDK metadata is missing')
 if channel=='stable' and 'application-debuggable' in badging:raise ValueError('Debuggable APKs must use the development channel')
 native=re.search(r'native-code: (.*)',badging)
 return {'package':match[1],'version_code':int(match[2]),'version':match[3],'min_sdk':int(minimum[1]),'abis':re.findall(r"'([^']+)'",native[1]) if native else ['No native CPU restriction'],'signer_sha256':expected}

def publish(args):
 apk=Path(args.apk).resolve()
 if not apk.is_file() or apk.stat().st_size>512*1024**2:raise ValueError('APK must exist and be at most 512 MiB')
 if not re.fullmatch(r'https://github\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:/[^\s]*)?',args.source_url):raise ValueError('Supply a public GitHub source/license URL')
 if not 1<=len(args.name)<=80 or len(args.notes)>2000:raise ValueError('Invalid name or release notes')
 root=Path(args.output).resolve();root.mkdir(parents=True,exist_ok=True,mode=0o755)
 # Work from a private copy so the compiler cannot change the APK after verification.
 with tempfile.TemporaryDirectory(prefix='africandroid-publish-') as tmp:
  checked=Path(tmp)/'checked.apk';shutil.copyfile(apk,checked)
  info=metadata(checked,args.aapt,args.apksigner,args.expected_signer_sha256,args.channel)
  digest=hashlib.sha256(checked.read_bytes()).hexdigest()
  with (root/'.publish.lock').open('a') as lock:
   fcntl.flock(lock,fcntl.LOCK_EX)
   catalog=root/'catalog.json';data=json.loads(catalog.read_text()) if catalog.exists() else {'releases':[]}
   downloads=root/'downloads';downloads.mkdir(mode=0o755,exist_ok=True)
   dest=downloads/(digest+'.apk')
   if not dest.exists():
    fd,stage=tempfile.mkstemp(dir=downloads,prefix='.upload-')
    try:
     with os.fdopen(fd,'wb') as stream,checked.open('rb') as source:shutil.copyfileobj(source,stream)
     os.chmod(stage,0o644);os.replace(stage,dest)
    finally:
     if os.path.exists(stage):os.unlink(stage)
   entry={**info,'name':args.name,'notes':args.notes,'channel':args.channel,'sha256':digest,'size_bytes':dest.stat().st_size,'filename':digest+'.apk','url':'/downloads/'+digest+'.apk','source_url':args.source_url,'published_at':datetime.datetime.now(datetime.timezone.utc).isoformat()}
   data['releases']=[entry]+[r for r in data['releases'] if not(r['package']==entry['package'] and r['channel']==args.channel)]
   fd,stage=tempfile.mkstemp(dir=root,prefix='.catalog-')
   with os.fdopen(fd,'w') as stream:json.dump(data,stream,indent=2)
   os.chmod(stage,0o644);os.replace(stage,catalog)
 return entry

def main():
 parser=argparse.ArgumentParser(description=__doc__)
 for key in ['apk','name','expected-signer-sha256','source-url']:parser.add_argument('--'+key,required=True)
 parser.add_argument('--channel',choices=['development','stable'],default='development');parser.add_argument('--notes',default='');parser.add_argument('--output',default='/var/lib/africandroid-public');parser.add_argument('--aapt',default='aapt');parser.add_argument('--apksigner',default='apksigner')
 try:print(json.dumps(publish(parser.parse_args())))
 except (ValueError,OSError,subprocess.SubprocessError) as error:raise SystemExit('APK publication failed: '+str(error))
if __name__=='__main__':main()
