#!/usr/bin/env python3
"""Author deterministic original pixel textures and Minecraft JSON review assets.

No generated concept image is cropped, resampled, or used as a game texture.
Only the Python standard library is needed. Run `build`, then `check`.
"""
import argparse
import base64
import hashlib
import json
import math
from pathlib import Path
import struct
import zlib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MOD = ROOT / 'mods/logistics'
RES = MOD / 'src/main/resources'
ASSETS = RES / 'assets/logistics'
ART = MOD / 'art'
REVIEW = ROOT / 'docs/design/logistics/art-v1'
CONFIG = json.loads((ART / 'tiers.json').read_text())
SHAPES = ['straight', 'left', 'right', 'merge_left', 'merge_right', 'ascending', 'descending']
FACES = ['down', 'up', 'north', 'south', 'west', 'east']


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n')


def rgb(value):
    return tuple(bytes.fromhex(value.lstrip('#'))) + (255,)


def png(path, rows):
    h, w = len(rows), len(rows[0])
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    raw = b''.join(b'\0' + bytes(c for px in row for c in px) for row in rows)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    data += chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def pixels(color, size=16):
    return [[rgb(color) for _ in range(size)] for _ in range(size)]


def texture(tier, name, frames):
    path = ASSETS / 'textures/block' / tier['id'] / (name + '.png')
    png(path, [row for frame in frames for row in frame])
    if len(frames) > 1:
        write_json(Path(str(path) + '.mcmeta'), {'animation': {'frametime': tier['frame_ticks'], 'width': len(frames[0][0]), 'height': len(frames[0]), 'interpolate': False}})
    return 'logistics:block/' + tier['id'] + '/' + name


def arrow(p, x, y, color, facing='north'):
    coords = [(0,0),(-1,1),(0,1),(1,1),(-2,2),(-1,2),(0,2),(1,2),(2,2),(0,3),(0,4),(0,5),(0,6)]
    for dx, dy in coords:
        if facing == 'east': dx,dy = -dy,dx
        if facing == 'west': dx,dy = dy,-dx
        xx, yy = x+dx, y+dy
        if 0 <= xx < len(p[0]) and 0 <= yy < len(p): p[yy][xx] = color


def make_textures(t):
    trim = list(map(rgb, t['trim']))
    frame = pixels(t['trim'][1])
    for y in range(16):
        for x in range(16):
            frame[y][x] = trim[2 if y in (0,8) else 0 if y in (7,15) else 1]
            if (x*7+y*11)%31 == 0: frame[y][x] = trim[2]
    for i in range(t['marks']):
        for y in (3,4,11,12):
            frame[y][5+i*3] = rgb(t['accent'])
    texture(t, 'frame', [frame])
    metal = pixels(t['trim'][2])
    for y in range(16):
        for x in range(16):
            metal[y][x] = trim[3 if x==0 or y==0 else 0 if x==15 or y==15 else 1 if (x+y*3)%19==0 else 2]
    texture(t, 'housing', [metal])
    dark = pixels('#20262b')
    for y in range(16):
        for x in range(16):
            dark[y][x] = rgb('#343d43' if (x+y)%4==0 else '#20262b')
    texture(t, 'underside', [dark])
    label = pixels(t['trim'][1])
    for i in range(t['marks']):
        for y in range(4,12):
            for x in range(3+i*4,5+i*4): label[y][x] = rgb(t['accent'])
    texture(t, 'badge', [label])
    size = CONFIG['belt_texture_size']
    for shape in ['straight','left','right','merge_left','merge_right']:
        frames=[]
        for phase in range(8):
            p=pixels('#30383d',size)
            for y in range(size):
                for x in range(size):
                    d=y
                    if shape == 'left': d=max(size-1-x,y)
                    if shape == 'right': d=max(x,y)
                    if shape == 'merge_left' and x<12 and 12<y<21: d=size-1-x
                    if shape == 'merge_right' and x>19 and 12<y<21: d=x
                    # Narrow, low-contrast seams; less noise than broad black treads.
                    stripe=(d+phase)%16
                    p[y][x]=rgb('#424c52' if stripe==0 else '#2b3338' if stripe==1 else '#30383d')
                    if x in (3,28): p[y][x]=rgb('#56636a')
            if shape in ('left','merge_left'):
                arrow(p,11,17,rgb(t['accent']),'east')
                arrow(p,16,7,rgb(t['accent']))
            elif shape in ('right','merge_right'):
                arrow(p,20,17,rgb(t['accent']),'west')
                arrow(p,16,7,rgb(t['accent']))
            else: arrow(p,16,11,rgb(t['accent']))
            frames.append(p)
        texture(t, 'belt_'+shape, frames)
        texture(t, 'belt_'+shape+'_idle', [frames[0]])


def box(name, a, b, tex='#frame', top=None, uv=None, faces=None):
    result={'name':name,'from':a,'to':b,'faces':{}}
    x0,y0,z0=a; x1,y1,z1=b
    defaults={'up':[x0,z0,x1,z1],'down':[x0,16-z1,x1,16-z0],
              'north':[16-x1,16-y1,16-x0,16-y0], 'south':[x0,16-y1,x1,16-y0],
              'west':[z0,16-y1,z1,16-y0], 'east':[16-z1,16-y1,16-z0,16-y0]}
    for f in faces or FACES:
        result['faces'][f]={'texture':top if f=='up' and top else tex,'uv':uv or [round(v,5) for v in defaults[f]]}
    return result


DISPLAY={'gui':{'rotation':[30,225,0],'translation':[0,1,0],'scale':[0.8,0.8,0.8]},
         'ground':{'translation':[0,3,0],'scale':[0.5,0.5,0.5]},
         'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[0.8,0.8,0.8]},
         'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,3,0],'scale':[0.5,0.5,0.5]},
         'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[0.375,0.375,0.375]}}


def model(t, shape, idle=False):
    tier=t['id']; slope=shape in ('ascending','descending')
    pattern='straight' if slope else shape
    textures={k:f'logistics:block/{tier}/{v}' for k,v in {'frame':'frame','housing':'housing','badge':'badge','bottom':'underside','belt':'belt_'+pattern+('_idle' if idle else '')}.items()}
    textures['particle']=textures['frame']
    e=[]
    if slope:
        # A single rotated slab spans a one-cell rise. Top endpoints are y=2/18,
        # z=16/0 for ascending; no staircase ribs or supporting structure.
        half=8*math.sqrt(2)
        for name,x0,x1,top in [('deck',0,16,10),('left_edge',0,1,10.35),('right_edge',15,16,10.35)]:
            element=box(name,[x0,10-math.sqrt(2),8-half],[x1,top,8+half],
                        '#bottom' if name=='deck' else '#frame',
                        '#belt' if name=='deck' else None,uv=[0,0,16,16])
            element['rotation']={'origin':[8,10,8],'axis':'x',
                                 'angle':45 if shape=='ascending' else -45,'rescale':False}
            e.append(element)
    else:
        e.append(box('deck',[0,0,0],[16,2,16],'#bottom','#belt'))
        if shape not in ('left','merge_left'): e.append(box('left_edge',[0,0,0],[1,2.5,16]))
        else:
            e.append(box('left_front',[0,0,0],[1,2.5,3]))
            e.append(box('left_back',[0,0,13],[1,2.5,16]))
        if shape not in ('right','merge_right'): e.append(box('right_edge',[15,0,0],[16,2.5,16]))
        else:
            e.append(box('right_front',[15,0,0],[16,2.5,3]))
            e.append(box('right_back',[15,0,13],[16,2.5,16]))
        # A turn has its input on the side and output north, with a closed south edge.
        if shape in ('left','right'): e.append(box('back_edge',[0,0,15],[16,2.5,16]))
    for element in e:
        if 'edge' in element['name'] or element['name'].startswith(('left_','right_')):
            for face in ('north','south','west','east'):
                if face in element['faces']:
                    element['faces'][face]['uv']=[0,0,16,8]
        elif slope:
            for face in ('north','south'):
                element['faces'][face]['uv']=[0,0,16,2]
    return {'parent':'minecraft:block/block','ambientocclusion':False,'textures':textures,'elements':e,'display':DISPLAY}


def inserter(t, part='complete'):
    textures={k:f'logistics:block/{t["id"]}/{v}' for k,v in {'frame':'frame','housing':'housing','badge':'badge','bottom':'underside'}.items()}
    textures['particle']=textures['housing']
    base=[box('foot',[5,0,5],[11,2,11],'#frame'),box('shaft',[7,2,7],[9,6,9],'#bottom'),box('motor',[5,5,5],[11,8,11],'#housing')]
    base[-1]['faces']['south']={'texture':'#badge','uv':[0,0,16,16]}
    arm=[box('shoulder',[6,8,6],[10,10,10],'#frame'),box('boom',[7,8,1],[9,10,8],'#housing'),box('wrist',[5,7,1],[11,9,3],'#frame'),box('left_finger',[5,5,0],[6,8,3],'#housing'),box('right_finger',[10,5,0],[11,8,3],'#housing')]
    return {'parent':'minecraft:block/block','ambientocclusion':False,'textures':textures,'elements':base if part=='base' else arm if part=='arm' else base+arm,'display':DISPLAY}


def build():
    REVIEW.mkdir(parents=True,exist_ok=True)
    inventory=[]; preview_models={}; zh={}; en={}
    for t in CONFIG['tiers']:
        make_textures(t)
        tier=t['id']
        for shape in SHAPES:
            for idle in (False,True):
                name=f'{tier}/belt_{shape}'+('_idle' if idle else '')
                m=model(t,shape,idle)
                write_json(ASSETS/'models/block'/f'{name}.json',m)
                preview_models[name]=m
        for part in ['complete','base','arm']:
            name=f'{tier}/inserter'+('' if part=='complete' else '_'+part)
            m=inserter(t,part)
            write_json(ASSETS/'models/block'/f'{name}.json',m)
            preview_models[name]=m
        for kind,label in [('conveyor','传送带'),('inserter','分拣器')]:
            ident=f'{tier}_{kind}'
            mref=f'logistics:block/{tier}/'+('belt_straight' if kind=='conveyor' else 'inserter')
            write_json(ASSETS/'items'/f'{ident}.json',{'model':{'type':'minecraft:model','model':mref}})
            zh['block.logistics.'+ident]=t['zh']+label
            en['block.logistics.'+ident]=t['en']+(' Conveyor' if kind=='conveyor' else ' Inserter')
            variants={}
            for rot,direction in enumerate(['north','east','south','west']):
                if kind=='conveyor':
                    for shape in SHAPES:
                        for running in [True,False]:
                            variants[f'facing={direction},running={str(running).lower()},shape={shape}']={'model':f'logistics:block/{tier}/belt_{shape}'+('' if running else '_idle'),'y':rot*90}
                else: variants['facing='+direction]={'model':mref,'y':rot*90}
            write_json(ASSETS/'blockstates'/f'{ident}.json',{'variants':variants})
            inventory.append({'id':'logistics:'+ident,'name':zh['block.logistics.'+ident],'tier':tier,'kind':kind,'model':mref})
    write_json(ASSETS/'lang/zh_cn.json',zh); write_json(ASSETS/'lang/en_us.json',en)
    write_json(RES/'pack.mcmeta',{'pack':{'min_format':[97,1],'max_format':[97,1],'description':'Logistics '+CONFIG['revision']+' · review resources · no gameplay'}})
    write_json(ART/'rig.json',{'inserter':{'base_model':'{tier}/inserter_base','arm_model':'{tier}/inserter_arm','pivot':[8,8,8],'axis':'y','preview_angles':[0,180],'note':'Review pose animation only. Does not implement item transfer or a block-entity renderer.'}})
    # Vanilla item_model components let these exact models be inspected without mod registration.
    pack=MOD/'build/logistics-art-v1-resources.zip'
    pack.parent.mkdir(parents=True,exist_ok=True)
    preview_aliases={}
    for t in CONFIG['tiers']:
        for shape in SHAPES:
            preview_aliases[f'assets/logistics_review/items/{t["id"]}_{shape}.json']={'model':{'type':'minecraft:model','model':f'logistics:block/{t["id"]}/belt_{shape}'}}
    with zipfile.ZipFile(pack,'w',zipfile.ZIP_DEFLATED) as z:
        for f in sorted(RES.rglob('*')):
            if f.is_file():
                info=zipfile.ZipInfo(f.relative_to(RES).as_posix(),(2026,10,31,0,0,0))
                info.compress_type=zipfile.ZIP_DEFLATED
                z.writestr(info,f.read_bytes())
        for name,obj in sorted(preview_aliases.items()):
            info=zipfile.ZipInfo(name,(2026,10,31,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
            z.writestr(info,json.dumps(obj))
    texture_data={}
    for f in sorted((ASSETS/'textures').rglob('*.png')):
        key='logistics:'+f.relative_to(ASSETS/'textures').with_suffix('').as_posix()
        meta=Path(str(f)+'.mcmeta')
        texture_data[key]={'url':'data:image/png;base64,'+base64.b64encode(f.read_bytes()).decode(), 'animation':json.loads(meta.read_text())['animation'] if meta.exists() else None}
    payload={'tiers':CONFIG['tiers'],'models':preview_models,'textures':texture_data,'items':inventory,'rig':json.loads((ART/'rig.json').read_text())}
    (REVIEW/'assets.js').write_text('window.LOGISTICS_ASSETS = '+json.dumps(payload,ensure_ascii=False,separators=(',',':'))+';\n')
    write_json(ART/'manifest.json',{'revision':CONFIG['revision'],'namespace':'logistics','items':inventory,'shapes':SHAPES,'gameplay_implemented':False,'resource_pack':str(pack.relative_to(ROOT)), 'resource_pack_sha256':hashlib.sha256(pack.read_bytes()).hexdigest()})
    print('Built',len(preview_models),'models,',len(texture_data),'textures,',len(inventory),'device definitions.')


def check():
    models=list((ASSETS/'models').rglob('*.json'))
    pngs=list((ASSETS/'textures').rglob('*.png'))
    assert len(models)==17*len(CONFIG['tiers']) and len(pngs)==14*len(CONFIG['tiers'])
    face_count=0
    for path in models:
        m=json.loads(path.read_text()); tex=m['textures']
        for k,ref in tex.items():
            assert ref.startswith('logistics:'), (path,k,ref)
            assert (ASSETS/'textures'/ (ref.split(':',1)[1]+'.png')).exists(), (path,ref)
        for e in m['elements']:
            assert all(-16<=v<=32 for v in e['from']+e['to']),path
            assert all(a<=b for a,b in zip(e['from'],e['to'])),path
            if 'rotation' in e:
                assert e['rotation']['axis'] in ('x','y','z')
                assert e['rotation']['angle'] in (-45,-22.5,0,22.5,45)
            for face,data in e['faces'].items():
                assert face in FACES and data['texture'][1:] in tex,(path,face)
                assert len(data['uv'])==4 and all(math.isfinite(v) for v in data['uv']),path
                face_count+=1
    for path in pngs:
        data=path.read_bytes(); assert data[:8]==b'\x89PNG\r\n\x1a\n'
        w,h=struct.unpack('>II',data[16:24]); assert w in (16,32) and h in (w,w*8),(path,w,h)
        meta=Path(str(path)+'.mcmeta')
        assert meta.exists()==(h==w*8),path
    for path in (ASSETS/'blockstates').glob('*.json'):
        for v in json.loads(path.read_text())['variants'].values():
            assert (ASSETS/'models'/(v['model'].split(':')[1]+'.json')).exists()
            assert v.get('y',0) in [0,90,180,270]
    for path in (ASSETS/'items').glob('*.json'):
        ref=json.loads(path.read_text())['model']['model']
        assert (ASSETS/'models'/(ref.split(':')[1]+'.json')).exists()
    manifest=json.loads((ART/'manifest.json').read_text())
    for lang in ['zh_cn','en_us']:
        labels=json.loads((ASSETS/'lang'/f'{lang}.json').read_text())
        assert all('block.'+i['id'].replace(':','.') in labels for i in manifest['items'])
    pack=ROOT/manifest['resource_pack']
    assert hashlib.sha256(pack.read_bytes()).hexdigest()==manifest['resource_pack_sha256']
    with zipfile.ZipFile(pack) as z:
        assert z.testzip() is None
        assert not any(n.startswith('assets/minecraft/') for n in z.namelist())
        assert len(z.namelist())==len([p for p in RES.rglob('*') if p.is_file()])+len(SHAPES)*len(CONFIG['tiers'])
    report={'status':'passed','models':len(models),'textures':len(pngs),'faces_checked':face_count,'devices':2*len(CONFIG['tiers']),'blockstates':2*len(CONFIG['tiers']),'animated_textures':5*len(CONFIG['tiers']),'resource_pack_sha256':manifest['resource_pack_sha256'],'scope':'Static JSON/resource/PNG/link/ZIP validation, not an in-game or gameplay test.'}
    write_json(ROOT/f'docs/verification/logistics-{CONFIG["revision"]}/static-check.json',report)
    print(json.dumps(report,ensure_ascii=False))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command',choices=['build','check'])
    args=parser.parse_args()
    (build if args.command=='build' else check)()
