# El carril Fabric 1.20.1 — receta verificada

Esto no es documentación general de Fabric: es **la combinación concreta que se sabe que funciona**, probada de punta a punta en un pack de 148 mods con Fabric Loader 0.19.3. Cada número está aquí porque cambiarlo costó un build fallido.

Si tu pack es otra versión, `tools/andamio.py` ajusta lo que sabe ajustar y **avisa** cuando la versión no está en su tabla, en vez de inventar.

---

## Las tres cosas que hay que saber antes de nada

### 1. El JDK que ejecuta Gradle no es el que compila el mod

Fabric Loom necesita un **JDK 21** para *correr Gradle*, aunque el mod salga compilado para **Java 17**. Tener instalado solo el JDK 17 no vale, y el error que da no lo explica.

```bash
JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" \
  sh ./gradlew build --no-daemon
```

`--no-daemon` no es opcional: el demonio de Gradle se queda con el `JAVA_HOME` de la primera invocación y luego "compila igual" con el JDK equivocado sin decir nada.

`tools/construir.py` hace esto solo: busca el JDK, lo pasa por el entorno y no toca el del sistema.

### 2. `loader_version` va clavado al del pack, o el mod no carga

La plantilla oficial de Fabric genera un proyecto que pide un loader más nuevo del que corre el pack. El resultado: **el mod no carga y el juego arranca sin decir nada**. No hay crash, no hay línea roja, el mod simplemente no está.

El dato bueno está en el log del propio juego:

```
[main/INFO]: Loading Minecraft 1.20.1 with Fabric Loader 0.19.3
```

`tools/radiografia.py` lo lee de ahí. Si el pack nunca se ha arrancado, no hay log: hay que arrancarlo una vez.

Ojo a la distinción, porque son dos cosas distintas:

- **`loader_version` en `gradle.properties`** → contra qué compilas. Debe ser **el del pack**.
- **`depends.fabricloader` en `fabric.mod.json`** → el mínimo que exiges en tiempo de ejecución. Debe ser un **suelo bajo** (`>=0.15.0`), nunca por encima del que corre el pack.

### 3. Mappings oficiales de Mojang, no yarn

Este carril usa `loom.officialMojangMappings()`. Casi toda la documentación de Fabric que hay por ahí usa `yarn`, y **los nombres de clases y métodos son distintos**. Copiar un ejemplo de yarn a un proyecto con mappings de Mojang da `cannot find symbol` en cada línea.

---

## `gradle.properties`

```properties
# Memoria para gradle
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
org.gradle.configuration-cache=false

# Fabric
# OJO: loader_version debe ser <= al que usa el pack.
# Y Loom necesita JDK 21 para EJECUTAR gradle, aunque el mod
# se compile para Java 17.
minecraft_version=1.20.1
loader_version=0.19.3
loom_version=1.17-SNAPSHOT

# Mod
version=0.1.0
group=net.midominio

# Dependencias
fabric_api_version=0.92.12+1.20.1
```

## `settings.gradle`

```groovy
pluginManagement {
	repositories {
		maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
		mavenCentral()
		gradlePluginPortal()
	}
}

rootProject.name = 'mimod'
```

## `build.gradle`

El plugin es `net.fabricmc.fabric-loom-remap` (no `fabric-loom` a secas) para este `loom_version`.

```groovy
plugins {
	id 'net.fabricmc.fabric-loom-remap' version "${loom_version}"
}

repositories {
	maven {
		name = 'Modrinth'
		url = uri('https://api.modrinth.com/maven')
		content { includeGroup 'maven.modrinth' }
	}
	// Solo si te integras con mods que viven en estos:
	maven { name = 'Shedaniel';      url = uri('https://maven.shedaniel.me/') }
	maven { name = 'TerraformersMC'; url = uri('https://maven.terraformersmc.com/releases/') }
}

// SOLO para mods de cliente: separa el codigo por entorno, de modo que
// una clase de cliente no pueda colarse en el servidor por accidente.
loom {
	splitEnvironmentSourceSets()

	mods {
		"mimod" {
			sourceSet sourceSets.main
			sourceSet sourceSets.client
		}
	}
}

dependencies {
	minecraft "com.mojang:minecraft:${project.minecraft_version}"
	mappings loom.officialMojangMappings()
	modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
	modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"

	// Solo para compilar contra sus APIs. NO se empaquetan ni se exigen en
	// runtime: los aporta el pack. Las coordenadas salen de radiografia.py.
	modCompileOnly "maven.modrinth:shoulder-surfing-reloaded:ovGsejgS"
	modCompileOnly "maven.modrinth:paragliders:6qmc3Nyl"
	modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:11.1.136") {
		exclude group: "net.fabricmc.fabric-api"
	}
	modCompileOnly "com.terraformersmc:modmenu:7.2.2"
}

// Sin esto, ${version} llega literal al jar y el mod carga con una version rota.
processResources {
	def version = project.version
	inputs.property "version", version
	filesMatching("fabric.mod.json") { expand "version": version }
}

tasks.withType(JavaCompile).configureEach { it.options.release = 17 }

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}
```

**`modCompileOnly` y no `modImplementation`** para los mods del pack: compilas contra su API pero no los empaquetas ni los exiges. Si usas `implementation` o `include`, acabas metiendo una copia del mod dentro del tuyo y el pack termina con dos.

El `exclude group: "net.fabricmc.fabric-api"` de cloth-config no es decorativo: sin él arrastra una versión de la API distinta de la del pack.

---

## `fabric.mod.json`

```json
{
	"schemaVersion": 1,
	"id": "mimod",
	"version": "${version}",
	"name": "Mi Mod",
	"description": "Una frase que diga qué hace, no cómo se llama.",
	"authors": ["Tu nombre"],
	"license": "MIT",
	"environment": "client",
	"entrypoints": {
		"client": ["net.midominio.mimod.MiModClient"],
		"modmenu": ["net.midominio.mimod.config.ModMenuIntegration"]
	},
	"mixins": [
		{ "config": "mimod.client.mixins.json", "environment": "client" }
	],
	"depends": {
		"fabricloader": ">=0.15.0",
		"minecraft": "~1.20.1",
		"java": ">=17",
		"fabric-api": "*"
	},
	"recommends": {
		"combatroll": "*"
	}
}
```

- `environment`: `"client"` para cámara, HUD, entrada y sonido; `"*"` para lógica de juego. Un mod de cliente en el servidor rompe el arranque.
- `depends` es **duro**: si falta uno, no arranca el juego entero. Lo que tu mod pueda esquivar va en `recommends` y se comprueba con `FabricLoader.getInstance().isModLoaded("id")`.

## `mimod.client.mixins.json`

```json
{
	"required": true,
	"minVersion": "0.8",
	"package": "net.midominio.mimod.mixin",
	"compatibilityLevel": "JAVA_17",
	"client": ["EntityTurnMixin", "GuiCrosshairMixin"],
	"injectors": { "defaultRequire": 1 }
}
```

**Cada mixin hay que listarlo aquí por nombre de clase.** Uno que no se liste no se aplica, y no da ningún error: el mod carga y no hace nada. `tools/probar.py` (Nivel 1) detecta tanto los que se listan y no existen como los que existen y nadie lista.

La lista se llama `"client"` en un mixin config de cliente, `"mixins"` en uno común y `"server"` en uno de servidor. Poner las clases en la lista equivocada tiene el mismo efecto que no ponerlas.

---

## Estructura de carpetas con `splitEnvironmentSourceSets()`

```
src/
  main/
    java/net/midominio/mimod/          <- codigo comun (si lo hay)
    resources/
      fabric.mod.json                  <- SIEMPRE en main, aunque el mod sea de cliente
  client/
    java/net/midominio/mimod/
      MiModClient.java                 <- entrypoint de cliente
      mixin/
    resources/
      mimod.client.mixins.json
      assets/mimod/lang/en_us.json
      <plugin_de_otro_mod>.json        <- en la RAIZ de resources = raiz del jar
```

`fabric.mod.json` va en `src/main/resources` aunque el mod sea íntegramente de cliente. Es el único fichero que no se mueve.

---

## Comprobación rápida antes de dar nada por bueno

```bash
python tools/construir.py "<PROYECTO>" --instalar-en "<PACK>"
python tools/probar.py    "<PROYECTO>" --pack "<PACK>"
```

Si `probar.py` sale con código distinto de cero, el mod no se entrega.
