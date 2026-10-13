from pathlib import Path
import hashlib,json,os,subprocess,tempfile,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[1]
for p in (r/'scripts').glob('*.sh'):
 subprocess.run(['bash','-n',str(p)],check=True)
for p in (r/'vendor').rglob('*.xml'): ET.parse(p)
assets=r/'vendor/africandroid/apps/Customizer/assets/languages'
index=json.loads((assets/'index.json').read_text())
rows=[row for name in index['files'] for row in json.loads((assets/name).read_text())]
assert len({x['id'] for x in rows})==len(rows)
for level,count in index['counts'].items(): assert sum(x['level']==level for x in rows)==count
assert index['counts']['language'] > 2000
assert all(x['system_translation']=='unverified' for x in rows)
assert any(x['iso639_3']=='yor' for x in rows)
assert any(x['iso639_3']=='hau' for x in rows)
with tempfile.TemporaryDirectory() as d:
 aosp=Path(d)/'aosp'; base=aosp/'device/google/cuttlefish/vsoc_x86_64_only/phone/aosp_cf.mk'
 base.parent.mkdir(parents=True);base.write_text('# Mock upstream product for integration checks\n')
 env=dict(os.environ,AOSP_DIR=str(aosp))
 subprocess.run([str(r/'scripts/integrate.sh')],env=env,check=True)
 subprocess.run([str(r/'scripts/integrate.sh')],env=env,check=True)
 assert (aosp/'vendor/africandroid/apps/Customizer/AndroidManifest.xml').read_bytes()==(r/'vendor/africandroid/apps/Customizer/AndroidManifest.xml').read_bytes()
 marker=aosp/'vendor/africandroid/.africandroid-managed'; marker.unlink()
 rejected=subprocess.run([str(r/'scripts/integrate.sh')],env=env,capture_output=True,text=True)
 assert rejected.returncode!=0 and 'unmanaged' in rejected.stderr
 checks=Path(d)/'checks';checks.mkdir();(checks/'sample.bin').write_bytes(b'example')
 subprocess.run(['python3',str(r/'scripts/checksums.py'),str(checks)],check=True)
 assert (checks/'SHA256SUMS').read_text()==hashlib.sha256(b'example').hexdigest()+'  sample.bin\n'
print(f'PASS: shell syntax, XML, {len(rows)} catalog records, idempotent integration, overwrite protection and checksums.')
print('NOT TESTED: Android compilation, Java/Soong integration, emulator boot, app runtime, CTS/VTS or physical hardware.')
