# Plano de melhoria — Village Colony (2026-10-14)

Plano de execução dos pontos críticos levantados na reavaliação pós-0.3.59
([`docs/audit/2026-10-14-reavaliacao-pos-0359.md`](../audit/2026-10-14-reavaliacao-pos-0359.md)),
que por sua vez refaz a varredura de engenharia de
[`14-10`](../audit/2026-10-14-varredura-engenharia.md).

**Natureza do plano.** Cada item é um **ciclo** (ADR-035 §2: uma linha de trabalho por vez, branch
curta a partir da `main`, PR de volta). Cada ciclo traz: a **evidência** que o motiva, o **aceite com
número**, a **verificação exata** (comandos) e a regra de honestidade do projeto — *não se marca
concluído o que não rodou; "tem teste" ≠ "foi verificado em jogo"* (CLAUDE.md).

**O que torna o plano "garantido".** Nenhum ciclo fecha sem: (a) `./gradlew test` + `runGametest`
verdes pela contagem honesta (XML / "All N required tests passed"); (b) para regra de lógica nova, uma
**mutação** que derrube o teste; (c) para efeito em jogo, o **playtest** com a métrica no alvo — o
GameTest prova o mecanismo, o ledger prova o efeito (Regra 50/51). A ordem respeita dependências:
higiene barata primeiro (desenvenena a medição), produto depois, dívida de forma por último.

**Correção de rumo embutida.** Ao fundamentar este plano, o PMD de 14-10 (22:53) mostrou o pior método
em **complexidade 26** (`VillagerScanner.scan`), e **nenhum método de mina no topo** — a "complexidade
62 do `MinerSteps`" da varredura já não existe. Portanto o antigo **A1** (refatorar um método monstro)
foi **reescopado**: o problema da mina hoje é **tamanho de subsistema** (31 classes) e **geografia**
(C1), não um método. O plano reflete isso.

---

## Fase 0 — Higiene e desenvenenamento da medição (barata, primeiro)

Antes de qualquer correção de produto: consertar o que mente sobre o estado e o que envenena o
diagnóstico. Sem isso, os ciclos seguintes medem errado.

### Ciclo H1 🔴 — Corrigir a contradição do STATE (I1)
- **Evidência:** o STATE tem topo 0.3.59, mas "O que o próximo jogo precisa mostrar" e a tabela de
  aceite ainda dizem "JAR 0.3.55 instalado". Quem lê não sabe a versão em teste.
- **Ação:** reescrever a seção "próximo jogo" e a de processo para a 0.3.59; mover os blocos de versão
  antigos (≥3 atrás) para o `Historico-2026-10.md`.
- **Aceite:** `grep -c "0\.3\.55" STATE.md` só em contexto histórico explícito; STATE ≤ 150 linhas
  (resolve I2 de quebra); uma única versão descrita como "em teste".
- **Verificação:** leitura; `wc -l STATE.md` ≤ 150. Sem código, sem build.
- **Custo:** baixo. **Depende de:** nada.

### Ciclo H2 🟠 — `time_ledger` separa dia/noite (C3) — ✅ RESOLVIDO POR VERIFICAÇÃO (premissa errada)
- **Evidência que refutou:** `WorkTime.sample` (`fabric/work/WorkTime.java:93-95`) já pula o aldeão
  quando `!WorkHours.isWorkTime` → `continue`. A linha `VC_TIME` **só conta o expediente, por
  construção, desde o commit da Regra 50** (`5ce71b0f`). O `time_ledger.py` também já pondera certo por
  `s=` (`total*share/100`), não faz média simples. O "BUILDER 93% bloqueado = noite" de 11-10 foi
  interpretação da época; o filtro já existia.
- **Conclusão:** não há viés dia/noite a corrigir no ledger. O que falta para medir bem é **jogar de
  dia** — que já é a regra do aceite do P0. H2 fechado sem alterar código. Ver memória
  `time-ledger-ja-so-expediente`.

### Ciclo H3 🟡 — Enxugar o TODO.md (I3) — ✅ FEITO (14-10)
- **Evidência:** 157 itens abertos em 396 linhas; a maioria eram checklists "Playtest da 0.3.X" de
  versões superadas (0.3.5→0.3.44), nunca verificados — o B3 materializado.
- **Feito:** checklists de playtest ≤0.3.44 movidos para `docs/archive/technical/TODO-ate-2026-10-07.md`
  (grep); TODO mantém só a fila viva (AUD, Fluxo físico, Erros abertos, Regra de teste, Bioma,
  Estrutura, Decisões do autor).
- **Aceite atingido:** TODO 396→**118 linhas** (< 120); 157→**26 itens abertos**.

---

## Fase 1 — Produto: fechar o P0 da mina e os laços improdutivos

### Ciclo P1 🔴 — Mina: 2º defeito em série (approach encalha a 4–5 blocos) — C1 — ✅ FEITO (15-10), falta playtest
- **Verificado em jogo que o pré-requisito fechou:** o playtest 0.3.59 (Spark `1nwGlUtitv`) provou o 1º
  defeito resolvido — MINED 0,8→~19/min, 1448 minerações. Restavam 22× NO_STANDING + 9× TOO_HIGH.
- **Feito (0.3.60):** `MinerHands.passOver` tenta `MineVein.roomBeside` antes de largar quando
  `worthRerouting` (só NO_STANDING); redireciona o alvo para a pedra vizinha. test 1521/0; runGametest
  769/769; mutação derruba `tooHighAndNoPathAreNotReroutable`. **Falta o playtest** para o aceite de
  efeito (NO_STANDING caindo, MINER Regra 51 < 15%).

<!-- registro original do ciclo, mantido para o aceite do playtest -->
#### P1 (definição original)
- **Evidência:** spike 0.3.59 — depois que o mineiro chega perto, o `closest` encalha em **4,1–5,2**
  (fora do REACH 4); 72× NO_STANDING_POSITION. O `roomBeside` (ADR-038 P6) só roda no fluxo do cursor,
  não quando o alvo é julgado de perto em `MinerSteps`/`MinerHands.passOver`.
- **Ação (alternativa A1+A3 da análise):** antes de `passOver` marcar e largar, tentar
  `MineVein.roomBeside` no alvo (abre o pé ao lado e mira nele); se também falha, o alvo conta para a
  curva do ramal (contorna, como bedrock) em vez de recusar em laço.
- **Aceite (playtest de dia, ≥30 min):** `action_report` com TARGET_REJECTED/NO_STANDING_POSITION caído
  > 70% vs 0.3.58; `MINED`/min > 0,8; `went one level deeper` > 1.
- **Verificação:** `./gradlew test` + `runGametest` verdes; mutação na nova condição de `passOver`
  derruba um teste de unidade; **playtest** com `time_ledger`/`action_report`/`cost_ledger`.
- **Rollback:** a mudança é local a `MinerHands.passOver`; reverter o commit restaura o comportamento.
- **Custo:** médio. **Depende de:** 0.3.59 jogada (confirmar que o 1º defeito fechou antes de medir o 2º).

### Ciclo P2 🔴 — Mina: a geografia (boca longe na vila grande) — C1 raiz
- **Evidência:** save 14-10 — vila 132×152, boca no canto SW a ~77 blocos do centro, alvo a ~95 do
  mineiro. A 0.3.55/0.3.59 tratam o percurso; a **distância** em si não.
- **Ação (decisão do autor entre duas, a escolher no brainstorming do ciclo):** (a) abrir boca nova
  perto do centro quando a existente passa de um limite de distância; ou (b) manter a frente de
  mineração a um raio caminhável do baú do mineiro. Exige ADR (muda a posição da mina).
- **Aceite:** distância mediana mineiro→frente < raio caminhável (a definir no ADR); sem "walking to"
  prolongado no `action_report`; `MINED`/min estável ao longo do playtest (não só em bursts).
- **Verificação:** GameTest de posicionamento da boca; playtest medindo a distância ao longo do tempo.
- **Rollback:** nova boca é aditiva; manter a antiga como fallback no primeiro playtest.
- **Custo:** médio-alto. **Depende de:** P1 (medir a geografia só depois que o approach não trava).

### Ciclo P3 🟡 — Profissões periféricas: trocar tarefa em vez de reinstalar (C4)
- **Evidência:** fundidor larga 84× ("no clay in any chest", clay inexistente no bioma); fazendeiro
  espera 58%; pastor solta COLLECT_WOOL "no sheep with wool".
- **Ação:** quando um recurso não existe no raio por N buscas, a profissão entra na cadeia de
  alternativas (Regra 51) em vez de reinstalar o mesmo pedido — o padrão já aplicado a outros ofícios
  (`[[rota-na-receita-nao-e-rota-no-mundo]]`, `[[castigo-por-oficio-vira-rodizio]]`).
- **Aceite:** `action_report` — soltas/pegas < 10% por profissão, sem o mesmo motivo repetido;
  Regra 51 (ocioso+bloqueado+encalhado) ≤ 15% para fundidor e fazendeiro no expediente.
- **Verificação:** `./gradlew test` + `runGametest`; mutação na condição de troca; playtest.
- **Rollback:** a troca é condicional; reverter o commit restaura a espera.
- **Custo:** baixo-médio. **Depende de:** H2 (medir a Regra 51 sem o viés da noite).

---

## Fase 2 — Dívida de forma (por último; não trava a vila, mas gera os P0)

### Ciclo F1 🟠 — Subpacotes em `work/` + fronteira ArchUnit (A2)
- **Evidência:** 173 arquivos num pacote só; ArchUnit existe mas não impõe "mina não chama obra".
- **Ação:** mover por subsistema (`work.mine`, `work.build`, `work.farm`, `work.craft`, `work.shepherd`,
  …) em **um commit por subsistema** (só move + ajusta imports, sem mudar lógica), e adicionar regras
  ArchUnit de dependência entre eles.
- **Aceite:** `work/` raiz com ≤ ~20 classes transversais; cada subpacote coeso; nova regra ArchUnit
  passando; `test` + `runGametest` verdes **sem mudança de comportamento** (só import/pacote).
- **Verificação:** `./gradlew test runGametest` verdes; `git diff` prova que são só movimentações.
- **Rollback:** cada subsistema é um commit isolado; reverter o que quebrar.
- **Custo:** médio (volume), risco baixo (mecânico). **Depende de:** nada, mas **só depois** da Fase 1
  (mover a mina enquanto se corrige a mina multiplicaria conflito).

### Ciclo F2 🟠 — Teto de 500 como gatilho de extração, não de poda (A3)
- **Evidência:** 4 arquivos exatamente em 500 (`TreeChoice`, `SmelterWork`, `BuilderWork`,
  `ColonyGoals`), ~20 entre 470–500; comentários sendo cortados para caber.
- **Ação:** nos 4 arquivos em 500, extrair a responsabilidade secundária para uma classe nova (não
  podar comentário); documentar na régua que 500 é gatilho de extração.
- **Aceite:** os 4 arquivos < 450 linhas por **extração** (não remoção de javadoc); `test`+`runGametest`
  verdes; sem regressão de comportamento.
- **Verificação:** `wc -l`; `git diff` mostra classe nova, não comentário apagado.
- **Rollback:** extração é commit por arquivo.
- **Custo:** médio. **Depende de:** F1 (extrair dentro do subpacote certo).

### Ciclo F3 🟡 — Reduzir as 80 violações de complexidade PMD (A1 reescopado)
- **Evidência:** PMD 14-10 — 80 violações; piores: `VillagerScanner.scan` 26, `VillageLogCommand.show`
  25, `WorkerEquipment.equip` 23. O `MinerSteps` **não** está entre os piores (correção do antigo A1).
- **Ação:** atacar os ~10 piores métodos extraindo sub-métodos nomeados (sem mudar comportamento),
  priorizando os de caminho quente (scanner, equipamento).
- **Aceite:** nenhum método > 20 de complexidade cognitiva no PMD; total de violações caindo; `test` +
  `runGametest` verdes.
- **Verificação:** `./gradlew pmdMain` (ou o relatório em `build/reports/pmd/main.xml`); comparar a
  contagem antes/depois.
- **Rollback:** extração método a método.
- **Custo:** médio. **Depende de:** F1/F2 (trabalhar nos arquivos já no lugar e no tamanho certo).

---

## Sequência recomendada e por quê

```
Fase 0 (higiene)     H1 → H2 → H3        desvenena a medição; barato; sem build
Fase 1 (produto)     P1 → P2 → P3        fecha o P0 (mina) e os laços; mede em jogo
Fase 2 (forma)       F1 → F2 → F3        dívida estrutural; mecânico; por último
```

- **Higiene antes de produto:** H2 (dia/noite) precisa estar pronto para P1/P3 medirem a Regra 51 sem
  o viés da noite. H1 antes de tudo porque o STATE é o ponto de entrada e hoje mente sobre a versão.
- **Produto antes de forma:** F1 move a mina; fazer isso durante a correção da mina (P1/P2) multiplica
  conflito. Primeiro estabiliza o comportamento, depois reorganiza a forma.
- **Uma linha por vez (ADR-035 §2):** cada ciclo é uma branch curta com PR para a `main`; nada de
  empilhar correções não jogadas (B3). Só bumpar versão quando houver o que medir em playtest.

## Como medir o progresso do plano

Ao fim de cada fase, rodar a reavaliação por medição (o método desta análise): PMD (complexidade/
violações), `wc -l` (arquivos ≥470), contagem honesta (`test`/`runGametest` via XML), e o trio de
ledgers sobre o playtest mais recente (Regra 50/51). O plano está "ganhando" quando: violações PMD
caindo, nenhum arquivo em 500, Regra 51 ≤ 15% por ofício no expediente, `MINED`/min > 0,8, e o STATE
≤ 150 linhas descrevendo um só P0.
