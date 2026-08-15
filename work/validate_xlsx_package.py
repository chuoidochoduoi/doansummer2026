import posixpath, zipfile, sys
from xml.etree import ElementTree as ET

p=sys.argv[1]
with zipfile.ZipFile(p) as z:
    names=set(z.namelist())
    missing=[]
    for relname in (n for n in names if n.endswith('.rels')):
        if relname == '_rels/.rels':
            base=''
        else:
            d=posixpath.dirname(relname)
            base=posixpath.dirname(d) if posixpath.basename(d)=='_rels' else d
        for rel in ET.fromstring(z.read(relname)):
            if rel.get('TargetMode')=='External':
                continue
            target=rel.get('Target','')
            resolved=target.lstrip('/') if target.startswith('/') else posixpath.normpath(posixpath.join(base,target))
            if resolved not in names:
                missing.append((relname,target,resolved))
    print({'zip_error':z.testzip(),'parts':len(names),'missing_relationship_targets':missing})
