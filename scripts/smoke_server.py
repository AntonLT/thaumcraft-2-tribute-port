#!/usr/bin/env python3
"""Boot an isolated dev server, run assertions, stop cleanly, and retain its full log."""
import argparse
from datetime import datetime, timezone
import os
from pathlib import Path
import queue
import signal
import subprocess
import threading
import time

parser=argparse.ArgumentParser()
parser.add_argument("loader",choices=["fabric","neoforge"])
parser.add_argument("--java-home",required=True)
parser.add_argument("--archive-world",action="store_true",help="Retain an existing development world under a timestamped name before a fresh run")
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
project=root/args.loader
pass_marker="THAUMCRAFT_SMOKE_TESTS_PASS"
run=project/"runs/server"
run.mkdir(parents=True,exist_ok=True)
if (run/"world").exists():
    if not args.archive_world:
        raise SystemExit("Refusing to reuse a development world: move runs/server/world or pass --archive-world before a fresh smoke test.")
    archived=run/("world-smoke-"+datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ"))
    (run/"world").rename(archived)
    print("Retained previous test world: "+str(archived),flush=True)
(run/"eula.txt").write_text("eula=true\n")
(run/"server.properties").write_text("server-ip=127.0.0.1\nserver-port="+{"fabric":"25575","neoforge":"25576"}[args.loader]+"\nonline-mode=false\nlevel-seed=216\nview-distance=3\nsimulation-distance=3\nmax-tick-time=120000\n")
env=os.environ.copy();env["JAVA_HOME"]=args.java_home;env["PATH"]=args.java_home+"/bin:"+env["PATH"]
log_path=root/".cache"/(args.loader+"-smoke.log");log_path.parent.mkdir(exist_ok=True)
if log_path.exists():
    log_path.rename(log_path.with_name(args.loader+"-smoke-"+datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")+".log"))
lines=queue.Queue();recent=[];passed=False;stopped=False
process=subprocess.Popen(["./gradlew",":"+args.loader+":runServer","-PsmokeTest","--console=plain"],cwd=root,env=env,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1,start_new_session=True)
def read():
    for line in process.stdout:lines.put(line)
threading.Thread(target=read,daemon=True).start()
deadline=time.monotonic()+360
with log_path.open("w") as log:
    while process.poll() is None or not lines.empty():
        if time.monotonic()>deadline:
            os.killpg(process.pid,signal.SIGTERM)
            try:process.wait(timeout=10)
            except subprocess.TimeoutExpired:os.killpg(process.pid,signal.SIGKILL);process.wait()
            raise SystemExit("Smoke test timed out; full log: "+str(log_path))
        try:line=lines.get(timeout=1)
        except queue.Empty:continue
        log.write(line);log.flush();recent.append(line);recent=recent[-100:]
        if pass_marker in line:
            passed=True;print(line.strip(),flush=True)
        if passed and not stopped and ('Done (' in line or pass_marker in line):
            # Gradle's run task is configured to forward this process's stdin.
            process.stdin.write("stop\n");process.stdin.flush();stopped=True
        if any(marker in line for marker in ["ERROR","Exception","AssertionError","FAILED","Starting minecraft server"]):print(line.strip(),flush=True)
code=process.wait()
if code or not passed:print("".join(recent))
print("Full log: "+str(log_path))
raise SystemExit(0 if code==0 and passed else 1)
