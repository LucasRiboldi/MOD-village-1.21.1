#!/usr/bin/env python3
"""
construir.py — compila el mod con el JDK correcto y lo deja en el pack.

Dos cosas que cuestan un build fallido cada una y que esta herramienta quita
de en medio:

  1. Fabric Loom necesita un JDK moderno para EJECUTAR Gradle, aunque el mod
     se compile para una version anterior de Java. Tener instalado solo el
     JDK 17 no vale para compilar un mod de Java 17. Aqui se busca el JDK
     bueno y se pasa por JAVA_HOME sin tocar el del sistema.

  2. Al copiar el jar nuevo a mods/ se queda el viejo al lado. Dos versiones
     del mismo mod id en mods/ es un fallo de arranque, y el mensaje culpa al
     mod, no al duplicado. Aqui se borran los del mismo id antes de copiar.

Uso:
    python construir.py <proyecto>
    python construir.py <proyecto> --instalar-en "<ruta_pack>"
    python construir.py <proyecto> --tarea clean build
    python construir.py <proyecto> --jdk "C:/Program Files/.../jdk-21"

Solo biblioteca estandar. Python 3.9+.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

for _flujo in (sys.stdout, sys.stderr):
    if hasattr(_flujo, "reconfigure"):
        _flujo.reconfigure(encoding="utf-8", errors="replace")

# Sitios donde suele vivir un JDK, por sistema.
RAICES_JDK = [
    Path("C:/Program Files/Eclipse Adoptium"),
    Path("C:/Program Files/Java"),
    Path("C:/Program Files/Microsoft/jdk"),
    Path("C:/Program Files/Amazon Corretto"),
    Path("C:/Program Files/Zulu"),
    Path("/usr/lib/jvm"),
    Path("/Library/Java/JavaVirtualMachines"),
    Path.home() / ".sdkman/candidates/java",
]


def version_de_jdk(carpeta: Path) -> int | None:
    """Saca el major del nombre de la carpeta, y si no, preguntando al binario."""
    m = re.search(r"(?:jdk|java|temurin|zulu|corretto)[-_]?(\d{1,2})", carpeta.name, re.I)
    if m:
        return int(m.group(1))
    binario = carpeta / "bin" / ("java.exe" if os.name == "nt" else "java")
    if binario.exists():
        try:
            salida = subprocess.run([str(binario), "-version"], capture_output=True,
                                    text=True, timeout=15).stderr
            m = re.search(r'version "(\d+)', salida)
            if m:
                return int(m.group(1))
        except (OSError, subprocess.SubprocessError):
            pass
    return None


def buscar_jdk(minimo: int) -> Path | None:
    """
    Devuelve el JDK mas bajo que llegue al minimo. El mas bajo a proposito:
    un JDK demasiado nuevo tambien rompe Loom, no solo uno demasiado viejo.
    """
    candidatos: list[tuple[int, Path]] = []
    for raiz in RAICES_JDK:
        if not raiz.is_dir():
            continue
        for hijo in raiz.iterdir():
            if not hijo.is_dir():
                continue
            # Un JRE no sirve para compilar.
            if "jre" in hijo.name.lower() and "jdk" not in hijo.name.lower():
                continue
            if not (hijo / "bin").is_dir():
                continue
            v = version_de_jdk(hijo)
            if v and v >= minimo:
                candidatos.append((v, hijo))
    if not candidatos:
        return None
    candidatos.sort(key=lambda par: (par[0], str(par[1])))
    return candidatos[0][1]


def leer_propiedades(proyecto: Path) -> dict:
    props: dict[str, str] = {}
    fichero = proyecto / "gradle.properties"
    if fichero.exists():
        for linea in fichero.read_text(encoding="utf-8", errors="replace").splitlines():
            linea = linea.strip()
            if linea and not linea.startswith("#") and "=" in linea:
                clave, _, valor = linea.partition("=")
                props[clave.strip()] = valor.strip()
    return props


def jdk_necesario(props: dict) -> int:
    """
    Loom pide un JDK moderno para correr Gradle, independientemente del
    'release' con el que se compile el mod. 21 cubre todo el rango 1.20-1.21.
    """
    mc = props.get("minecraft_version", "")
    if re.match(r"1\.(2[2-9]|[3-9]\d)", mc):
        return 25
    return 21


def id_del_jar(jar: Path) -> str | None:
    try:
        with zipfile.ZipFile(jar) as zf:
            if "fabric.mod.json" in zf.namelist():
                crudo = zf.read("fabric.mod.json").decode("utf-8", "replace")
                crudo = re.sub(r",(\s*[}\]])", r"\1", re.sub(r"//[^\n\r]*", "", crudo))
                return (json.loads(crudo) or {}).get("id")
    except (zipfile.BadZipFile, OSError, json.JSONDecodeError, KeyError):
        return None
    return None


def main() -> int:
    ap = argparse.ArgumentParser(description="Compila el mod con el JDK correcto.")
    ap.add_argument("proyecto", help="carpeta del proyecto del mod")
    ap.add_argument("--instalar-en", default=None,
                    help="pack o instancia donde dejar el jar (su mods/)")
    ap.add_argument("--jdk", default=None, help="ruta del JDK con el que ejecutar gradle")
    ap.add_argument("--tarea", nargs="+", default=["build"], help="tareas de gradle")
    ap.add_argument("--sin-limpiar", action="store_true",
                    help="no borra versiones antiguas del mismo mod en mods/")
    args = ap.parse_args()

    proyecto = Path(args.proyecto).expanduser().resolve()
    gradlew = proyecto / ("gradlew.bat" if os.name == "nt" else "gradlew")
    if not gradlew.exists():
        print(f"ERROR: no hay gradlew en {proyecto}", file=sys.stderr)
        return 2

    props = leer_propiedades(proyecto)
    minimo = jdk_necesario(props)

    if args.jdk:
        jdk = Path(args.jdk).expanduser().resolve()
        if not (jdk / "bin").is_dir():
            print(f"ERROR: {jdk} no parece un JDK (no tiene bin/).", file=sys.stderr)
            return 2
    else:
        jdk = buscar_jdk(minimo)

    if not jdk:
        print(f"ERROR: no encuentro ningun JDK {minimo} o superior.", file=sys.stderr)
        print(f"Loom necesita JDK {minimo} para EJECUTAR gradle, aunque el mod se",
              file=sys.stderr)
        print(f"compile para Java {props.get('java_version', '17')}. Instalar solo el",
              file=sys.stderr)
        print("JDK de compilacion no vale.  https://adoptium.net", file=sys.stderr)
        return 2

    print(f"  Proyecto   {proyecto}")
    print(f"  Minecraft  {props.get('minecraft_version', '?')}")
    print(f"  Loader     {props.get('loader_version', '?')}")
    print(f"  JDK gradle {jdk}  (detectado: Java {version_de_jdk(jdk)})")
    print(f"  Tareas     {' '.join(args.tarea)}")
    print()

    entorno = dict(os.environ)
    entorno["JAVA_HOME"] = str(jdk)
    entorno["PATH"] = str(jdk / "bin") + os.pathsep + entorno.get("PATH", "")

    # --no-daemon: el demonio se queda con el JAVA_HOME de la primera vez y
    # luego "compila igual" con el JDK equivocado sin decir nada.
    orden = [str(gradlew), *args.tarea, "--no-daemon"]
    proceso = subprocess.run(orden, cwd=proyecto, env=entorno,
                             capture_output=True, text=True, errors="replace")

    salida = proceso.stdout + proceso.stderr
    if proceso.returncode != 0:
        print(salida[-6000:])
        print()
        print("=" * 68)
        print("  BUILD FALLIDO")
        print("=" * 68)
        pistas = [
            ("Unsupported class file major version",
             "El JDK que ejecuta gradle es demasiado nuevo o demasiado viejo para Loom."),
            ("invalid source release",
             "El 'release' de build.gradle pide un Java que este JDK no sabe generar."),
            ("Could not find net.fabricmc",
             "Version de loader o de fabric-api que no existe. Comprueba gradle.properties."),
            ("Could not resolve maven.modrinth",
             "Una coordenada de Modrinth no existe: vuelve a sacarla con radiografia.py."),
            ("cannot find symbol",
             "Una API cambio entre versiones. Comprueba que compilas contra la version del pack."),
        ]
        for aguja, explicacion in pistas:
            if aguja.lower() in salida.lower():
                print(f"  {explicacion}")
        return 1

    libs = proyecto / "build" / "libs"
    jars = [j for j in libs.glob("*.jar")
            if not j.name.endswith(("-sources.jar", "-dev.jar", "-sources-dev.jar"))]
    if not jars:
        print("ERROR: el build dijo que fue bien pero no hay jar en build/libs.",
              file=sys.stderr)
        return 1
    jar = max(jars, key=lambda j: j.stat().st_mtime)

    print(f"  BUILD OK -> {jar}  ({jar.stat().st_size:,} bytes)")

    # ---- despliegue -------------------------------------------------------
    if args.instalar_en:
        raiz = Path(args.instalar_en).expanduser().resolve()
        mods = raiz / "mods"
        if not mods.is_dir():
            for hijo in (".minecraft", "minecraft", "pack"):
                if (raiz / hijo / "mods").is_dir():
                    mods = raiz / hijo / "mods"
                    break
        if not mods.is_dir():
            print(f"ERROR: no encuentro mods/ en {raiz}", file=sys.stderr)
            return 1

        mid = id_del_jar(jar)
        if mid and not args.sin_limpiar:
            for viejo in mods.glob("*.jar"):
                if viejo.name != jar.name and id_del_jar(viejo) == mid:
                    viejo.unlink()
                    print(f"  Quitado el anterior: {viejo.name}")

        destino = mods / jar.name
        shutil.copy2(jar, destino)
        print(f"  Instalado en {destino}")
        print()
        print("  El jar ya esta en el pack, pero eso NO es una prueba.")
        print("  Antes de dar el mod por bueno:")
        print(f'      python tools/probar.py "{proyecto}" --pack "{raiz}"')

    return 0


if __name__ == "__main__":
    sys.exit(main())
