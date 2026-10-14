# Varredura de engenharia — Village Colony (2026-10-14)

Análise profissional read-only do projeto: código, arquitetura, processo e produto. Baseada em
medição (PMD, contagem de linhas/classes, git, Spark, ledgers) e nos defeitos recorrentes
registrados nas memórias e no STATE — não em impressão. Cada item traz a **evidência**, o **porquê
em termos de Minecraft/engenharia**, e o **risco**.

> Nota de calibração: o ferramental de qualidade é **excelente** (NullAway, ErrorProne, PMD de
> complexidade, PITest, JaCoCo, ArchUnit, GameTest) — raro num mod. Os problemas abaixo **não** são
> de descuido de código (0 catch vazio, 2 TODOs, 4 `@SuppressWarnings`); são de **arquitetura,
> processo e produto**. É um projeto cuidadoso que está grande e difícil de fechar.

---

## A. Arquitetura

### A1 🔴 A mina é dívida estrutural — 4 redesenhos, ~45 classes, método de complexidade 62
- **Evidência:** `fabric/work/` tem **31 classes `Mine*` + 14 `Miner*`** (~45) para uma profissão.
  O PMD aponta o método mais complexo do projeto em **`MinerSteps` com complexidade cognitiva 62**
  (limite ~15 → 4×), seguido de `MineEdge` 48, `MineCuts` 46. O STATE (C3) registra "a mina mudou
  de desenho 4 vezes"; as memórias de mineiro travado se repetem desde setembro.
- **Por quê:** cada playtest que achava o mineiro parado gerou um remendo novo em vez de simplificar
  o núcleo. Complexidade 62 num método que decide caminhar/cavar/descer/desviar é o terreno onde os
  bugs "o guarda X não via o caso Y" nascem — e é exatamente o arquivo que mais aparece nas falhas.
- **Risco:** altíssimo. Toda correção da mina tem alta chance de regressão; o custo de entender o
  fluxo antes de mexer já é proibitivo. É a causa raiz recorrente dos P0.
- **Recomendação:** congelar features da mina e fazer **uma** refatoração de `MinerSteps.step` numa
  máquina de estados explícita (procurar → caminhar → descer → cavar → recuar), com os guardas como
  transições nomeadas. Medir complexidade antes/depois.

### A2 🟠 `fabric/work/` é um god-package — 172 classes sem subpacotes
- **Evidência:** `ls fabric/work/*.java` = **172 arquivos** num pacote só.
- **Por quê:** sem fronteiras de pacote, o ArchUnit não consegue impor "a mina não chama a obra
  direto", e qualquer classe acessa qualquer outra. Acoplamento implícito cresce sem barreira.
- **Risco:** médio-alto; amplifica A1 e dificulta testar unidades isoladas.
- **Recomendação:** subpacotes por profissão/subsistema (`work.mine`, `work.build`, `work.farm`,
  `work.craft`, …) e regras ArchUnit de dependência entre eles.

### A3 🟠 O teto de 500 linhas virou colete de forças
- **Evidência:** **15 arquivos entre 490–500 linhas**, vários exatamente em 500 (`TreeChoice`,
  `SmelterWork`, `BuilderWork`, `ColonyGoals` todos em 500). Comentários estão sendo **cortados para
  caber** (aconteceu nesta sessão, na 0.3.54).
- **Por quê:** a regra é boa como sinal ("o arquivo faz demais"), mas usada como limite rígido ela
  premia espremer em vez de dividir. Um arquivo em 500 não é menos complexo — só menos comentado.
- **Risco:** médio; corrói a documentação local (o javadoc que explica a regra) justamente nos
  arquivos mais quentes.
- **Recomendação:** tratar 500 como gatilho de **extração de classe**, não de poda de comentário;
  quando um arquivo bate no teto, dividir responsabilidade (liga-se a A1/A2).

---

## B. Processo

### B1 🔴 Branch única com 74 commits à frente da main; PR #26 nunca mergeia
- **Evidência:** `git rev-list --count main..HEAD` = **74**; a sessão está em
  `claude/autossuficiencia` desde 11-10, acumulando 0.3.25→0.3.55. O próprio ADR-035 §2 manda
  "uma linha por vez, volta por PR curto para a main".
- **Por quê:** um PR de 74 commits não é revisável, não integra, e diverge da main por dias — o
  oposto da regra que o projeto escreveu para si. Risco de conflito e de trabalho órfão cresce a
  cada dia.
- **Risco:** alto (processo). Viola uma decisão arquitetural própria.
- **Recomendação:** mergear a branch na main agora (está verde) e abrir branches curtas por ciclo.

### B2 🟠 STATE.md acumula em vez de sobrescrever — 315 linhas, 24 versões, P0 velho
- **Evidência:** o próprio STATE diz "sobrescreve, não acumula; se passar de 150 linhas algo está
  errado" e tem **315 linhas** com **24 blocos de versão** (0.3.32→0.3.55). A seção "P0" e "o que o
  próximo jogo precisa mostrar" ainda citam o **JAR 0.3.24** e o playtest de 11-10.
- **Por quê:** o estado vivo deixou de ser vivo — mistura changelog (que é do git/Histórico) com o
  estado atual. Quem lê não sabe mais qual é o P0 de hoje.
- **Risco:** médio; leva a decisões sobre informação velha (o "aceite da autossuficiência" descreve
  uma meta de 11-10 que o fluxo já passou).
- **Recomendação:** mover os blocos de versão para o `Historico`, deixar no STATE só o estado de
  hoje + o P0 atual, e reescrever o "próximo jogo precisa mostrar" para a 0.3.55.

### B3 🟠 Micro-versões por playtest, quase nenhuma jogada
- **Evidência:** 41 commits em 11-10, 20 em 12-10, 17 em 13-10; quase toda versão no STATE é "**não
  jogado**". Vão 0.3.24→0.3.55 em ~3 dias.
- **Por quê:** o ciclo corrige-por-log mais rápido do que o playtest valida. Acumulam-se correções
  não verificadas em jogo, e cada playtest mede uma vila diferente — difícil saber o que a mudança
  X causou.
- **Risco:** médio; é o padrão que as próprias memórias flagram ("tem teste" ≠ "foi verificado
  rodando"). Erros se empilham entre playtests.
- **Recomendação:** lotes maiores por playtest; só bumpar versão quando houver o que medir, e medir
  um ciclo antes de empilhar o próximo.

### B4 🟡 PITest com `continue-on-error` já ficou parado sem ninguém notar
- **Evidência:** o comentário no `ci.yml` registra que "o PIT ficou parado do 78e7efc ao 7619b1d"
  por causa do `continue-on-error` (memória `ci-verde-esconde-pit-parado`).
- **Por quê:** mutação é a rede que prova que os testes medem algo; com `continue-on-error`, o CI
  fica verde mesmo com o PIT quebrado.
- **Risco:** baixo-médio; já conhecido. Verificar se ainda está `continue-on-error` e exigir o
  marcador "Generated N mutations" no log.

---

## C. Produto / lógica de Minecraft

### C1 🔴 A mina longe da vila grande (geografia) — a dor recorrente
- **Evidência (save 14-10):** vila 132×152, centro no canto NE (291,1223), boca da mina no SW
  (250,1158) — **~77 blocos** centro→boca, alvo a ~95 do mineiro. A 0.3.55 trata o *percurso*
  (waypoints), mas a **causa geográfica** (boca num canto de uma vila enorme) segue aberta.
- **Por quê:** a regra "boca na borda" + "vila cresce sem teto" produz distâncias que a navegação
  Vanilla não cobre. O villager deposita perto do centro e atravessa a vila para minerar.
- **Risco:** alto (é o P0 de fato dos últimos playtests).
- **Recomendação:** abrir boca nova perto do centro quando a existente passa de um limite, OU
  manter a frente de mineração a um raio caminhável do baú do mineiro.

### C2 🟠 Produção antecipada sem teto / cadeia de material trava a obra
- **Evidência:** STATE P0 — `AdvanceStock` sem teto (318 lajes, 356 escadas surgidas), e obras
  travando em peças sem rota (`white_terracotta` "no route delivered it"); memórias
  `cadeia-de-producao-recursiva`, `rota-na-receita-nao-e-rota-no-mundo`, `obra-esperando-tijolo`.
- **Por quê:** o planejamento de material não distingue "a receita existe" de "o bioma/colônia tem
  como fazer", e a produção antecipada enche o baú do que ninguém pediu.
- **Risco:** alto; trava a obra (BUILDER 90%+ bloqueado em vários playtests) e cascata nas outras.
- **Recomendação:** teto de antecipação por obra aberta (já decidido em parte); e a cadeia descer
  a receita até a folha com verificação de disponibilidade no mundo antes de prometer a peça.

### C3 🟠 Dependência do ciclo dia/noite mascara a medição
- **Evidência:** playtest 14-10 foi ~84% "off hours" (54 off vs 10 work); BUILDER lido como 93%
  bloqueado era, em boa parte, a noite (a `GoToWorkTargetTask` não começa fora do expediente).
- **Por quê:** os ledgers contam espera noturna como bloqueio, distorcendo a Regra 51.
- **Risco:** baixo (não é bug), mas **envenena o diagnóstico**: leva a "corrigir" o que é só a noite.
- **Recomendação:** o `time_ledger` separar janelas de dia/noite (ou marcar off-hours) para a Regra
  51 medir só o expediente; e sempre playtest de dia / cobrindo um ciclo inteiro.

### C4 🟡 Profissões periféricas em laço improdutivo
- **Evidência:** pastor solta COLLECT_WOOL "no sheep with wool" 9–25×/playtest; fundidor espera cru
  (areia/argila) 60%+; fazendeiro sem lote ("no empty plot within 32 blocks").
- **Por quê:** a profissão repete um pedido que o ambiente não pode atender, sem recuar para outra
  tarefa — o oposto do "nunca parado" que o autor pede (Regra 51).
- **Risco:** médio; ócio crônico dessas profissões.
- **Recomendação:** quando um recurso não existe no raio por N tentativas, a profissão troca de
  alvo/tarefa em vez de reinstalar o mesmo pedido.

---

## D. Testes

### D1 🟠 GameTests de navegação são não-determinísticos
- **Evidência:** os GameTests de aproximação da mina (0.3.55) alternavam 2↔3 falhas entre execuções
  pelo pathfinder variar; o `MinerDirectPathGameTest` é flake conhecido; memória
  `bateria-gametest-instavel` (~25% num teste).
- **Por quê:** o pathfinder do Vanilla não é determinístico num espaço pequeno; testes de navegação
  afirmam sobre o resultado dele.
- **Risco:** médio; "N/N passaram" só vale com repetição, e flakes mascaram regressão real.
- **Recomendação:** testar a **lógica pura** em unidade (como `MineApproachProgressTest`) e deixar o
  comportamento de navegação para o playtest; marcar/ isolar os flakes conhecidos.

### D2 🟡 Cobertura do cliente e de `fabric.command`/`network` baixa
- **Evidência (STATE):** cliente 0% (sem cliente no teste), `fabric.command` 27%, `fabric.network`
  67%.
- **Risco:** baixo (o cliente é overlay/UI), mas comandos sem teste quebram calado.

---

## Prioridade sugerida (impacto × risco)

1. **C1** mina longe (P0 real de produção) · 2. **A1** refatorar `MinerSteps` (complexidade 62) ·
3. **B1** mergear a branch / voltar a PRs curtos · 4. **C2** teto de antecipação + cadeia no mundo ·
5. **B2** enxugar o STATE · 6. **C3** dia/noite no ledger · 7. **A2/A3** subpacotes + teto como
extração · 8. **C4** profissões em laço · 9. **D1** flakes · 10. **B4** PIT.

O fio comum: **a mina** concentra o risco de código (A1), o P0 de produto (C1) e o grosso dos
playtests falhos. Atacá-la com refatoração (não mais um remendo) é o maior ganho único.
