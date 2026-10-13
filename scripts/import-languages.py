#!/usr/bin/env python3
from pathlib import Path
import csv,json,hashlib,sys
r=Path(__file__).resolve().parents[1]
rows=list(csv.DictReader(open(sys.argv[1],encoding='utf-8')))
entries=[]
for x in rows:
 if x['Level'] not in ('language','dialect') or 'Africa' not in x['Macroarea'].split(';'): continue
 entries.append({'id':x['ID'],'name':x['Name'],'iso639_3':x['ISO639P3code'],'level':x['Level'],'countries':x['Countries'].split(';') if x['Countries'] else [],'parent':x['Language_ID'],'system_translation':'unverified','keyboard':'unverified','speech':'unverified'})
entries.sort(key=lambda x:(x['name'].casefold(),x['id']))
assets=r/'vendor/africandroid/apps/Customizer/assets/languages';assets.mkdir(parents=True,exist_ok=True)
for old in assets.glob('part-*.json'): old.unlink()
shards=[]
for i in range(0,len(entries),150):
 name=f'part-{i//150:03}.json';(assets/name).write_text(json.dumps(entries[i:i+150],ensure_ascii=False,separators=(',',':'))+'\n');shards.append(name)
counts={k:sum(x['level']==k for x in entries) for k in ('language','dialect')}
meta={'source':'Glottolog 5.3','url':'https://github.com/glottolog/glottolog-cldf/tree/v5.3','license':'CC-BY-4.0','source_sha256':hashlib.sha256(Path(sys.argv[1]).read_bytes()).hexdigest(),'filter':'Macroarea includes Africa; Level language or dialect','counts':counts,'files':shards,'note':'Geographic catalog, not a claim of exhaustiveness or translated Android UI. Includes dialects and extinct varieties where present in source; missing macroareas require manual review.'}
(assets/'index.json').write_text(json.dumps(meta,indent=2)+'\n')
