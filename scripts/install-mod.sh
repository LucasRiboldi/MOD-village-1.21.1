#!/usr/bin/env bash
# Instala o JAR buildado na pasta de mods do TLauncher e confere o hash.
#
# Motivo (15-10): por 3 versões os JARs enviados ao chat não foram copiados para
# a pasta de mods — os playtests rodaram uma versão antiga sem ninguém notar.
# Ver memória jars-nao-instalados-deslocamento.
#
# Uso:  scripts/install-mod.sh [versão]
#   sem argumento, usa o mod_version de gradle.properties.
#   desativa os outros .jar (vira .jar.disabled), copia o novo, confere o sha256
#   contra build/libs, e imprime a versão para o grep de confirmação no log.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODS="${MINECRAFT_MODS:-$APPDATA/.minecraft/mods}"
# No Git Bash do Windows, $APPDATA pode vir em formato Windows; normaliza.
MODS="$(cygpath -u "$MODS" 2>/dev/null || echo "$MODS")"

VERSION="${1:-$(grep -E '^mod_version=' "$ROOT/gradle.properties" | cut -d= -f2)}"
JAR="$ROOT/build/libs/village-colony-$VERSION.jar"

if [[ ! -f "$JAR" ]]; then
  echo "ERRO: $JAR não existe. Rode ./gradlew build primeiro." >&2
  exit 1
fi
if [[ ! -d "$MODS" ]]; then
  echo "ERRO: pasta de mods não encontrada: $MODS" >&2
  echo "      defina MINECRAFT_MODS se o caminho for outro." >&2
  exit 1
fi

# Desativa qualquer village-colony ativo (não mexe nos já .disabled).
for active in "$MODS"/village-colony-*.jar; do
  [[ -e "$active" ]] || continue
  mv "$active" "$active.disabled"
  echo "desativado: $(basename "$active")"
done

cp "$JAR" "$MODS/village-colony-$VERSION.jar"

want="$(sha256sum "$JAR" | awk '{print toupper($1)}')"
got="$(sha256sum "$MODS/village-colony-$VERSION.jar" | awk '{print toupper($1)}')"
if [[ "$want" != "$got" ]]; then
  echo "ERRO: hash do JAR instalado não bate com o build." >&2
  exit 1
fi

echo "instalado: village-colony-$VERSION.jar  ($got)"
echo "confirme no log do jogo:  grep -iE 'villagecolony $VERSION' latest.log"
