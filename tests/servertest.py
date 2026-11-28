#!/usr/bin/env python3
"""Headless scenario test harness for Don't Starve Craft.

Usage: tests/servertest.py <project-dir> <scenario-file>... [--port N] [--player NAME] [--keep-world]

Starts `gradlew runServer` in <project-dir> (world in ./run) once, waits for "Done",
then runs the scenarios one after another over RCON in that single server session,
printing responses and evaluating the assertions. Between scenarios `ds test reset`
moves the run to a fresh island, restores test switches and game rules and sends the
player home as a fresh survivor, so scenarios start from a clean slate without a
server restart. Scenarios that use {P} need a real online player: before the first
of them the player is started once as a headless, silent game client
(tools/autotest.sh in gamescope) that joins the test server by itself and stays for
the rest of the session. Exit code 0 = every assertion of every scenario passed.

Scenario syntax (one entry per line, commands without the slash):
- plain command      -> sent over RCON, response printed; a "[x, y, z]" triple
                        in the response is captured into X/Y/Z variables that
                        later lines can use as {X}, {X+16} (also LX/LZ aliases)
- `// text`          -> comment
- `#sleep N`         -> wait N seconds
- `#wait-player NAME [timeout]` -> block until that player is online
- `#expect REGEX`    -> assert the previous command's response matches
- `#expect-log REGEX [timeout]` -> wait for a server log line to appear
- `#ds-wait N`       -> run N DS seconds of world time (N x 2.5 x 20 ticks) at full
                        speed with `tick sprint`, then continue
- `#shot NAME [hud]` -> the client saves a screenshot, HUD hidden unless `hud`;
                        copied to build/screenshots/<scenario>/NAME.png
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

def needs_player(path):
    return '{P}' in open(path).read()

class Session:
    """One test server (and at most one client) shared by every scenario of the run."""

    def __init__(self, project, port, player, keep):
        self.project, self.port, self.player = project, port, player
        self.game_port = port - 100
        self.lines = []
        self.cursor = 0
        self.client_proc = None
        for p in (port, self.game_port):
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
        with open(os.path.join(run, 'server.properties'), 'w') as f:
            f.write(f"""enable-rcon=true
rcon.password={RCON_PASSWORD}
rcon.port={port}
server-port={self.game_port}
online-mode=false
level-seed=dst-test
spawn-protection=0
view-distance=6
simulation-distance=6
motd=dst-test
max-tick-time=-1
# The test world is throwaway: fsync per chunk write stalls worldgen on slow disks.
sync-chunk-writes=false
spawn-npcs=false
allow-flight=true
spawn-animals=true
""")
        self.log = open(os.path.join(run, 'test-server.log'), 'w')
        self.env = dict(os.environ)
        if os.environ.get('DST_JAVA_HOME'):
            self.env['JAVA_HOME'] = os.environ['DST_JAVA_HOME']
        self.proc = subprocess.Popen(['./gradlew', '--console=plain', 'runServer'], cwd=project,
                                     env=self.env, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                     stderr=subprocess.STDOUT, text=True, bufsize=1)
        threading.Thread(target=self._pump, daemon=True).start()
        if not self.wait_for(r'Done \(.*\)! For help', 900):
            print('SERVER DID NOT START; tail of log:')
            print(''.join(self.lines[-80:]))
            self.proc.kill()
            sys.exit(2)
        print('SERVER STARTED')
        self.sock = socket.create_connection(('127.0.0.1', port), timeout=30)
        self.sock.sendall(rcon_packet(1, 3, RCON_PASSWORD))
        rcon_read(self.sock)

    def _pump(self):
        for line in self.proc.stdout:
            self.log.write(line); self.log.flush()
            self.lines.append(line)

    def wait_for(self, regex, timeout):
        # Only lines after the previous match count, so repeated waits work.
        pat = re.compile(regex)
        start = time.time(); seen = self.cursor
        while time.time() - start < timeout:
            while seen < len(self.lines):
                if pat.search(self.lines[seen]):
                    self.cursor = seen + 1
                    return self.lines[seen]
                seen += 1
            if self.proc.poll() is not None:
                return None
            time.sleep(0.5)
        return None

    def command(self, cmd):
        # Long replies arrive as several packets back to back: read until a short silence.
        self.sock.sendall(rcon_packet(2, 2, cmd))
        self.sock.settimeout(180)
        parts = [rcon_read(self.sock)[1]]
        self.sock.settimeout(0.5)
        try:
            while True:
                parts.append(rcon_read(self.sock)[1])
        except (socket.timeout, TimeoutError):
            pass
        self.sock.settimeout(180)
        return ''.join(parts)

    def player_online(self, name):
        return 'has the following entity data' in self.command(f'data get entity {name} UUID')

    def start_client(self):
        self.client_proc = subprocess.Popen(
            ['sh', 'tools/autotest.sh', 'runAutotestClient',
             f'-PautotestServer=localhost:{self.game_port}', f'-PautotestUser={self.player}'],
            cwd=self.project, env=self.env, stdin=subprocess.DEVNULL,
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, start_new_session=True)
        print(f'CLIENT STARTED as {self.player} (log: build/autotest.log)')

    def reset(self):
        """Clean slate between scenarios (TestReset.java)."""
        target = ' @a' if self.client_proc is not None else ''
        print(f'> ds test reset{target}\n{self.command("ds test reset" + target)}')
        time.sleep(1)

    def run_scenario(self, cmdfile):
        """Runs one scenario; returns (checks passed, checks total, failed names)."""
        player = self.player
        checks = []

        def check(name, ok, detail=''):
            checks.append((name, ok))
            print(f'{"PASS" if ok else "FAIL"}: {name}' + (f'  [{detail[:160]}]' if detail and not ok else ''))

        name = os.path.splitext(os.path.basename(cmdfile))[0]
        first_line = len(self.lines)
        self.cursor = first_line
        vars_ = {'P': player}
        last_resp = ''
        for raw in open(cmdfile):
            cmd = raw.strip()
            if not cmd or cmd.startswith('//'):
                continue
            if self.proc.poll() is not None:
                check('server alive', False, 'the server stopped')
                break
            if cmd.startswith('#sleep'):
                time.sleep(float(cmd.split()[1])); continue
            if cmd.startswith('#wait-player'):
                parts = cmd.split(maxsplit=2)
                who = parts[1].replace('{P}', player) if len(parts) > 1 else player
                timeout = int(parts[2]) if len(parts) > 2 else 180
                start = time.time(); online = False
                while time.time() - start < timeout:
                    if self.player_online(who):
                        online = True; break
                    time.sleep(2)
                check(f'player {who} online', online)
                continue
            if cmd.startswith('#ds-wait'):
                ticks = round(float(cmd.split()[1]) * 2.5 * 20)
                # Scan only lines after the sprint starts and leave the #expect-log cursor alone,
                # so log lines printed during the sprint stay visible to later #expect-log.
                start_line = len(self.lines)
                self.sock.sendall(rcon_packet(2, 2, f'tick sprint {ticks}'))
                rcon_read(self.sock)
                done = None
                deadline = time.time() + max(120, ticks / 100)
                while done is None and time.time() < deadline and self.proc.poll() is None:
                    done = next((l for l in self.lines[start_line:] if 'Sprint completed' in l), None)
                    time.sleep(0.2)
                check(f'ds-wait {cmd.split()[1]} DS s ({ticks} ticks)', done is not None)
                continue
            if cmd.startswith('#shot'):
                parts = cmd.split()
                shot = parts[1]
                hud = len(parts) > 2 and parts[2] == 'hud'
                src = os.path.join(self.project, 'run-autotest', 'screenshots', shot + '.png')
                if os.path.exists(src):
                    os.remove(src)
                self.command(f'ds shot {player} {shot}' + (' hud' if hud else ''))
                start = time.time(); size = -1; ok = False
                while time.time() - start < 60:
                    if os.path.exists(src):
                        now = os.path.getsize(src)
                        if now > 0 and now == size:
                            ok = True; break
                        size = now
                    time.sleep(1)
                dest = os.path.join(self.project, 'build', 'screenshots', name, shot + '.png')
                if ok:
                    os.makedirs(os.path.dirname(dest), exist_ok=True)
                    os.replace(src, dest)
                    print(f'SHOT: {dest}')
                check(f'shot {shot}', ok)
                continue
            if cmd.startswith('#expect-log'):
                m = re.match(r'(.*)\s+(\d+)$', cmd[len('#expect-log'):].strip())
                regex = m.group(1) if m else cmd[len('#expect-log'):].strip()
                timeout = int(m.group(2)) if m else 120
                found = self.wait_for(regex, timeout)
                check(f'log ~ /{regex}/', found is not None, str(found))
                continue
            if cmd.startswith('#expect'):
                regex = cmd[len('#expect'):].strip()
                check(f'response ~ /{regex}/', re.search(regex, last_resp) is not None, last_resp)
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
            resp = self.command(cmd)
            last_resp = resp
            print(f'> {cmd}\n{resp}')
            # Island messages contain decoys like "island 900 centered at 7372800, 0, 0":
            # only accept triples whose first component looks like a coordinate.
            # "centered at X, Y, Z" is always an island centre, even island 0 at the origin.
            loc = (re.search(r'centered at (-?\d+), (-?\d+), (-?\d+)', resp)
                   or re.search(r'\[?(-?\d{4,}), (-?\d{1,5}), (-?\d{4,}|-?\d{1,3})\]?', resp))
            if loc:
                vars_.update(X=int(loc.group(1)), Y=int(loc.group(2)), Z=int(loc.group(3)))
        exceptions = [l for l in self.lines[first_line:] if re.search(r'Exception|ERROR', l)
                      and 'Failed to load recipe' not in l]
        if exceptions:
            print(f'--- {len(exceptions)} exception/error log lines (first 10) ---')
            print(''.join(exceptions[:10]))
        failed = [n for n, ok in checks if not ok]
        print(f'=== {len(checks) - len(failed)}/{len(checks)} checks passed'
              + (f': FAILED {failed}' if failed else ''))
        return failed

    def stop(self):
        if self.client_proc is not None:
            try:
                os.killpg(self.client_proc.pid, 15)
                self.client_proc.wait(timeout=60)
            except Exception:
                pass
        try:
            self.sock.sendall(rcon_packet(3, 2, 'stop'))
        except Exception:
            pass
        try:
            self.proc.wait(timeout=120)
        except subprocess.TimeoutExpired:
            self.proc.kill()
        # The server JVM is a child of the Gradle daemon, not of gradlew: if it did
        # not stop by itself it would keep the ports, so kill it by its RCON port.
        for pid in listening_pids(self.port):
            print(f'!! server JVM {pid} did not stop, killing it')
            subprocess.run(['kill', '-9', str(pid)])

def main():
    args = sys.argv[1:]
    port = 25699
    keep = False
    player = 'Tester'
    if '--port' in args:
        i = args.index('--port'); port = int(args[i + 1]); del args[i:i + 2]
    if '--player' in args:
        i = args.index('--player'); player = args[i + 1]; del args[i:i + 2]
    if '--client' in args:
        args.remove('--client')  # accepted for compatibility: the client starts when a scenario needs it
    if '--keep-world' in args:
        keep = True; args.remove('--keep-world')
    project, scenarios = args[0], args[1:]
    session = Session(project, port, player, keep)
    results = {}
    try:
        for i, scenario in enumerate(scenarios):
            name = os.path.splitext(os.path.basename(scenario))[0]
            print(f'=== {name} ===')
            if session.proc.poll() is not None:
                results[name] = ['server stopped']
                print('=== 0/1 checks passed: FAILED [server stopped]')
                continue
            if i > 0:
                session.reset()
            if needs_player(scenario) and session.client_proc is None:
                session.start_client()
            results[name] = session.run_scenario(scenario)
    finally:
        session.stop()
    bad = [n for n, failed in results.items() if failed]
    print(f'=== all: {"OK" if not bad else "FAILED " + " ".join(bad)} ===')
    sys.exit(1 if bad else 0)

if __name__ == '__main__':
    main()
