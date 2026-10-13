#!/usr/bin/env python3
"""Validate a supplied native dialer APK and generate its AOSP preload configuration."""
from pathlib import Path
import argparse,hashlib,json,re,shutil,subprocess,xml.etree.ElementTree as ET,zipfile
ANDROID='{http://schemas.android.com/apk/res/android}'
PACKAGE='com.dobhrap.olamide'
def validate_manifest(xml):
 root=ET.fromstring(xml)
 if root.get('package')!=PACKAGE:raise ValueError('Unexpected Olamide package name')
 app=root.find('application')
 if app is None:raise ValueError('Missing application')
 dial=False;in_call=False
 for activity in list(app.findall('activity'))+list(app.findall('activity-alias')):
  if activity.get(ANDROID+'exported')!='true':continue
  for intent in activity.findall('intent-filter'):
   actions={n.get(ANDROID+'name') for n in intent.findall('action')}
   categories={n.get(ANDROID+'name') for n in intent.findall('category')}
   schemes={n.get(ANDROID+'scheme') for n in intent.findall('data')}
   if 'android.intent.action.DIAL' in actions and 'android.intent.category.DEFAULT' in categories and 'tel' in schemes:dial=True
 for service in app.findall('service'):
  if service.get(ANDROID+'permission')!='android.permission.BIND_INCALL_SERVICE' or service.get(ANDROID+'exported')!='true':continue
  ui=any(n.get(ANDROID+'name')=='android.telecom.IN_CALL_SERVICE_UI' and n.get(ANDROID+'value')=='true' for n in service.findall('meta-data'))
  actions={n.get(ANDROID+'name') for f in service.findall('intent-filter') for n in f.findall('action')}
  if ui and 'android.telecom.InCallService' in actions:in_call=True
 if not dial or not in_call:raise ValueError('Require exported tel ACTION_DIAL and UI InCallService protected by BIND_INCALL_SERVICE')
 return PACKAGE

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--aosp',required=True);p.add_argument('--report',required=True);p.add_argument('--enabled',action='store_true');p.add_argument('--apk');p.add_argument('--sha256');p.add_argument('--apkanalyzer',default='apkanalyzer');p.add_argument('--apksigner',default='apksigner');a=p.parse_args()
 aosp=Path(a.aosp).resolve();vendor=aosp/'vendor/africandroid';dest=vendor/'olamide'
 if not (vendor/'.africandroid-managed').is_file():p.error('Integrate the managed AfricAndroid vendor tree first')
 report={'package':PACKAGE,'preloaded':False,'native_sip_call_testing_passed':False,'runtime_role_eligibility_verified':False}
 if a.enabled:
  if not a.apk or not a.sha256 or not re.fullmatch('[a-fA-F0-9]{64}',a.sha256):p.error('Enabled integration requires APK and trusted SHA-256')
  apk=Path(a.apk)
  if hashlib.sha256(apk.read_bytes()).hexdigest()!=a.sha256.lower():p.error('APK checksum mismatch')
  verification=subprocess.check_output([a.apksigner,'verify','--print-certs',str(apk)],text=True)
  certificates=re.findall(r'Signer #[0-9]+ certificate SHA-256 digest: ([a-fA-F0-9]{64})',verification)
  if len(certificates)!=1:p.error('Require one APK signing certificate for profile handoff trust')
  signer=certificates[0].lower()
  xml=subprocess.check_output([a.apkanalyzer,'manifest','print',str(apk)],text=True)
  try:validate_manifest(xml)
  except ValueError as error:p.error(str(error))
  with zipfile.ZipFile(apk) as z:
   abis={name.split('/')[1] for name in z.namelist() if name.startswith('lib/') and name.endswith('.so')}
  if abis and 'x86_64' not in abis:p.error('Cuttlefish target requires x86_64 native libraries; this APK lacks them')
  report.update(preloaded=True,apk_sha256=a.sha256.lower(),native_abis=sorted(abis),signer_sha256=signer)
 if dest.exists():
  if not (dest/'.africandroid-generated').is_file():p.error('Refusing to replace unmanaged Olamide directory')
  shutil.rmtree(dest)
 if not a.enabled:
  trust=vendor/'apps/Customizer/res/values/sip_trust.xml'
  if trust.parent.is_dir():trust.write_text('<resources><string name="sip_trusted_signer" translatable="false"></string></resources>\n')
 if a.enabled:
  dest.mkdir();(dest/'.africandroid-generated').touch();shutil.copyfile(a.apk,dest/'OlamideSip.apk')
  (dest/'Android.bp').write_text('''android_app_import {
    name: "OlamideSip",
    apk: "OlamideSip.apk",
    presigned: true,
    product_specific: true,
}
''')
  trust=vendor/'apps/Customizer/res/values/sip_trust.xml'
  trust.write_text('<resources><string name="sip_trusted_signer" translatable="false">'+signer+'</string></resources>\n')
  (dest/'product.mk').write_text('PRODUCT_PACKAGES += OlamideSip\nDEVICE_PACKAGE_OVERLAYS += vendor/africandroid/olamide/overlay\n')
  overlay=dest/'overlay/frameworks/base/core/res/res/values';overlay.mkdir(parents=True)
  (overlay/'config.xml').write_text('<resources><string name="config_defaultDialer" translatable="false">'+PACKAGE+'</string></resources>\n')
 target=Path(a.report);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(report,indent=2)+'\n')
 print('Olamide preloaded with factory dialer configuration.' if a.enabled else 'Olamide APK integration disabled; no native SIP APK supplied.')
if __name__=='__main__':main()
