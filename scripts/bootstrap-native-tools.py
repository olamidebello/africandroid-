#!/usr/bin/env python3
"""Install pinned Gradle and Android SDK compile tools on an authorized build host."""
from pathlib import Path
import argparse,hashlib,io,json,os,stat,subprocess,tempfile,urllib.request,xml.etree.ElementTree as ET,zipfile

def local(tag):return tag.split('}')[-1]
def value(node,name):
 for child in node.iter():
  if local(child.tag)==name:return (child.text or '').strip()
 return ''
def fetch(url):
 with urllib.request.urlopen(url,timeout=120) as r:return r.read()
def extract(data,target,prefix):
 with zipfile.ZipFile(io.BytesIO(data)) as archive:
  for entry in archive.infolist():
   parts=Path(entry.filename).parts
   if not entry.filename.startswith(prefix+'/') or '..' in parts or entry.filename.startswith('/') or stat.S_ISLNK(entry.external_attr>>16):raise ValueError('Unsafe upstream archive path')
  archive.extractall(target)
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--sdk',required=True);p.add_argument('--gradle-parent',required=True);p.add_argument('--accept-sdk-licenses',action='store_true');a=p.parse_args()
 if not a.accept_sdk_licenses:p.error('SDK license acceptance is required to provision compile tools')
 sdk=Path(a.sdk).resolve();parent=Path(a.gradle_parent).resolve();sdk.mkdir(parents=True,exist_ok=True);parent.mkdir(parents=True,exist_ok=True)
 manager=sdk/'cmdline-tools/latest/bin/sdkmanager'
 if not manager.exists():
  tree=ET.fromstring(fetch('https://dl.google.com/android/repository/repository2-1.xml'))
  selected=None
  for package in tree.iter():
   if local(package.tag)=='remotePackage' and package.get('path')=='cmdline-tools;16.0':
    for archive in package.iter():
     if local(archive.tag)=='archive' and value(archive,'host-os')=='linux':selected=archive;break
  if selected is None:raise RuntimeError('Pinned Linux Android command-line tools 16.0 not found in official repository')
  url='https://dl.google.com/android/repository/'+value(selected,'url');expected=value(selected,'checksum');payload=fetch(url)
  algorithm='sha256' if len(expected)==64 else 'sha1'
  if hashlib.new(algorithm,payload).hexdigest()!=expected:raise ValueError('Android SDK archive checksum mismatch')
  with tempfile.TemporaryDirectory(dir=sdk) as temp:
   extract(payload,temp,'cmdline-tools');target=sdk/'cmdline-tools/latest';target.parent.mkdir(exist_ok=True)
   if target.exists():raise ValueError('Refusing to replace an incomplete/unmanaged command-line-tools directory')
   import shutil;shutil.move(str(Path(temp)/'cmdline-tools'),target)
  for executable in manager.parent.iterdir():
   if executable.is_file():executable.chmod(executable.stat().st_mode|0o111)
 gradle=parent/'gradle-8.13/bin/gradle'
 if not gradle.exists():
  base='https://services.gradle.org/distributions/gradle-8.13-bin.zip';expected=fetch(base+'.sha256').decode().strip();payload=fetch(base)
  if hashlib.sha256(payload).hexdigest()!=expected:raise ValueError('Gradle archive checksum mismatch')
  extract(payload,parent,'gradle-8.13');gradle.chmod(gradle.stat().st_mode|0o111)
 subprocess.run([str(manager),'--sdk_root='+str(sdk),'--licenses'],input='y\n'*200,text=True,check=True,stdout=subprocess.DEVNULL)
 subprocess.run([str(manager),'--sdk_root='+str(sdk),'platform-tools','platforms;android-35','build-tools;35.0.0'],check=True)
 print(json.dumps({'sdk':str(sdk),'gradle':str(gradle),'android_platform':35,'build_tools':'35.0.0'}))
if __name__=='__main__':main()
