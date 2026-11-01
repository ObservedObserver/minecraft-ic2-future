#!/usr/bin/env python3
"""Prepare an isolated vanilla 26.3 client for resource review, never player saves.

Uses official Mojang downloads, checks SHA-1, and reuses matching installed libraries
read-only. Does not read account files. Sound assets are omitted to conserve disk.
This is an art review harness, not the future mod development launcher.
"""
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'.local/logistics-art'
INSTALLED=Path.home()/'Library/Application Support/minecraft'


def fetch(url):
    with urllib.request.urlopen(url,timeout=90) as r: return r.read()


def download(path,info):
    if path.exists() and hashlib.sha1(path.read_bytes()).hexdigest()==info['sha1']: return
    path.parent.mkdir(parents=True,exist_ok=True)
    last=None
    for _ in range(3):
        try:
            data=fetch(info['url'])
            assert hashlib.sha1(data).hexdigest()==info['sha1'],info['url']
            path.write_bytes(data)
            return
        except Exception as e: last=e
    raise last


def allowed(lib):
    result=not bool(lib.get('rules'))
    for rule in lib.get('rules',[]):
        os_rule=rule.get('os',{})
        if os_rule.get('name','osx')!='osx': continue
        if os_rule.get('arch','arm64') not in ('arm64','aarch64'):continue
        result=rule['action']=='allow'
    return result


def prepare():
    WORK.mkdir(parents=True,exist_ok=True)
    version_path=WORK/'26.3.json'
    if not version_path.exists():
        manifest=json.loads(fetch('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
        v=next(v for v in manifest['versions'] if v['id']=='26.3')
        version_path.write_bytes(fetch(v['url']))
    version=json.loads(version_path.read_text())
    runtimes=json.loads(fetch('https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json'))
    runtime=json.loads(fetch(runtimes['mac-os-arm64']['java-runtime-epsilon'][0]['manifest']['url']))
    jobs=[]; executables=[]; links=[]
    for name,entry in runtime['files'].items():
        path=WORK/'runtime'/name
        if entry['type']=='file':
            jobs.append((path,entry['downloads']['raw']))
            if entry.get('executable'):executables.append(path)
        elif entry['type']=='directory':path.mkdir(parents=True,exist_ok=True)
        elif entry['type']=='link':links.append((path,entry['target']))
    jobs.append((WORK/'client.jar',version['downloads']['client']))
    classpath=[]
    for lib in version['libraries']:
        if not allowed(lib):continue
        info=lib['downloads'].get('artifact')
        if not info:continue
        existing=INSTALLED/'libraries'/info['path']
        if existing.exists() and hashlib.sha1(existing.read_bytes()).hexdigest()==info['sha1']:
            classpath.append(str(existing))
        else:
            path=WORK/'libraries'/info['path'];jobs.append((path,info));classpath.append(str(path))
    assets=WORK/'assets'
    index_path=assets/'indexes'/f'{version["assetIndex"]["id"]}.json'
    download(index_path,version['assetIndex'])
    index=json.loads(index_path.read_text())
    omitted=0
    for name,obj in index['objects'].items():
        if name.endswith(('.ogg','.mp3','.wav')):omitted+=1;continue
        sha=obj['hash'];path=assets/'objects'/sha[:2]/sha
        existing=INSTALLED/'assets/objects'/sha[:2]/sha
        if existing.exists() and hashlib.sha1(existing.read_bytes()).hexdigest()==sha:
            path.parent.mkdir(parents=True,exist_ok=True)
            if not path.exists():path.symlink_to(existing)
        else:jobs.append((path,{'url':f'https://resources.download.minecraft.net/{sha[:2]}/{sha}','sha1':sha}))
    print('Downloading/verifying',len(jobs),'runtime, library and non-audio assets;',omitted,'audio assets omitted.',flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        list(pool.map(lambda task:download(*task),jobs))
    for path in executables:path.chmod(path.stat().st_mode|0o111)
    for path,target in links:
        path.parent.mkdir(parents=True,exist_ok=True)
        if not path.exists() and not path.is_symlink():path.symlink_to(target)
    game=WORK/'game';game.mkdir(exist_ok=True)
    (game/'resourcepacks').mkdir(exist_ok=True)
    shutil.copy2(ROOT/'mods/logistics/build/logistics-art-v1-resources.zip',game/'resourcepacks/logistics-art-v1-resources.zip')
    if not (game/'options.txt').exists():
        (game/'options.txt').write_text('resourcePacks:["vanilla","file/logistics-art-v1-resources.zip"]\nlang:en_us\nguiScale:2\nrenderDistance:6\nsimulationDistance:5\nmaxFps:60\nsoundCategory_master:0.0\npauseOnLostFocus:false\nshowSubtitles:false\nonboardAccessibility:false\n')
    java=next((WORK/'runtime').rglob('bin/java'))
    args=[str(java),'-XstartOnFirstThread','-Xmx1536m','-Djava.net.preferIPv4Stack=true','-cp',os.pathsep.join(classpath+[str(WORK/'client.jar')]),version['mainClass'],'--username','ArtReview','--version','26.3','--gameDir',str(game),'--assetsDir',str(assets),'--assetIndex',version['assetIndex']['id'],'--uuid','49a9ea61d3da4ab4a13498b303dedca1','--accessToken','0','--userType','legacy','--versionType','release','--width','1280','--height','800']
    (WORK/'launch.json').write_text(json.dumps(args))
    print('Prepared isolated client:',java,flush=True)


if __name__=='__main__':
    if sys.argv[1:] == ['launch']:
        args=json.loads((WORK/'launch.json').read_text())
        os.chdir(WORK)
        os.execv(args[0],args)
    else:prepare()
