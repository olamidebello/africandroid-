from pathlib import Path
import importlib.util,json,subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[1]
def module(name,file):
 spec=importlib.util.spec_from_file_location(name,ROOT/'scripts'/file);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value
profile=module('mobile_profile','mobile-profile.py');dialer=module('olamide','integrate-olamide.py')
class MobileIntegrationTests(unittest.TestCase):
 def test_profile_limits_and_types(self):
  self.assertEqual(profile.validate(profile.DEFAULTS.copy()),profile.DEFAULTS)
  for value in (0,300001,True,'180000'):
   bad=profile.DEFAULTS.copy();bad['max_sample_age_ms']=value
   with self.assertRaises(ValueError):profile.validate(bad)
 def test_profile_applies_to_android_resources(self):
  with tempfile.TemporaryDirectory() as d:
   subprocess.run(['python3',str(ROOT/'scripts/mobile-profile.py'),'--apply-to',d+'/app','--report',d+'/report.json'],check=True,capture_output=True)
   self.assertIn('geo_max_sample_age_ms',Path(d,'app/res/values/mobile_policy.xml').read_text())
   self.assertFalse(json.loads(Path(d,'report.json').read_text())['monitoring_enabled_by_default'])
 def manifest(self):
  return '''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.dobhrap.olamide"><application>
  <activity android:exported="true"><intent-filter><action android:name="android.intent.action.DIAL"/><category android:name="android.intent.category.DEFAULT"/><data android:scheme="tel"/></intent-filter></activity>
  <service android:exported="true" android:permission="android.permission.BIND_INCALL_SERVICE"><meta-data android:name="android.telecom.IN_CALL_SERVICE_UI" android:value="true"/><intent-filter><action android:name="android.telecom.InCallService"/></intent-filter></service>
  </application></manifest>'''
 def test_dialer_contract_rejects_wrappers_and_wrong_package(self):
  self.assertEqual(dialer.validate_manifest(self.manifest()),'com.dobhrap.olamide')
  for xml in (self.manifest().replace('com.dobhrap.olamide','evil.package'),self.manifest().replace('android.permission.BIND_INCALL_SERVICE',''),self.manifest().replace('android.intent.action.DIAL','android.intent.action.VIEW')):
   with self.assertRaises(ValueError):dialer.validate_manifest(xml)
 def test_no_apk_does_not_claim_native_dialer(self):
  with tempfile.TemporaryDirectory() as d:
   vendor=Path(d,'aosp/vendor/africandroid');vendor.mkdir(parents=True);(vendor/'.africandroid-managed').touch()
   subprocess.run(['python3',str(ROOT/'scripts/integrate-olamide.py'),'--aosp',d+'/aosp','--report',d+'/dialer.json'],check=True,capture_output=True)
   self.assertFalse(json.loads(Path(d,'dialer.json').read_text())['preloaded']);self.assertFalse((vendor/'olamide').exists())
 def test_enabled_apk_rejects_checksum_before_tool_execution(self):
  with tempfile.TemporaryDirectory() as d:
   vendor=Path(d,'aosp/vendor/africandroid');vendor.mkdir(parents=True);(vendor/'.africandroid-managed').touch()
   Path(d,'fake.apk').write_bytes(b'not an APK')
   result=subprocess.run(['python3',str(ROOT/'scripts/integrate-olamide.py'),'--aosp',d+'/aosp','--report',d+'/report.json','--enabled','--apk',d+'/fake.apk','--sha256','0'*64],capture_output=True,text=True)
   self.assertEqual(result.returncode,2);self.assertIn('checksum mismatch',result.stderr)
if __name__=='__main__':unittest.main()
