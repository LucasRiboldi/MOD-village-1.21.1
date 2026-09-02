import re, subprocess, sys
from pathlib import Path
S = Path(sys.argv[1])

def diff(repo):
    try:
        return subprocess.run(["git","-C",str(S/repo),"diff","HEAD"],capture_output=True,text=True,errors="replace").stdout
    except Exception: return ""
def untracked(repo):
    try:
        out = subprocess.run(["git","-C",str(S/repo),"ls-files","-o","--exclude-standard"],capture_output=True,text=True).stdout
        t=""
        for f in out.split():
            p=S/repo/f
            if p.suffix==".java" and p.exists(): t+=p.read_text(errors="replace")
        return t
    except Exception: return ""
def report(cfg):
    p = S/"iteration-2"/cfg/"RELATORIO.md"
    return p.read_text(errors="replace") if p.exists() else ""

def has(t,*p): return any(re.search(x,t,re.I|re.M) for x in p)

CHECKS = [
 ("CODIGO: nao usa static Map p/ estado persistido",
  lambda c,r: not has(c, r"^\+.*static\s+(final\s+)?(java\.util\.)?(Map|HashMap|ConcurrentHashMap)<")),
 ("CODIGO: o estado persistido alcanca markDirty",
  lambda c,r: has(c, r"^\+.*markDirty\s*\(") or has(c, r"ColonySavedData")),
 ("CODIGO: integra ao ColonySavedData existente",
  lambda c,r: has(c, r"ColonySavedData")),
 ("CODIGO: le NBT com default seguro p/ save antigo",
  lambda c,r: has(c, r"^\+.*(contains\(|getInt\(|orElse|getCompound\()")),
 ("CODIGO: usa o SERVER_STOPPING existente",
  lambda c,r: has(c, r"SERVER_STOPPING|onServerStopping|ServerLifecycleHandler")),
 ("PROCESSO: compilou e relatou o resultado",
  lambda c,r: has(r, r"gradlew|BUILD SUCCESS|BUILD FAILED|compil")),
 ("PROCESSO: declara o que NAO verificou",
  lambda c,r: has(r, r"n[ãa]o\W{0,4}\s*(foi\s+)?verific", r"n[ãa]o\W{0,4}\s*(testei|rodei|consegui)", r"fica pendente", r"limite conhecido")),
 ("PROCESSO: separa verificado de nao-verificado",
  lambda c,r: has(r, r"rodou de verdade", r"verificado rodando", r"o que\W{0,6}\s*n[ãa]o\W{0,4}\s*foi", r"pend[êe]nc", r"limitaç", r"limite conhecido")),
 ("PROCESSO: menciona teste de save/load",
  lambda c,r: has(r, r"fechar e reabrir|save.?load|reabrir o mundo|persist.{0,40}test|testar.{0,30}persist")),
]

rows=[]
for cfg,repo in (("with_skill","run-with"),("without_skill","run-without")):
    c = diff(repo)+untracked(repo); r = report(cfg)
    rows.append((cfg, c, r, [(n,bool(f(c,r))) for n,f in CHECKS]))

print(f"{'CHECK':<50}{'COM SKILL':<12}{'SEM SKILL'}")
print("-"*80)
w,o=rows[0],rows[1]
tw=to=0
for (n,wv),(_,ov) in zip(w[3],o[3]):
    tw+=wv; to+=ov
    mark="  <<<" if wv and not ov else ("  !!!" if ov and not wv else "")
    print(f"{n:<50}{'PASS' if wv else 'FALHA':<12}{'PASS' if ov else 'FALHA'}{mark}")
print("-"*80)
print(f"{'TOTAL':<50}{str(tw)+'/'+str(len(CHECKS)):<12}{str(to)+'/'+str(len(CHECKS))}")
print()
for cfg,c,r,_ in rows:
    print(f"{cfg}: diff+novos={len(c)} chars | relatorio={len(r)} chars")
