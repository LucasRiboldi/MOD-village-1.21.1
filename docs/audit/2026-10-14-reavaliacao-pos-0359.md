# Reavaliação de pontos críticos — pós-0.3.59 (2026-10-14)

Refaz a varredura de [`2026-10-14-varredura-engenharia.md`](2026-10-14-varredura-engenharia.md) depois
das versões 0.3.56→0.3.59 e do playtest 0.3.58 (Spark `YOT1oqyhIv`). Só medição. Marca o que **mudou**,
o que **persiste** e as **incoerências novas**. Não repete a análise velha como se fosse nova.

## Calibração (confirmada, melhorou)

O ferramental segue excelente e a disciplina de código, também: **4 marcadores de dívida** no código-
fonte (e os 4 são menções a `TODO.md` em javadoc, não TODOs reais), **0 catch vazio**. Os **205 Maps
estáticos** — o anti-padrão nº1 do domínio de aldeões — **não** são vazamento: a amostra dos suspeitos
(`ConstructionOrder.WORKSHOPS`, `MasonStock`, `WorkDemand`, `FlockHarvest`) são tabelas constantes
(bloco estático, 0 mutações) ou Maps locais de método (A* do `DetourPlanner`); o estado vivo por
UUID/colônia usa `ServerMemory.register(...)` (confirmado em `MineApproachPath`, `MineVein`,
`MinerDirectPath`). O problema continua sendo **arquitetura, processo e produto**, não descuido.

## Resolvidos desde 14-10

- **B1 (branch 74 commits sem merge) — RESOLVIDO.** A `main` foi integrada; agora só 2 commits locais à
  frente do remoto (os desta sessão). A regra "uma linha por vez" voltou a valer.
- **B4 (PITest com `continue-on-error`) — RESOLVIDO.** O `ci.yml` roda `./gradlew pitest` direto; o
  `continue-on-error` virou só comentário histórico.

## Persistem (sem mudança real)

### A1 🔴 A mina é dívida estrutural — INTOCADA
- 31 classes `Mine*`/`Miner*` em `work/`; `MinerSteps` ainda em **492 linhas** (complexidade 62).
- **O conserto da 0.3.59 foi num arquivo auxiliar (`MinerDirectPath`), não no núcleo.** Resolveu o
  sintoma dominante (falso caminho à superfície) sem reduzir a complexidade. A recomendação original
  (refatorar `MinerSteps.step` em máquina de estados) continua aberta e continua sendo a causa raiz
  recorrente dos P0.

### A2 🟠 `work/` é god-package — PIOROU
- **173 arquivos** num pacote só (eram 172). Há ArchUnit (`ArchitectureRulesTest`), mas nenhuma regra
  `work.mine`/`work.build` — a fronteira entre mina e obra não é imposta.

### A3 🟠 Teto de 500 linhas como colete de forças — PIOROU
- **4 arquivos exatamente em 500** (`TreeChoice`, `SmelterWork`, `BuilderWork`, `ColonyGoals`) e **~20
  entre 470–500**. Produção total: 502 arquivos, 84.015 linhas. O teto segue premiando espremer em vez
  de extrair classe.

### C1 🔴 Mina longe da vila grande — CONFIRMADA e refinada pelo playtest 0.3.58
- A causa geográfica segue aberta, e o spike da 0.3.59 mostrou **um segundo mecanismo em série**: além
  da distância, o `findPathTo` sobe o alvo sólido à superfície (E32) e o mineiro andava para cima da
  pedra sem descer. A 0.3.59 fecha esse; a geografia (boca no canto de uma vila 132×152) continua.
- **2º defeito ainda aberto:** o approach encalha a **4-5 blocos** (fora do REACH 4) quando o mineiro
  chega perto — próximo ciclo.

### C3 🟠 Dia/noite mascara a medição — NÃO RESOLVIDO
- O `time_ledger.py` não separa off-hours (3 menções, nenhuma lógica de janela dia/noite). A Regra 51
  segue contando espera noturna. O playtest 0.3.58, porém, foi de 100 min cobrindo dia e noite — menos
  envenenado que o de 11-10.

### C2 🟠 / C4 🟡 Cadeia de material e profissões periféricas — PARCIAL
- C4: o fundidor ainda larga 84× ("no clay in any chest", clay inexistente no bioma) e o fazendeiro
  espera 58% — o laço "reinstala em vez de trocar de tarefa" segue. C2: 1× `VC_SUPPLY_ERROR` no
  playtest (bem menos que antes, mas o padrão persiste).

## Incoerências novas (introduzidas nesta rodada)

### I1 🔴 STATE.md se contradiz sobre a versão em teste
- O topo é **0.3.59** e o JAR jogado foi **0.3.58**, mas a seção "O que o próximo jogo precisa mostrar"
  (e a tabela de aceite) ainda dizem **"JAR 0.3.55 instalado"** e citam o playtest de 14-10 da 0.3.55.
  Quem lê não sabe qual versão está em teste. **Introduzido em parte nesta sessão** (adicionei o bloco
  0.3.59 no topo sem atualizar o rodapé).

### I2 🟠 STATE voltou a passar do próprio teto
- 156 linhas (o arquivo diz "se passar de 150, algo está errado"), 5 blocos de versão. Melhor que os
  315 de 14-10, mas o B2 reincide: changelog voltando a se acumular no estado vivo.

### I3 🟡 TODO.md com 157 itens abertos em 396 linhas
- Fila grande; difícil distinguir o que é P0 real do acúmulo. Liga-se a B3 (micro-versões por playtest).

## Prioridade sugerida

1. **I1** (corrigir a contradição do STATE sobre a versão) — barato, e o STATE é o ponto de entrada.
2. **C1 2º defeito** (approach 4-5 bl) — fecha o P0 da mina pelo lado que falta.
3. **A1** (refatorar `MinerSteps` em máquina de estados) — a dívida que gera os P0; exige congelar
   features da mina. É a decisão estrutural de fundo.
4. **C3** (time_ledger separar dia/noite) — barato e desenvenena toda medição futura.
5. **A2/A3** (subpacotes em `work/` + tratar 500 como gatilho de extração) — dívida de forma.
