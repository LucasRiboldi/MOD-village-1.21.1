#!/usr/bin/env bash
#
# preflight.sh — verificação de ambiente antes de qualquer tarefa.
#
# Roda em segundos. Falha cedo se algo estiver errado, para não gastar
# uma sessão de jogo com o ambiente quebrado.
#
# Uso:  scripts/preflight.sh
#
# Códigos de saída:
#   0  tudo ok
#   1  build ou testes falharam
#   2  jar desatualizado
#   3  ambiente incompatível
#

set -uo pipefail

# ---------------------------------------------------------------- cores
if [ -t 1 ]; then
    R='\033[0;31m'; G='\033[0;32m'; Y='\033[0;33m'; B='\033[0;34m'
    N='\033[0m'; BOLD='\033[1m'
else
    R=''; G=''; Y=''; B=''; N=''; BOLD=''
fi

ok()   { printf "  ${G}✓${N} %s\n" "$1"; }
warn() { printf "  ${Y}!${N} %s\n" "$1"; }
fail() { printf "  ${R}✗${N} %s\n" "$1"; }
info() { printf "  ${B}·${N} %s\n" "$1"; }
hdr()  { printf "\n${BOLD}%s${N}\n" "$1"; }

# ------------------------------------------------------------ variáveis
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

JAR_NAME="village-colony-0.3.0.jar"
JAR_BUILT="build/libs/$JAR_NAME"
JAR_DOWNLOADS="downloads/$JAR_NAME"
GRADLE_PROPS="gradle.properties"

EXIT=0

# --------------------------------------------------------------- cabeçalho
printf "${BOLD}Village Colony — preflight${N}\n"
printf "  root: %s\n" "$ROOT"

# ============================================================
# 1. Java 21
# ============================================================
hdr "1. Java 21"

if ! command -v java >/dev/null 2>&1; then
    fail "java não encontrado no PATH"
    EXIT=3
else
    JAVA_VER="$(java -version 2>&1 | head -n1 | sed -E 's/.*"([0-9]+).*/\1/')"
    if [ "${JAVA_VER:-0}" -ge 21 ] 2>/dev/null; then
        ok "java $JAVA_VER"
    else
        warn "java $JAVA_VER encontrado, mas o projeto exige 21"
        info "use JAVA_HOME=\$HOME/.jdks/jdk-21* (o PATH da máquina pode ter Java 8)"
    fi
fi

# ============================================================
# 2. Versões fixadas
# ============================================================
hdr "2. Versões fixadas (gradle.properties)"

if [ ! -f "$GRADLE_PROPS" ]; then
    fail "$GRADLE_PROPS não encontrado"
    EXIT=3
else
    MC=$(grep -E '^minecraft_version=' "$GRADLE_PROPS" | cut -d= -f2-)
    YARN=$(grep -E '^yarn_mappings=' "$GRADLE_PROPS" | cut -d= -f2-)
    LOADER=$(grep -E '^loader_version=' "$GRADLE_PROPS" | cut -d= -f2-)
    FAPI=$(grep -E '^fabric_version=' "$GRADLE_PROPS" | cut -d= -f2-)

    [ "$MC" = "1.21.1" ]     && ok "minecraft   $MC"     || { fail "minecraft   $MC (esperado 1.21.1)"; EXIT=3; }
    [ "$YARN" = "1.21.1+build.3" ] && ok "yarn        $YARN" || warn "yarn        $YARN"
    [ "$LOADER" = "0.19.3" ] && ok "loader      $LOADER" || warn "loader      $LOADER"
    ok "fabric-api  $FAPI"

    # Regra do projeto: nada de latest, +, ou dinâmico
    if grep -qE '(latest|SNAPSHOT|\+)' "$GRADLE_PROPS" 2>/dev/null; then
        # "+" é legítimo em 1.21.1+build.3
        if grep -vE '^\s*#|yarn_mappings' "$GRADLE_PROPS" | grep -qE '(latest|SNAPSHOT)'; then
            fail "gradle.properties tem 'latest' ou 'SNAPSHOT'"
            EXIT=3
        fi
    fi
fi

# ============================================================
# 3. Git status
# ============================================================
hdr "3. Git status"

if ! command -v git >/dev/null 2>&1; then
    warn "git não encontrado"
elif [ ! -d .git ]; then
    warn "não é um repositório git"
else
    BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
    DIRTY="$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')"
    if [ "$DIRTY" = "0" ]; then
        ok "limpo — branch $BRANCH"
    else
        warn "$DIRTY arquivo(s) modificado(s) — branch $BRANCH"
        git status --porcelain | head -n 8 | sed 's/^/    /'
        [ "$DIRTY" -gt 8 ] && info "... e mais $((DIRTY - 8))"
    fi
fi

# ============================================================
# 4. Jar em downloads/ — a armadilha nº 1
# ============================================================
hdr "4. Jar em downloads/"

if [ ! -f "$JAR_DOWNLOADS" ]; then
    warn "downloads/$JAR_NAME não existe"
    info "regra do autor: todo 'commit push' leva o jar atualizado"
else
    if [ -f "$JAR_BUILT" ]; then
        H_BUILT="$(md5sum "$JAR_BUILT" | cut -d' ' -f1)"
        H_DOWN="$(md5sum "$JAR_DOWNLOADS" | cut -d' ' -f1)"
        if [ "$H_BUILT" = "$H_DOWN" ]; then
            ok "downloads/ bate com build/  (${H_BUILT:0:8})"
        else
            warn "downloads/ divergente do build"
            info "  build:     ${H_BUILT:0:8}"
            info "  downloads: ${H_DOWN:0:8}"
            info "corrija com: ./gradlew build && cp build/libs/$JAR_NAME downloads/"
            EXIT=2
        fi
    else
        info "build/ não existe ainda — rode ./gradlew build"
    fi
fi

# ============================================================
# 5. Jar em .minecraft/mods/ — a armadilha nº 2
# ============================================================
hdr "5. Jar em .minecraft/mods/"

if [ -n "${APPDATA:-}" ]; then
    MODS_DIR="$APPDATA/.minecraft/mods"
elif [ -d "$HOME/.minecraft/mods" ]; then
    MODS_DIR="$HOME/.minecraft/mods"
else
    MODS_DIR=""
fi

if [ -z "$MODS_DIR" ] || [ ! -d "$MODS_DIR" ]; then
    info "pasta mods/ não encontrada — pule se não estiver testando em jogo"
else
    INSTALLED="$MODS_DIR/$JAR_NAME"
    if [ ! -f "$INSTALLED" ]; then
        warn "jar não está instalado em $MODS_DIR"
        info "regra do autor: substituir sempre que salvar em downloads/"
    else
        H_DOWN="$(md5sum "$JAR_DOWNLOADS" 2>/dev/null | cut -d' ' -f1)"
        H_MODS="$(md5sum "$INSTALLED" | cut -d' ' -f1)"
        if [ "$H_DOWN" = "$H_MODS" ]; then
            ok "instalação de teste em dia  (${H_MODS:0:8})"
        else
            warn "jar de mods/ difere de downloads/"
            info "  downloads: ${H_DOWN:0:8}"
            info "  mods:      ${H_MODS:0:8}"
            info "copiar com o Minecraft FECHADO — copiar com o jogo aberto falha em silêncio"
            info "corrija com: cp downloads/$JAR_NAME \"$MODS_DIR/\""
        fi
    fi
fi

# ============================================================
# 6. Módulos Python (utilitários do projeto)
# ============================================================
hdr "6. Utilitários Python"

if ! command -v python >/dev/null 2>&1 && ! command -v python3 >/dev/null 2>&1; then
    warn "python não encontrado — scripts/gauntlet.py e graphify_relabel.py não rodam"
else
    PY=$(command -v python3 || command -v python)
    info "interpreter: $PY"
    [ -f tests/test_gauntlet.py ] && ok "tests/test_gauntlet.py presente"
    [ -f scripts/graphify_relabel.py ] && ok "scripts/graphify_relabel.py presente"
fi

# ============================================================
# 7. Documentos-chave referenciados pelo CLAUDE.md
# ============================================================
hdr "7. Documentos do projeto"

check_doc() {
    if [ -f "$1" ]; then
        ok "$1"
    else
        warn "$1 — referenciado pelo CLAUDE.md, ainda não existe"
    fi
}

check_doc "STATE.md"
check_doc "docs/PATTERNS.md"
check_doc "docs/RULES.md"
check_doc "docs/technical/Plano-de-Correcao.md"
check_doc "docs/proxima-sessao.md"
check_doc "PROJECT_CONSTITUTION.md"

# ============================================================
# 8. Bateria — só se --full
# ============================================================
if [ "${1:-}" = "--full" ]; then
    hdr "8. Bateria (build + gametest)"
    info "isso leva alguns minutos..."

    if JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-21.0.12+8}" ./gradlew build --rerun-tasks -q; then
        ok "build ok"
    else
        fail "build falhou"
        EXIT=1
    fi

    if JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-21.0.12+8}" ./gradlew runGametest --rerun-tasks -q; then
        ok "gametest ok"
    else
        fail "gametest falhou"
        EXIT=1
    fi
else
    hdr "8. Bateria"
    info "pulei build e gametest — rode com --full para conferir"
fi

# ============================================================
# Resumo
# ============================================================
printf "\n"
case "$EXIT" in
    0) printf "${G}${BOLD}Tudo pronto.${N}\n" ;;
    1) printf "${R}${BOLD}Bateria falhou — não comece código antes de consertar.${N}\n" ;;
    2) printf "${Y}${BOLD}Jar desatualizado — o autor vai testar código velho.${N}\n" ;;
    3) printf "${R}${BOLD}Ambiente incompatível — conserte antes de prosseguir.${N}\n" ;;
esac

printf "\nLembretes:\n"
printf "  • Leia STATE.md antes de qualquer tarefa.\n"
printf "  • Todo 'commit push' leva o jar atualizado em downloads/.\n"
printf "  • Copiar jar com Minecraft aberto NÃO testa nada.\n"
printf "  • /time set noon, não /time set day.\n\n"

exit "$EXIT"