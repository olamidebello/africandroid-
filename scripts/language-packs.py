#!/usr/bin/env python3
"""Validate/import human-reviewed Customizer packs; never claims system localization."""
from pathlib import Path
from collections import Counter
import argparse,json,re,xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
PATTERN=re.compile(r'%(?:\d+\$)?[sd]')
TAG=re.compile(r'[a-z]{2,3}(?:-[A-Z][a-z]{3})?(?:-(?:[A-Z]{2}|\d{3}))?')

def load_packs(folder):
    template=json.loads((ROOT/'localization/template.json').read_text())['strings']
    packs=[];seen=set()
    for path in sorted(Path(folder).glob('*.json')):
        data=json.loads(path.read_text(encoding='utf-8'));tag=data.get('locale','')
        if not TAG.fullmatch(tag) or tag=='en' or tag in seen: raise ValueError(f'{path}: invalid, duplicate or default locale')
        if data.get('reviewed') is not True or not data.get('reviewer','').strip() or not data.get('license','').strip():
            raise ValueError(f'{path}: reviewed=true, reviewer and license required')
        translated=data.get('strings',{})
        if set(translated)!=set(template): raise ValueError(f'{path}: exact default string keys required')
        for key,base in template.items():
            value=translated[key]
            if not isinstance(value,str) or not value.strip(): raise ValueError(f'{path}: empty/invalid {key}')
            if any(ord(c)<32 and c not in '\n\t' for c in value): raise ValueError(f'{path}: control character in {key}')
            if Counter(PATTERN.findall(value))!=Counter(PATTERN.findall(base)): raise ValueError(f'{path}: placeholders differ in {key}')
        if all(translated[k]==v for k,v in template.items()): raise ValueError(f'{path}: unchanged English template is not a translation')
        seen.add(tag);packs.append(data)
    return packs

def android_text(value):
    return '"'+value.replace('\\','\\\\').replace('\n','\\n').replace('\t','\\t').replace('"','\\"')+'"'

def apply(packs,app):
    app=Path(app).resolve();res=app/'res';marker=app/'.translation-managed.json'
    # Delete only generated files named in our previous manifest; never remove a resource tree.
    if marker.exists():
        for relative in json.loads(marker.read_text())['files']:
            old=app/relative
            if not re.fullmatch(r'res/values-b\+[A-Za-z0-9+]+/strings-africandroid.xml',relative): raise ValueError('Unsafe managed-file path')
            old.unlink(missing_ok=True)
    generated=[]
    for pack in packs:
        relative='res/values-b+'+pack['locale'].replace('-','+')+'/strings-africandroid.xml'
        path=app/relative;path.parent.mkdir(parents=True,exist_ok=True)
        root=ET.Element('resources')
        for key,value in pack['strings'].items(): ET.SubElement(root,'string',name=key).text=android_text(value)
        ET.indent(root);ET.ElementTree(root).write(path,encoding='utf-8',xml_declaration=True);generated.append(relative)
    root=ET.Element('locale-config',{'xmlns:android':'http://schemas.android.com/apk/res/android'})
    for tag in ['en']+[x['locale'] for x in packs]: ET.SubElement(root,'locale',{'android:name':tag})
    path=res/'xml/locales_config.xml';path.parent.mkdir(parents=True,exist_ok=True)
    ET.ElementTree(root).write(path,encoding='utf-8',xml_declaration=True)
    marker.write_text(json.dumps({'files':generated},indent=2)+'\n')

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--packs',default=str(ROOT/'localization/packs'))
    parser.add_argument('--apply-to',help='Customizer app directory in a synced AOSP tree')
    parser.add_argument('--report',required=True)
    parser.add_argument('--required-locales',default='')
    args=parser.parse_args()
    if not Path(args.packs).is_dir(): parser.error('Language pack directory does not exist')
    try: packs=load_packs(args.packs)
    except (ValueError,KeyError,TypeError) as error: parser.error(str(error))
    required=[x for x in args.required_locales.split(',') if x]
    provided={'en'}|{x['locale'] for x in packs}
    missing=sorted(set(required)-provided)
    report={'scope':'AfricAndroid Customizer and launcher only','reviewed_app_locales':sorted(provided),'missing_required_app_locales':missing,'fully_translated_android_system':False,'native_review':'Declared by pack authors; machine checks do not verify linguistic accuracy','packs':[{'locale':x['locale'],'reviewer':x['reviewer'],'license':x['license']} for x in packs]}
    target=Path(args.report);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(report,indent=2)+'\n')
    if missing: parser.exit(2,'Missing required reviewed app language packs: '+','.join(missing)+'\n')
    if args.apply_to: apply(packs,args.apply_to)
    print(f'Validated {len(packs)} reviewed app language packs. Full Android system localization: NOT implemented.')

if __name__=='__main__': main()
