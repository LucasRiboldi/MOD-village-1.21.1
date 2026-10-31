# La escalera de pruebas

Un mod de Minecraft falla de una forma especialmente mala: **casi nunca casca**. Se queda sin cargar, o carga y no hace nada, y el juego arranca tan tranquilo. "Compila" no significa nada, y "arranca" tampoco significa que tu mod esté haciendo algo.

De ahí esta escalera. Cada nivel cuesta más que el anterior y encuentra cosas que el anterior no puede ver.

| Nivel | Qué encuentra | Coste | Automático |
|---|---|---|---|
| 1 estático | el jar está mal montado por dentro | segundos | sí |
| 2 contra el pack | el jar no encaja en ese pack | segundos | sí |
| 3 arranque real | el mod rompe el arranque | minutos | sí |
| 4 dentro del pack | choca con otro de los 148 mods | minutos | parcial |
| 5 mando virtual | el mod no reacciona a la entrada | minutos | sí, si instrumentas |
| 6 jugarlo | la mecánica no se siente bien | — | **no, nunca** |

---

## Niveles 1 y 2 — la puerta

```bash
python tools/probar.py "<PROYECTO>" --pack "<RUTA_DEL_PACK>"
```

Sale con código distinto de cero si hay algún FALLO. **Si está en rojo, el mod no se entrega.**

**Nivel 1 — el jar contra sí mismo.** Abre el jar y comprueba lo que solo se puede ver desde dentro:

- `fabric.mod.json` existe y es JSON válido
- no quedan `${...}` sin expandir (falta `processResources` en `build.gradle`)
- cada clase de `entrypoints` **existe dentro del jar** — si no, el mod carga y no hace nada
- cada `mixins.json` declarado está, y cada clase que lista existe
- **mixins compilados que nadie lista** — están en el jar, no se aplican, y no dan error
- el `accessWidener` declarado está presente
- los plugins de otros mods (JSON en la raíz) apuntan a clases que existen

**Nivel 2 — el jar contra el pack de destino.** Aquí está el fallo número uno:

- **`depends.fabricloader` ≤ el loader que corre el pack.** Si pides más, el mod no carga y el juego arranca sin decir nada. Es el fallo más difícil de ver a ojo y el más común.
- la versión de Minecraft del mod es la del pack
- **todas** las dependencias declaradas están en el pack (contando las anidadas por Jar-in-Jar) — con una que falte, el loader para el arranque entero
- el `environment` es coherente con los entrypoints
- no hay dos jars con el mismo mod id en `mods/`

---

## Nivel 3 — que un servidor de verdad arranque

```bash
python tools/probar.py "<PROYECTO>" --pack "<PACK>" --arranque
```

Levanta un servidor Fabric real con el mod, espera a que llegue a `Done`, y se apaga solo. Busca en el log las marcas que importan: `Mixin apply failed`, `Could not execute entrypoint`, `Incompatible mod set`, dependencias que faltan.

Vale para mods con lógica de servidor (`environment: "*"`). Para un mod solo de cliente no arranca nada útil y el script lo dice.

La primera vez tarda: descarga y descompila Minecraft. Súbele el tope con `--minutos` si hace falta.

---

## Nivel 4 — dentro del pack de verdad

Los niveles 1-3 prueban tu mod. El 4 prueba tu mod **rodeado de los otros 147**, que es donde aparecen los choques de mixin.

```bash
python tools/construir.py "<PROYECTO>" --instalar-en "<PACK>"
```

Luego el usuario arranca el pack. Qué mirar, **en este orden**:

1. `crash-reports/`, el más reciente — si hay, ahí está el error real
2. `logs/latest.log` — busca el mod id y la palabra `Mixin`
3. **el log del propio launcher** — cuando un mod muere *antes* de que arranque el juego, el error solo aparece ahí y `latest.log` no dice nada útil

Y lee la excepción de verdad, no la primera línea roja:

| Lo que dice | Lo que significa |
|---|---|
| `Missing dependency` / `requires X` | falta un mod: vuelve al Nivel 2 |
| `Mixin apply failed` | tu mixin no encajó, o alguien se puso por delante |
| `NoSuchMethodError` / `NoClassDefFoundError` | compilaste contra una versión distinta de la que corre |
| `NullPointerException` al registrar | un ID que no existe |
| **nada, el juego arranca normal** | tu mod no cargó — Nivel 2 otra vez |

Ese último es el caso raro y el más frecuente. **Si el juego arranca y tu mod no aparece en la lista de mods, no ha cargado.**

Deshacer es fácil: quitar el jar de `mods/` deja el pack exactamente como estaba. Un mod propio no toca nada más.

---

## Nivel 5 — mando virtual

Para mods de entrada: lock-on, combos, cámara, esquemas de mando.

Es **lo único de todo el kit que necesita instalar algo**:

- **ViGEmBus**, el driver que hace que Windows se crea el mando — https://github.com/nefarius/ViGEmBus/releases (pide administrador)
- `pip install vgamepad`
- solo Windows

```bash
python tools/mando-virtual.py --comprobar
```

### Esto solo es una prueba si instrumentas el mod

Mover sticks a ciegas no comprueba nada. El método que funciona tiene dos piezas:

**1. Tu mod escribe lo que le pasa por dentro:**

```java
if (DEPURACION) LOG.info("[mimod] objetivo enganchado: {}", entidad.getName());
if (DEPURACION) LOG.info("[mimod] objetivo soltado");
```

**2. El script pulsa los botones y comprueba que esas líneas salieron, en orden:**

```bash
python tools/mando-virtual.py --guion guion.json \
    --log "<PACK>/logs/latest.log" \
    --esperar "objetivo enganchado" \
    --esperar "objetivo soltado"
```

Compara el log solo desde el momento de empezar, así que no se confunde con ejecuciones anteriores. Si el log no ha crecido nada, te dice que o el juego no tenía el foco o el mod no escribe nada.

### Un guion de ejemplo

```json
{
  "descripcion": "engancha, cambia de objetivo con el stick, suelta",
  "pasos": [
    {"esperar": 3.0, "nota": "volver a la ventana del juego"},
    {"boton": "RIGHT_THUMB", "duracion": 0.1, "nota": "enganchar"},
    {"esperar": 1.0},
    {"stick_derecho": [1.0, 0.0], "duracion": 0.25, "nota": "golpe de stick a la derecha"},
    {"stick_derecho": [0.0, 0.0]},
    {"esperar": 1.0},
    {"boton": "RIGHT_THUMB", "duracion": 0.1, "nota": "soltar"},
    {"esperar": 0.5}
  ]
}
```

Botones: `A B X Y LEFT_SHOULDER RIGHT_SHOULDER BACK START LEFT_THUMB RIGHT_THUMB DPAD_UP DPAD_DOWN DPAD_LEFT DPAD_RIGHT`.
Gatillos: `{"gatillo_izq": 0.0-1.0}`, `{"gatillo_der": 0.0-1.0}`.
Sticks: `{"stick_izq": [x, y]}`, `{"stick_derecho": [x, y]}`, de -1.0 a 1.0.

El `--calentamiento` (5 s por defecto) está para darte tiempo a poner el foco en la ventana del juego. Sin foco, Minecraft no lee el mando y la prueba sale vacía.

---

## Nivel 6 — jugarlo, que no lo hace ningún script

Ningún script comprueba que una mecánica **se sienta bien**. Que el lock-on enganche a quien tú mirabas y no al más cercano. Que el parry perdone lo justo. Que la cámara no maree. Que la stamina castigue sin frustrar.

Eso lo dice jugar, y lo dice el usuario.

**Los niveles 1-5 existen para que, cuando el usuario lo pruebe, lo que falle sea el diseño y no el montaje.** Es una diferencia importante: el diseño se ajusta con una conversación y un número; el montaje se lleva una tarde.

Por eso, al entregar, **nunca digas que el mod "funciona"**. Di qué pasó la puerta, y di qué falta comprobar jugando, en frases concretas que el usuario pueda verificar en dos minutos:

> Pasó los niveles 1-3. Lo que falta por ver jugando:
> - que enganche al enemigo del centro de la pantalla, no al más cercano
> - que al taparse el enemigo tras un bloque no se suelte al instante
> - que a 120 fps la cámara no se pegue de golpe

Eso es una lista que se puede comprobar. "Probar el lock-on" no lo es.
