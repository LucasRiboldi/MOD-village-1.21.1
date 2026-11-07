---
name: e52-curral-prende-aldeoes
description: "Aldeões presos em curral; 1º conserto (48d77ac) falhou em jogo às 09:19 de 01-10; PenEscape (gate/pulo, rota própria) substitui; armadilhas do poste fino e do chunk sem tique"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-01T13:20:54.975Z
---

Playtest 2026-10-01 04:54–08:46: 8 trabalhadores presos num curral Vanilla
(-253..-246, 71, 421..428, portão -246,71,426); entram pulando do feno.

**1º conserto 48d77ac falhou** (sessão 09:16, JAR 5E7A3A05): abriu o portão e
1 s depois "is out after 0 steps" — a 2ª medição passava PELO portão aberto.
E mineiro 3400e902 + construtores 5bbbca81/676ac9fd nem foram marcados: quem
desiste da tarefa ou anda na cerca não "congela duas vezes".

**2º conserto (PenEscape, 01-10):** varre todo trabalhador a ≤6 de cerca a cada
40 tiques; "fora" = sair do reach medido com portão FECHADO; anda por rota
própria (BFS do FencedIn) via MoveControl, não WALK_TARGET; portão primeiro,
depois pula (vy 0,65, só avança depois de subir 1,55); WalkOverride pausa a
GoToWorkTargetTask.

**Why / armadilhas medidas:**
- WALK_TARGET para bloco a 2 passos no curral: Vanilla dava cantReach e o
  aldeão ficava parado 400 tiques (intermitente) → rota própria.
- Poste de cerca/portão fechado tem 1/4 de bloco: encostado por dentro, o
  centro já está no bloco da cerca → "fora" falso. Só a mutação (portão que não
  abre) revelou; isOut agora recusa qualquer bloco de cerca/muro/portão.
- Faixa entre cerca e barranco também "não chega a 12": saída só conta se o
  lado de lá é mais largo (senão pularia PARA DENTRO do curral).
- GameTest: pulo cai em chunk vizinho sem tique de entidade e congela no ar →
  setChunkForced no cenário. Log do mod some no meio da bateria: o teste grava
  o próprio rastro na mensagem de falha.

**How to apply:** commit d892934 (sem push); JAR md5 21d7af35 instalado em mods 01-10 ~10h30 (anterior 74cd0281 no scratchpad). No próximo log, `is fenced in at` seguido de `is out of the
pen`; contar `time this session` > 1 (aldeão voltando ao curral = falta
prevenção, ex. feno). Ver [[sessao-de-jogo-destrava-o-plano]],
[[teste-vazio-por-condicao-impossivel]], [[cenario-longe-da-arena-e-sorteado]].
