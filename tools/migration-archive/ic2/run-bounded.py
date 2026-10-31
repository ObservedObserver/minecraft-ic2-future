#!/usr/bin/env python3
"""Run only this checkout's child process group with a finite deadline and an audit record."""
import argparse,json,os,signal,subprocess,time
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--seconds',type=int,required=True);p.add_argument('--log',required=True);p.add_argument('--stop-file');p.add_argument('command',nargs=argparse.REMAINDER);args=p.parse_args()
command=args.command
if command and command[0]=='--':command=command[1:]
if not command:raise SystemExit('command required')
log=Path(args.log);log.parent.mkdir(parents=True,exist_ok=True)
start=time.time();timeout=False;stopped=False
with log.open('w') as out:
 process=subprocess.Popen(command,stdout=out,stderr=subprocess.STDOUT,start_new_session=True)
 log.with_suffix(log.suffix+'.started.json').write_text(json.dumps({'pid':process.pid,'command':command,'cwd':str(Path.cwd())},indent=2)+'\n')
 try:
  while True:
   remaining=args.seconds-(time.time()-start)
   if remaining<=0:raise subprocess.TimeoutExpired(command,args.seconds)
   if args.stop_file and Path(args.stop_file).exists():
    stopped=True;raise subprocess.TimeoutExpired(command,args.seconds)
   try:
    code=process.wait(timeout=min(1,remaining));break
   except subprocess.TimeoutExpired:pass
 except subprocess.TimeoutExpired:
  timeout=not stopped;os.killpg(process.pid,signal.SIGTERM)
  try:code=process.wait(timeout=20)
  except subprocess.TimeoutExpired:
   os.killpg(process.pid,signal.SIGKILL);code=process.wait()
record={'command':command,'cwd':str(Path.cwd()),'pid':process.pid,'deadline_seconds':args.seconds,'timed_out':timeout,'stop_requested':stopped,'exit_code':code,'elapsed_seconds':round(time.time()-start,2)}
log.with_suffix(log.suffix+'.run.json').write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record));raise SystemExit(124 if timeout else code)
