#!/usr/bin/env python3
import hashlib, pathlib, sys
root = pathlib.Path(sys.argv[1]).resolve()
with (root / 'SHA256SUMS').open('w') as out:
    for p in sorted(root.rglob('*')):
        if not p.is_file() or p.name == 'SHA256SUMS':
            continue
        h = hashlib.sha256()
        with p.open('rb') as stream:
            for block in iter(lambda: stream.read(1024 * 1024), b''):
                h.update(block)
        out.write(f'{h.hexdigest()}  {p.relative_to(root)}\n')
