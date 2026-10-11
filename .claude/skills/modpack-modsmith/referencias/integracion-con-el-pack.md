# Integrarse con un pack que ya existe

Escribir un mod solo es fácil. Escribirlo para que conviva con otros ciento y pico que ya se repartieron el terreno es lo que cuesta. Esta referencia va de eso.

---

## La idea que lo gobierna todo: tú no eres el dueño de nada

En un pack serio, cada subsistema tiene un dueño. Un mod gobierna la cámara, otro la entrada, otro el HUD, otro el combate. Ese mod **suele haber sustituido el mecanismo vanilla entero**, no haberlo extendido.

Consecuencia directa: **un mixin tuyo sobre la clase vanilla puede no dispararse nunca**, porque el dueño ya no pasa por ahí. Y esto no da ningún error. El mod carga, el juego arranca, y tu código simplemente no corre. Se pierden horas buscando el bug en el mixin cuando el problema es que el mixin era la herramienta equivocada.

`tools/radiografia.py` lista quién manda en cada área del pack. **Míralo antes de decidir por dónde te enganchas, no después.**

### El orden a seguir, de más estable a más frágil

| Vía | Cuándo | Estabilidad |
|---|---|---|
| **API pública del dueño** | tiene clases pensadas para que otros las usen | aguanta actualizaciones |
| **Sistema de plugins del dueño** | tiene su propio registro de extensiones | aguanta, pero hay que descubrir cómo se declara |
| **Eventos** (los de Fabric API o los suyos) | lo que quieres hacer encaja en un evento existente | buena |
| **Mixin sobre el dueño** | no hay API y hay que meterse en su código | frágil: se rompe cuando él actualiza |
| **Mixin sobre vanilla** | **solo si nadie manda en esa área** | buena, si de verdad no hay dueño |

---

## Los plugins de otros mods NO son entrypoints de Fabric

Esta es la trampa que más tiempo cuesta descubrir, porque todo parece correcto.

Varios mods definen su propio sistema de extensiones, y **no se declaran en `fabric.mod.json`**. Se declaran en **un fichero JSON propio, en la raíz del jar**:

```
mimod.jar
├── fabric.mod.json
├── shouldersurfing_plugin.json     <- aqui, en la raiz, no dentro de entrypoints
└── net/midominio/mimod/...
```

```json
{
	"entrypoints": ["net.midominio.mimod.compat.MiPlugin"]
}
```

En el proyecto, eso significa ponerlo en la **raíz de `resources`**:

```
src/client/resources/shouldersurfing_plugin.json
```

Si lo declaras en `entrypoints` de `fabric.mod.json`, **el loader no sabe qué es y no pasa nada**: ni error, ni carga.

### Cómo averiguar si un mod tiene uno de estos

No lo busques en la documentación: búscalo **en el propio pack**, que es donde hay ejemplos vivos. Si tienes la skill `modpack-migrator` instalada:

```bash
python <ruta>/modpack-migrator/tools/buscar-en-jars.py "<PACK>" "_plugin.json" --en nombres
```

O directamente, mirando qué ficheros JSON hay en la raíz de los jars del pack. Un mod del pack que ya extienda a otro es la mejor documentación que vas a encontrar, porque está probado contra esas versiones exactas.

---

## Dependencias: duras, blandas y de compilación

Son **tres cosas distintas** y se mezclan a menudo.

```groovy
// COMPILACION: para poder llamar a su API. No se empaqueta ni se exige.
modCompileOnly "maven.modrinth:paragliders:6qmc3Nyl"
```

```json
// RUNTIME DURO: si falta, no arranca el juego entero.
"depends": { "paraglider": "*" }

// RUNTIME BLANDO: si falta, tu mod tiene que apañarse.
"recommends": { "combatroll": "*" }
```

Y en el código, lo blando se comprueba:

```java
private static final boolean HAY_ROLL =
    FabricLoader.getInstance().isModLoaded("combatroll");

if (HAY_ROLL) {
    // ...
}
```

**Prefiere blando siempre que puedas.** Un pack cambia; el día que su dueño quite ese mod, el tuyo debe degradarse, no tumbar el juego de todos. Y si tu mod se publica, cada `depends` es un usuario menos que puede instalarlo.

Cuidado con una asimetría: `modCompileOnly` sin `depends` compila bien y revienta en runtime con `NoClassDefFoundError` si el mod no está **y tú tocas su clase sin comprobar antes**. La comprobación con `isModLoaded` debe estar **antes** de que se cargue la clase que usa su API — normalmente metiendo ese código en una clase aparte que solo se toca dentro del `if`.

---

## Los números: por tick o por frame

Un valor de seguimiento, de interpolación o de suavizado calibrado a **20 ticks por segundo** no vale tal cual en el bucle de **render**, que corre a los fps que dé la máquina. Aplicado por frame sin convertir:

- a 120 fps se aplica seis veces más a menudo → se pega de golpe
- a 20 fps se aplica menos → tiembla y va a tirones

Es un fallo que **no se ve leyendo el código** y que solo aparece cuando lo prueba otra persona con otro monitor. Si el número viene de descompilar un mod que lo aplicaba por tick, y tú lo aplicas por frame, hay que convertirlo con el delta del frame.

Regla práctica: **anota siempre en el comentario si un número es por tick o por frame.** El que lo lea dentro de tres meses lo va a necesitar.

---

## Ingeniería inversa: hasta dónde se puede

**Se puede** descompilar un mod para entender cómo funciona y con qué números. Es lo normal cuando quieres recrear una mecánica que solo existe en otro loader.

**No se puede** copiar su código al tuyo, salvo que su licencia lo permita explícitamente (MIT, Apache 2.0, y similares) **y cites la autoría**. Muchos mods no llevan licencia o llevan una que prohíbe la redistribución.

Lo que se reimplementa desde cero a partir de lo entendido es tuyo. La frontera es: **se lee para entender, se escribe de nuevo**.

Y hay un techo honesto que conviene decir antes de empezar: **la lógica y los números se reconstruyen; las animaciones no.** Un sistema de combate con movesets por arma es trabajo de un animador sobre un esqueleto propio, y eso no sale de escribir código. Si lo que le gusta al usuario de un pack son las animaciones, decirlo el primer día ahorra una decepción al décimo.

---

## Cliente y servidor: el reparto

| Va en el cliente | Va en ambos (o solo servidor) |
|---|---|
| cámara, HUD, crosshair | daño, salud, stamina |
| entrada, mando, teclas | IA de mobs, spawns |
| sonido, partículas, shaders | drops, loot, recetas |
| pantallas de config | mundo, estructuras |
| animaciones y render | cualquier cosa que deba ser igual para todos |

Dos reglas:

1. **Un mod de cliente en el servidor rompe el arranque**, y el error casi nunca lo nombra.
2. **Si la mecánica afecta al juego, tiene que vivir en el servidor.** Una stamina que solo existe en el cliente es una stamina que no existe: el servidor no la ve, y en multijugador cada uno ve una cosa distinta.

`splitEnvironmentSourceSets()` en `build.gradle` hace que el compilador te avise si una clase de cliente se cuela en el lado común, en vez de descubrirlo en producción.

---

## Instrumenta el mod mientras lo escribes

Esto no es una buena práctica genérica: es lo que hace que el mod se pueda **probar** (ver `pruebas.md`).

```java
if (DEPURACION) LOG.info("[mimod] objetivo enganchado: {}", entidad.getName());
```

Con eso, `tools/mando-virtual.py` puede pulsar los botones y comprobar que esas líneas salieron, en orden. Sin eso, la única forma de saber si el mod reacciona es mirarlo con los ojos.

Deja la depuración detrás de una opción de config, apagada por defecto. Un mod que escribe en el log en cada tick es un mod que llena el disco de otra persona.
