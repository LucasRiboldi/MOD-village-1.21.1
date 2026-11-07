---
name: castigo-por-oficio-vira-rodizio
description: Castigo por item numa lista de N vira rodízio; e teste que mede a punição em repouso não vê o defeito, que mora no movimento
metadata:
  type: feedback
---

O rodízio de ofícios de 2026-09-19: um trabalhador largou **seis ofícios em
41 minutos**, e as obras paravam no meio porque o construtor da vez era o
mineiro de dois minutos atrás.

A linha de reserva **funcionava exatamente como escrita** — cada ofício
largado ficava de castigo. O defeito era a aritmética: o castigo é **por
ofício** e a colônia tem **sete**. Largar um é receber o seguinte da ordem
*na mesma passagem*, e o trabalhador atravessa a lista inteira antes de o
primeiro castigo vencer. Sempre sobra item virgem.

**Duas lições, e a segunda é a cara:**

1. **Castigo por item numa lista de N precisa de um freio global.** Punir o
   item sem limitar a *taxa* de troca só empurra o problema para o item
   seguinte. O conserto foi `BETWEEN_TRADES_CYCLES`: uma espera que vale
   para *qualquer* próximo item, não para o que falhou.

2. **Teste que mede a punição em repouso não vê o defeito.** O
   `ProfessionShunTest` tinha 14 testes verdes e cobria escada, janela,
   decaimento e piso — mas o trabalhador dele larga o ofício e **fica
   parado** enquanto os ciclos passam (`cyclesGoBy`). Em jogo ele é
   **recontratado no mesmo ciclo**. A cobertura media o estado estático e
   o defeito mora na transição. Ao testar cooldown/backoff, escreva um
   caso onde o sujeito **continua agindo** entre as punições.

Corolário do §1 do projeto: dois desfechos somados escondem o diagnóstico.
`SHUNNED` ("não quero ESTA vaga") e `BETWEEN_TRADES` ("não quero vaga
nenhuma agora") viram o mesmo nada no log se compartilharem contador.

Ver [[contagem-honesta-de-testes]], [[teste-vazio-por-condicao-impossivel]],
[[gametest-copia-do-predicado]].
