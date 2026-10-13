#!/usr/bin/env python3
"""Validate a supplied browser APK and generate its AOSP preload configuration."""
from pathlib import Path
import argparse,hashlib,json,re,shutil,subprocess,xml.etree.ElementTree as ET,zipfile
ANDROID='{http://schemas.android.com/apk/res/android}'
PACKAGE='org.africanbrowser.browser'
def validate_manifest(xml):
 root=ET.fromstring(xml)
 if root.get('package')!=PACKAGE:raise ValueError('Unexpected AfricanBrowser package name')
 app=root.find('application')
 if app is None or app.get(ANDROID+'enabled')=='false':raise ValueError('Missing application')
 schemes=set()
 for activity in list(app.findall('activity'))+list(app.findall('activity-alias')):
  if activity.get(ANDROID+'exported')!='true' or activity.get(ANDROID+'enabled')=='false':continue
  for intent in activity.findall('intent-filter'):
   actions={n.get(ANDROID+'name') for n in intent.findall('action')}
   categories={n.get(ANDROID+'name') for n in intent.findall('category')}
   data=intent.findall('data')
   if any(any(key in node.attrib for key in [ANDROID+'host',ANDROID+'port',ANDROID+'path',ANDROID+'pathPrefix',ANDROID+'pathPattern',ANDROID+'mimeType']) for node in data):continue
   if 'android.intent.action.VIEW' in actions and {'android.intent.category.DEFAULT','android.intent.category.BROWSABLE'}<=categories:schemes.update(n.get(ANDROID+'scheme') for n in data)
 if not {'http','https'}<=schemes:raise ValueError('Require exported unrestricted http and https VIEW handlers with DEFAULT and BROWSABLE')
 return PACKAGE

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--aosp',required=True);p.add_argument('--report',required=True);p.add_argument('--enabled',action='store_true');p.add_argument('--apk');p.add_argument('--sha256');p.add_argument('--apkanalyzer',default='apkanalyzer');p.add_argument('--apksigner',default='apksigner');a=p.parse_args()
 aosp=Path(a.aosp).resolve();vendor=aosp/'vendor/africandroid';dest=vendor/'africanbrowser'
 if not (vendor/'.africandroid-managed').is_file():p.error('Integrate the managed AfricAndroid vendor tree first')
 report={'package':PACKAGE,'preloaded':False,'browser_runtime_testing_passed':False,'runtime_role_eligibility_verified':False}
 if a.enabled:
  if not a.apk or not a.sha256 or not re.fullmatch('[a-fA-F0-9]{64}',a.sha256):p.error('Enabled integration requires APK and trusted SHA-256')
  apk=Path(a.apk)
  if hashlib.sha256(apk.read_bytes()).hexdigest()!=a.sha256.lower():p.error('APK checksum mismatch')
  verification=subprocess.check_output([a.apksigner,'verify','--print-certs',str(apk)],text=True)
  certificates=re.findall(r'Signer #[0-9]+ certificate SHA-256 digest: ([a-fA-F0-9]{64})',verification)
  if len(certificates)!=1:p.error('Require one APK signing certificate for browser provenance')
  signer=certificates[0].lower()
  xml=subprocess.check_output([a.apkanalyzer,'manifest','print',str(apk)],text=True)
  try:validate_manifest(xml)
  except ValueError as error:p.error(str(error))
  with zipfile.ZipFile(apk) as z:
   abis={name.split('/')[1] for name in z.namelist() if name.startswith('lib/') and name.endswith('.so')}
  if abis and 'x86_64' not in abis:p.error('Cuttlefish target requires x86_64 native libraries; this APK lacks them')
  report.update(preloaded=True,apk_sha256=a.sha256.lower(),native_abis=sorted(abis),signer_sha256=signer)
 if dest.exists():
  if not (dest/'.africandroid-generated').is_file():p.error('Refusing to replace unmanaged AfricanBrowser directory')
  shutil.rmtree(dest)
 if a.enabled:
  dest.mkdir();(dest/'.africandroid-generated').touch();shutil.copyfile(a.apk,dest/'AfricanBrowser.apk')
  (dest/'Android.bp').write_text('''android_app_import {
    name: "AfricanBrowser",
    apk: "AfricanBrowser.apk",
    presigned: true,
    product_specific: true,
}
''')
  (dest/'product.mk').write_text('PRODUCT_PACKAGES += AfricanBrowser\nDEVICE_PACKAGE_OVERLAYS += vendor/africandroid/africanbrowser/overlay\n')
  overlay=dest/'overlay/frameworks/base/core/res/res/values';overlay.mkdir(parents=True)
  (overlay/'config.xml').write_text('<resources><string name="config_defaultBrowser" translatable="false">'+PACKAGE+'</string></resources>\n')
 target=Path(a.report);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(report,indent=2)+'\n')
 print('AfricanBrowser preloaded with factory browser configuration.' if a.enabled else 'AfricanBrowser APK integration disabled; no browser APK supplied.')
if __name__=='__main__':main()
