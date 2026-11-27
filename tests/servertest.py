#!/usr/bin/env python3
"""Headless scenario test harness for Don't Starve Craft.

Usage: tests/servertest.py <project-dir> <scenario-file> [--port N] [--player NAME] [--client] [--keep-world]

Starts `gradlew runServer` in <project-dir> (world in ./run), waits for "Done",
sends the commands of the scenario over RCON, prints responses, evaluates the
assertions, and stops the server. Exit code 0 = every assertion passed.
Scenarios that need a real online player pass --player NAME and use {P};
--client also starts that player as a headless, silent game client
(tools/autotest.sh in gamescope) that joins the test server by itself.

Scenario syntax (one entry per line, commands without the slash):
- plain command      -> sent over RCON, response printed; a "[x, y, z]" triple
                        in the response is captured into X/Y/Z variables that
                        later lines can use as {X}, {X+16} (also LX/LZ aliases)
- `// text`          -> comment
- `#sleep N`         -> wait N seconds
- `#expect REGEX`    -> assert the previous command's response matches
- `#expect-log REGEX [timeout]` -> wait for a server log line to appear
Environment: DST_JAVA_HOME (a JDK 21, else JAVA_HOME is used as is).
"""
import os, re, socket, struct, subprocess, sys, threading, time

RCON_PASSWORD = 'dst-test'

def rcon_packet(req_id, ptype, body):
    data = struct.pack('<ii', req_id, ptype) + body.encode('utf-8') + b'\x00\x00'
    return struct.pack('<i', len(data)) + data

def rcon_read(sock):
    raw = b''
    while len(raw) < 4:
        chunk = sock.recv(4 - len(raw))
        if not chunk:
            raise ConnectionError('rcon closed')
        raw += chunk
    (length,) = struct.unpack('<i', raw)
    data = b''
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise ConnectionError('rcon closed')
        data += chunk
    req_id, ptype = struct.unpack('<ii', data[:8])
    return req_id, data[8:-2].decode('utf-8', 'replace')

def listening_pids(port):
    out = subprocess.run(['ss', '-ltnpH', f'sport = :{port}'], capture_output=True, text=True).stdout
    return sorted({int(m) for m in re.findall(r'pid=(\d+)', out)})

def main():
    args = sys.argv[1:]
    port = 25699
    keep = False
    player = 'Tester'
    if '--port' in args:
        i = args.index('--port'); port = int(args[i + 1]); del args[i:i + 2]
    if '--player' in args:
        i = args.index('--player'); player = args[i + 1]; del args[i:i + 2]
    client = False
    if '--client' in args:
        client = True; args.remove('--client')
    if '--keep-world' in args:
        keep = True; args.remove('--keep-world')
    project, cmdfile = args[0], args[1]
    game_port = port - 100
    for p in (port, game_port):
        with socket.socket() as probe:
            probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            try:
                probe.bind(('0.0.0.0', p))
            except OSError:
                print(f'port {p} already in use — another server still running? '
                      f'(find it with: ss -tlnp | grep {p})')
                sys.exit(3)
    run = os.path.join(project, 'run')
    os.makedirs(run, exist_ok=True)
    if not keep:
        for world in ('world', 'world_nether', 'world_the_end', 'dontstarve_islands'):
            subprocess.run(['rm', '-rf', os.path.join(run, world)])
    with open(os.path.join(run, 'eula.txt'), 'w') as f:
        f.write('eula=true\n')
    game_port = port - 100
    with open(os.path.join(run, 'server.properties'), 'w') as f:
        f.write(f"""enable-rcon=true
rcon.password={RCON_PASSWORD}
rcon.port={port}
server-port={game_port}
online-mode=false
level-seed=dst-test
spawn-protection=0
view-distance=6
simulation-distance=6
motd=dst-test
max-tick-time=-1
spawn-npcs=false
spawn-animals=true
""")
    log_path = os.path.join(run, 'test-server.log')
    log = open(log_path, 'w')
    env = dict(os.environ)
    if os.environ.get('DST_JAVA_HOME'):
        env['JAVA_HOME'] = os.environ['DST_JAVA_HOME']
    proc = subprocess.Popen(['./gradlew', '--console=plain', 'runServer'], cwd=project,
                            env=env, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, bufsize=1)
    lines = []
    def pump():
        for line in proc.stdout:
            log.write(line); log.flush()
            lines.append(line)
    threading.Thread(target=pump, daemon=True).start()

    cursor = [0]

    def wait_for(regex, timeout):
        # Only lines after the previous match count, so repeated waits work.
        pat = re.compile(regex)
        start = time.time(); seen = cursor[0]
        while time.time() - start < timeout:
            while seen < len(lines):
                if pat.search(lines[seen]):
                    cursor[0] = seen + 1
                    return lines[seen]
                seen += 1
            if proc.poll() is not None:
                return None
            time.sleep(0.5)
        return None

    checks = []  # (name, ok)

    def check(name, ok, detail=''):
        checks.append((name, ok))
        print(f'{"PASS" if ok else "FAIL"}: {name}' + (f'  [{detail[:160]}]' if detail and not ok else ''))

    if not wait_for(r'Done \(.*\)! For help', 900):
        print('SERVER DID NOT START; tail of log:')
        print(''.join(lines[-80:]))
        proc.kill()
        sys.exit(2)
    print('SERVER STARTED')
    client_proc = None
    if client:
        client_proc = subprocess.Popen(
            ['sh', 'tools/autotest.sh', 'runAutotestClient',
             f'-PautotestServer=localhost:{game_port}', f'-PautotestUser={player}'],
            cwd=project, env=env, stdin=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)
        print(f'CLIENT STARTED as {player} (log: build/autotest.log)')
    sock = socket.create_connection(('127.0.0.1', port), timeout=30)
    sock.sendall(rcon_packet(1, 3, RCON_PASSWORD))
    rcon_read(sock)
    vars_ = {'P': player}
    last_resp = ['']
    for raw in open(cmdfile):
        cmd = raw.strip()
        if not cmd or cmd.startswith('//'):
            continue
        if cmd.startswith('#sleep'):
            time.sleep(float(cmd.split()[1])); continue
        if cmd.startswith('#wait-player'):
            parts = cmd.split(maxsplit=2)
            name = parts[1].replace('{P}', player) if len(parts) > 1 else player
            timeout = int(parts[2]) if len(parts) > 2 else 180
            start = time.time(); online = False
            while time.time() - start < timeout:
                sock.sendall(rcon_packet(2, 2, f'data get entity {name} UUID'))
                resp = rcon_read(sock)[1]
                if 'has the following entity data' in resp:
                    online = True; break
                time.sleep(2)
            check(f'player {name} online', online)
            continue
        if cmd.startswith('#expect-log'):
            m = re.match(r'(.*)\s+(\d+)$', cmd[len('#expect-log'):].strip())
            regex = m.group(1) if m else cmd[len('#expect-log'):].strip()
            timeout = int(m.group(2)) if m else 120
            found = wait_for(regex, timeout)
            check(f'log ~ /{regex}/', found is not None, str(found))
            continue
        if cmd.startswith('#expect'):
            regex = cmd[len('#expect'):].strip()
            check(f'response ~ /{regex}/', re.search(regex, last_resp[0]) is not None, last_resp[0])
            continue
        # {P} is the online player name; {X}/{Y}/{Z} ({X+16}) come from the last
        # "[x, y, z]" triple. Braces holding anything else (NBT tags) pass through.
        cmd = cmd.replace('{P}', player)
        def subst(m):
            try:
                return str(int(eval(m.group(1), {'__builtins__': {}}, vars_)))
            except NameError:
                print(f'!! {m.group(0)} used before any "[x, y, z]" response was captured')
                return m.group(0)
        cmd = re.sub(r'\{\s*([XYZ](?:\s*[-+*/()\s0-9]+)?)\}', subst, cmd)
        # Long replies arrive as several packets back to back: read until a short silence.
        sock.sendall(rcon_packet(2, 2, cmd))
        sock.settimeout(180)
        parts = [rcon_read(sock)[1]]
        sock.settimeout(0.5)
        try:
            while True:
                parts.append(rcon_read(sock)[1])
        except (socket.timeout, TimeoutError):
            pass
        sock.settimeout(180)
        resp = ''.join(parts)
        last_resp[0] = resp
        print(f'> {cmd}\n{resp}')
        # Island messages contain decoys like "island 900 centered at 7372800, 0, 0":
        # only accept triples whose first component looks like a coordinate.
        # "centered at X, Y, Z" is always an island centre, even island 0 at the origin.
        loc = (re.search(r'centered at (-?\d+), (-?\d+), (-?\d+)', resp)
               or re.search(r'\[?(-?\d{4,}), (-?\d{1,5}), (-?\d{4,}|-?\d{1,3})\]?', resp))
        if loc:
            vars_.update(X=int(loc.group(1)), Y=int(loc.group(2)), Z=int(loc.group(3)))
    if client_proc is not None:
        try:
            os.killpg(client_proc.pid, 15)
            client_proc.wait(timeout=60)
        except Exception:
            pass
    try:
        sock.sendall(rcon_packet(3, 2, 'stop'))
    except Exception:
        pass
    try:
        proc.wait(timeout=120)
    except subprocess.TimeoutExpired:
        proc.kill()
    # The server JVM is a child of the Gradle daemon, not of gradlew: if it did
    # not stop by itself it would keep the ports, so kill it by its RCON port.
    for pid in listening_pids(port):
        print(f'!! server JVM {pid} did not stop, killing it')
        subprocess.run(['kill', '-9', str(pid)])

    failed = [name for name, ok in checks if not ok]
    exceptions = [l for l in lines if re.search(r'Exception|ERROR', l)
                  and 'Failed to load recipe' not in l]
    if exceptions:
        print(f'--- {len(exceptions)} exception/error log lines (first 10) ---')
        print(''.join(exceptions[:10]))
    print(f'=== {len(checks) - len(failed)}/{len(checks)} checks passed'
          + (f': FAILED {failed}' if failed else ''))
    sys.exit(1 if failed else 0)

if __name__ == '__main__':
    main()
