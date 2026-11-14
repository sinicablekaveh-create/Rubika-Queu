from pathlib import Path
import json
from datetime import datetime, timezone
import zipfile,sys,hashlib,re
project=Path(sys.argv[1]).resolve()
apk=Path(sys.argv[2]).resolve()
archive=Path(sys.argv[3]).resolve()
required_dirs=['app','scripts','overrides','patches','tests','gradle','.github','docs','examples','prebuilt','source','phone-companion']
root_files=['AGENTS.md','README.md','README-HTTPS-FA.md','README-V180-CPP.md','README-V2-FA.md','README-FIXED-FA.md','BUILD_REPORT_V163.md','TEST_REPORT_V163.md','REORGANIZATION-PROPOSAL.md','build-fixed-apk.sh','build.sh','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','.gitignore']
disallowed_ext={'.p12','.pfx','.jks','.keystore','.pem','.key','.pyc'}
paths=[]
for name in required_dirs:
 p=project/name
 if p.exists():
  paths.extend(v for v in p.rglob('*') if v.is_file())
paths.extend(project/n for n in root_files if (project/n).is_file())
sensitive_markers=[b'-----BEGIN '+b'PRIVATE KEY-----',b'github_'+b'pat_',b'gh'+b'o_',b'gh'+b'p_']
for f in sorted(set(paths)):
 rel=f.relative_to(project)
 if any(part in {'.git','.gradle','.local-build','__pycache__','.idea','build','outputs','.cxx'} for part in rel.parts):continue
 if f.suffix.lower() in disallowed_ext:continue
 if any(x.lower() in str(rel).lower() for x in ['hosts.yml','session-store','keystore','private-key']):continue
 data=f.read_bytes()
 if any(m in data for m in sensitive_markers):
  print('Refusing archive due to credential marker:',rel)
  sys.exit(3)
# selected, bounded input archives; no private signing artifacts
 if len(data)>20_000_000:continue
 filestr=str(rel).replace('\\','/')
 with_null=False
 # Avoid implicit symlinks outside project
 if f.is_symlink():continue
 # archive only approved files
 if len(paths)>5000:raise RuntimeError('too many files')
 with_null=False
 # accumulate
 if filestr:globals().setdefault('selected',[]).append((filestr,data))
archive.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for name,data in selected:
  z.writestr(name,data)
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
checksum=archive.parent/'Rubika-Triple-Queue-v2.0.0-SHA256SUMS.txt'
checksum.write_text(f"{digest(apk)}  {apk.name}\n{digest(archive)}  {archive.name}\n",encoding='utf-8')
print('ARCHIVE_FILES',len(selected),'SOURCE_ZIP',archive.stat().st_size,'APK_BYTES',apk.stat().st_size)
print('SOURCE_SHA256',digest(archive))
print('APK_SHA256',digest(apk))

