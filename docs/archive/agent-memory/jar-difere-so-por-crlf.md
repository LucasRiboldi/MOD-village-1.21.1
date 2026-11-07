---
name: jar-difere-so-por-crlf
description: md5 do jar recém-buildado difere do de downloads/ sem que nenhuma classe mude — são três arquivos de texto com CRLF vs LF
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-11T01:47:01.037Z
---

Rodar `./gradlew build` e comparar o `build/libs/village-colony-0.3.0.jar` com
o de `downloads/` pode dar **md5 diferente com o mesmo código-fonte**. Medido em
2026-09-10: `53ab4a82` contra `54138d70`, no commit `a1ce82c`, sem uma única
classe divergente.

A diferença são **três arquivos de texto** que o Git normaliza para CRLF na
árvore de trabalho e que entram no jar como estão:

- `LICENSE_village-colony`
- `data/villagecolony/catalog/vanilla_structures.json`
- `data/villagecolony/structure/houses/README.md`

**Como distinguir de um jar realmente velho**, que é o caso que importa
(ver [[minecraft-test-install]]):

```
mkdir -p a b && (cd a && unzip -oq <jar1>) && (cd b && unzip -oq <jar2>) && diff -rq a b
```

Só arquivos de texto na lista → é quebra de linha, o jar de `downloads/` já está
em dia e **não deve ser substituído** (o commit ficaria com um jar binário
alterado sem mudança de comportamento). Qualquer `.class` na lista → o jar está
mesmo defasado e a cópia é necessária.

Conferência direta de que um conserto entrou no jar instalado:

```
unzip -l "$APPDATA/.minecraft/mods/village-colony-0.3.0.jar" | grep -iE 'MineLease|MineMarks'
```

**Por que isso vale saber:** a regra do projeto manda comparar md5 entre
`downloads/` e a pasta `mods`, e md5 diferente normalmente significa jar velho —
o caso que já custou uma sessão inteira de diagnóstico. Este é o falso positivo
dessa regra, e sem abrir os dois zips ele parece idêntico ao problema real.
