#!/usr/bin/env python3
"""
probar.py — la puerta de entrega. Comprueba el mod ANTES de decir que esta.

Un mod de Minecraft falla de una forma especialmente mala: casi nunca casca.
Se queda sin cargar, o carga y no hace nada, y el juego arranca tan tranquilo.
"Compila" no significa nada. Estas son las pruebas, de menos a mas coste:

  NIVEL 1  estatico     el jar contra si mismo: entrypoints, mixins,
                        ficheros declarados, version sin expandir
  NIVEL 2  contra pack  el jar contra el pack real: loader, version de MC,
                        dependencias, entorno, choques de mixin
  NIVEL 3  arranque     un servidor Fabric de verdad levanta con el mod y
                        llega a "Done"; se apaga solo
  NIVEL 4  en el pack   arranque con los mods del pack alrededor

Los niveles 1 y 2 son la puerta: si estan en rojo, el mod NO se entrega.
El 3 es rapido y automatico para mods con logica de servidor. El 4 y las
pruebas de mando (mando-virtual.py) necesitan mas manos.

Uso:
    python probar.py <proyecto_o_jar>
    python probar.py <proyecto> --pack "<ruta_pack>"
    python probar.py <proyecto> --pack "<ruta_pack>" --arranque
    python probar.py <proyecto> --json informe.json

Codigo de salida 0 solo si no hay ningun FALLO. Uselo como puerta.

Solo biblioteca estandar. Python 3.9+.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from radiografia import detectar_loader, identidad_jar, json_laxo, localizar_raiz  # noqa: E402

for _flujo in (sys.stdout, sys.stderr):
    if hasattr(_flujo, "reconfigure"):
        _flujo.reconfigure(encoding="utf-8", errors="replace")

OK, AVISO, FALLO = "OK", "AVISO", "FALLO"


class Informe:
    def __init__(self) -> None:
        self.filas: list[tuple[str, str, str, str]] = []

    def add(self, nivel: str, estado: str, prueba: str, detalle: str = "") -> None:
        self.filas.append((nivel, estado, prueba, detalle))

    @property
    def fallos(self) -> int:
        return sum(1 for f in self.filas if f[1] == FALLO)

    @property
    def avisos(self) -> int:
        return sum(1 for f in self.filas if f[1] == AVISO)

    def imprimir(self) -> None:
        nivel_previo = None
        for nivel, estado, prueba, detalle in self.filas:
            if nivel != nivel_previo:
                print()
                print("=" * 72)
                print(f"  {nivel}")
                print("=" * 72)
                nivel_previo = nivel
            marca = {OK: "  ok  ", AVISO: " AVISO", FALLO: " FALLO"}[estado]
            print(f"[{marca}] {prueba}")
            if detalle:
                for linea in detalle.splitlines():
                    print(f"          {linea}")


def trozos(v: str) -> tuple:
    nums = tuple(int(x) for x in re.findall(r"\d+", v)[:3])
    return nums or (0,)


def suelo_de(rango: str) -> str | None:
    """De '>=0.15.0' saca '0.15.0'. De '*' o '~1.20.1' no saca suelo util."""
    m = re.search(r">=?\s*([\d.]+)", rango or "")
    return m.group(1) if m else None


def localizar_jar(destino: Path) -> Path | None:
    if destino.is_file() and destino.suffix == ".jar":
        return destino
    libs = destino / "build" / "libs"
    if libs.is_dir():
        jars = [j for j in libs.glob("*.jar")
                if not j.name.endswith(("-sources.jar", "-dev.jar", "-sources-dev.jar"))]
        if jars:
            return max(jars, key=lambda j: j.stat().st_mtime)
    return None


# --------------------------------------------------------------------------
# NIVEL 1 — el jar contra si mismo
# --------------------------------------------------------------------------

def nivel_estatico(jar: Path, inf: Informe) -> dict:
    N = "NIVEL 1 - el jar contra si mismo"
    try:
        zf = zipfile.ZipFile(jar)
    except (zipfile.BadZipFile, OSError) as e:
        inf.add(N, FALLO, "el jar se abre", str(e))
        return {}

    with zf:
        nombres = set(zf.namelist())
        clases = {n[:-6].replace("/", ".") for n in nombres if n.endswith(".class")}

        if "fabric.mod.json" not in nombres:
            inf.add(N, FALLO, "el jar lleva fabric.mod.json",
                    "sin esto el loader ni mira el jar")
            return {}
        inf.add(N, OK, "el jar lleva fabric.mod.json")

        meta = json_laxo(zf.read("fabric.mod.json").decode("utf-8", "replace"))
        if not isinstance(meta, dict):
            inf.add(N, FALLO, "fabric.mod.json es JSON valido")
            return {}
        inf.add(N, OK, "fabric.mod.json es JSON valido")

        # La trampa clasica: processResources no expandio ${version}.
        crudo = json.dumps(meta)
        if "${" in crudo:
            inf.add(N, FALLO, "no quedan variables sin expandir",
                    "hay un ${...} literal en fabric.mod.json.\n"
                    "Falta el bloque processResources { expand } en build.gradle.")
        else:
            inf.add(N, OK, "no quedan variables sin expandir")

        for campo in ("id", "version"):
            if not meta.get(campo):
                inf.add(N, FALLO, f"fabric.mod.json declara '{campo}'")
        if meta.get("id") and meta.get("version"):
            inf.add(N, OK, f"identidad: {meta['id']} {meta['version']}")

        # Entrypoints: la clase tiene que estar DENTRO del jar.
        total = 0
        for tipo, lista in (meta.get("entrypoints") or {}).items():
            for ep in lista:
                total += 1
                clase = ep if isinstance(ep, str) else ep.get("value", "")
                clase = clase.split("::")[0]
                if clase and clase not in clases:
                    inf.add(N, FALLO, f"entrypoint '{tipo}' -> {clase}",
                            "esa clase no esta en el jar: el mod cargara y no hara nada")
                else:
                    inf.add(N, OK, f"entrypoint '{tipo}' -> {clase}")
        if total == 0:
            inf.add(N, AVISO, "el mod no declara ningun entrypoint",
                    "solo tiene sentido si el mod es puro mixin o solo assets")

        # Mixins: el fichero de config, y cada clase que lista.
        for entrada in meta.get("mixins") or []:
            cfg = entrada if isinstance(entrada, str) else entrada.get("config", "")
            if cfg not in nombres:
                inf.add(N, FALLO, f"mixin config '{cfg}'", "declarado pero no esta en el jar")
                continue
            datos = json_laxo(zf.read(cfg).decode("utf-8", "replace")) or {}
            paquete = datos.get("package", "")
            listadas = []
            for clave in ("mixins", "client", "server"):
                listadas += datos.get(clave) or []
            if not listadas:
                inf.add(N, AVISO, f"mixin config '{cfg}' no lista ninguna clase",
                        "un mixin que no se lista no se aplica, y no da error")
                continue
            faltan = [c for c in listadas if f"{paquete}.{c}" not in clases]
            if faltan:
                inf.add(N, FALLO, f"mixin config '{cfg}'",
                        "clases listadas que no estan en el jar: " + ", ".join(faltan))
            else:
                inf.add(N, OK, f"mixin config '{cfg}' ({len(listadas)} mixins, todos presentes)")

        # Mixins compilados que NADIE lista: escritos y muertos.
        if meta.get("mixins"):
            declaradas = set()
            for entrada in meta.get("mixins") or []:
                cfg = entrada if isinstance(entrada, str) else entrada.get("config", "")
                if cfg in nombres:
                    d = json_laxo(zf.read(cfg).decode("utf-8", "replace")) or {}
                    p = d.get("package", "")
                    for clave in ("mixins", "client", "server"):
                        declaradas |= {f"{p}.{c}" for c in (d.get(clave) or [])}
            huerfanas = [c for c in clases
                         if ".mixin" in c and "$" not in c and c not in declaradas]
            if huerfanas:
                inf.add(N, AVISO, "hay mixins compilados que nadie lista",
                        "\n".join(huerfanas) +
                        "\nEstan en el jar pero no se aplican. Listalos en el mixins.json.")

        # accessWidener declarado pero ausente.
        aw = meta.get("accessWidener")
        if aw:
            inf.add(N, OK if aw in nombres else FALLO, f"accessWidener '{aw}'",
                    "" if aw in nombres else "declarado pero no esta en el jar")

        # Entrypoints que NO son de Fabric (plugins de otros mods en la raiz).
        for raiz_json in [n for n in nombres
                          if n.endswith("_plugin.json") and "/" not in n]:
            datos = json_laxo(zf.read(raiz_json).decode("utf-8", "replace")) or {}
            eps = datos.get("entrypoints") or []
            malas = [c for c in eps if isinstance(c, str) and c not in clases]
            inf.add(N, FALLO if malas else OK, f"plugin de otro mod '{raiz_json}'",
                    ("clases que no estan en el jar: " + ", ".join(malas)) if malas
                    else "va en la raiz del jar, no es un entrypoint de Fabric: correcto")

        return meta


# --------------------------------------------------------------------------
# NIVEL 2 — el jar contra el pack de destino
# --------------------------------------------------------------------------

def nivel_pack(meta: dict, pack: Path, inf: Informe) -> None:
    N = "NIVEL 2 - el jar contra el pack de destino"
    raiz = localizar_raiz(pack)
    info = detectar_loader(raiz)

    presentes: dict[str, str] = {}
    import io as _io
    for j in sorted((raiz / "mods").rglob("*.jar")):
        ident = identidad_jar(j)
        if ident and ident.get("id"):
            presentes[ident["id"]] = ident.get("version", "")
        # Los anidados (Jar-in-Jar) tambien cuentan como presentes.
        try:
            with zipfile.ZipFile(j) as zf:
                for n in zf.namelist():
                    if n.startswith("META-INF/jars/") and n.endswith(".jar"):
                        try:
                            with zipfile.ZipFile(_io.BytesIO(zf.read(n))) as inner:
                                if "fabric.mod.json" in inner.namelist():
                                    d = json_laxo(inner.read("fabric.mod.json")
                                                  .decode("utf-8", "replace")) or {}
                                    if d.get("id"):
                                        presentes.setdefault(d["id"], "")
                        except (zipfile.BadZipFile, OSError, KeyError):
                            continue
        except (zipfile.BadZipFile, OSError):
            continue

    # El loader los aporta siempre.
    for base in ("fabricloader", "fabric-loader", "java", "minecraft", "fabric"):
        presentes.setdefault(base, "")

    depende = meta.get("depends") or {}

    # --- LA prueba: el suelo de fabricloader no puede pasarse del pack.
    suelo = suelo_de(depende.get("fabricloader", ""))
    if suelo and info["version_loader"] != "?":
        if trozos(suelo) > trozos(info["version_loader"]):
            inf.add(N, FALLO, "fabricloader pedido vs el del pack",
                    f"pides >={suelo} y el pack corre {info['version_loader']}.\n"
                    "El mod NO SE CARGA y el juego arranca sin decir nada.\n"
                    f"Baja 'fabricloader' en fabric.mod.json a <= {info['version_loader']}.")
        else:
            inf.add(N, OK, f"fabricloader >={suelo} <= pack {info['version_loader']}")
    elif info["version_loader"] == "?":
        inf.add(N, AVISO, "version del loader del pack desconocida",
                "arranca el pack una vez para que escriba logs/latest.log")

    # --- version de Minecraft
    rango_mc = depende.get("minecraft", "")
    mc_pack = info["minecraft"]
    if rango_mc and mc_pack != "?":
        base = re.sub(r"^[~^>=<]+", "", rango_mc).strip()
        familia_mod = ".".join(str(x) for x in trozos(base)[:2])
        familia_pack = ".".join(str(x) for x in trozos(mc_pack)[:2])
        if familia_mod and familia_mod != familia_pack:
            inf.add(N, FALLO, "version de Minecraft",
                    f"el mod pide {rango_mc} y el pack es {mc_pack}")
        else:
            inf.add(N, OK, f"version de Minecraft: mod {rango_mc} / pack {mc_pack}")

    # --- dependencias declaradas
    faltan = [d for d in depende if d not in presentes]
    if faltan:
        inf.add(N, FALLO, "dependencias declaradas presentes en el pack",
                "no estan en el pack: " + ", ".join(faltan) +
                "\nCon una sola que falte, el loader para el arranque entero.")
    else:
        inf.add(N, OK, f"las {len(depende)} dependencias declaradas estan en el pack")

    for d in (meta.get("recommends") or {}):
        if d not in presentes:
            inf.add(N, AVISO, f"'{d}' es recomendado y no esta",
                    "el mod tiene que funcionar sin el: comprueba que lo hace")

    # --- entorno
    entorno = meta.get("environment", "*")
    eps = meta.get("entrypoints") or {}
    if entorno == "*" and set(eps) <= {"client", "modmenu"} and eps:
        inf.add(N, AVISO, "entorno declarado",
                'declaras "*" pero solo tienes entrypoints de cliente.\n'
                'Si esto va a un servidor, deberia ser "client".')
    else:
        inf.add(N, OK, f'entorno "{entorno}" coherente con los entrypoints')

    # --- choque de id: el mismo mod ya en mods/
    mid = meta.get("id")
    hermanos = [j.name for j in (raiz / "mods").glob("*.jar")
                if (identidad_jar(j) or {}).get("id") == mid]
    if len(hermanos) > 1:
        inf.add(N, FALLO, "no hay dos versiones del mod en mods/",
                "hay varias: " + ", ".join(hermanos) +
                "\nDos jars con el mismo mod id son un fallo de arranque.")
    else:
        inf.add(N, OK, "no hay versiones duplicadas del mod en mods/")


# --------------------------------------------------------------------------
# NIVEL 3 — que un servidor Fabric de verdad arranque con el mod
# --------------------------------------------------------------------------

def nivel_arranque(proyecto: Path, inf: Informe, minutos: int) -> None:
    N = "NIVEL 3 - arranque real (servidor Fabric headless)"
    gradlew = proyecto / ("gradlew.bat" if os.name == "nt" else "gradlew")
    if not gradlew.exists():
        inf.add(N, AVISO, "arranque de servidor", "no hay gradlew: se salta")
        return

    from construir import buscar_jdk, jdk_necesario, leer_propiedades
    props = leer_propiedades(proyecto)
    jdk = buscar_jdk(jdk_necesario(props))
    if not jdk:
        inf.add(N, AVISO, "arranque de servidor", "no hay JDK adecuado: se salta")
        return

    run = proyecto / "run"
    run.mkdir(exist_ok=True)
    (run / "eula.txt").write_text("eula=true\n", encoding="utf-8")

    entorno = dict(os.environ)
    entorno["JAVA_HOME"] = str(jdk)

    print(f"\n  Levantando un servidor Fabric de verdad (tope {minutos} min)...")
    try:
        proceso = subprocess.run(
            [str(gradlew), "runServer", "--no-daemon"],
            cwd=proyecto, env=entorno, capture_output=True, text=True,
            errors="replace", timeout=minutos * 60,
            input="stop\n",
        )
        salida = proceso.stdout + proceso.stderr
    except subprocess.TimeoutExpired as e:
        salida = ((e.stdout or "") + (e.stderr or "")) if isinstance(e.stdout, str) else ""
        inf.add(N, AVISO, "el servidor no termino en el tiempo dado",
                "puede ser normal en el primer arranque (descarga y descompila)")

    marcas = [
        ("Mixin apply failed", FALLO, "un mixin no se pudo aplicar"),
        ("Mixin transformation of", FALLO, "fallo transformando con mixin"),
        ("Could not execute entrypoint", FALLO, "el entrypoint casco al arrancar"),
        ("Incompatible mod set", FALLO, "conjunto de mods incompatible"),
        ("requires .* which is missing", FALLO, "falta una dependencia"),
        ("Exception in thread \"main\"", FALLO, "excepcion al arrancar"),
    ]
    problema = False
    for aguja, estado, texto in marcas:
        m = re.search(aguja, salida)
        if m:
            contexto = salida[max(0, m.start() - 200): m.start() + 400]
            inf.add(N, estado, texto, contexto.strip()[:600])
            problema = True

    if re.search(r'Done \([\d.]+s\)! For help', salida):
        inf.add(N, OK, "el servidor llego a 'Done': el mod carga y no rompe el arranque")
    elif not problema:
        inf.add(N, AVISO, "no vi el 'Done' del servidor",
                "revisa la salida a mano; puede ser un mod solo de cliente")

    # El propio mod deberia haber dicho algo.
    if re.search(r"\[[\w-]+\] cargado", salida):
        inf.add(N, OK, "el mod escribio su linea de arranque en el log")


# --------------------------------------------------------------------------

def main() -> int:
    ap = argparse.ArgumentParser(description="Puerta de entrega: comprueba el mod.")
    ap.add_argument("destino", help="carpeta del proyecto o ruta de un .jar")
    ap.add_argument("--pack", default=None, help="pack donde va a vivir (nivel 2)")
    ap.add_argument("--arranque", action="store_true",
                    help="levanta un servidor Fabric de verdad (nivel 3)")
    ap.add_argument("--minutos", type=int, default=12, help="tope del nivel 3")
    ap.add_argument("--json", default=None, help="vuelca el informe a un JSON")
    args = ap.parse_args()

    destino = Path(args.destino).expanduser().resolve()
    jar = localizar_jar(destino)
    if not jar:
        print(f"ERROR: no encuentro ningun jar en {destino}", file=sys.stderr)
        print("Compila primero:  python tools/construir.py <proyecto>", file=sys.stderr)
        return 2

    inf = Informe()
    print(f"  Jar   {jar}")
    meta = nivel_estatico(jar, inf)

    if args.pack and meta:
        nivel_pack(meta, Path(args.pack).expanduser().resolve(), inf)
    elif not args.pack:
        inf.add("NIVEL 2 - el jar contra el pack de destino", AVISO,
                "no se ha comprobado contra ningun pack",
                "pasa --pack \"<ruta>\": el nivel 1 no ve el fallo mas comun,\n"
                "que es pedir un loader mas nuevo del que corre el pack")

    if args.arranque and destino.is_dir():
        nivel_arranque(destino, inf, args.minutos)

    inf.imprimir()

    print()
    print("=" * 72)
    if inf.fallos:
        print(f"  {inf.fallos} FALLO(S), {inf.avisos} aviso(s) -- NO ENTREGAR ESTE MOD")
        print("=" * 72)
    else:
        print(f"  Sin fallos, {inf.avisos} aviso(s).")
        print("  Puerta pasada. Lo que queda solo lo dice jugar:")
        print("  que la mecanica se sienta bien no lo verifica ningun script.")
        print("=" * 72)

    if args.json:
        Path(args.json).write_text(json.dumps(
            {"jar": str(jar), "fallos": inf.fallos, "avisos": inf.avisos,
             "filas": [{"nivel": n, "estado": e, "prueba": p, "detalle": d}
                       for n, e, p, d in inf.filas]},
            ensure_ascii=False, indent=2), encoding="utf-8")

    return 1 if inf.fallos else 0


if __name__ == "__main__":
    sys.exit(main())
