# Decisões em aberto — a resposta mais simples para cada uma

**Data:** 2026-09-25 · **Pedido do autor:** "buscar alternativas para responder
as questões, de modo mais simples e econômico, deixar o jogo fluido sem travas".

**Régua usada:** para cada decisão, a menor mudança que tira a trava do jogo.
Refatoração grande só entra quando a pequena não resolve. "Custo" é o esforço
estimado; "fluidez" é o efeito em jogo.

---

## 1. Travas achadas na varredura — antes da lista

A varredura procurou esperas sem prazo, porque é isso que o jogador vê como
"a vila parou".

| # | Trava | Evidência | Resposta simples | Custo |
|---|---|---|---|---|
| T1 | **Obra com todas as peças restantes adiadas nunca fecha nem é abandonada** | `[FATO]` `WaitingWork.giveUpIfStalled` (linha 279): sem peça colocável, zera os relógios e devolve `false` para sempre; a ADR-023 diz "pode aguardar indefinidamente". `[INFERÊNCIA]` com a vaga única de obra, a vila para de construir. | **Entregar a obra como está**: sem peça colocável e só com adiadas, registrar a `Building` e fechar o projeto. As peças adiadas continuam gravadas pela assinatura, e o reparo cíclico as retoma se o apoio aparecer. | baixo (um ramo + GameTest) |
| T2 | **Encalhado sem saída fica fora da escala para sempre** | `[FATO]` `StrandedEscape` marca `HOPELESS` e o log diz "stays out of the work queue until someone frees it"; o desvio da fase 2 tenta 3 vezes e depois cai no mesmo estado. | **Último recurso com prazo**: depois de N minutos de expediente sem sair, levar o aldeão ao baú dele (teleporte curto, com partícula e som). O autor preferiu o natural em 09-24; isto só roda depois de o natural falhar. | baixo — **pede o autor** |
| T3 | Mina esgotada sem boca do lado oposto "fica aguardando" (ADR-013 §5) | `[FATO]` superado em 09-16: voltas sem pedra giram a espiral, abrem boca nova e, sem boca, raspam pedra exposta. | Nada a fazer; **atualizar a ADR-013** com a nota de que o §5 foi superado. | nulo |

**Regra geral sugerida (uma linha na RULES):** *toda espera tem prazo e plano
B*. Espera sem prazo é trava, mesmo quando o motivo é bom.

---

## 2. As decisões da lista, com a alternativa simples

### 🔴 Crítico

| # | Pergunta | Resposta cara (a que estava no plano) | Resposta simples recomendada | Custo | Fluidez |
|---|---|---|---|---|---|
| 1 | Ciclo de tarefa comum aos 7 ofícios (item 9) | ADR + refatorar os 7 `Job`/`giveUp` num ciclo único | **Não refatorar.** Reusar só o que destrava: o `DetourWalker` já é genérico (objetivo = "alcanço o alvo"). Ligar o mesmo `tryDetour` no guarda de travamento do construtor, lenhador, fazendeiro e pastor — os 7 ofícios já usam `WorkStall`. | médio-baixo | **alta**: ninguém mais desiste por um buraco ou um bloco no caminho |
| 2 | Regras do `fabric` para o `core` (item 10) | ADR + mover as regras | **Oportunista**: toda regra que der defeito vai para o `core` como função pura na correção dela. Sem projeto dedicado. | nulo | nenhuma direta |
| 3 | 89 campos estáticos (R2) | contexto por servidor, mexe em meio projeto | **Aceitar o `ServerMemory` como a solução**: a limpeza entre mundos já é garantida e testada. O que o contexto daria a mais (dois mundos no mesmo processo ao mesmo tempo) não existe no jogo. Registrar como decisão e fechar o C05 por ela. | nulo | nenhuma |
| 4 | Material de planta sem tipo de recurso | produzir tudo ou montar cadeias novas | **Trocar ou pular na hora de planejar**: se a peça não tem rota (nem na receita, nem no bioma), trocar pelo equivalente da paleta; sem equivalente e decorativa, pular a peça. Nunca abrir obra que depende de bloco inalcançável. A ADR-022 já cobre a troca; falta a regra de pular. | baixo | alta (a obra de 10 min parada por `white_terracotta`) |
| 5 | E38: baú assoreia com vara, maçã, muda | cada item com consumidor próprio | **Teto por item no baú pessoal**: guardar até 16 mudas (replantio) e 16 maçãs (comida); vara e o excedente não entram. Uma checagem no depósito. | baixo | média (baú cheio para o trabalho) |
| 6 | TASK-048: o que a colônia abandonada deixa de fazer | ciclo de vida completo | **Só não pode ser vila foco.** O ciclo já só roda com jogador a até 128 blocos e só a vila foco planeja; tirar a abandonada da escolha do foco basta. | mínimo (um filtro) | média (o foco não fica preso numa vila morta) |

### 🟠 Importante

| # | Pergunta | Resposta simples recomendada | Custo |
|---|---|---|---|
| 7 | Publicar o `.jar` com as fases 1 e 2 | **Sim, agora** — é o que destrava a sessão de jogo que vai decidir o resto. | nulo |
| 8 | Merge do PR #3 | Depois da sessão de jogo, se ela não achar regressão. | nulo — **pede o autor** |
| 9 | Fusão de vilas (ADR-007, nunca implementada) | **Arquivar.** Sem fusão, duas vilas próximas são duas colônias; não trava nada. Reabrir só se o jogo mostrar problema. | nulo |
| 10 | Orientação dos blocos (ADR-008) | Provavelmente já resolvido (peças de parede e escadas orientadas desde 09). **Confirmar e fechar.** | nulo |
| 11 | Versões das dependências | **Manter as faixas** no `fabric.mod.json`: é a prática dos mods Fabric, e fixar exato quebra o mod a cada atualização do loader. A regra de "versões fixas" vale para o build (`gradle.properties`), não para o que o jogador instala. | nulo |
| 12 | 7 documentos que descrevem classes inexistentes | **Mover para `docs/historical/`** com uma linha dizendo o que substituiu cada um. | mínimo |
| 13 | Ligar os hooks | Só o autor pode. | — |

### 🟡 Melhoria

| # | Pergunta | Resposta simples recomendada | Custo |
|---|---|---|---|
| 14 | Areia de praia colada na água | **Manter a regra.** A areia a um bloco da margem está seca, e a margem continua de pé segurando a água — que é justamente o que evita o alagamento. | nulo |
| 15 | Fase 3: registro de veios | **Sem registro persistido.** Tabela fixa de valor (diamante > esmeralda > ouro > lápis > redstone > ferro > cobre > carvão) e, entre os minérios que a parede do túnel mostra agora, o mais valioso primeiro. Sem estado novo no save. | baixo |
| 16 | Oficina para lenhador, mineiro, construtor | **Não fazer agora.** Não trava nada. | — |
| 17 | Vila foco no save | **Não fazer.** Refaz-se em poucos ciclos. | — |

---

## 3. Ordem sugerida, pelo que mais destrava por menos

1. **T1** — obra com peças adiadas fecha como está.
2. **#7** — publicar o `.jar` e jogar.
3. **#6** — abandonada fora do foco.
4. **#5** — teto por item no baú pessoal.
5. **#4** — pular peça decorativa sem rota.
6. **#1** — desvio nos outros ofícios.
7. **T2** — só se o autor aceitar o teleporte como último recurso.
8. Fechar por decisão, sem código: #2, #3, #9, #10, #11, #14, #16, #17; e
   mover os documentos (#12).

## 4. O que ainda não foi verificado

- T1: o efeito "trava a vila" depende da vaga única de obra, dada pela
  memória do projeto; não reli o planejador nesta varredura.
- #10: não conferi a ADR-008 contra o código, só o histórico das peças de
  parede.
- #4: não tracei o caminho exato de uma peça sem `ResourceType` hoje; a
  recomendação vale para os dois casos (espera ou troca).
