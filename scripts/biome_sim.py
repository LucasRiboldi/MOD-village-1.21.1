"""Bancada de simulação por bioma — pedido do autor, 2026-10-20.

Sobe o servidor de desenvolvimento sem jogador (``gradlew runBiomeSim``, com a chave
``-Dvillagecolony.simulate=true``), acha uma vila, força os chunks em volta dela — o centro deles
conta como um jogador dentro da vila (``SimulatedObserver``) — acelera o tempo com ``/tick sprint`` e,
no fim, roda ``time_ledger``, ``action_report`` e ``cost_ledger`` sobre o log daquela vila.

Dois modos de mundo:

    natural  mundo normal com semente fixa; a vila é a do tipo pedido (village_desert, ...).
             Terreno, bordas de bioma e recursos reais.
    fixed    o bioma inteiro é um só (pacote de dados com ``biome_source`` fixo). Vila natural
             nos biomas que geram vila; nos outros, ``/place structure`` de uma vila de planície.
             O relevo continua o do ruído normal: mede recurso e chão, não a montanha do bioma.

Uso:

    python scripts/biome_sim.py desert taiga                # alvos nomeados (ver TARGETS)
    python scripts/biome_sim.py --all --days 3
    python scripts/biome_sim.py --list

Limites (escritos no relatório): sem jogador perto não nasce monstro, então não há ataque nem
saque; os chunks fora da área forçada não simulam.
"""

import argparse
import os
import re
import secrets
import shutil
import socket
import struct
import subprocess
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RUN = os.path.join(ROOT, "run", "biome-sim")
REPORTS = os.path.join(RUN, "reports")
RCON_PORT = 25576
DEFAULT_SEED = "20261020"

# nome: (modo, bioma fixo ou None, estrutura a localizar, estrutura a colocar ou None)
TARGETS = {
    # As cinco vilas que o jogo gera, no mundo natural.
    "plains": ("natural", None, "minecraft:village_plains", None),
    "desert": ("natural", None, "minecraft:village_desert", None),
    "savanna": ("natural", None, "minecraft:village_savanna", None),
    "taiga": ("natural", None, "minecraft:village_taiga", None),
    "snowy": ("natural", None, "minecraft:village_snowy", None),
    # Biomas de vila com o mundo todo do mesmo bioma: sem borda, só o recurso dele.
    "fixed-meadow": ("fixed", "minecraft:meadow", "minecraft:village_plains", None),
    "fixed-snowy_taiga": ("fixed", "minecraft:snowy_taiga", None, "minecraft:village_taiga"),
    "fixed-savanna_plateau": ("fixed", "minecraft:savanna_plateau", "minecraft:village_savanna", None),
    # Biomas em que o jogo não gera vila: uma vila de planície posta à força.
    "fixed-forest": ("fixed", "minecraft:forest", None, "minecraft:village_plains"),
    "fixed-river": ("fixed", "minecraft:river", None, "minecraft:village_plains"),
    "fixed-jungle": ("fixed", "minecraft:jungle", None, "minecraft:village_plains"),
    "fixed-swamp": ("fixed", "minecraft:swamp", None, "minecraft:village_plains"),
    "fixed-badlands": ("fixed", "minecraft:badlands", None, "minecraft:village_desert"),
    "fixed-cherry_grove": ("fixed", "minecraft:cherry_grove", None, "minecraft:village_plains"),
    "fixed-dark_forest": ("fixed", "minecraft:dark_forest", None, "minecraft:village_plains"),
    "fixed-ice_spikes": ("fixed", "minecraft:ice_spikes", None, "minecraft:village_snowy"),
    "fixed-mushroom_fields": ("fixed", "minecraft:mushroom_fields", None, "minecraft:village_plains"),
}

# Frases do log que dizem se a vila andou: contadas no resumo de cada alvo.
SIGNALS = {
    # Colônia criada = 0 com vila no lugar é a vila ignorada pelo bioma (VillageScanner).
    "colônia criada": r"Colony created at",
    "atendida": r"is attended",
    "abandonada": r"is now ABANDONED",
    "obra planejada": r"Colony \S+ planned ",
    "obra concluída": r"Colony \S+ finished \S+ at",
    "sem lote numa varredura": r"found no lot for a",
    "estoque fabricado": r"The colony stocked",
    "encalhado": r"Stranded worker",
    "teleporte pela cama": r"was teleported from .* to its bed",
}


class Rcon:
    """Cliente RCON mínimo (protocolo Source): login tipo 3, comando tipo 2."""

    def __init__(self, port, password, timeout=120):
        self.sock = socket.create_connection(("127.0.0.1", port), timeout=timeout)
        self.next_id = 1

        if self._send(3, password) == -1:
            raise RuntimeError("RCON recusou a senha")

    def _send(self, kind, body):
        request_id = self.next_id
        self.next_id += 1
        payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)

        size = struct.unpack("<i", self._read(4))[0]
        data = self._read(size)
        answer_id = struct.unpack("<i", data[:4])[0]
        self.last = data[8:-2].decode("utf-8", "replace")

        return answer_id

    def _read(self, count):
        data = b""

        while len(data) < count:
            chunk = self.sock.recv(count - len(data))

            if not chunk:
                raise ConnectionError("RCON fechou a conexão")

            data += chunk

        return data

    def run(self, command):
        self._send(2, command)

        return self.last

    def close(self):
        self.sock.close()


def write_server_files(level, seed, password):
    os.makedirs(RUN, exist_ok=True)

    with open(os.path.join(RUN, "eula.txt"), "w", encoding="utf-8") as out:
        out.write("eula=true\n")

    properties = {
        "level-name": level,
        "level-seed": seed,
        "online-mode": "false",
        "enable-rcon": "true",
        "rcon.port": str(RCON_PORT),
        "rcon.password": password,
        "broadcast-rcon-to-ops": "false",
        "spawn-protection": "0",
        "view-distance": "6",
        "simulation-distance": "6",
        # O /tick sprint passa do tempo de um tique de propósito: o vigia não pode derrubar o servidor.
        "max-tick-time": "-1",
        "sync-chunk-writes": "false",
        "motd": "Village Colony - simulacao por bioma",
    }

    with open(os.path.join(RUN, "server.properties"), "w", encoding="utf-8") as out:
        for key, value in properties.items():
            out.write(f"{key}={value}\n")


def write_fixed_biome_pack(level, biome):
    """Pacote de dados que troca a fonte de biomas do mundo normal por um bioma só."""
    pack = os.path.join(RUN, level, "datapacks", "vc_fixed_biome")
    dimension = os.path.join(pack, "data", "minecraft", "dimension")
    os.makedirs(dimension, exist_ok=True)

    with open(os.path.join(pack, "pack.mcmeta"), "w", encoding="utf-8") as out:
        out.write('{"pack": {"pack_format": 48, "description": "Village Colony: bioma fixo"}}\n')

    with open(os.path.join(dimension, "overworld.json"), "w", encoding="utf-8") as out:
        out.write(
            '{"type": "minecraft:overworld", "generator": {"type": "minecraft:noise",'
            ' "settings": "minecraft:overworld",'
            f' "biome_source": {{"type": "minecraft:fixed", "biome": "{biome}"}}}}}}\n')


def gradle_command():
    wrapper = "gradlew.bat" if os.name == "nt" else "gradlew"

    return [os.path.join(ROOT, wrapper), "runBiomeSim", "--console=plain", "-q"]


def wait_for(path, pattern, process, timeout):
    """Espera uma frase aparecer no arquivo; falha se o processo morrer antes."""
    deadline = time.time() + timeout
    expression = re.compile(pattern)

    while time.time() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"o servidor saiu (código {process.returncode}) antes de '{pattern}'")

        if os.path.exists(path):
            with open(path, encoding="utf-8", errors="replace") as source:
                if expression.search(source.read()):
                    return

        time.sleep(2)

    raise TimeoutError(f"'{pattern}' não apareceu em {timeout} s")


def force_around(rcon, x, z):
    # 13 x 13 chunks: a vila, a caixa que cresce e a mina embaixo dela.
    print("  " + rcon.run(f"forceload add {x - 96} {z - 96} {x + 96} {z + 96}").strip())


def wait_loaded(rcon, x, z, process, timeout=600):
    """O /forceload só marca os chunks; a geração é assíncrona. Espera o centro e os quatro cantos."""
    points = [(x, z), (x - 90, z - 90), (x + 90, z - 90), (x - 90, z + 90), (x + 90, z + 90)]
    deadline = time.time() + timeout

    while time.time() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"o servidor saiu (código {process.returncode}) gerando a área")

        if all("passed" in rcon.run(f"execute if loaded {px} 64 {pz}") for px, pz in points):
            return

        time.sleep(3)

    raise TimeoutError(f"a área da vila não carregou em {timeout} s")


def villagers_near(rcon, x, z):
    answer = rcon.run(f"execute positioned {x} 64 {z} if entity @e[type=minecraft:villager,distance=..160]")
    found = re.search(r"count: (\d+)", answer)

    return int(found.group(1)) if found else 0


def locate_or_place(rcon, locate, place):
    """A coluna da vila: a natural mais perto da origem, ou uma posta à força em (0, 0)."""
    if place:
        # O /place recusa posição não carregada: força os chunks antes.
        force_around(rcon, 0, 0)
        answer = rcon.run(f"place structure {place} 0 64 0")
        print(f"  place: {answer.strip()}")

        if "Generated" not in answer and "gerad" not in answer.lower():
            raise RuntimeError(f"/place structure falhou: {answer.strip()}")

        return 0, 0

    answer = rcon.run(f"execute in minecraft:overworld positioned 0 64 0 run locate structure {locate}")
    print(f"  locate: {answer.strip()}")
    found = re.search(r"\[(-?\d+), [^,]+, (-?\d+)\]", answer)

    if not found:
        raise RuntimeError(f"nenhuma {locate} encontrada: {answer.strip()}")

    return int(found.group(1)), int(found.group(2))


def wait_game_ticks(rcon, ticks, process, timeout):
    """Acelera o jogo e espera o relógio do mundo andar ``ticks`` tiques."""
    start = game_time(rcon)
    rcon.run(f"tick sprint {ticks}")
    deadline = time.time() + timeout

    while time.time() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"o servidor saiu (código {process.returncode}) durante a simulação")

        now = game_time(rcon)
        print(f"  tique {now - start}/{ticks}", end="\r", flush=True)

        if now - start >= ticks:
            print()
            return

        time.sleep(10)

    raise TimeoutError(f"a simulação não completou {ticks} tiques em {timeout} s")


def stop(rcon):
    try:
        rcon.run("stop")
    except (ConnectionError, OSError):
        pass  # o servidor fecha a conexão ao parar

    rcon.close()


def game_time(rcon):
    found = re.search(r"(\d+)", rcon.run("time query gametime"))

    return int(found.group(1)) if found else 0


def count_signals(log_path):
    with open(log_path, encoding="utf-8", errors="replace") as source:
        text = source.read()

    return {name: len(re.findall(pattern, text)) for name, pattern in SIGNALS.items()}


def run_report(script, args, out_path):
    result = subprocess.run([sys.executable, os.path.join(ROOT, "scripts", script), *args],
                            capture_output=True, text=True, encoding="utf-8", errors="replace")

    with open(out_path, "w", encoding="utf-8") as out:
        out.write(result.stdout)
        out.write(result.stderr)


def simulate(name, days, seed, keep_world):
    mode, biome, locate, place = TARGETS[name]
    level = f"sim-{name}"
    password = secrets.token_hex(12)
    report = os.path.join(REPORTS, name)

    shutil.rmtree(os.path.join(RUN, level), ignore_errors=True)
    shutil.rmtree(report, ignore_errors=True)
    os.makedirs(report)

    logs = os.path.join(RUN, "logs")
    shutil.rmtree(logs, ignore_errors=True)

    write_server_files(level, seed, password)

    if mode == "fixed":
        write_fixed_biome_pack(level, biome)

    print(f"[{name}] subindo o servidor ({mode}{', ' + biome if biome else ''})")
    console = open(os.path.join(report, "console.txt"), "w", encoding="utf-8")
    process = subprocess.Popen(gradle_command(), cwd=ROOT, stdout=console, stderr=subprocess.STDOUT,
                               stdin=subprocess.DEVNULL)
    latest = os.path.join(logs, "latest.log")

    try:
        wait_for(latest, r"RCON running on", process, timeout=900)
        rcon = Rcon(RCON_PORT, password)

        x, z = locate_or_place(rcon, locate, place)
        print(f"  vila em ({x}, {z})")

        if not place:
            force_around(rcon, x, z)

        wait_loaded(rcon, x, z, process)
        before = villagers_near(rcon, x, z)
        print(f"  área carregada; {before} aldeões")

        wait_game_ticks(rcon, days * 24000, process, timeout=days * 24000)
        after = villagers_near(rcon, x, z)
        print(f"  fim: {after} aldeões")

        rcon.run("save-all flush")
        stop(rcon)
        process.wait(timeout=300)
    finally:
        if process.poll() is None:
            try:
                stop(Rcon(RCON_PORT, password, timeout=10))
                process.wait(timeout=120)
            except Exception:  # o servidor já não responde: derruba o processo
                process.kill()

        console.close()

    for found in os.listdir(logs):
        shutil.copy(os.path.join(logs, found), report)

    log = os.path.join(report, "latest.log")
    journals = [os.path.join(report, f) for f in os.listdir(report) if f.startswith("villagecolony-actions-")]
    run_report("time_ledger.py", [log], os.path.join(report, "time_ledger.txt"))
    run_report("cost_ledger.py", [log], os.path.join(report, "cost_ledger.txt"))

    if journals:
        run_report("action_report.py", journals, os.path.join(report, "action_report.txt"))

    signals = count_signals(log)

    with open(os.path.join(report, "signals.txt"), "w", encoding="utf-8") as out:
        out.write(f"alvo={name} modo={mode} bioma={biome} vila=({x}, {z}) dias={days} semente={seed}\n")
        out.write(f"aldeões: {before} no início, {after} no fim\n")

        for key, value in signals.items():
            out.write(f"{key}: {value}\n")

    if not keep_world:
        shutil.rmtree(os.path.join(RUN, level), ignore_errors=True)

    print(f"[{name}] pronto: {report}")

    return signals


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("targets", nargs="*")
    parser.add_argument("--all", action="store_true")
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--days", type=int, default=2, help="dias de jogo por alvo (padrão 2)")
    parser.add_argument("--seed", default=DEFAULT_SEED)
    parser.add_argument("--keep-world", action="store_true", help="não apaga o mundo depois")
    args = parser.parse_args(argv)

    if args.list:
        for name, (mode, biome, locate, place) in TARGETS.items():
            print(f"{name:24} {mode:8} {biome or '-':28} {locate or place}")
        return 0

    chosen = list(TARGETS) if args.all else args.targets
    unknown = [name for name in chosen if name not in TARGETS]

    if not chosen or unknown:
        parser.error(f"alvos desconhecidos: {unknown}" if unknown else "diga um alvo, ou --all")

    summary = {}

    for name in chosen:
        try:
            summary[name] = simulate(name, args.days, args.seed, args.keep_world)
        except Exception as failure:  # um alvo que falha não derruba os outros
            print(f"[{name}] FALHOU: {failure}")
            summary[name] = {"falha": str(failure)}

    print("\nResumo:")

    for name, signals in summary.items():
        print(f"  {name}: {signals}")

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
