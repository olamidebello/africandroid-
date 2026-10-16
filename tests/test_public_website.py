import argparse,importlib.util,json,subprocess,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1]
def load(name,filename):
 spec=importlib.util.spec_from_file_location(name,ROOT/'website/tools'/filename);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module
publisher=load('public_apk_publisher','publish-apk.py');nginx=load('public_site_nginx','configure-nginx.py')
SIGNATURE='a'*64
BADGING="package: name='com.example.app' versionCode='2' versionName='1.2'\nsdkVersion:'30'\nnative-code: 'arm64-v8a' 'x86_64'\napplication-debuggable\n"
class PublicWebsiteTest(unittest.TestCase):
 def test_publish_verified_artifact_and_preserve_prior_on_failure(self):
  with tempfile.TemporaryDirectory() as temp:
   root=Path(temp);apk=root/'test.apk';apk.write_bytes(b'unit test mock artifact')
   args=argparse.Namespace(apk=str(apk),name='App',notes='Testing only',output=str(root/'public'),aapt='aapt',apksigner='apksigner',expected_signer_sha256=SIGNATURE,channel='development',source_url='https://github.com/example/app')
   with patch.object(publisher,'command',side_effect=['Signer #1 certificate SHA-256 digest: '+SIGNATURE,BADGING]):entry=publisher.publish(args)
   self.assertEqual(entry['min_sdk'],30);self.assertEqual(entry['abis'],['arm64-v8a','x86_64'])
   self.assertEqual((root/'public/downloads'/entry['filename']).read_bytes(),apk.read_bytes())
   before=(root/'public/catalog.json').read_bytes()
   with patch.object(publisher,'command',return_value='Signer #1 certificate SHA-256 digest: '+'b'*64):
    with self.assertRaises(ValueError):publisher.publish(args)
   self.assertEqual((root/'public/catalog.json').read_bytes(),before)
 def test_reject_debug_stable_and_metadata_absent(self):
  with patch.object(publisher,'command',side_effect=['certificate SHA-256 digest: '+SIGNATURE,BADGING]):
   with self.assertRaisesRegex(ValueError,'Debuggable'):publisher.metadata('test.apk','aapt','apksigner',SIGNATURE,'stable')
  with patch.object(publisher,'command',side_effect=['certificate SHA-256 digest: '+SIGNATURE,'not badging']):
   with self.assertRaisesRegex(ValueError,'metadata'):publisher.metadata('test.apk','aapt','apksigner',SIGNATURE,'development')
 def test_nginx_migrates_managed_site_retaining_tls_and_is_idempotent(self):
  with tempfile.TemporaryDirectory() as temp:
   base=Path(temp);a=base/'available';e=base/'enabled';a.mkdir();e.mkdir();target=a/'olamide-static-8'
   target.write_text('server {\n listen 443 ssl;\n server_name africandroid.world;\n root /var/www/old;\n ssl_certificate /etc/letsencrypt/live/africandroid.world/fullchain.pem;\n}\n');(e/target.name).symlink_to(target)
   with patch.object(nginx.subprocess,'run'):
    self.assertTrue(nginx.configure('africandroid.world','/var/www/africandroid.world',a,e)['changed'])
    self.assertFalse(nginx.configure('africandroid.world','/var/www/africandroid.world',a,e)['changed'])
   self.assertIn('ssl_certificate /etc/letsencrypt/',target.read_text());self.assertIn('root /var/www/africandroid.world;',target.read_text());self.assertEqual(target.read_text().count('include /etc/nginx/snippets/africandroid-public.conf;'),1)
 def test_nginx_rolls_back_failed_validation(self):
  with tempfile.TemporaryDirectory() as temp:
   base=Path(temp);a=base/'available';e=base/'enabled';a.mkdir();e.mkdir()
   with patch.object(nginx.subprocess,'run',side_effect=subprocess.CalledProcessError(1,'nginx')):
    with self.assertRaises(subprocess.CalledProcessError):nginx.configure('africandroid.world','/var/www/africandroid.world',a,e)
   self.assertFalse((e/'africandroid-public').exists());self.assertFalse((a/'africandroid-public').exists())
 def test_nginx_refuses_unrelated_site(self):
  with tempfile.TemporaryDirectory() as temp:
   base=Path(temp);a=base/'available';e=base/'enabled';a.mkdir();e.mkdir();(e/'unrelated').write_text('server_name africandroid.world; root /var/www/old;')
   with self.assertRaisesRegex(ValueError,'not a recognized'):nginx.configure('africandroid.world','/var/www/africandroid.world',a,e)
