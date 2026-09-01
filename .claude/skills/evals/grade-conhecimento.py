import json, re, sys
from pathlib import Path

BASE = Path(sys.argv[1] if len(sys.argv) > 1 else ".") / "iteration-1"

def load(ev, cfg):
    p = BASE / ev / cfg / "outputs" / "resposta.md"
    return p.read_text(encoding="utf-8", errors="replace").lower() if p.exists() else None

def has(t, *pats):
    return any(re.search(p, t, re.I | re.S) for p in pats)

CHECKS = {
 "eval-0-walk-target": [
  ("usa WALK_TARGET",            lambda t: has(t, r"walk_target")),
  ("explica que o Brain sobrescreve", lambda t: has(t, r"brain.{0,80}(sobrescr|reescr|substitu)", r"(sobrescr|reescr).{0,80}brain", r"cérebro.{0,80}(sobrescr|reescr)")),
  ("diz que precisa MANTER a memoria", lambda t: has(t, r"(mant|repõe|repor|renov|a cada tick|keeprunning|shouldkeeprunning).{0,120}walk_target", r"walk_target.{0,120}(mant|repõe|repor|renov|keeprunning)")),
  ("NAO reduz a so ajustar velocidade", lambda t: not has(t, r"^\s*(basta|só|apenas).{0,40}(velocidade|speed)")),
 ],
 "eval-1-profession-trades": [
  ("SEPARA trades da profissao", lambda t: has(t, r"trade.{0,200}(separad|não faz parte|nao faz parte|fora d|outro sistema|não está|nao esta)", r"(separad|outro sistema).{0,200}trade", r"tradeoffer")),
  ("POI antes da profissao",      lambda t: has(t, r"poi.{0,300}(antes|primeiro)", r"(antes|primeiro).{0,200}poi", r"pointofinterest.{0,400}villagerprofession")),
  ("record VillagerProfession 1.21.1", lambda t: has(t, r"heldworkstation", r"acquirableworkstation")),
  ("comportamento vem de TASK",   lambda t: has(t, r"task.{0,200}(brain|comportamento)", r"(multiticktask|settasklist)")),
  ("menciona resources/registro", lambda t: has(t, r"lang|workSound|textura|incondicional")),
 ],
 "eval-2-poi-blockstates": [
  ("causa = block states faltando", lambda t: has(t, r"block ?state.{0,200}(todos|falta|apenas|só|um)", r"(todos|falta).{0,200}block ?state")),
  ("recomenda TODOS os states",     lambda t: has(t, r"getstatemanager\(\)\.getstates|todos os (block ?)?states|todos os estados")),
  ("explica o mecanismo",           lambda t: has(t, r"(acesa|acende|true).{0,300}(deixa de|não .{0,20}poi|nao .{0,20}poi|perde)", r"perde.{0,200}(acesa|acende)")),
 ],
 "eval-3-simple-item": [
  ("registro correto",       lambda t: has(t, r"maxdamage\(\s*180") and has(t, r"maxcount\(\s*1")),
  ("menciona resources",     lambda t: has(t, r"lang") and has(t, r"model|modelo") and has(t, r"textur")),
  ("SEM over-engineering",   lambda t: not has(t, r"itemfactory|itemmanager|itemservice|abstractitem\w*factory")),
  ("item group ou receita",  lambda t: has(t, r"itemgroup|item group|grupo.{0,20}criativ|recipe|receita")),
 ],
 # eval-4/eval-5: nomes de método/classe abaixo foram confirmados por javap
 # contra o jar merged 1.21.1+build.3 em 2026-09-01 (ver AVALIACAO.md). Os
 # dois checks "NAO cita ... inventado" existem porque o Haiku 4.5 inventou
 # exatamente esses nomes nessa data — sem generalizar demais, mas vale
 # manter como sentinela de regressão conhecida, não como lista exaustiva.
 "eval-4-mixin-vs-escada": [
  ("cita metodo vanilla real (nao inventado)", lambda t: has(t, r"checkdespawn|canimmediatelydespawn|cannotdespawn|ispersistent\(\)|setpersistent\(\)")),
  ("NAO cita metodo inventado conhecido (Haiku, 2026-09-01)", lambda t: not has(t, r"\bcandespawn\s*\(\)|\bshoulddespawn\s*\(\)")),
  ("acha a solucao sem Mixin (recomenda setPersistent)", lambda t: has(t, r"setpersistent")),
  ("aponta risco de colisao de Mixin com outro mod", lambda t: has(t, r"(colis|conflit|concorr).{0,150}mixin", r"mixin.{0,150}(colis|conflit|outro mod)")),
 ],
 "eval-5-perf-antes-de-otimizar": [
  ("recomenda profiler de sampling (spark)", lambda t: has(t, r"\bspark\b")),
  ("exige teste causal antes de mudar frequencia", lambda t: has(t, r"(a/b|stub|no-?op|desativ|comenta|stubad).{0,150}(sensor|frequ)", r"(sensor|frequ).{0,150}(a/b|stub|no-?op)")),
  ("cita metodo POI real (nao inventado)", lambda t: has(t, r"getnearestposition|getinsquare|getincircle|pointofintereststorage")),
  ("NAO cita metodo POI inventado conhecido (Haiku, 2026-09-01)", lambda t: not has(t, r"findclosestpoiposition|findclosestpoitypes")),
 ],
}

results = {}
for ev, checks in CHECKS.items():
    results[ev] = {}
    for cfg in ("with_skill", "without_skill"):
        t = load(ev, cfg)
        if t is None:
            results[ev][cfg] = None
            continue
        results[ev][cfg] = {"len": len(t), "checks": [(n, bool(f(t))) for n, f in checks]}

print(f"{'EVAL':<26} {'CHECK':<38} {'COM SKILL':<11} {'SEM SKILL'}")
print("-" * 88)
tot_w = tot_o = tot_n = 0
for ev, r in results.items():
    w, o = r["with_skill"], r["without_skill"]
    if w is None or o is None:
        print(f"{ev:<26} {'(pendente)':<38}")
        continue
    for (n, wv), (_, ov) in zip(w["checks"], o["checks"]):
        tot_n += 1; tot_w += wv; tot_o += ov
        mark = "  <<<" if wv and not ov else ("  !!!" if ov and not wv else "")
        print(f"{ev:<26} {n:<38} {'PASS' if wv else 'FALHA':<11} {'PASS' if ov else 'FALHA'}{mark}")
    print(f"{'':<26} {'(tamanho da resposta, chars)':<38} {w['len']:<11} {o['len']}")
    print()
if tot_n:
    print("-" * 88)
    print(f"TOTAL  com skill: {tot_w}/{tot_n}   sem skill: {tot_o}/{tot_n}")
    print("<<< = a skill fez diferenca    !!! = a skill PIOROU (regressao)")

# sentinela: pasta de eval sem critérios em CHECKS fica invisível no loop
# acima (silenciosamente, sem nem aparecer como "(pendente)") — avisa em vez
# de deixar passar. Aconteceu uma vez (eval-4/eval-5 rodaram sem critério
# antes desta linha existir).
if BASE.exists():
    on_disk = {p.name for p in BASE.iterdir() if p.is_dir() and p.name.startswith("eval-")}
    sem_criterio = sorted(on_disk - CHECKS.keys())
    if sem_criterio:
        print()
        print(f"AVISO: {len(sem_criterio)} pasta(s) em '{BASE.name}' sem critério em CHECKS — não apareceram acima: " + ", ".join(sem_criterio))
