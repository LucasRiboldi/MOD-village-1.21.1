#!/usr/bin/env python3
"""
radiografia.py — lee el modpack donde va a vivir tu mod y te da los datos
exactos con los que hay que compilarlo.

Un mod que se escribe para un pack concreto no se compila "contra Fabric":
se compila contra EL Fabric que corre ese pack, y contra LAS versiones de los
mods con los que se integra. Si te desvias, el mod no casca: simplemente no
carga, y no hay mensaje que lo diga.

Esta herramienta responde tres preguntas:

  1. Que loader y que version de loader corre el pack de verdad
     (no la que dice la web: la que aparece en su propio log).
  2. Que bloque de gradle.properties hay que escribir.
  3. Para cada mod con el que te quieras integrar, la linea modCompileOnly
     exacta -- resuelta por el hash del jar contra la API de Modrinth, que
     es la unica forma de acertar la version sin adivinar.

Uso:
    python radiografia.py <ruta_pack>
    python radiografia.py <ruta_pack> --integrar paragliders,sword-parry
    python radiografia.py <ruta_pack> --integrar-todo --salida rx.json
    python radiografia.py <ruta_pack> --sin-red        # no consulta Modrinth

Solo biblioteca estandar. Python 3.9+.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path

# La consola de Windows es cp1252 y los nombres de mod traen Unicode.
for _flujo in (sys.stdout, sys.stderr):
    if hasattr(_flujo, "reconfigure"):
        _flujo.reconfigure(encoding="utf-8", errors="replace")

AGENTE = "modpack-modsmith/1.0 (skill de Claude Code)"
API = "https://api.modrinth.com/v2"

# Versiones de Minecraft plausibles que NO vengan precedidas de otro digito.
# Sin el lookbehind, "cloth-config-11.1.136" se lee como "1.13".
RE_VERSION_MC = re.compile(r"(?<![\d.])(1\.(?:1[6-9]|2\d)(?:\.\d{1,2})?)(?![\d])")

# La linea que escupe el propio juego al arrancar. Es la fuente mas fiable
# que existe: no es lo que el pack dice que usa, es lo que uso.
RE_LOG_LOADER = re.compile(
    r"Loading Minecraft ([\d.]+) with (Fabric|Quilt) Loader ([\d.+\w-]+)", re.I
)

# Subsistemas que suelen tener un dueno unico dentro de un pack. Si vas a
# tocar uno de estos, el dueno manda: tu mixin puede no dispararse nunca.
# (Ver referencias/integracion-con-el-pack.md)
DUENOS = {
    "camara": [
        ("shouldersurfing", "Shoulder Surfing Reloaded"),
        ("shoulder_surfing", "Shoulder Surfing Reloaded"),
        ("betterthirdperson", "Better Third Person"),
        ("dynamiccrosshair", "Dynamic Crosshair"),
    ],
    "entrada / mando": [
        ("midnightcontrols", "MidnightControls"),
        ("controlify", "Controlify"),
        ("controllable", "Controllable"),
    ],
    "combate cuerpo a cuerpo": [
        ("bettercombat", "Better Combat"),
        ("epicfight", "Epic Fight"),
        ("combatroll", "Combat Roll"),
        ("frycparry", "Fryc's Parry"),
        ("parry", "un mod de parry"),
    ],
    "aguante / stamina": [
        ("paraglider", "Paragliders"),
        ("stamina", "un mod de stamina"),
        ("spellengine", "Spell Engine"),
    ],
    "HUD": [
        ("raised", "Raised"),
        ("betterhud", "Better HUD"),
        ("appleskin", "AppleSkin"),
    ],
    "renderizado / shaders": [
        ("iris", "Iris"),
        ("sodium", "Sodium"),
        ("embeddium", "Embeddium"),
    ],
}


# --------------------------------------------------------------------------
# lectura de jars
# --------------------------------------------------------------------------

def json_laxo(texto: str):
    """Muchos fabric.mod.json traen comentarios o comas colgando."""
    try:
        return json.loads(texto)
    except json.JSONDecodeError:
        pass
    limpio = re.sub(r"//[^\n\r]*", "", texto)
    limpio = re.sub(r"/\*.*?\*/", "", limpio, flags=re.S)
    limpio = re.sub(r",(\s*[}\]])", r"\1", limpio)
    try:
        return json.loads(limpio)
    except json.JSONDecodeError:
        return None


def texto_de(valor) -> str:
    if isinstance(valor, str):
        return valor
    if isinstance(valor, dict):
        for clave in ("text", "name", "value"):
            if isinstance(valor.get(clave), str):
                return valor[clave]
    if isinstance(valor, list) and valor:
        return texto_de(valor[0])
    return ""


def identidad_jar(ruta: Path) -> dict | None:
    """Saca id, version, entorno y dependencias de un jar de mod."""
    try:
        with zipfile.ZipFile(ruta) as zf:
            nombres = set(zf.namelist())

            if "fabric.mod.json" in nombres:
                datos = json_laxo(zf.read("fabric.mod.json").decode("utf-8", "replace"))
                if not isinstance(datos, dict):
                    return None
                return {
                    "id": datos.get("id", ""),
                    "nombre": texto_de(datos.get("name")) or datos.get("id", ""),
                    "version": texto_de(datos.get("version")),
                    "loader": "fabric",
                    "entorno": datos.get("environment", "*"),
                    "depende": datos.get("depends", {}) or {},
                    "fichero": ruta.name,
                }

            if "quilt.mod.json" in nombres:
                datos = json_laxo(zf.read("quilt.mod.json").decode("utf-8", "replace"))
                ql = (datos or {}).get("quilt_loader", {})
                return {
                    "id": ql.get("id", ""),
                    "nombre": texto_de(ql.get("metadata", {}).get("name")) or ql.get("id", ""),
                    "version": texto_de(ql.get("version")),
                    "loader": "quilt",
                    "entorno": "*",
                    "depende": {},
                    "fichero": ruta.name,
                }

            for meta in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
                if meta in nombres:
                    crudo = zf.read(meta).decode("utf-8", "replace")
                    mid = re.search(r'modId\s*=\s*"([^"]+)"', crudo)
                    ver = re.search(r'version\s*=\s*"([^"]+)"', crudo)
                    return {
                        "id": mid.group(1) if mid else "",
                        "nombre": mid.group(1) if mid else ruta.stem,
                        "version": ver.group(1) if ver else "",
                        "loader": "neoforge" if "neoforge" in meta else "forge",
                        "entorno": "*",
                        "depende": {},
                        "fichero": ruta.name,
                    }
    except (zipfile.BadZipFile, OSError, KeyError):
        return None
    return None


def sha512_de(ruta: Path) -> str:
    h = hashlib.sha512()
    with open(ruta, "rb") as f:
        for trozo in iter(lambda: f.read(1 << 20), b""):
            h.update(trozo)
    return h.hexdigest()


# --------------------------------------------------------------------------
# la pregunta que mas cuesta: que loader corre el pack
# --------------------------------------------------------------------------

def localizar_raiz(ruta: Path) -> Path:
    """Admite que te pasen la instancia, el pack, o la carpeta que los contiene."""
    if (ruta / "mods").is_dir():
        return ruta
    for hijo in (".minecraft", "minecraft", "pack", "overrides"):
        if (ruta / hijo / "mods").is_dir():
            return ruta / hijo
    return ruta


def detectar_loader(raiz: Path) -> dict:
    """
    Busca la version del loader por orden de fiabilidad. La primera fuente que
    responde gana, y se registra CUAL fue: no es lo mismo leerlo del log del
    juego que deducirlo del nombre de una carpeta.
    """
    # 1. El log del propio juego. Es lo que de verdad se cargo.
    for patron in ("logs/latest.log", "logs/*.log", "logs/*.log.gz"):
        for log in sorted(raiz.glob(patron), key=lambda p: -p.stat().st_mtime)[:6]:
            try:
                if log.suffix == ".gz":
                    import gzip
                    crudo = gzip.open(log, "rt", encoding="utf-8", errors="replace").read(200_000)
                else:
                    crudo = log.read_text(encoding="utf-8", errors="replace")[:200_000]
            except OSError:
                continue
            m = RE_LOG_LOADER.search(crudo)
            if m:
                return {
                    "minecraft": m.group(1),
                    "loader": m.group(2).lower(),
                    "version_loader": m.group(3),
                    "fuente": f"{log.relative_to(raiz)} (el juego lo dijo al arrancar)",
                    "confianza": "alta",
                }

    # 2. Indice de un .mrpack ya extraido.
    idx = raiz / "modrinth.index.json"
    if not idx.exists():
        idx = raiz.parent / "modrinth.index.json"
    if idx.exists():
        datos = json_laxo(idx.read_text(encoding="utf-8", errors="replace")) or {}
        dep = datos.get("dependencies", {}) or {}
        for clave, nombre in (("fabric-loader", "fabric"), ("quilt-loader", "quilt"),
                              ("neoforge", "neoforge"), ("forge", "forge")):
            if dep.get(clave):
                return {
                    "minecraft": dep.get("minecraft", "?"),
                    "loader": nombre,
                    "version_loader": dep[clave],
                    "fuente": "modrinth.index.json",
                    "confianza": "alta",
                }

    # 3. Prism / MultiMC.
    mmc = raiz / "mmc-pack.json"
    if not mmc.exists():
        mmc = raiz.parent / "mmc-pack.json"
    if mmc.exists():
        datos = json_laxo(mmc.read_text(encoding="utf-8", errors="replace")) or {}
        mc = loader = ver = None
        for comp in datos.get("components", []):
            uid = comp.get("uid", "")
            if uid == "net.minecraft":
                mc = comp.get("version")
            elif uid == "net.fabricmc.fabric-loader":
                loader, ver = "fabric", comp.get("version")
            elif uid == "org.quiltmc.quilt-loader":
                loader, ver = "quilt", comp.get("version")
            elif uid == "net.neoforged":
                loader, ver = "neoforge", comp.get("version")
            elif uid == "net.minecraftforge":
                loader, ver = "forge", comp.get("version")
        if loader:
            return {"minecraft": mc or "?", "loader": loader, "version_loader": ver or "?",
                    "fuente": "mmc-pack.json", "confianza": "alta"}

    # 4. CurseForge.
    for nombre in ("minecraftinstance.json", "manifest.json"):
        cf = raiz / nombre
        if not cf.exists():
            cf = raiz.parent / nombre
        if cf.exists():
            datos = json_laxo(cf.read_text(encoding="utf-8", errors="replace")) or {}
            bloque = datos.get("minecraft") or datos.get("baseModLoader") or {}
            mc = bloque.get("version") or bloque.get("minecraftVersion")
            cargadores = bloque.get("modLoaders") or []
            crudo = (cargadores[0].get("id") if cargadores else bloque.get("name")) or ""
            m = re.match(r"(fabric|forge|neoforge|quilt)-(.+)", str(crudo))
            if m:
                return {"minecraft": mc or "?", "loader": m.group(1), "version_loader": m.group(2),
                        "fuente": nombre, "confianza": "alta"}

    # 5. Carpeta de version del launcher oficial: fabric-loader-0.19.3-1.20.1
    for base in (raiz / "versions", raiz.parent / "versions"):
        if base.is_dir():
            for carpeta in base.iterdir():
                m = re.match(r"(fabric|quilt)-loader-([\d.]+)-([\d.]+)", carpeta.name)
                if m:
                    return {"minecraft": m.group(3), "loader": m.group(1),
                            "version_loader": m.group(2),
                            "fuente": f"versions/{carpeta.name}", "confianza": "media"}

    # 6. Servidor dedicado: la libreria esta en disco.
    lib = raiz / "libraries" / "net" / "fabricmc" / "fabric-loader"
    if lib.is_dir():
        vers = sorted(p.name for p in lib.iterdir() if p.is_dir())
        if vers:
            return {"minecraft": "?", "loader": "fabric", "version_loader": vers[-1],
                    "fuente": "libraries/net/fabricmc/fabric-loader", "confianza": "media"}

    return {"minecraft": "?", "loader": "?", "version_loader": "?",
            "fuente": "no encontrada", "confianza": "ninguna"}


def deducir_mc_de_mods(mods: list[dict], jars: list[Path]) -> str:
    """Ultimo recurso: la version que mas se repite en los nombres de fichero."""
    cuenta: dict[str, int] = {}
    for j in jars:
        for v in RE_VERSION_MC.findall(j.name):
            cuenta[v] = cuenta.get(v, 0) + 1
    if not cuenta:
        return "?"
    return max(cuenta.items(), key=lambda kv: kv[1])[0]


# --------------------------------------------------------------------------
# Modrinth: del hash del jar a la coordenada maven exacta
# --------------------------------------------------------------------------

def consultar_modrinth(hashes: dict[str, Path]) -> dict:
    """
    Pregunta a Modrinth por sha512. Es la unica forma de saber que version
    EXACTA es un jar sin fiarse del nombre del fichero, que miente a menudo.
    Devuelve {sha512: {"version_id":..., "project_id":..., "numero":...}}.
    """
    import urllib.error
    import urllib.request

    cuerpo = json.dumps({"hashes": list(hashes), "algorithm": "sha512"}).encode()
    pet = urllib.request.Request(
        f"{API}/version_files",
        data=cuerpo,
        headers={"Content-Type": "application/json", "User-Agent": AGENTE},
        method="POST",
    )
    try:
        with urllib.request.urlopen(pet, timeout=25) as r:
            crudo = json.loads(r.read().decode("utf-8"))
    except (urllib.error.URLError, OSError, json.JSONDecodeError, TimeoutError) as e:
        raise RuntimeError(f"no se pudo consultar Modrinth: {e}") from e

    salida = {}
    for h, ver in crudo.items():
        salida[h] = {
            "version_id": ver.get("id", ""),
            "project_id": ver.get("project_id", ""),
            "numero": ver.get("version_number", ""),
            "loaders": ver.get("loaders", []),
            "juegos": ver.get("game_versions", []),
        }
    return salida


def slugs_de(project_ids: list[str]) -> dict[str, str]:
    """El slug es lo legible; el project_id tambien vale como coordenada maven."""
    import urllib.error
    import urllib.parse
    import urllib.request

    if not project_ids:
        return {}
    q = urllib.parse.quote(json.dumps(sorted(set(project_ids))))
    pet = urllib.request.Request(f"{API}/projects?ids={q}", headers={"User-Agent": AGENTE})
    try:
        with urllib.request.urlopen(pet, timeout=25) as r:
            datos = json.loads(r.read().decode("utf-8"))
    except (urllib.error.URLError, OSError, json.JSONDecodeError, TimeoutError):
        return {}
    return {p["id"]: p.get("slug", p["id"]) for p in datos if isinstance(p, dict) and p.get("id")}


# --------------------------------------------------------------------------
# informe
# --------------------------------------------------------------------------

def bloque_gradle(info: dict, mc: str) -> str:
    loader = info["version_loader"]
    return f"""# --- generado por radiografia.py, pegar en gradle.properties ---
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
org.gradle.configuration-cache=false

minecraft_version={mc}
# CLAVADO al loader que corre el pack ({info['fuente']}).
# Si pides uno mas nuevo, el mod NO CARGA y no hay mensaje de error.
loader_version={loader}

version=0.1.0
group=net.midominio
"""


def main() -> int:
    ap = argparse.ArgumentParser(
        description="Radiografia del modpack donde va a vivir tu mod."
    )
    ap.add_argument("pack", help="carpeta del pack o de la instancia")
    ap.add_argument("--integrar", default="",
                    help="ids o slugs de mods con los que te integras, separados por coma")
    ap.add_argument("--integrar-todo", action="store_true",
                    help="resuelve la coordenada maven de TODOS los mods del pack")
    ap.add_argument("--sin-red", action="store_true",
                    help="no consulta Modrinth (las coordenadas saldran como TODO)")
    ap.add_argument("--salida", default=None, help="vuelca el informe completo a un JSON")
    args = ap.parse_args()

    raiz = localizar_raiz(Path(args.pack).expanduser().resolve())
    carpeta_mods = raiz / "mods"
    if not carpeta_mods.is_dir():
        print(f"ERROR: no encuentro {carpeta_mods}", file=sys.stderr)
        print("Pasa la carpeta de la instancia (la que tiene mods/ dentro).", file=sys.stderr)
        return 2

    jars = sorted(p for p in carpeta_mods.rglob("*.jar"))
    if not jars:
        print(f"ERROR: {carpeta_mods} no tiene ningun .jar", file=sys.stderr)
        return 2

    print(f"Leyendo {len(jars)} jars de {carpeta_mods} ...\n")

    mods: list[dict] = []
    por_id: dict[str, dict] = {}
    for j in jars:
        ident = identidad_jar(j)
        if ident and ident.get("id"):
            ident["ruta"] = str(j)
            mods.append(ident)
            por_id[ident["id"]] = ident

    info = detectar_loader(raiz)
    mc = info["minecraft"] if info["minecraft"] != "?" else deducir_mc_de_mods(mods, jars)

    # ---- 1. identidad del pack ------------------------------------------
    print("=" * 68)
    print("  EL PACK")
    print("=" * 68)
    print(f"  Minecraft        {mc}")
    print(f"  Loader           {info['loader']}")
    print(f"  Version loader   {info['version_loader']}")
    print(f"  Fuente del dato  {info['fuente']}  [confianza: {info['confianza']}]")
    print(f"  Mods instalados  {len(mods)}")

    if info["confianza"] == "ninguna":
        print()
        print("  AVISO: no he podido leer la version del loader.")
        print("  Arranca el pack una vez y vuelve a ejecutar esto: la linea")
        print("  'Loading Minecraft X with Fabric Loader Y' aparece en logs/latest.log")
        print("  y es el unico dato que no miente.")

    # ---- 2. duenos de subsistema ----------------------------------------
    hallados = []
    vistos: set[tuple[str, str]] = set()
    for area, candidatos in DUENOS.items():
        for clave, _nombre in candidatos:
            for mid in por_id:
                # Varias claves pueden casar con el mismo mod ("frycparry" y
                # "parry"): se cuenta una vez por area.
                if clave in mid.lower() and (area, mid) not in vistos:
                    vistos.add((area, mid))
                    hallados.append((area, por_id[mid]["nombre"], mid))
                    break
    if hallados:
        print()
        print("=" * 68)
        print("  QUIEN MANDA EN CADA COSA")
        print("=" * 68)
        print("  Si tu mod toca una de estas areas, el dueno manda: puede llevar")
        print("  su propia rotacion, su propio bucle o su propio manejo de entrada,")
        print("  y entonces tu mixin sobre la clase vanilla no se dispara nunca.")
        print()
        area_previa = None
        for area, nombre, mid in hallados:
            if area != area_previa:
                print(f"  {area}:")
                area_previa = area
            print(f"      {nombre}  ({mid})")

    # ---- 3. gradle.properties -------------------------------------------
    print()
    print("=" * 68)
    print("  GRADLE.PROPERTIES")
    print("=" * 68)
    print(bloque_gradle(info, mc))

    # ---- 4. coordenadas maven -------------------------------------------
    pedidos: list[dict] = []
    if args.integrar_todo:
        pedidos = list(mods)
    elif args.integrar:
        buscados = [s.strip().lower() for s in args.integrar.split(",") if s.strip()]
        for b in buscados:
            golpe = None
            for m in mods:
                if m["id"].lower() == b or b in m["fichero"].lower() or b in m["id"].lower():
                    golpe = m
                    break
            if golpe:
                pedidos.append(golpe)
            else:
                print(f"  AVISO: '{b}' no esta en el pack; no puedes integrarte con el.")

    resueltos = []
    if pedidos:
        print("=" * 68)
        print("  DEPENDENCIAS PARA build.gradle")
        print("=" * 68)

        mapa_hash: dict[str, Path] = {}
        if not args.sin_red:
            print(f"  Calculando hash de {len(pedidos)} jars y preguntando a Modrinth...")
            for m in pedidos:
                mapa_hash[sha512_de(Path(m["ruta"]))] = Path(m["ruta"])

        respuesta: dict = {}
        slugs: dict[str, str] = {}
        if mapa_hash:
            try:
                respuesta = consultar_modrinth(mapa_hash)
                slugs = slugs_de([v["project_id"] for v in respuesta.values()])
            except RuntimeError as e:
                print(f"  {e}")
                print("  Sigo sin red: las coordenadas saldran como TODO.\n")

        print()
        print("  // Solo para compilar contra sus APIs: los aporta el pack en runtime.")
        print("  // NO se empaquetan (no uses include ni implementation).")
        for m in pedidos:
            h = sha512_de(Path(m["ruta"])) if (not args.sin_red and mapa_hash) else None
            dato = respuesta.get(h) if h else None
            if dato and dato["version_id"]:
                slug = slugs.get(dato["project_id"], dato["project_id"])
                linea = f'  modCompileOnly "maven.modrinth:{slug}:{dato["version_id"]}"'
                print(f'{linea}  // {m["nombre"]} {dato["numero"]}')
                resueltos.append({**m, "slug": slug, "version_id": dato["version_id"],
                                  "numero": dato["numero"]})
            else:
                print(f'  // TODO {m["nombre"]} {m["version"]} -- no esta en Modrinth por hash.')
                print(f'  //      Puede ser un jar modificado a mano o de CurseForge.')
                print(f'  //      Alternativa: mete el jar en libs/ y usa')
                print(f'  //      modCompileOnly files("libs/{Path(m["ruta"]).name}")')
                resueltos.append({**m, "slug": None, "version_id": None})

        print()
        print('  // Y en fabric.mod.json, dentro de "depends" (o "recommends" si')
        print('  // tu mod sabe funcionar sin ellos):')
        for m in pedidos:
            print(f'  //   "{m["id"]}": "*"')

    # ---- 5. entorno ------------------------------------------------------
    print()
    print("=" * 68)
    print("  ENTORNO DEL MOD")
    print("=" * 68)
    solo_cliente = [m["id"] for m in pedidos if m.get("entorno") == "client"]
    if solo_cliente:
        print("  Estos con los que te integras son SOLO CLIENTE:")
        for m in solo_cliente:
            print(f"      {m}")
        print()
        print('  Tu mod tiene que declarar "environment": "client" y no puede ir')
        print("  en el jar del servidor. Un mod de cliente en el servidor rompe")
        print("  el arranque, y el error casi nunca nombra al culpable.")
    else:
        print('  Nada te obliga a ser solo cliente. Usa "environment": "*" si la')
        print("  logica corre en ambos lados, o \"client\" si es camara, HUD o entrada.")

    # ---- salida JSON -----------------------------------------------------
    if args.salida:
        destino = Path(args.salida)
        destino.write_text(json.dumps({
            "pack": str(raiz),
            "minecraft": mc,
            "loader": info["loader"],
            "version_loader": info["version_loader"],
            "fuente_loader": info["fuente"],
            "confianza": info["confianza"],
            "mods": mods,
            "integraciones": resueltos,
            "duenos": [{"area": a, "mod": n, "id": i} for a, n, i in hallados],
        }, ensure_ascii=False, indent=2), encoding="utf-8")
        print()
        print(f"  Informe completo en {destino}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
