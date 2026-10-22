# Avaliação de engenharia — "Regras Consolidadas v2.0" (proposta) — 2026-10-22

**Pedido do autor:** avaliar o documento `Village_Colony_Regras_Consolidadas_v2.md` como engenheiro, com foco em
travamentos, erros e falhas de lógica, e ver ponto a ponto onde ele pode virar atualização do projeto.

**Método:**
- Cada regra R-xxx foi comparada com `docs/RULES.md` (Regras 1–109) e com o código da `main` (0.3.92).
- Os pontos marcados **(conferido)** foram lidos no código nesta avaliação. Os demais vêm do `RULES.md` e do
  histórico desta sessão.

**Legenda:**
- ✅ já vale no código;
- 🟡 vale em parte;
- ⛔ não existe e é trabalho novo;
- ⚠️ conflita com regra atual, ou é perigoso como está escrito;
- ℹ️ princípio sem ação direta.

---

## 1. Problemas do próprio documento (corrigir antes de adotar)

1. **A numeração colide com a atual.** O documento numera R-001 a R-164, e o projeto tem Regras 1–109 citadas
   em código, commits, logs e testes ("Regra 74" é a cama perto do trabalho; "R-074" seria agricultura por
   capacidade). Adotar os números novos quebraria a rastreabilidade que a Regra 50 e a §0.5 do `CLAUDE.md`
   exigem.
   **Recomendação:** manter 1–109, mapear cada R-xxx para a regra existente que ele reescreve, e dar 110+ só
   ao que for novo.
2. **Referências erradas na §16:**
   - C-08 cita a "Regra 113", que não existe (a última é a 109).
   - C-10 trata 75–86 como "mineração", mas 75–78 são movimento, 79–85 mina e 86 peça inalcançável.
   - C-04, C-06, C-07 e C-09 estão coerentes com a numeração de 22-10.
3. **Duas resoluções já estão feitas:**
   - **C-01:** a Regra 4 já consta como "substituída na prática", com a cota que cresce com a população
     (`ProfessionAssigner.targetCount`).
   - **C-02:** as Regras 21 e 32 já estão "a decidir (C-02 da auditoria)". A cama vem da planta, e não existe
     `BuilderWork.furnish`.
   O documento repete as duas como pendentes.
4. **A proposta mistura três coisas de custo muito diferente:**
   - (a) reescrever regras que já valem, que só custa documentação;
   - (b) contratos e diagnóstico gerais (R-047–R-052, R-155), uma refatoração média;
   - (c) sistemas que não existem: transporte físico, configuração, área protegida pelo jogador e adaptador
     de versão. É trabalho grande.
   Tratar tudo como "regra" esconde o custo de (c).

---

## 2. Hipóteses do documento que NÃO batem com o código

| Item | O documento supõe | O código hoje | Consequência |
|---|---|---|---|
| R-140 a R-142, C-06, critério global "itens em transporte" | Transporte físico, com carga em trânsito | **(conferido)** `ChestTransfer` move o item de baú para baú **na hora**; não há carga em trânsito | As regras de trânsito são N/A hoje. Adotá-las significa criar transporte físico (carregador, viagem, reserva de carga): um sistema novo grande, com novos modos de travar |
| R-153, R-154, "configurável" em R-053, R-106 etc. | Um sistema de configuração com valores padrão seguros | **(conferido)** só existe a `ProfessionPolicy` (ofício ligado e ordem, com permissão de operador); todo limite é constante no código | "Configurável" é trabalho novo: arquivo de config, validação, sincronização com o cliente |
| R-035 | O jogador marca áreas protegidas | **(conferido)** não existe; o mais perto é a Tocha das Almas cancelando a obra (Regra 108) | Funcionalidade nova |
| R-052 | Lista única de códigos de bloqueio | **(conferido)** há vários vocabulários: `IdleReason` (10 códigos), `SkipReason`, `ScanRefusalReason` e frases livres nos GAVE_UP do diário | Unificar é viável e útil, mas é refatoração em muitos ofícios |
| R-086 | Marcar o item gerado por exceção para que ele não vire produção | Pilha Vanilla não guarda origem; marcar com NBT quebraria o empilhamento com o item igual | Só dá para contar por quantidade (há `SupplyGapLedger`), não seguir o item |
| R-003, R-163 (26.3) | Adaptador de versão | ✅ o Core não importa Minecraft (ArchUnit), e o Fabric fica em adaptadores | Já preparado; porte real só com ambiente da versão |

---

## 3. Riscos de travamento ou falha de lógica que a proposta introduz

1. **R-118 e R-138 ("a obra só termina com todas as peças obrigatórias") reabrem o impasse da obra eterna.**
   - Hoje há saídas deliberadas: a peça é pulada após 5 falhas (Regra 51), a peça sem apoio é largada
     (`UnsupportedRelease`, emenda 14), e a peça inalcançável surge no lugar (86).
   - Exigir completude sem essas saídas reproduz os travamentos de setembro ("casa não sobe", "riscado não é
     assentado").
   - **Correção:** a lista de peças obrigatórias vale só para peças **funcionais** (cama, porta, baú); o
     resto mantém as saídas atuais.
2. **R-029 (autoria desconhecida → não destruir) paralisaria o deserto.**
   - O chão das vilas do deserto conta como "da vila gerada" (87% da amostra de 09-18 era areia e arenito).
     Com essa política, a 0.3.x teve 0 lotes aprovados até a exceção do chão do bioma
     (`LotGround.isBiomeGround`).
   - **Correção:** escrever a exceção explícita: o chão do bioma não é construção.
3. **R-061 ("parar a produção na meta") contra a Regra 95 (todo ofício trabalha) e a Regra 51 (≤ 15% ocioso).**
   - Parar a produção aumenta o ócio medido. O que existe hoje é teto (64/256, `StockCeiling`) mais a ajuda à
     obra (Regra 59).
   - Compatível, se a regra disser o que fazer depois de parar: ajudar a obra, fazer manutenção ou recolher.
4. **R-019 / R-021 (cinco estados de disponibilidade e memória com validade):**
   - Hoje o `KnownAbsent` vale 24.000 tiques **(conferido)**, mas não é invalidado por evento (alcance
     cresceu, rota nova, terreno mudou).
   - Sem invalidação por evento, a vila que cresce espera um dia inteiro para rever o recurso.
   - É uma melhoria real e de baixo risco.
5. **R-043 (troca de cama transacional) expõe um risco REAL na Regra 74 (0.3.92):**
   - O `BedChoice` solta o registro do baú ao mudar a cama, e o baú novo é o livre ao lado da cama nova
     (`ChestScanner`/`ChestSpawner`).
   - Se não houver baú livre nem lugar para um ao lado da cama nova, o trabalhador fica sem baú.
   - **(conferido)** O `VacancyEnforcer` tira o ofício de quem está sem baú quando há candidato.
   - **Correção:** só oferecer cama com baú livre ao lado (ou lugar garantido para um); senão a cama não
     entra no plano.
6. **R-148 (custo real de movimento) contra a Regra 74.** O `BedPlan` mede distância em linha reta (3D). Numa
   vila de duas alturas, linha reta curta pode ser caminhada longa. Melhoria: usar o comprimento do caminho no
   `ReachMap` quando disponível.
7. **R-150 (nada de tentar a cada tique) já tem dois furos conhecidos nesta sessão:**
   - a busca do corredor com corte sem teto (corrigida na 0.3.91);
   - o vaivém do fundidor a cada ~30 s (pendente no TODO).
   Vale como regra, porque transforma isso em critério de revisão.
8. **R-054 (projetos em paralelo):** hoje é uma obra por colônia. Paralelizar aumenta a disputa por
   construtor e material, e a R-048 (reserva exclusiva) teria de vir antes.

---

## 4. Matriz ponto a ponto

### Princípios (R-001 a R-008)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 001 | ℹ️ | ColonyCycle, ADR-002 | princípio |
| 002 | 🟡 | 45, 98 (`KnownAbsent`) | o conhecimento tem prazo, mas não é invalidado por evento |
| 003 | ✅ | ArchUnit Core/Fabric | — |
| 004 | 🟡 | 63 (socorro por prazo), 80 (lease) | falta um contrato único (ver R-047) |
| 005 | 🟡 | lição das emendas da 27 | a conta e o consumidor já divergiram antes; vale como regra |
| 006 | ✅ | 3, 3-e1 | ver o risco 2 (chão do bioma) |
| 007 | ✅ | 56 (Emenda 0.3.44) | — |
| 008 | 🟡 | diário, `VC_*`, `IdleLog` | há agregação parcial; spam ainda aparece (WARNED por classe) |

### Identidade e estado (R-009 a R-016)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 009 | ✅ | 105 (`ColonyIdentity`, E51) | — |
| 010 | ✅ | 105 (`ColonyMergeTrigger`) | — |
| 011 | ✅ | 106 (caixa medida) | — |
| 012 | ✅ | 12 | — |
| 013 | ✅ **(conferido)** | adoção atômica em `VillageAdoption`; árvores e animais só com `created` | abandono marca, não apaga: não recria animais |
| 014 | ✅ | 61 | — |
| 015 | ✅ | 62 | — |
| 016 | ✅ **(conferido)** | `SaveMigration` com `saveVersion` | — |

### Ambiente e biomas (R-017 a R-027)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 017 | 🟡 | `ReachMap`, `SurfaceHits`, `KnownAbsent` | espalhado; não há um "perfil" único |
| 018 | ✅ | 20, `VillageStyle` | — |
| 019 | ⛔ | — | há "ausente" e "inalcançável" separados em lugares diferentes; os cinco estados juntos são novos |
| 020 | ✅ | `SweepDeadline`, `PlanningBudget` | — |
| 021 | 🟡 | 98 | falta invalidar por evento |
| 022 | ✅ | 99 | — |
| 023 | 🟡 | `ProductionChain` (E5) | — |
| 024 | 🟡 | bancada de biomas | o deserto ainda gera pendências |
| 025 | 🟡 | tags onde existem; listas de nome (arenito) onde o jogo não tem tag | a justificativa está no `LotGround` |
| 026 | ⛔ | — | não há integração com outros mods |
| 027 | 🟡 | `PlayerWorldChangeHandler`, `RoadIndex` | parcial |

### Proteção e terreno (R-028 a R-035)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 028 | 🟡 | `BlockProtection` | sem a categoria "candidata à expansão" |
| 029 | ⚠️ | 3 | ver o risco 2 |
| 030 | ✅ | 3-e1, quebra a quebra | — |
| 031 | ✅ | 104 | — |
| 032 | ✅ | 34 | — |
| 033 | ✅ | 90 (`LotTerraform`), 70 (corte do corredor até 12) | — |
| 034 | ✅ | 71, 75 | — |
| 035 | ⛔ | — | funcionalidade nova |

### Profissões (R-036 a R-046)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 036–038 | ✅ | 11, 38 | — |
| 039 | ✅ | 4 (substituída) | — |
| 040 | ✅ | decisão de 10-11 | — |
| 041 | ✅ | 44 | — |
| 042 | 🟡 | 74 | linha reta (risco 6) |
| 043 | ⚠️ | 74 | risco 5: o trabalhador pode ficar sem baú |
| 044–045 | ✅ | 47, 46 | — |
| 046 | ✅ | 50, 51 | — |

### Tarefas e recuperação (R-047 a R-059)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 047 | ⛔ | — | `Task` tem tipo, alvo e estado; faltam prazo e critério de sucesso declarados |
| 048 | 🟡 | 80, 81 (pedra e ramal), lote reservado | trabalhador, lote e alvo de mina, sim; recurso em baú, só pelo `BuildReserve` |
| 049 | ✅ | 79 (lease), 63 | — |
| 050 | 🟡 | 63 | prazos ainda duplicados por ofício (`MinerSteps`, `WalkLegs`…) |
| 051 | 🟡 | `WorkerStrikes`, desistências | — |
| 052 | 🟡 | `IdleReason`, `SkipReason`… | unificar |
| 053 | 🟡 | 49 | só a ordem das obras; a prioridade global entre ofícios não existe |
| 054 | ✅ | uma obra por vez | ver o risco 8 |
| 055 | ✅ | `ConstructionResume`, emenda 14 | — |
| 056 | ✅ | 92 | — |
| 057 | ✅ | 108 | — |
| 058 | ✅ | 48 | — |
| 059 | 🟡 | 58 | há caminhos que perdem item: o `LotTerraform` corta sem guardar; o `CorridorCut` guarda, mas o que não cabe some |

### Economia e produção (R-060 a R-079)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 060 | 🟡 | 1, 5, `StockCeiling` | falta a reserva mínima explícita por item |
| 061 | ⚠️ | 95 | risco 3 |
| 062 | ✅ | emenda 1/5 (10-11) | — |
| 063 | 🟡 | `BuildReserve` | — |
| 064 | 🟡 | lição da 27 | — |
| 065 | ✅ | 26 e emenda (3 degraus) | — |
| 066 | ✅ | emenda 1/5 | — |
| 067 | ✅ | 30-e1, 85 (`IronRest`) | — |
| 068–069 | ✅ | 42, 58 | — |
| 070 | ✅ | 97 | — |
| 071–072 | ✅ | 45, 96 | — |
| 073 | ✅ | 39 | — |
| 074 | ✅ | 67 | — |
| 075 | ✅ | emenda 31 | — |
| 076 | 🟡 | fundidor | o vaivém de ~30 s está pendente |
| 077 | ✅ | 2-e1, 37 | — |
| 078 | ✅ | 65 | — |
| 079 | ✅ | 101 | — |

### Suprimento excepcional (R-080 a R-089)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 080 | 🟡 | 41, 51, 88, 100 | as exceções existem; falta a **lista única declarada** |
| 081 | ✅ | emenda 51 (`ChestTransfer`) | — |
| 082 | ✅ | 51 | — |
| 083 | ✅ | 69 | — |
| 084 | ✅ | 100 | — |
| 085 | ⛔ | — | distinção nova na telemetria |
| 086 | ⚠️ | — | ver a §2: rastrear o item é inviável; contar por quantidade, sim |
| 087 | ✅ | 34 | — |
| 088 | ✅ | 87 | — |
| 089 | ✅ | 88 | — |

### Madeira, mina e navegação (R-090 a R-102)

| R | Situação | Regra atual / onde |
|---|---|---|
| 090 | ✅ | 3-e1 |
| 091 | ✅ | 7 + emendas, 104 |
| 092 | ✅ | `TreeChoice` |
| 093 | ✅ | 30, 82 |
| 094 | ✅ | 54, 81 |
| 095 | ✅ | 80 |
| 096 | 🟡 | 85, 98; sem região |
| 097 | ✅ | 71, `MineReturn` (casca) |
| 098 | ✅ | 84 |
| 099 | ✅ | 71, 75 |
| 100 | ✅ | 76, 77 |
| 101 | ✅ | 78 |
| 102 | ✅ | 79 |

### Agricultura e animais (R-103 a R-114)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 103 | ✅ | 31 | — |
| 104 | 🟡 | 67 | — |
| 105 | 🟡 | 67 | sem a produtividade observada |
| 106 | ✅ | 102 | uma vez, conferido; não é configurável |
| 107 | 🟡 | — | animal inacessível ainda conta |
| 108–109 | ✅ | 72 | — |
| 110–111 | ✅ | 43, 68 | — |
| 112 | ✅ | 68 | — |
| 113 | ✅ | — | não recria animal |
| 114 | 🟡 | `PenEscape`, E52 | — |

### Construção e expansão (R-115 a R-138)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 115–116 | ✅ | 27-e3, 20, 13 | — |
| 117 | ✅ | 109 | — |
| 118 | ⚠️ | 21, 32 | risco 1 |
| 119–120 | ✅ | 16, 22, 19 | — |
| 121 | ✅ | 90 | — |
| 122 | ✅ | 17, 89 | — |
| 123 | ✅ | 64 | — |
| 124 | ✅ | 70 | — |
| 125 | ✅ | 73 | — |
| 126–128 | ✅ | 56 + emendas, A5 | — |
| 129 | ✅ | 49, 67, `UrgentPlans` | — |
| 130 | ✅ **(conferido)** | `HOUSE_SURPLUS = 4` | — |
| 131 | ✅ | 67 | — |
| 132 | ✅ | 60 | — |
| 133 | ✅ | 92 | — |
| 134 | ✅ | 14, 86 | — |
| 135 | ✅ | emenda 14 | — |
| 136–137 | ✅ | 22, 69 | — |
| 138 | ⚠️ | — | risco 1 |

### Logística (R-139 a R-145)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 139 | ✅ | emenda 51 | — |
| 140–142 | ⛔ | — | ver a §2: não há transporte físico |
| 143 | 🟡 | 58 | o excedente vai para o baú público; sem lugar, pode sumir |
| 144 | ✅ | `ChestDepositor` | — |
| 145 | ✅ | 8, 96 | — |

### Tempo e desempenho (R-146 a R-150)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 146–147 | ✅ | 18-e1, 2-e1 | — |
| 148 | 🟡 | `DetourMoves.Costs` na mina; linha reta na 74 | — |
| 149 | 🟡 | `PlanningBudget`, teto de 50 ms do corredor | — |
| 150 | 🟡 | — | risco 7 |

### Interface e configuração (R-151 a R-155)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 151 | 🟡 | `/vc log`, overlays | — |
| 152 | ✅ | 107 | — |
| 153–154 | ⛔ / 🟡 | — | só a `ProfessionPolicy` tem permissão |
| 155 | 🟡 | — | — |

### Testes e versão (R-156 a R-164)

| R | Situação | Regra atual / onde | Observação |
|---|---|---|---|
| 156 | ✅ | prática atual: GameTest e mutação | — |
| 157 | 🟡 | bancada por bioma | — |
| 158 | ⛔ | — | não há uma bateria de escassez sistemática |
| 159 | 🟡 | — | — |
| 160 | 🟡 | `ConstructionResume` | sem testes de recarga no meio de transporte ou troca de cama |
| 161 | ✅ | KF, mutação, bateria | — |
| 162 | ✅ | o README é gerado do `RULES.md` (22-10) | — |
| 163–164 | ✅ / 🟡 | — | — |

---

## 5. Totais

Das 164 regras propostas:
- **cerca de 100 já valem no código** (são as Regras 1–109 reescritas);
- **cerca de 40 valem em parte**;
- **13 são trabalho novo:** R-019, 026, 035, 047, 085, 140–142, 153–154, 158 e 052/R-155 como unificação;
- **7 conflitam ou são perigosas como estão escritas:** R-029, 043 (risco na 74), 061, 086, 118, 138, e a
  numeração.

## 6. O que tem mais valor, na ordem

1. **Corrigir o risco real da Regra 74** (cama sem baú → perda do ofício). É pequeno e evita uma falha nova.
2. **Invalidação por evento do conhecimento de ausência** (R-021, R-027). Pequeno e destrava a vila que cresce.
3. **Códigos de bloqueio unificados + contrato mínimo de tarefa** (R-047, R-052, R-155). Médio; dá o
   diagnóstico que o próprio documento pede e que os relatórios usam.
4. **Lista única declarada de suprimentos excepcionais** (R-080, R-086 por quantidade). Pequeno a médio;
   controla a "economia infinita".
5. **Resolver C-02** (Regras 21/32) com peças funcionais obrigatórias, mantendo as saídas da obra (risco 1).
   Decisão do autor.
6. **Grandes e só com decisão:** configuração (R-153), área protegida (R-035), transporte físico (R-140–142) e
   outros mods (R-026).

---

## 7. Decisões do autor (2026-10-22)

- **Numeração:** as Regras 1–109 ficam com o número que têm. Cada R-xxx do v2 vira redação ou emenda da regra
  existente que ele cobre, e só o que for novo ganha número a partir de 110.
- **Feito agora:**
  - o risco da Regra 74 (baú garantido ao lado da cama nova; distância pela caminhada real);
  - itens cortados não somem (R-059);
  - códigos de bloqueio únicos e lista declarada das exceções de suprimento (R-052, R-080).
- **C-02 (Regras 21/32):** cama, porta e baú da planta são obrigatórios para a obra terminar; as outras peças
  mantêm as saídas atuais.
- **Sistema grande:** a configuração (R-153/154) será desenhada antes de qualquer código; o autor trocou a
  escolha inicial (área protegida). Área protegida, transporte físico e outros mods ficam como futuro.
