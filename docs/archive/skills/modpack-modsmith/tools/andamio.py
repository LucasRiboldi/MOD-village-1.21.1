#!/usr/bin/env python3
"""
andamio.py — crea el esqueleto de un mod ya cuadrado con el pack de destino.

La plantilla oficial de Fabric no sirve para esto: genera un proyecto que pide
un loader mas nuevo del que corre tu pack, y ese mod no carga sin dar ningun
error visible. Este andamio hace lo contrario: lee el pack primero y clava
todos los numeros a lo que ese pack usa de verdad.

Lo que genera:
    settings.gradle, build.gradle, gradle.properties
    gradlew / gradlew.bat / gradle/wrapper/     (copiados de una plantilla)
    src/main/resources/fabric.mod.json
    src/main/java/<paquete>/<Clase>.java        (entrypoint)
    src/client/...                              (si el mod es de cliente)
    mixins.json + un mixin de ejemplo comentado
    assets/<modid>/lang/en_us.json y es_es.json
    .gitignore

Uso:
    python andamio.py --pack "<ruta_pack>" --destino "<carpeta_nueva>" \
        --id tpstamina --nombre "Mi Mod" --paquete net.midominio.mimod

    # mod de cliente (camara, HUD, entrada) con codigo separado por entorno:
    python andamio.py ... --entorno client

    # de donde copiar el gradle wrapper (si no, lo busca solo):
    python andamio.py ... --plantilla "<ruta_de_otro_mod_tuyo>"

Solo biblioteca estandar. Python 3.9+.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from radiografia import detectar_loader, localizar_raiz, identidad_jar  # noqa: E402

for _flujo in (sys.stdout, sys.stderr):
    if hasattr(_flujo, "reconfigure"):
        _flujo.reconfigure(encoding="utf-8", errors="replace")

AGENTE = "modpack-modsmith/1.0 (skill de Claude Code)"

# Ultimo recurso si no hay red. La API de Modrinth es la fuente buena.
FABRIC_API_CONOCIDA = {
    "1.20.1": "0.92.12+1.20.1",
    "1.20.4": "0.97.2+1.20.4",
    "1.20.6": "0.100.8+1.20.6",
    "1.21":   "0.102.0+1.21",
    "1.21.1": "0.115.6+1.21.1",
    "1.21.4": "0.119.2+1.21.4",
    "1.21.5": "0.127.1+1.21.5",
}

# Java y Loom cambian con la version de Minecraft. Estos son los pares que
# se sabe que casan; fuera de la tabla se avisa en vez de inventar.
CARRIL = {
    "1.20.1": {"java": 17, "loom": "1.17-SNAPSHOT", "jdk_gradle": 21},
    "1.20.4": {"java": 17, "loom": "1.17-SNAPSHOT", "jdk_gradle": 21},
    "1.20.6": {"java": 21, "loom": "1.17-SNAPSHOT", "jdk_gradle": 21},
    "1.21":   {"java": 21, "loom": "1.17-SNAPSHOT", "jdk_gradle": 21},
    "1.21.1": {"java": 21, "loom": "1.17-SNAPSHOT", "jdk_gradle": 21},
    "1.21.4": {"java": 21, "loom": "1.17-SNAPSHOT", "jdk_gradle": 21},
    "1.21.5": {"java": 21, "loom": "1.17-SNAPSHOT", "jdk_gradle": 21},
}


def fabric_api_para(mc: str) -> str:
    """Pregunta a Modrinth la ultima fabric-api para esa version de MC."""
    import urllib.error
    import urllib.parse
    import urllib.request

    gv = urllib.parse.quote(json.dumps([mc]))
    url = (f"https://api.modrinth.com/v2/project/fabric-api/version"
           f"?game_versions={gv}&loaders=%5B%22fabric%22%5D")
    try:
        pet = urllib.request.Request(url, headers={"User-Agent": AGENTE})
        with urllib.request.urlopen(pet, timeout=20) as r:
            versiones = json.loads(r.read().decode("utf-8"))
        if versiones:
            return versiones[0].get("version_number", "")
    except Exception:  # noqa: BLE001 - sin red se tira de tabla
        pass
    return FABRIC_API_CONOCIDA.get(mc, "")


def buscar_wrapper(pistas: list[Path]) -> Path | None:
    """El gradle-wrapper.jar no se puede escribir a mano: hay que copiarlo."""
    for base in pistas:
        if not base or not base.exists():
            continue
        for cand in [base, *sorted(p for p in base.iterdir() if p.is_dir())]:
            if (cand / "gradle" / "wrapper" / "gradle-wrapper.jar").exists():
                return cand
    return None


def escribir(ruta: Path, contenido: str) -> None:
    ruta.parent.mkdir(parents=True, exist_ok=True)
    # Sin BOM y con \n: Gradle y el loader leen mal los ficheros con BOM.
    ruta.write_text(contenido, encoding="utf-8", newline="\n")
    print(f"    {ruta}")


def main() -> int:
    ap = argparse.ArgumentParser(description="Crea el esqueleto de un mod para un pack concreto.")
    ap.add_argument("--pack", required=True, help="pack donde va a vivir el mod")
    ap.add_argument("--destino", required=True, help="carpeta nueva del proyecto")
    ap.add_argument("--id", required=True, help="mod id (minusculas, sin espacios)")
    ap.add_argument("--nombre", default=None, help="nombre visible del mod")
    ap.add_argument("--paquete", required=True, help="paquete java, p.ej. net.midominio.mimod")
    ap.add_argument("--descripcion", default="", help="que hace el mod, en una frase")
    ap.add_argument("--autor", default="", help="tu nombre o el del proyecto")
    ap.add_argument("--entorno", choices=["client", "both"], default="both",
                    help="client = camara/HUD/entrada; both = logica de juego")
    ap.add_argument("--integrar", default="",
                    help="ids de mods del pack con los que te integras (coma)")
    ap.add_argument("--plantilla", default=None, help="proyecto del que copiar el gradle wrapper")
    ap.add_argument("--licencia", default="MIT")
    args = ap.parse_args()

    if not re.fullmatch(r"[a-z][a-z0-9_-]{1,63}", args.id):
        print("ERROR: el mod id debe ser minusculas, digitos, _ o -, y empezar por letra.",
              file=sys.stderr)
        return 2
    if not re.fullmatch(r"[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+", args.paquete):
        print("ERROR: el paquete java debe ser tipo net.midominio.mimod", file=sys.stderr)
        return 2

    raiz_pack = localizar_raiz(Path(args.pack).expanduser().resolve())
    info = detectar_loader(raiz_pack)
    mc = info["minecraft"]
    if mc == "?":
        print("ERROR: no he podido determinar la version de Minecraft del pack.", file=sys.stderr)
        print("Arranca el pack una vez para que escriba logs/latest.log.", file=sys.stderr)
        return 2
    if info["loader"] not in ("fabric", "quilt"):
        print(f"ERROR: este andamio genera proyectos Fabric; el pack es {info['loader']}.",
              file=sys.stderr)
        print("Para NeoForge/Forge, usa la skill minecraft-modding para el esqueleto y",
              file=sys.stderr)
        print("vuelve aqui para la integracion con el pack y las pruebas.", file=sys.stderr)
        return 2

    carril = CARRIL.get(mc)
    if not carril:
        print(f"AVISO: {mc} no esta en la tabla de carriles conocidos.")
        print("Uso Java 21 / Loom 1.17-SNAPSHOT. Verificalo antes de fiarte.")
        carril = {"java": 21, "loom": "1.17-SNAPSHOT", "jdk_gradle": 21}

    destino = Path(args.destino).expanduser().resolve()
    if destino.exists() and any(destino.iterdir()):
        print(f"ERROR: {destino} ya existe y no esta vacia.", file=sys.stderr)
        return 2

    nombre = args.nombre or args.id
    clase = "".join(p.capitalize() for p in re.split(r"[_-]", args.id)) or "MiMod"
    ruta_paq = args.paquete.replace(".", "/")
    grupo = ".".join(args.paquete.split(".")[:-1]) or args.paquete
    es_cliente = args.entorno == "client"
    api = fabric_api_para(mc)

    # ---- dependencias del pack -------------------------------------------
    integraciones = []
    if args.integrar:
        buscados = [s.strip().lower() for s in args.integrar.split(",") if s.strip()]
        for j in sorted((raiz_pack / "mods").rglob("*.jar")):
            ident = identidad_jar(j)
            if ident and ident.get("id", "").lower() in buscados:
                integraciones.append(ident)

    print()
    print(f"  Pack:      {raiz_pack}")
    print(f"  Minecraft: {mc}   Loader: {info['loader']} {info['version_loader']}"
          f"  ({info['fuente']})")
    print(f"  Java:      {carril['java']} para compilar, "
          f"JDK {carril['jdk_gradle']} para EJECUTAR gradle")
    print(f"  Fabric API {api or '(no resuelta - revisala a mano)'}")
    print()
    print("  Creando:")

    # ---- gradle.properties ------------------------------------------------
    escribir(destino / "gradle.properties", f"""# Memoria para gradle
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
org.gradle.configuration-cache=false

# Fabric
# OJO: loader_version esta clavado al que corre el pack ({info['fuente']}).
# Si pides uno mas nuevo, el mod NO CARGA y no avisa de forma obvia.
# OJO 2: Loom necesita JDK {carril['jdk_gradle']} para EJECUTAR gradle,
# aunque el mod se compile para Java {carril['java']}.
minecraft_version={mc}
loader_version={info['version_loader']}
loom_version={carril['loom']}

# Mod
version=0.1.0
group={grupo}

# Dependencias
fabric_api_version={api}
""")

    # ---- settings.gradle --------------------------------------------------
    escribir(destino / "settings.gradle", f"""pluginManagement {{
\trepositories {{
\t\tmaven {{
\t\t\tname = 'Fabric'
\t\t\turl = 'https://maven.fabricmc.net/'
\t\t}}
\t\tmavenCentral()
\t\tgradlePluginPortal()
\t}}
}}

rootProject.name = '{args.id}'
""")

    # ---- build.gradle -----------------------------------------------------
    bloque_loom = (f"""loom {{
\tsplitEnvironmentSourceSets()

\tmods {{
\t\t"{args.id}" {{
\t\t\tsourceSet sourceSets.main
\t\t\tsourceSet sourceSets.client
\t\t}}
\t}}
}}
""" if es_cliente else "")

    lineas_compat = "\n".join(
        f'\t// modCompileOnly "maven.modrinth:<slug>:<versionID>"   // {m["nombre"]} {m["version"]}'
        for m in integraciones
    )
    nota_compat = (f"""
\t// Solo para compilar contra sus APIs. No se empaquetan: los aporta el pack.
\t// Saca las coordenadas exactas con:
\t//   python tools/radiografia.py "<pack>" --integrar {args.integrar}
{lineas_compat}
""" if integraciones else """
\t// Para integrarte con un mod que ya esta en el pack, anade aqui:
\t//   modCompileOnly "maven.modrinth:<slug>:<versionID>"
\t// La coordenada exacta te la da tools/radiografia.py --integrar <id>
""")

    escribir(destino / "build.gradle", f"""plugins {{
\tid 'net.fabricmc.fabric-loom-remap' version "${{loom_version}}"
}}

repositories {{
\tmaven {{
\t\tname = 'Modrinth'
\t\turl = uri('https://api.modrinth.com/maven')
\t\tcontent {{
\t\t\tincludeGroup 'maven.modrinth'
\t\t}}
\t}}
}}
{bloque_loom}
dependencies {{
\tminecraft "com.mojang:minecraft:${{project.minecraft_version}}"
\tmappings loom.officialMojangMappings()
\tmodImplementation "net.fabricmc:fabric-loader:${{project.loader_version}}"
\tmodImplementation "net.fabricmc.fabric-api:fabric-api:${{project.fabric_api_version}}"
{nota_compat}}}

processResources {{
\tdef version = project.version
\tinputs.property "version", version

\tfilesMatching("fabric.mod.json") {{
\t\texpand "version": version
\t}}
}}

tasks.withType(JavaCompile).configureEach {{
\tit.options.release = {carril['java']}
}}

java {{
\tsourceCompatibility = JavaVersion.VERSION_{carril['java']}
\ttargetCompatibility = JavaVersion.VERSION_{carril['java']}
}}
""")

    # ---- fabric.mod.json --------------------------------------------------
    # El suelo de fabricloader NUNCA debe estar por encima del loader del pack:
    # si lo pides mas nuevo, el mod no carga y no aparece ningun error. Se usa
    # un suelo bajo y conocido, y solo se baja mas si el pack va por debajo.
    def trozos(v: str) -> tuple:
        return tuple(int(x) for x in re.findall(r"\d+", v)[:3]) or (0,)

    suelo = "0.15.0"
    if info["version_loader"] != "?" and trozos(info["version_loader"]) < trozos(suelo):
        suelo = info["version_loader"]

    depende = {
        "fabricloader": f">={suelo}",
        "minecraft": f"~{mc}",
        "java": f">={carril['java']}",
        "fabric-api": "*",
    }
    for m in integraciones:
        depende[m["id"]] = "*"

    entrypoints = ({"client": [f"{args.paquete}.{clase}Client"]} if es_cliente
                   else {"main": [f"{args.paquete}.{clase}"]})

    meta = {
        "schemaVersion": 1,
        "id": args.id,
        "version": "${version}",
        "name": nombre,
        "description": args.descripcion or f"{nombre}.",
        "authors": [args.autor] if args.autor else [],
        "license": args.licencia,
        "environment": "client" if es_cliente else "*",
        "entrypoints": entrypoints,
        "mixins": [{"config": f"{args.id}.mixins.json",
                    **({"environment": "client"} if es_cliente else {})}],
        "depends": depende,
    }
    escribir(destino / "src/main/resources/fabric.mod.json",
             json.dumps(meta, ensure_ascii=False, indent="\t") + "\n")

    # ---- codigo -----------------------------------------------------------
    if es_cliente:
        escribir(destino / f"src/client/java/{ruta_paq}/{clase}Client.java", f"""package {args.paquete};

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada de cliente de {nombre}.
 *
 * Corre DESPUES de que el loader haya cargado todos los mods, asi que aqui ya
 * puedes preguntar por los que esperas del pack con FabricLoader.getInstance()
 * .isModLoaded("id"). Hazlo: un pack cambia, y un mod que asume que algo esta
 * ahi revienta el dia que el dueno del pack lo quita.
 */
public class {clase}Client implements ClientModInitializer {{
\tpublic static final String MOD_ID = "{args.id}";
\tpublic static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

\t@Override
\tpublic void onInitializeClient() {{
\t\tLOG.info("[{args.id}] cargado");
\t}}
}}
""")
    else:
        escribir(destino / f"src/main/java/{ruta_paq}/{clase}.java", f"""package {args.paquete};

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada de {nombre}.
 *
 * Corre DESPUES de que el loader haya cargado todos los mods, asi que aqui ya
 * puedes preguntar por los que esperas del pack con FabricLoader.getInstance()
 * .isModLoaded("id"). Hazlo: un pack cambia, y un mod que asume que algo esta
 * ahi revienta el dia que el dueno del pack lo quita.
 */
public class {clase} implements ModInitializer {{
\tpublic static final String MOD_ID = "{args.id}";
\tpublic static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

\t@Override
\tpublic void onInitialize() {{
\t\tLOG.info("[{args.id}] cargado");
\t}}
}}
""")

    # ---- mixins -----------------------------------------------------------
    carpeta_mixin = "client" if es_cliente else "main"
    clave_mixin = "client" if es_cliente else "mixins"
    escribir(destino / f"src/{carpeta_mixin}/resources/{args.id}.mixins.json",
             json.dumps({
                 "required": True,
                 "minVersion": "0.8",
                 "package": f"{args.paquete}.mixin",
                 "compatibilityLevel": f"JAVA_{carril['java']}",
                 clave_mixin: [],
                 "injectors": {"defaultRequire": 1},
             }, ensure_ascii=False, indent="\t") + "\n")

    escribir(destino / f"src/{carpeta_mixin}/java/{ruta_paq}/mixin/LEEME.md", f"""# Mixins de {nombre}

Cada mixin que anadas aqui hay que listarlo **por nombre de clase** en
`{args.id}.mixins.json`, en la lista `"{clave_mixin}"`. Si no lo listas, no se
aplica y no hay ningun error: el mod carga y no hace nada.

Antes de escribir un mixin sobre una clase vanilla, comprueba quien manda de
verdad en eso dentro del pack:

```bash
python tools/radiografia.py "<pack>"
```

Si un mod del pack ya gobierna esa area (camara, entrada, HUD), tu mixin sobre
la clase vanilla puede no dispararse nunca porque ese mod la esquiva. En ese
caso hay que integrarse por su API, no por encima de el.
""")

    # ---- lang -------------------------------------------------------------
    for idioma in ("en_us", "es_es"):
        escribir(destino / f"src/{carpeta_mixin}/resources/assets/{args.id}/lang/{idioma}.json",
                 json.dumps({f"{args.id}.name": nombre}, ensure_ascii=False, indent="\t") + "\n")

    # ---- .gitignore -------------------------------------------------------
    escribir(destino / ".gitignore", """.gradle/
build/
run/
.idea/
*.iml
out/
""")

    # ---- wrapper ----------------------------------------------------------
    pistas = [Path(args.plantilla).expanduser().resolve()] if args.plantilla else []
    pistas += [destino.parent, Path.cwd()]
    origen_wrapper = buscar_wrapper([p for p in pistas if p])

    print()
    if origen_wrapper:
        for rel in ("gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar",
                    "gradle/wrapper/gradle-wrapper.properties"):
            src = origen_wrapper / rel
            if src.exists():
                dst = destino / rel
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, dst)
        print(f"  Gradle wrapper copiado de {origen_wrapper}")
    else:
        print("  AVISO: no he encontrado un gradle wrapper que copiar.")
        print("  gradle-wrapper.jar es binario: no se puede escribir a mano.")
        print("  Opciones:")
        print("    - copiar gradlew, gradlew.bat y gradle/ de otro mod tuyo")
        print("    - o, con gradle instalado:  gradle wrapper --gradle-version 8.10")

    print()
    print("=" * 68)
    print(f"  Proyecto creado en {destino}")
    print("=" * 68)
    print("  Siguiente paso -- construir con el JDK correcto:")
    print(f'      python tools/construir.py "{destino}"')
    print("  Y antes de dar nada por bueno:")
    print(f'      python tools/probar.py "{destino}" --pack "{raiz_pack}"')
    return 0


if __name__ == "__main__":
    sys.exit(main())
