#!/usr/bin/env python3
"""Report scoped feature gaps. Source presence is not runtime verification."""
from pathlib import Path
import argparse,json
ROOT=Path(__file__).resolve().parents[1]
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--report',required=True);p.add_argument('--strict',action='store_true');a=p.parse_args()
 definitions=json.loads((ROOT/'config/capabilities.json').read_text())
 missing=[x['id'] for x in definitions['features'] if x['status']!='runtime_verified']
 report={'exact_cross_os_parity':False,'scope':definitions['scope'],'features':definitions['features'],'not_runtime_verified':missing}
 target=Path(a.report);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(report,indent=2)+'\n')
 print(f'{len(missing)} of {len(definitions["features"])} scoped capabilities lack runtime verification; exact cross-OS parity is not implemented.')
 if a.strict and missing:p.exit(2,'Strict feature release gate failed. See report for outstanding capabilities.\n')
if __name__=='__main__':main()
