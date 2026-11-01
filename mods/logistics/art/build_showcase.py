#!/usr/bin/env python3
"""Create an optional art-only datapack for a NEW, isolated creative review world.

It clears a small display volume near 0,80,0. Never run in a player world.
Uses item displays to inspect resource models, not functional conveyors.
"""
import json
from pathlib import Path

MOD=Path(__file__).resolve().parents[1]
OUT=MOD/'build/logistics-art-showcase'
TIERS=['low','medium','high']
SHAPES=['straight','left','right','merge_left','merge_right','ascending','descending']
OUT.mkdir(parents=True,exist_ok=True)
(OUT/'pack.mcmeta').write_text(json.dumps({'pack':{'min_format':[121,0],'max_format':[121,0],'description':'Logistics isolated art showcase ONLY'}}))
commands=['kill @e[type=minecraft:item_display,tag=logistics_art]',
          'fill -12 80 -8 12 90 16 minecraft:air',
          'fill -12 79 -8 12 79 16 minecraft:smooth_stone',
          'time set noon','weather clear','gamemode creative @s']

def display(x,y,z,model):
    nbt='{Tags:["logistics_art"],item:{id:"minecraft:paper",count:1,components:{"minecraft:item_model":"'+model+'"}},item_display:"none",brightness:{block:15,sky:15},transformation:{translation:[0.5f,0.5f,0.5f],scale:[1f,1f,1f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}'
    commands.append(f'summon minecraft:item_display {x} {y} {z} {nbt}')

for i,tier in enumerate(TIERS):
    x=(i-1)*4
    for z in range(4):display(x,80,z,'logistics:'+tier+'_conveyor')
    display(x,80,5,'logistics:'+tier+'_inserter')
    for k,shape in enumerate(SHAPES):display(-9+k*3,82,9+i*2,'logistics_review:'+tier+'_'+shape)
    for kind in ['conveyor','inserter']:
        commands.append(f'give @s minecraft:paper[minecraft:item_model="logistics:{tier}_{kind}"]')
commands.append('tp @s 8 85 7 130 30')
fn=OUT/'data/logistics_review/function/setup.mcfunction';fn.parent.mkdir(parents=True,exist_ok=True)
fn.write_text('\n'.join(commands)+'\n')
print(OUT)
