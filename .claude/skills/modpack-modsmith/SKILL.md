---
name: modpack-modsmith
description: "Escribe un mod propio de Minecraft que tiene que convivir con un modpack que ya existe, y no lo entrega hasta haberlo probado. Usa esta skill cuando el usuario quiera crear un mod desde cero, recrear una mecánica que vio en otro pack, añadir una feature que ningún mod existente cubre, integrar su mod con mods que ya lleva el pack, o cuando su mod compila pero no carga o no hace nada. Triggers: 'quiero crear un mod', 'hazme un mod que haga X', 'no existe ningún mod que haga esto', 'recrea esta mecánica en mi pack', 'busca una alternativa a este mod', 'mi mod no carga', 'mi mod compila pero no hace nada', 'el mixin no se dispara', 'quiero portar esta mecánica pero es código Java', 'escribir un mod de Fabric', 'integrar mi mod con otro mod del pack', 'probar mi mod', 'mod para mi modpack', 'crear mod Minecraft'."
---

# Modpack Modsmith — escribir un mod que tiene que vivir dentro de un pack

Un mod que se escribe **para un pack concreto** no es un mod cualquiera. No se compila contra "Fabric": se compila contra **el** Fabric que corre ese pack, contra **las** versiones de los mods con los que se integra, y tiene que convivir con otros ciento y pico mods que ya se repartieron el terreno.

**La regla que gobierna todo lo demás: en Minecraft, un mod mal integrado casi nunca casca.** Se queda sin cargar, o carga y no hace nada, y el juego arranca tan tranquilo. Por eso "compila" no significa nada, y por eso esta skill **no entrega un mod sin pasar la Fase 6**.

Las herramientas están en `tools/`, junto a este fichero. Python 3.9+ de biblioteca estándar; lo único que necesita instalar algo es el mando virtual, y es opcional.

> **Esta skill no es un manual de la API de Minecraft.** Para la sintaxis de registros, block entities, networking o datagen, tira de la skill `minecraft-modding` si está disponible, o de la documentación del loader. Aquí está lo otro: **cómo se hace que ese código entre en un pack que ya existe y se sepa que funciona.**

---

## Fase 0 — ¿De verdad hace falta un mod?

Escribir un mod es la opción cara. Antes de aceptarla, descarta las baratas **en este orden**:

| Si lo que se quiere es… | La respuesta correcta es | Y NO un mod |
|---|---|---|
| Recetas, loot, tags, avances | un **datapack** | funciona en cualquier loader y no se compila |
| Texturas, modelos, HUD, fuentes, sonidos | un **resourcepack** | mismo motivo |
| Números, balance, dificultad | **editar la config** del mod que ya lo gobierna | tocar `config/`, no escribir Java |
| Una mecánica que ya existe en otro mod del mismo loader | **instalar ese mod** | y decirlo claramente |
| Una mecánica que existe pero **en otro loader** | ahí sí: **recrearla** | un jar de Forge no carga en Fabric jamás |
| Una mecánica que no existe en ningún sitio | ahí sí: **escribirla** | |

Si el usuario viene de la skill `modpack-migrator` con el veredicto *"la feature es código Java, no se puede copiar"*, **ese es exactamente el punto de entrada de esta skill**: la Fase 5 del migrator termina donde empieza la Fase 1 de aquí.

**Un aviso que hay que dar una vez, y sin rodeos:** si el usuario pide "una alternativa" a un mod, casi nunca quiere que le instales otro mod ajeno parecido. Quiere lo que ese mod hace, en su pack. Instalar el mod de otro es lo contrario de lo que ha pedido. Pregunta cuál de las dos cosas es si hay duda, pero no des por hecho la fácil.

**Y una decisión de forma que conviene tomar pronto: un mod por mecánica, no un mod paraguas.** Cada pieza se prueba, se versiona, se desactiva y se publica por separado. Un mod que hace cinco cosas es un mod que, cuando una falla, no arranca ninguna.

---

## Fase 1 — Radiografía del pack, antes de escribir una línea

Esto va **primero**. Sin estos datos no se puede ni crear el proyecto.

```bash
python tools/radiografia.py "<RUTA_DEL_PACK>"
```

Devuelve cuatro cosas, y las cuatro importan:

1. **La versión del loader que corre el pack de verdad**, leída de `logs/latest.log` (la línea `Loading Minecraft 1.20.1 with Fabric Loader 0.19.3`). Es el dato que más cuesta y el que nadie mira. **Si tu mod pide un loader más nuevo que ese, no carga, y no hay mensaje de error.** La plantilla oficial de Fabric genera justamente eso.
2. **El bloque de `gradle.properties`** ya cuadrado con el pack.
3. **Quién manda en cada subsistema** (cámara, entrada, HUD, combate). Ver la Fase 4: esto decide cómo se escribe el mod, no solo con qué se compila.
4. **La coordenada maven exacta** de cada mod con el que te quieras integrar:

```bash
python tools/radiografia.py "<PACK>" --integrar paraglider,frycparry,shouldersurfing
```

Resuelve el jar **por su hash sha512 contra la API de Modrinth**, que es la única forma de acertar la versión sin adivinar: el nombre del fichero y la etiqueta de la web mienten a menudo. Devuelve líneas listas para pegar:

```groovy
modCompileOnly "maven.modrinth:paragliders:6qmc3Nyl"   // Paragliders 20.1.4
```

Si el pack no tiene `logs/latest.log`, **dile al usuario que arranque el pack una vez** y vuelve a ejecutarlo. Es más rápido que adivinar.

---

## Fase 2 — Decidir la forma del mod

Tres preguntas, y las tres cambian el proyecto:

**¿Cliente o ambos lados?** Cámara, HUD, entrada, mando y sonido son **cliente**. Daño, stamina, IA, drops, mundo y cualquier cosa que deba ser igual para todos los jugadores es **ambos** (o servidor). Un mod de cliente colado en el servidor rompe el arranque, y el error casi nunca nombra al culpable.

**¿Dependencia dura o blanda?** Un mod en `depends` que falte **para el arranque entero del juego**. Si tu mod puede funcionar sin él, va en `recommends` y se comprueba en tiempo de ejecución:

```java
if (FabricLoader.getInstance().isModLoaded("paraglider")) { ... }
```

Hazlo siempre que puedas: un pack cambia, y el día que su dueño quite ese mod, el tuyo tiene que degradarse, no tumbar el juego.

**¿Por dónde te enganchas?** Ver Fase 4. Se decide antes de escribir, no después de que el mixin no se dispare.

---

## Fase 3 — Crear el proyecto

```bash
python tools/andamio.py \
  --pack "<RUTA_DEL_PACK>" \
  --destino "<CARPETA_NUEVA>" \
  --id mimod --nombre "Mi Mod" --paquete net.midominio.mimod \
  --entorno client \
  --integrar shouldersurfing,frycparry \
  --plantilla "<otro proyecto tuyo, para copiar el gradle wrapper>"
```

**No uses la plantilla oficial de Fabric para esto.** Genera un proyecto que pide un loader más nuevo del que corre el pack, y ese mod no carga sin dar ningún error. El andamio hace lo contrario: lee el pack primero y clava los números a lo que ese pack usa.

`gradle-wrapper.jar` es binario y no se puede escribir a mano: el andamio lo copia de otro proyecto (`--plantilla`, o lo busca solo en carpetas hermanas). Si no encuentra ninguno, lo dice y explica cómo generarlo.

---

## Fase 4 — Escribir la mecánica, sabiendo quién manda

Aquí es donde se pierde el tiempo si no se sabe esto:

### Dentro de un pack, tú no eres el dueño de nada

Un mixin sobre una clase vanilla **solo funciona si nadie se ha puesto por delante**. Un pack de combate serio tiene un mod que gobierna la cámara, otro la entrada, otro el HUD. Si tocas su área, tu mixin puede no dispararse **nunca**, y no hay error: simplemente el código no corre.

La radiografía te dice quién manda. El orden a seguir es:

1. **¿El dueño tiene API pública o sistema de plugins?** Úsalo. Es lo estable.
2. **¿Tiene eventos?** Suscríbete.
3. **Mixin sobre el dueño**, no sobre vanilla. Frágil pero funciona.
4. **Mixin sobre vanilla**: solo si nadie manda en esa área.

**Los plugins de otros mods no son entrypoints de Fabric.** Varios mods se extienden por un fichero JSON propio **en la raíz del jar**, no por `fabric.mod.json`. Si escribes un plugin de esos y lo pones en `entrypoints`, no se carga nunca. La forma de averiguarlo es mirar cómo lo hace un mod del propio pack que ya lo haga.

### Lo que se calibra por tick no vale por frame

Un valor de seguimiento afinado a 20 ticks por segundo, aplicado en el bucle de render, **se comporta distinto en cada máquina**: a 120 fps se pega de golpe, a 20 tiembla. Si el número se calibró por tick y se aplica por frame, hay que convertirlo con el delta del frame. Es un fallo que no se ve en el código y solo aparece cuando lo prueba otra persona con otro monitor.

### Ingeniería inversa: leer sí, copiar no

Descompilar un mod para entender **cómo** funciona y con **qué números** es trabajo normal y legítimo. Copiar su código a tu mod no lo es, salvo que su licencia lo permita (MIT, Apache) **y cites la autoría**. Lo que se reimplementa desde cero a partir de lo entendido es tuyo. Si el usuario va a publicar el mod, esto hay que decirlo antes de escribir, no después.

Ver `referencias/integracion-con-el-pack.md` para los patrones concretos.

---

## Fase 5 — Construir

```bash
python tools/construir.py "<PROYECTO>" --instalar-en "<RUTA_DEL_PACK>"
```

Resuelve las dos trampas del build:

- **El JDK que ejecuta Gradle no es el mismo con el que se compila el mod.** Fabric Loom necesita un JDK moderno para *correr*, aunque el mod salga compilado para una versión anterior de Java. Tener instalado solo el JDK de compilación no vale. La herramienta busca el JDK bueno y lo pasa por `JAVA_HOME` sin tocar el del sistema, y usa `--no-daemon` porque el demonio se queda con el `JAVA_HOME` de la primera vez y luego compila con el equivocado sin decir nada.
- **Al instalar, borra las versiones anteriores del mismo mod id.** Dos jars con el mismo id en `mods/` es un fallo de arranque, y el mensaje culpa al mod, no al duplicado.

Si el build falla, la herramienta traduce los errores más comunes (`Unsupported class file major version`, `Could not resolve maven.modrinth`, `cannot find symbol`) a lo que significan de verdad.

---

## Fase 6 — La puerta: no se entrega sin probar

**Un mod no está hecho porque compile.** Esta fase es obligatoria y su resultado se enseña al usuario.

```bash
python tools/probar.py "<PROYECTO>" --pack "<RUTA_DEL_PACK>"
```

La escalera, de menos a más coste:

| Nivel | Qué comprueba | Coste | ¿Obligatorio? |
|---|---|---|---|
| **1 — estático** | el jar contra sí mismo: entrypoints que existen, mixins listados y presentes, `${version}` expandida, ficheros declarados | segundos | **sí, siempre** |
| **2 — contra el pack** | loader ≤ el del pack, versión de MC, todas las dependencias presentes, entorno coherente, sin jars duplicados | segundos | **sí, siempre que haya pack** |
| **3 — arranque real** | un servidor Fabric de verdad levanta con el mod y llega a `Done`; se apaga solo | minutos | sí, si el mod tiene lógica de servidor |
| **4 — dentro del pack** | arrancar el pack completo con el mod puesto y leer el log | manual | sí, antes de darlo por bueno |
| **5 — mando virtual** | pulsar botones sin manos y comprobar que el mod reacciona | opcional | solo mods de entrada |

**Los niveles 1 y 2 son la puerta.** `probar.py` sale con código distinto de cero si hay algún FALLO. Si está en rojo, el mod no se entrega: se arregla y se vuelve a pasar.

Nivel 3, cuando el mod tiene lógica de servidor:

```bash
python tools/probar.py "<PROYECTO>" --pack "<PACK>" --arranque
```

Nivel 5, para mods de entrada (cámara, mando, combos). Requiere ViGEmBus y `pip install vgamepad`; es **lo único de todo el kit que necesita instalar algo**:

```bash
python tools/mando-virtual.py --comprobar
python tools/mando-virtual.py --guion guion.json \
    --log "<PACK>/logs/latest.log" \
    --esperar "objetivo enganchado" --esperar "objetivo soltado"
```

Esto solo es una prueba de verdad si **tu mod escribe en el log lo que le pasa por dentro**. Escribe esas líneas con `LOG.info` mientras desarrollas: el guion pulsa los botones y el script comprueba que las marcas salieron y en orden. Sin eso, mover sticks a ciegas no prueba nada.

### El límite, dicho claro

Ningún script comprueba que una mecánica **se sienta bien**. Que el lock-on enganche a quien tú mirabas, que el parry perdone lo justo, que la cámara no maree: eso lo dice jugar, y lo dice el usuario. Las pruebas automáticas están para que **cuando el usuario lo pruebe, lo que falle sea el diseño y no el montaje**. Nunca digas que un mod "funciona" porque pasó la puerta: di que pasó la puerta y que falta jugarlo.

---

## Fase 7 — Entregar

Al terminar, presenta:

1. **Qué hace el mod** y qué decisiones de diseño se tomaron (los números elegidos y por qué)
2. **Dónde está el proyecto** y dónde el jar
3. **El resultado de `probar.py`**, con los avisos que queden y por qué se aceptan
4. **Qué falta por comprobar jugando**, en frases concretas: *"engancha al enemigo del centro de la pantalla, no al más cercano"*, no *"probar el lock-on"*
5. **Con qué mods del pack se integra** y qué pasa si el usuario quita alguno
6. **Qué hacer si va mal**: quitar el jar de `mods/` deja el pack como estaba, porque un mod propio no toca nada más

---

## Trampas que cuestan arranques

- **Pedir un loader más nuevo que el del pack.** El mod no carga y no hay ningún error. Es el fallo número uno y el más difícil de ver. Lo detecta el Nivel 2.
- **El JDK de ejecutar Gradle no es el de compilar.** Instalar solo el JDK del `release` no basta.
- **Un mixin que no se lista en el `mixins.json` no se aplica**, y no da error. Lo detecta el Nivel 1.
- **`${version}` sin expandir** si falta el bloque `processResources`. El mod carga con una versión literal `${version}` y todo lo que dependa de la versión se comporta raro.
- **Dos versiones del mismo mod en `mods/`** después de compilar: fallo de arranque que culpa al mod equivocado.
- **Los plugins de otros mods van en la raíz del jar**, no en `entrypoints` de `fabric.mod.json`.
- **Un mod de cliente en el servidor** rompe el arranque sin nombrarse.
- **El crash real no siempre está en `logs/latest.log`.** Si el mod muere antes de que arranque el juego, mira `crash-reports/` y el log del propio launcher.
- **`options.txt` lo reescribe Minecraft al cerrarse.** Editarlo con el juego abierto no sirve de nada.
- **Las etiquetas de versión mienten.** Fíate del hash del jar (lo que hace `radiografia.py`), no del nombre del fichero ni de la web.

---

## Referencias

- `referencias/fabric-1.20.1.md` — el carril completo y verificado: build.gradle, gradle.properties, fabric.mod.json, JDK, mixins. Es el caso probado de punta a punta.
- `referencias/integracion-con-el-pack.md` — cómo engancharse a un mod que ya está en el pack: APIs, eventos, plugins que no son entrypoints, dependencias blandas.
- `referencias/pruebas.md` — la escalera de pruebas en detalle, con guiones de mando de ejemplo y cómo instrumentar el mod para que sea verificable.
