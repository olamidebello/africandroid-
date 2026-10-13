#!/usr/bin/env python3
"""Validate build-time location policy and optionally install AOSP app resources."""
from pathlib import Path
import argparse,json,xml.etree.ElementTree as ET
DEFAULTS={'sample_interval_ms':30000,'max_sample_age_ms':180000,'event_history_limit':200}
LIMITS={'sample_interval_ms':(1000,3600000),'max_sample_age_ms':(1000,300000),'event_history_limit':(1,2000)}
def validate(data):
 if set(data)!=set(DEFAULTS):raise ValueError('Exact profile keys required')
 for key,(low,high) in LIMITS.items():
  if type(data[key]) is not int or not low<=data[key]<=high:raise ValueError(f'Invalid {key}; require integer {low}..{high}')
 return data

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--profile');p.add_argument('--apply-to');p.add_argument('--report',required=True);a=p.parse_args()
 data=validate(json.loads(Path(a.profile).read_text()) if a.profile else DEFAULTS.copy())
 if a.apply_to:
  target=Path(a.apply_to)/'res/values/mobile_policy.xml';target.parent.mkdir(parents=True,exist_ok=True)
  root=ET.Element('resources')
  for key,value in data.items():ET.SubElement(root,'integer',name='geo_'+key).text=str(value)
  ET.indent(root);ET.ElementTree(root).write(target,encoding='utf-8',xml_declaration=True)
 target=Path(a.report);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps({'geofencing':data,'location_permissions_granted':False,'monitoring_enabled_by_default':False,'scope':'owner opt-in, launcher-only app restrictions; carrier labels are local app display'},indent=2)+'\n')
 print('Mobile integration profile validated. Permission and monitoring consent remain with the device owner.')
if __name__=='__main__':main()
