"""Build and sign Dolmiş with the Gradle wrapper, JDK 17 and Android SDK 36.
Private signing material is kept outside deliverables when --signing-directory is supplied.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import secrets
import subprocess
import sys

ROOT=Path(__file__).resolve().parent

def run(args, env):
    result=subprocess.run([str(x) for x in args],cwd=ROOT,env=env,capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=300)
    if result.returncode:
        raise RuntimeError(result.stdout+'\n'+result.stderr)
    return result.stdout

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk',type=Path,default=Path(os.environ.get('ANDROID_HOME',str(Path(os.environ.get('LOCALAPPDATA',''))/'Android'/'Sdk'))))
    parser.add_argument('--signing-directory',type=Path,default=ROOT/'.signing')
    parser.add_argument('--output',type=Path,default=ROOT/'dist'/'Dolmis-0.2.1.apk')
    args=parser.parse_args()
    sdk=args.sdk.resolve();tools=sdk/'build-tools'/'35.0.0'
    env=os.environ.copy();env['ANDROID_HOME']=str(sdk)
    wrapper=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
    print(run([wrapper,':app:assembleRelease'],env)[-2500:])
    build=Path(env.get('DOLMIS_BUILD_ROOT',str(ROOT/'.build'))).resolve()/'app'
    unsigned=build/'outputs'/'apk'/'release'/'app-release-unsigned.apk'
    aligned=build/'dolmis-aligned.apk'
    align=tools/('zipalign.exe' if os.name=='nt' else 'zipalign')
    run([align,'-f','-P','16','4',unsigned,aligned],env)
    private=args.signing_directory.resolve();private.mkdir(parents=True,exist_ok=True)
    key=private/'dolmis.p12';password=private/'password.txt'
    if key.exists()!=password.exists():raise RuntimeError('Restore matching signing key and password; do not replace an existing key.')
    if not key.exists():password.write_text(secrets.token_urlsafe(32),encoding='utf-8')
    env['DOLMIS_SIGN_PASSWORD']=password.read_text(encoding='utf-8').strip()
    if not key.exists():
        run(['keytool','-genkeypair','-keystore',key,'-storetype','PKCS12','-alias','dolmis','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Dolmis Independent Transit, O=Dolmis, C=TR','-storepass:env','DOLMIS_SIGN_PASSWORD','-keypass:env','DOLMIS_SIGN_PASSWORD','-noprompt'],env)
    output=args.output.resolve();output.parent.mkdir(parents=True,exist_ok=True)
    signer=tools/'lib'/'apksigner.jar'
    run(['java','-jar',signer,'sign','--ks',key,'--ks-key-alias','dolmis','--ks-pass','env:DOLMIS_SIGN_PASSWORD','--key-pass','env:DOLMIS_SIGN_PASSWORD','--v4-signing-enabled','false','--out',output,aligned],env)
    print(run(['java','-jar',signer,'verify','--verbose','--print-certs',output],env))
    run([align,'-c','-P','16','4',output],env)
    print(json.dumps({'apk':str(output),'sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'bytes':output.stat().st_size,'package':'org.dolmis.diyarbakir','version':'0.2.1'},ensure_ascii=False,indent=2))
    print('RESULT=signed')

if __name__=='__main__':
    try:main()
    except Exception as exc:print(str(exc),file=sys.stderr);sys.exit(1)
