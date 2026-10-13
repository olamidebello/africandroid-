import hashlib,importlib.util,json,subprocess,sys,tempfile,unittest,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('browser',ROOT/'scripts/integrate-browser.py');browser=importlib.util.module_from_spec(spec);spec.loader.exec_module(browser)
MANIFEST='''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="org.africanbrowser.browser"><application><activity android:name=".Browser" android:exported="true"><intent-filter><action android:name="android.intent.action.VIEW"/><category android:name="android.intent.category.DEFAULT"/><category android:name="android.intent.category.BROWSABLE"/><data android:scheme="http"/><data android:scheme="https"/></intent-filter></activity></application></manifest>'''
class BrowserIntegrationTests(unittest.TestCase):
 def test_browser_contract_rejects_wrong_package_private_and_site_only_handlers(self):
  self.assertEqual(browser.validate_manifest(MANIFEST),browser.PACKAGE)
  for xml in [MANIFEST.replace(browser.PACKAGE,'com.other.browser'),MANIFEST.replace('exported="true"','exported="false"'),MANIFEST.replace('android:scheme="https"','android:scheme="https" android:host="example.org"'),MANIFEST.replace('android.intent.category.BROWSABLE','OTHER')]:
   with self.assertRaises(ValueError):browser.validate_manifest(xml)
 def test_preload_is_presigned_and_default_only_after_validation(self):
  with tempfile.TemporaryDirectory() as tmp:
   base=Path(tmp);vendor=base/'vendor/africandroid';vendor.mkdir(parents=True);(vendor/'.africandroid-managed').touch()
   apk=base/'browser.apk'
   with zipfile.ZipFile(apk,'w') as z:z.writestr('lib/x86_64/libchrome.so',b'fixture')
   tool=base/'tool';tool.write_text('#!'+sys.executable+'\nimport sys\nprint('+repr(MANIFEST)+' if sys.argv[1]=="manifest" else "Signer #1 certificate SHA-256 digest: '+('a'*64)+'")\n');tool.chmod(0o755)
   args=[sys.executable,str(ROOT/'scripts/integrate-browser.py'),'--aosp',tmp,'--report',str(base/'report.json')]
   enabled=args+['--enabled','--apk',str(apk),'--sha256',hashlib.sha256(apk.read_bytes()).hexdigest(),'--apkanalyzer',str(tool),'--apksigner',str(tool)]
   subprocess.run(enabled,check=True,capture_output=True)
   self.assertTrue(json.loads((base/'report.json').read_text())['preloaded'])
   self.assertIn('presigned: true',(vendor/'africanbrowser/Android.bp').read_text())
   self.assertIn('config_defaultBrowser',(vendor/'africanbrowser/overlay/frameworks/base/core/res/res/values/config.xml').read_text())
   bad=enabled.copy();bad[bad.index('--sha256')+1]='0'*64
   self.assertNotEqual(subprocess.run(bad,capture_output=True).returncode,0)
   self.assertTrue((vendor/'africanbrowser/AfricanBrowser.apk').exists())
   subprocess.run(args,check=True,capture_output=True)
   self.assertFalse((vendor/'africanbrowser').exists())
   self.assertFalse(json.loads((base/'report.json').read_text())['preloaded'])
