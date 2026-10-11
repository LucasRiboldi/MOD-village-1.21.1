#!/usr/bin/env python3
"""
mando-virtual.py — nivel 5: mueve un mando que no existe para probar un mod
de entrada sin tener las manos puestas.

Que hace de verdad y que NO:

  SI  crea un mando de Xbox 360 virtual que el sistema y el juego ven como
      real, y ejecuta una secuencia de botones y sticks escrita en JSON.
  SI  lee el log del juego despues y busca las marcas que tu mod escribe,
      asi que la comprobacion es automatica de principio a fin.
  NO  sabe si la mecanica "se siente bien". Eso no lo mide ningun script:
      lo dice jugar. Este script comprueba que el mod REACCIONA, no que
      la reaccion sea la correcta.

Por eso el metodo que funciona es el de dos piezas:

  1. tu mod escribe en el log lo que le pasa por dentro (un modo depuracion
     con LOG.info), y
  2. este script pulsa los botones y luego comprueba que esas lineas salieron
     en el orden esperado.

Sin la pieza 1 esto solo mueve sticks a ciegas.

REQUISITOS (es lo unico de todo el kit que necesita instalar algo):
    - ViGEmBus, un driver que crea mandos virtuales. Necesita admin.
      https://github.com/nefarius/ViGEmBus/releases
    - pip install vgamepad
    - Solo Windows.

Uso:
    python mando-virtual.py --comprobar
    python mando-virtual.py --guion guion.json
    python mando-virtual.py --guion guion.json --log "<pack>/logs/latest.log" \\
        --esperar "lockon: objetivo" --esperar "lockon: soltado"

Formato del guion (JSON):
    {
      "descripcion": "engancha, cambia de objetivo, suelta",
      "pasos": [
        {"esperar": 3.0, "nota": "tiempo para volver a la ventana del juego"},
        {"boton": "RIGHT_THUMB", "duracion": 0.1},
        {"stick_derecho": [1.0, 0.0], "duracion": 0.25},
        {"stick_derecho": [0.0, 0.0]},
        {"boton": "RIGHT_THUMB", "duracion": 0.1}
      ]
    }

Botones: A B X Y LEFT_SHOULDER RIGHT_SHOULDER BACK START
         LEFT_THUMB RIGHT_THUMB DPAD_UP DPAD_DOWN DPAD_LEFT DPAD_RIGHT
Gatillos: {"gatillo_izq": 0.0-1.0} / {"gatillo_der": 0.0-1.0}
Sticks:   {"stick_izq": [x, y]} / {"stick_derecho": [x, y]}, de -1.0 a 1.0
"""

from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path

for _flujo in (sys.stdout, sys.stderr):
    if hasattr(_flujo, "reconfigure"):
        _flujo.reconfigure(encoding="utf-8", errors="replace")

AYUDA_INSTALACION = """
Para mover un mando virtual hacen falta dos cosas, en este orden:

  1. ViGEmBus -- el driver que hace que Windows se crea el mando.
     Descarga el instalador de:
         https://github.com/nefarius/ViGEmBus/releases
     Instalalo (pide permisos de administrador) y reinicia si lo pide.

  2. La libreria de Python que lo maneja:
         pip install vgamepad

Solo funciona en Windows. En Linux el equivalente seria uinput y en macOS
no hay una via limpia; en esos sistemas hay que probar el mod a mano.

Esto es lo unico del kit que necesita instalar algo. Todo lo demas
(radiografia, andamio, construir, probar) va con la biblioteca estandar.
"""


def comprobar_entorno(verboso: bool = True) -> bool:
    problemas = []
    if sys.platform != "win32":
        problemas.append(f"el sistema es {sys.platform}; vgamepad solo va en Windows")
    try:
        import vgamepad  # noqa: F401
    except ImportError:
        problemas.append("falta la libreria vgamepad  ->  pip install vgamepad")
    except Exception as e:  # noqa: BLE001 - vgamepad lanza de todo si falta el driver
        problemas.append(f"vgamepad esta pero no arranca: {e}\n"
                         "    casi siempre significa que falta el driver ViGEmBus")

    if verboso:
        if problemas:
            print("  El mando virtual NO esta disponible:")
            for p in problemas:
                print(f"    - {p}")
            print(AYUDA_INSTALACION)
        else:
            print("  Mando virtual disponible: vgamepad y ViGEmBus responden.")
    return not problemas


def cargar_guion(ruta: Path) -> list[dict]:
    datos = json.loads(ruta.read_text(encoding="utf-8"))
    if isinstance(datos, list):
        return datos
    pasos = datos.get("pasos")
    if not isinstance(pasos, list):
        raise ValueError("el guion debe ser una lista de pasos o {'pasos': [...]}")
    if datos.get("descripcion"):
        print(f"  Guion: {datos['descripcion']}")
    return pasos


def ejecutar(pasos: list[dict], calentamiento: float) -> None:
    import vgamepad as vg

    mando = vg.VX360Gamepad()
    print(f"  Mando virtual creado. Arranco en {calentamiento:.0f} s:")
    print("  PON EL FOCO EN LA VENTANA DE MINECRAFT AHORA.")
    for queda in range(int(calentamiento), 0, -1):
        print(f"    {queda}...", end="\r", flush=True)
        time.sleep(1)
    print("  Ejecutando.        ")

    def boton_de(nombre: str):
        clave = f"XUSB_GAMEPAD_{nombre.upper()}"
        if not hasattr(vg.XUSB_BUTTON, clave):
            raise ValueError(f"boton desconocido: {nombre}")
        return getattr(vg.XUSB_BUTTON, clave)

    for i, paso in enumerate(pasos, 1):
        nota = f"  [{i}/{len(pasos)}] {paso.get('nota', '')}".rstrip()
        duracion = float(paso.get("duracion", 0.08))

        if "esperar" in paso:
            print(f"{nota}  esperar {paso['esperar']}s")
            time.sleep(float(paso["esperar"]))
            continue

        if "boton" in paso:
            b = boton_de(paso["boton"])
            print(f"{nota}  boton {paso['boton']} ({duracion}s)")
            mando.press_button(button=b)
            mando.update()
            time.sleep(duracion)
            mando.release_button(button=b)
            mando.update()

        if "stick_izq" in paso or "stick_derecho" in paso:
            if "stick_izq" in paso:
                x, y = paso["stick_izq"]
                mando.left_joystick_float(x_value_float=float(x), y_value_float=float(y))
            if "stick_derecho" in paso:
                x, y = paso["stick_derecho"]
                mando.right_joystick_float(x_value_float=float(x), y_value_float=float(y))
            print(f"{nota}  stick {paso.get('stick_izq') or paso.get('stick_derecho')}")
            mando.update()
            if duracion:
                time.sleep(duracion)

        if "gatillo_izq" in paso:
            mando.left_trigger_float(value_float=float(paso["gatillo_izq"]))
            mando.update()
            time.sleep(duracion)
        if "gatillo_der" in paso:
            mando.right_trigger_float(value_float=float(paso["gatillo_der"]))
            mando.update()
            time.sleep(duracion)

        time.sleep(0.05)

    mando.reset()
    mando.update()
    print("  Secuencia terminada, mando en reposo.")


def revisar_log(log: Path, esperadas: list[str], desde: int) -> int:
    """Comprueba que el mod dijo lo que tenia que decir, EN ORDEN."""
    if not log.exists():
        print(f"  AVISO: no encuentro {log}")
        return 1
    texto = log.read_text(encoding="utf-8", errors="replace")[desde:]
    print()
    print("=" * 68)
    print("  LO QUE DIJO EL MOD")
    print("=" * 68)
    if not texto.strip():
        print("  El log no ha crecido nada desde antes de la secuencia.")
        print("  O el juego no estaba en foco, o tu mod no escribe nada.")
        return 1

    fallos = 0
    posicion = 0
    for marca in esperadas:
        encontrado = texto.find(marca, posicion)
        if encontrado < 0:
            # ?Existe pero desordenado, o no existe?
            if marca in texto:
                print(f"[ FALLO] '{marca}' salio, pero fuera de orden")
            else:
                print(f"[ FALLO] '{marca}' no aparecio en el log")
            fallos += 1
        else:
            print(f"[  ok  ] '{marca}'")
            posicion = encontrado + len(marca)

    if not esperadas:
        print("  No has pasado ningun --esperar. Ultimas lineas nuevas del log:")
        for linea in texto.strip().splitlines()[-15:]:
            print(f"    {linea}")
    return fallos


def main() -> int:
    ap = argparse.ArgumentParser(description="Mando virtual para probar mods de entrada.")
    ap.add_argument("--comprobar", action="store_true",
                    help="solo dice si el mando virtual esta disponible")
    ap.add_argument("--guion", default=None, help="JSON con la secuencia a ejecutar")
    ap.add_argument("--log", default=None, help="latest.log del pack, para verificar")
    ap.add_argument("--esperar", action="append", default=[],
                    help="texto que tu mod deberia escribir (repetible, en orden)")
    ap.add_argument("--calentamiento", type=float, default=5.0,
                    help="segundos antes de empezar, para dar foco al juego")
    args = ap.parse_args()

    if args.comprobar or not args.guion:
        listo = comprobar_entorno()
        if not args.guion:
            if listo:
                print("\n  Pasa --guion <fichero.json> para ejecutar una secuencia.")
            return 0 if listo else 1

    if not comprobar_entorno(verboso=False):
        comprobar_entorno()
        return 1

    guion = Path(args.guion).expanduser().resolve()
    if not guion.exists():
        print(f"ERROR: no existe {guion}", file=sys.stderr)
        return 2

    log = Path(args.log).expanduser().resolve() if args.log else None
    desde = log.stat().st_size if (log and log.exists()) else 0

    try:
        pasos = cargar_guion(guion)
    except (ValueError, json.JSONDecodeError) as e:
        print(f"ERROR en el guion: {e}", file=sys.stderr)
        return 2

    ejecutar(pasos, args.calentamiento)

    if log:
        time.sleep(1.5)  # deja que el juego termine de volcar el log
        fallos = revisar_log(log, args.esperar, desde)
        print()
        if fallos:
            print(f"  {fallos} comprobacion(es) fallida(s).")
            return 1
        print("  Todas las marcas esperadas salieron y en orden.")
        print("  Recuerda: esto prueba que el mod REACCIONA, no que la")
        print("  mecanica se sienta bien. Eso solo lo dice jugarlo.")
    else:
        print()
        print("  Sin --log no hay verificacion automatica: solo has movido el mando.")
        print("  Para que esto sea una prueba de verdad, haz que tu mod escriba")
        print("  con LOG.info lo que le pasa por dentro y pasa --log y --esperar.")

    return 0


if __name__ == "__main__":
    sys.exit(main())
