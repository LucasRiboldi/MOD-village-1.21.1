---
name: assinatura-numerica-casa-no-meio
description: "Assinatura de log que comeca com digito casa dentro de outro numero; \"0 survived\" casou em \"30 survived\" e refutou tres itens de uma vez"
metadata: 
  node_type: memory
  type: feedback
  modified: 2026-09-19T05:50:49.674Z
---

Assinatura de log que **começa com dígito** e é casada com `str.count`
casa no meio de outro número. Em 2026-09-19 a refutação
`"0 survived every check"` casou dentro de `"30 survived every check"` — a
linha de **sucesso** da sessão — e derrubou **três** itens ao mesmo tempo
(P0.7, P1.0, P1.3 dividem essa assinatura). O veredito honesto era 0
refutados.

**Why:** o número errado era plausível, então não parecia defeito de
ferramenta — parecia três regressões no mod. Quase investiguei todas as
três. E não era só o 30: qualquer contagem terminada em zero (10, 20, 100)
dispararia para sempre.

**How to apply:** quando vários itens independentes viram REFUTADO na mesma
passagem, **suspeite da ferramenta antes do código** — especialmente se cada
um mostra exatamente `contra 1`, que é a marca de uma assinatura
compartilhada. Confira a assinatura contra a linha real com
`grep -o` antes de acreditar no veredito. Em assinatura numérica, ancore o
início do número (`(?<![0-9])`).

Na correção, teste **as duas metades**: que o falso positivo sumiu e que a
refutação de verdade **ainda dispara**. Só a primeira passa com "nunca casar
nada", que cega a ferramenta em vez de afiná-la — o mesmo erro de
[[teste-vazio-por-condicao-impossivel]]. Ver também
[[frase-ausente-refuta-mecanismo]] e [[log-decide-a-constante]].
