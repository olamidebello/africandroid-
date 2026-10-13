from pathlib import Path
import importlib.util,json,subprocess,tempfile,unittest,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('packs',ROOT/'scripts/language-packs.py');packs=importlib.util.module_from_spec(spec);spec.loader.exec_module(packs)
class LanguagePackTests(unittest.TestCase):
 def fixture(self):
  template=json.loads((ROOT/'localization/template.json').read_text())['strings']
  return {'locale':'yo','reviewed':True,'reviewer':'TEST FIXTURE, not a real translation','license':'test-only','strings':{k:'TEST '+v for k,v in template.items()}}
 def test_complete_pack_import_and_retirement(self):
  with tempfile.TemporaryDirectory() as d:
   folder=Path(d)/'packs';folder.mkdir();(folder/'yo.json').write_text(json.dumps(self.fixture()))
   loaded=packs.load_packs(folder);app=Path(d)/'app';packs.apply(loaded,app)
   resource=app/'res/values-b+yo/strings-africandroid.xml';self.assertTrue(resource.exists());ET.parse(resource)
   packs.apply([],app);self.assertFalse(resource.exists())
   locales=ET.parse(app/'res/xml/locales_config.xml').getroot();self.assertEqual(len(locales),1)
 def test_reject_unreviewed_incomplete_and_bad_placeholders(self):
  for change in ('review','missing','format','locale'):
   with self.subTest(change=change),tempfile.TemporaryDirectory() as d:
    data=self.fixture()
    if change=='review':data['reviewed']=False
    if change=='missing':data['strings'].pop('security')
    if change=='format':data['strings']['android_model']='Wrong placeholders'
    if change=='locale':data['locale']='../../escape'
    Path(d,'pack.json').write_text(json.dumps(data))
    with self.assertRaises(ValueError):packs.load_packs(d)
 def test_required_locale_and_strict_feature_gates_fail(self):
  with tempfile.TemporaryDirectory() as d:
   lang=subprocess.run(['python3',str(ROOT/'scripts/language-packs.py'),'--report',d+'/lang.json','--required-locales','yo'],capture_output=True)
   self.assertEqual(lang.returncode,2)
   feature=subprocess.run(['python3',str(ROOT/'scripts/feature-audit.py'),'--report',d+'/features.json','--strict'],capture_output=True)
   self.assertEqual(feature.returncode,2);self.assertFalse(json.loads(Path(d,'features.json').read_text())['exact_cross_os_parity'])
 def test_resource_references_exist_and_java_parses(self):
  import re,javalang
  app=ROOT/'vendor/africandroid/apps/Customizer'
  keys={node.attrib['name'] for node in ET.parse(app/'res/values/strings.xml').getroot()}
  for source in (app/'src').rglob('*.java'):
   content=source.read_text();javalang.parse.parse(content)
   self.assertTrue(set(re.findall(r'(?<!android\.)R\.string\.(\w+)',content))<=keys)
if __name__=='__main__':unittest.main()
