# Auditoria de recuperação do projeto — Village Colony

**Data:** 2026-10-06 · **Snapshot:** `main` em `6022021e` · **Método:** VILA-RESET
(ver [RECOVERY-ROADMAP.md](RECOVERY-ROADMAP.md) §6).
**Escopo:** só diagnóstico. Nenhuma linha de código, teste ou regra foi alterada.
Branches fora da `main` foram analisadas por `git log/diff/merge-tree`, sem
checkout.

Documentos desta auditoria (cinco, sem mais):

| Documento | Para que serve |
|---|---|
| **PROJECT-RECOVERY-AUDIT.md** (este) | resumo executivo, explicação para leigo, veredito |
| [SYSTEM-MAP.md](SYSTEM-MAP.md) | arquitetura real, inventário, ciclo, dependências, performance |
| [RULE-RECONCILIATION.md](RULE-RECONCILIATION.md) | regras: intenção × código × evidência, conflitos |
| [TEST-STRATEGY.md](TEST-STRATEGY.md) | o que os testes protegem, o que sobra, o que falta |
| [RECOVERY-ROADMAP.md](RECOVERY-ROADMAP.md) | problemas, melhorias, decisões, método, plano |
| [GUIA-DO-LEIGO.md](GUIA-DO-LEIGO.md) | o mod explicado para quem nunca viu o código (pedido explícito do autor; sexto documento) |

### Como ler a evidência

| Marca | Significa |
|---|---|
| **CONFIRMADO** | visto no código ou no git nesta auditoria, com `arquivo:linha` |
| **PROVÁVEL** | mecanismo confirmado, efeito em jogo não reproduzido |
| **SUSPEITO** | indício, falta medir |
| **NÃO CONFIRMADO** | não verificado; não use como fato |

Nada aqui foi "testado rodando". Contagens de teste vêm de `grep`, não de
execução. Números de performance vêm de perfis Spark antigos, citados com data.

---

## 1. Resumo para leigo

**O que é o mod.** No Minecraft existem vilas com aldeões que, sozinhos, quase
não fazem nada. Este mod transforma cada vila numa *colônia*: os aldeões ganham
empregos, buscam materiais, fabricam peças e constroem casas novas. O jogador
encontra a vila e pode ir embora — ela continua crescendo enquanto ele estiver
por perto.

**O que acontece quando uma vila é encontrada.** O mod procura camas. Onde há
pelo menos 3 camas e 2 aldeões, ele "adota" a vila: coloca um baú ao lado de
cada cama, ergue uma casa grande (a *BigHouseMOD*) e dá emprego aos aldeões.

**As profissões (8).** Minerador (pedra e minério), lenhador (madeira e
replantio), agricultor (comida, pão), pastor (lã), fundidor (vidro, areia,
fornalha), carpinteiro (peças de madeira), pedreiro (peças de pedra) e
construtor (coloca os blocos da obra). Sete delas são contratadas logo na
fundação; as demais vagas abrem conforme a população cresce.

**Como a vila decide.** A cada 30 segundos a colônia faz uma "reunião": olha os
baús, compara com a meta (encher os baús, garantir comida e pedra), vê qual
obra está aberta e cria pedidos de trabalho. Cada aldeão livre pega um pedido
que sabe fazer. **O aldeão não decide estratégia — só executa.**

**Como os recursos circulam.** Quem coleta guarda no baú. Quem fabrica tira do
baú, fabrica e devolve. O construtor tira do baú e coloca na obra. Se falta uma
peça, a colônia procura quem saiba fazê-la, descendo a receita até a matéria
bruta.

**Como uma casa é construída.** A colônia escolhe o que construir (primeiro
casas se faltam camas, depois oficinas, depois alterna), procura um lote plano
ao lado de uma rua, abre a obra e o construtor coloca bloco por bloco.

**Quando falta material.** A obra espera até 10 minutos. Depois disso é
abandonada e pode ser retomada mais tarde. Alguns itens difíceis (corante,
linha) aparecem sozinhos; peça que não existe no bioma vai direto ao baú na 3ª
tentativa.

**Quando um aldeão fica preso.** Se ele fica parado tempo demais, descansa e
tenta de novo. Se falha muito, larga a profissão e recebe outra. Se está
cercado ou enterrado, cava, sobe ou volta pelo caminho que fez.

**Quando o jogo é salvo.** Os blocos do mundo são salvos pelo Minecraft. O
"cérebro" da colônia (quem trabalha em quê, obras, minas) **só é gravado quando
o servidor fecha normalmente** — ver P0-02.

**O que o jogador faz.** Quase nada: pode configurar profissões no Mod Menu,
cancelar uma obra pondo uma tocha de alma no canteiro, ver o que cada aldeão
faz com `/vc log`.

**Regras que nunca deveriam ser quebradas** (da Constituição, ainda válidas):
o núcleo não depende do Minecraft; nunca `@Overwrite`; nunca destruir o que o
jogador construiu; lava nunca é colocada automaticamente; a colônia decide e o
aldeão executa.

---

## 2. O que o mod faz hoje (fato, não intenção)

- **Ativo e coerente:** detecção e adoção de vila, caixa da vila, contratação
  por cota, 8 profissões, ciclo de 30 s com metas, tarefas e reserva, cadeia
  de fabricação recursiva, construção por blueprint NBT, ruas, mina em escada,
  fuga de encalhe, overlays no cliente, política de profissões no Mod Menu,
  telemetria `VC_TIME`. (SYSTEM-MAP §3)
- **Não existe:** sistema de *skills*, níveis ou XP de aldeão. A única
  "progressão" é trocar para uma ferramenta melhor do baú e as cotas que
  crescem com a população. (RULE-RECONCILIATION §1)
- **Nada visto em jogo desde 02-10.** O STATE.md lista mais de 20 correções
  "testadas, pendentes de playtest".

## 3. Arquitetura atual (resumo)

```text
Minecraft/Fabric (eventos, mixins: 2, sem @Overwrite)
      │
fabric/event  ── VillageDetectionHandler.onServerTick  (único tick do servidor)
      │            ├─ todo tick: *Work.tick (1 passo por trabalhador)
      │            ├─ 20 ticks: foco, IdleHands, SiteMarker, fuga, refeições
      │            └─ 600 ticks: detecção → ColonyCycleRunner.runColonyCycles
      │
fabric/work + fabric/integration (≈180 classes: profissões, varreduras, baús)
      │
core/ (Java puro, sem import de Minecraft — CONFIRMADO por grep e ArchUnit)
      └─ ColonyCycle, ColonyGoals, WorkAssignment, TaskService, ProfessionAssigner
data/save ── 3 PersistentState (colônias, marcas, política)
```

A separação core × Fabric **existe e é cumprida**. O problema não é falta de
camadas: é que a camada `fabric/` tem ≈180 classes com 240 coleções estáticas
e 10 ciclos de pacote congelados. Detalhes em SYSTEM-MAP.

## 4–8. Profissões, recursos, construção, simulação, persistência

Ver RULE-RECONCILIATION §1–§8 (regras com `arquivo:linha`) e SYSTEM-MAP §2–§3.
O ponto crítico de persistência está em P0-02 abaixo.

## 9. Regras

36 regras reconciliadas, **14 conflitos** entre documento e código. Os mais
sérios: a Constituição §9 ("nada surge do nada") é contrariada por cinco
mecanismos do código; a ADR-030 diz uma coisa sobre a fundação e o código faz
outra; o RULES.md marca como feita uma regra cujo método não existe.
(RULE-RECONCILIATION §9)

## 10. Problemas encontrados (resumo — tabela completa no ROADMAP §1)

| ID | Problema | Evidência | Prioridade |
|---|---|---|---|
| P0-01 | Duas linhas de desenvolvimento (Claude e Codex) divergiram da `main` e resolveram os **mesmos três pedidos** de formas incompatíveis (mina, varredura de fluidos, painel). Merge de teste: 22 conflitos, 1 quebra de compilação e 1 duplicação que o git não acusa. | CONFIRMADO (`git merge-tree`) | P0 |
| P0-02 | O registro da colônia só é copiado para o disco no `SERVER_STOPPING`. Crash, queda de energia ou processo morto perdem colônias, obras e minas desde o início da sessão, enquanto os blocos do mundo (autosave) ficam. | Mecanismo CONFIRMADO (`ServerLifecycleHandler.java:44,201-230`; único chamador de `sync`); perda em jogo PROVÁVEL | P0 |
| P1-01 | Fora o planejador, o ciclo da colônia não tem orçamento de tempo e escala linearmente com colônias vigiadas. | CONFIRMADO no código; custo real SUSPEITO | P1 |
| P1-02 | Busca do lenhador era 73% do custo do mod no Spark de 30-09; nunca remedida. | CONFIRMADO no perfil de 30-09 | P1 |
| P1-03 | Não há teste de reload real nem de "vila nova → casa inteira". | CONFIRMADO (lacuna) | P1 |
| P1-04 | 14 conflitos regra × código; decisões do autor pendentes. | CONFIRMADO | P1 |
| P1-05 | Documentação de agosto descreve uma arquitetura que não existe (29 classes inexistentes só em `Class-Architecture.md`); 4 contagens de teste diferentes circulam. | CONFIRMADO | P1 |

## 11. Dívida técnica (principal)

- Estado global: 8 serviços singleton + ≈240 coleções estáticas, limpos por
  `ServerMemory.resetAll` (79 registradores).
- 4 classes de `fabric/event` herdaram imports de uma classe antiga dividida
  (até 54 de 77 imports sem uso).
- Código morto em produção: `ColonyFocus`, `PlanAffordability` (só testes os
  usam); utilitários de teste em `src/main`: `MinerProbe`, `EnduranceReport`.
- `TestBarrier` é regra de produção com nome de teste.

## 12. Performance

Último dado: TPS 20, mod 1,7% (Spark 01-10 e 02-10). Não há crise de
performance **medida**. Há riscos de escala sem medida: ciclo sem orçamento
fora do planejador, varredura do lenhador, pathfinding Vanilla invisível ao
mod. Ver SYSTEM-MAP §6.

## 13. Testes

1.293 unitários, 1 propriedade jqwik, 564 GameTests em 91 classes (todas
registradas), 88 Python (só ferramentas). CI bloqueante em tudo. Pontos fracos:
nenhum reload real, nenhum fim-a-fim de casa, nenhum cenário de duas colônias
simultâneas; MinerGameTest com 85 testes e 5.890 linhas; zero teste
parametrizado. Ver TEST-STRATEGY.

## 14. Documentação

≈400 arquivos `.md`, dos quais ≈10 governam o projeto. `START_PROJECT.md` manda
ler 20 documentos de agosto (contradiz o CLAUDE.md); `docs/archive/design-2026-08/claude/CLAUDE-antigo.md` concorre
com o da raiz; `agent/skills` duplica `.claude/skills` com conteúdo divergente;
CLAUDE.md remete a §1 e §2 que não existem; duas ADR-025. Ver ROADMAP §3.

## 15. Comparação com projetos externos

| Projeto | Ideia útil | Adotar? |
|---|---|---|
| MineColonies | pedido de material explícito com estado terminal visível; resolvedores em ordem (mãos → baú → fabricar → natureza → jogador) | **Sim, a ideia** (não o código nem o Courier) |
| Verdant Villagers | paleta de blocos escolhida pelo que existe em volta | Sim, barato; ataca "rota na receita não existe no bioma" |
| RimWorld (WorkGiver) | ordenação de tarefas por chaves fixas e explícitas | Sim, barato |
| Fabric GameTest | `batchId`, `maxAttempts`, relatório em arquivo | Só para instabilidade de ambiente comprovada |
| Behavior Trees / GOAP | — | **Não**: reescreve o que já funciona e tem teste |

Fontes e detalhes: ROADMAP §5.

## 16–19. Simplificar, manter, refatorar, não tocar

Ver as listas TOP 10 abaixo e o ROADMAP §2 e §4.

## 20. Roadmap

Ver [RECOVERY-ROADMAP.md](RECOVERY-ROADMAP.md) §7.

---

## Tabela executiva

| Área | Estado | Problema | Prioridade | Ação | Custo | Evidência |
|---|---|---|---|---|---|---|
| Git / linhas paralelas | 🔴 | duas linhas colidem em mina, fluidos e painel | P0 | 3 decisões do autor → integração A depois B numa branch | M | `git merge-tree` 22 conflitos |
| Persistência | 🔴 | registro só gravado ao fechar | P0 | gravar também periodicamente (ex.: a cada ciclo longo) | B | `ServerLifecycleHandler.java:44,205` |
| Regras | 🟠 | 14 conflitos doc × código | P1 | autor decide cada conflito; RULES.md vira única fonte | B | RULE-RECONCILIATION §9 |
| Documentação | 🟠 | ~390 docs de ruído para ~10 úteis | P1 | arquivar com `git mv`, banner "histórico" | B | ROADMAP §3 |
| Testes | 🟡 | sem reload/fim-a-fim/2 colônias; excesso no mineiro | P1/P2 | 3 testes novos; parametrizar depois | M | TEST-STRATEGY §4 |
| Performance | 🟡 | sem medida recente por fase | P1 | playtest com Spark + breakdown do ciclo | B | SYSTEM-MAP §6 |
| Arquitetura | 🟢 | core puro ok; fabric acoplado e estático | P2 | limpar imports, mover utilitários de teste | B | SYSTEM-MAP §4 |
| CI | 🟢 | tudo bloqueante, sem continue-on-error | — | manter | — | `.github/workflows/ci.yml` |
| Dependências | 🟢 | versões fixas, sem `+`/SNAPSHOT | — | manter | — | `gradle.properties` |

## TOP 10 problemas

1. P0-01 Linhas paralelas com colisão lógica (mina 7 vs 8, dois índices de fluido, dois painéis).
2. P0-02 Registro da colônia só gravado no `SERVER_STOPPING`.
3. ~~P1-04 Constituição §9 contrariada por 5 mecanismos que criam itens do nada~~ — resolvido em 06-10 (ADR-034).
4. P1-03 Nenhum teste de reload real de servidor.
5. P1-03 Nenhum teste fim-a-fim "vila nova → casa pronta".
6. P1-01 Ciclo da colônia sem orçamento fora do planejador.
7. P1-02 Lenhador 73% do custo do mod (30-09), sem remedição.
8. P1-05 Docs de arquitetura de agosto descrevem classes que não existem.
9. P1-04 RULES.md contradiz a si mesmo sobre quem vence (regra × código).
10. P2 Pathfinding Vanilla dos encalhados não é medido pelo mod.

## TOP 10 melhorias

1. Gravar o registro da colônia periodicamente (resolve P0-02).
2. Decidir e integrar as duas linhas (resolve P0-01).
3. Uma frase de reconciliação por conflito em RULES.md (resolve P1-04).
4. Arquivar docs de agosto e START_PROJECT com `git mv`.
5. GameTest de duas colônias lado a lado.
6. Teste de reload (save → recarregar o PersistentState → comparar).
7. Medir o ciclo por fase em todo ciclo (não só ≥ 50 ms), amostrado.
8. Teste de paridade `@GameTest` × `fabric.mod.json`.
9. Pedido de material explícito com motivo visível (ideia MineColonies) — **depois** da estabilização.
10. Parametrizar MinerGameTest e LumberjackGameTest.

## TOP 10 testes mais importantes (manter a qualquer custo)

1. `ArchitectureRulesTest` (core puro, mixin só delega).
2. `ColonyGoalsTest` (54 — metas de recurso).
3. `ColonyCycleTest` (24 — ciclo e isolamento entre colônias).
4. `WorkAssignmentTest` (28 — reserva de tarefa).
5. `ProfessionAssignerTest` (27 — contratação).
6. `VillageDetectorTest` + `ColonyDetectionGameTest` (detecção).
7. `data/save/*Test` (75 — formato e migração do save).
8. `ConstructionResumeGameTest` (obra retomada).
9. `StrandedEscapeGameTest` (encalhe).
10. `ColonyEnduranceGameTest` (200 ciclos sem vazamento).

## TOP 10 coisas que podem ser simplificadas

1. Pasta `agent/skills` (duplica `.claude/skills`).
2. `claude/` (CLAUDE.md antigo em inglês + regras de agosto).
3. `START_PROJECT.md` (contradiz o CLAUDE.md).
4. 13 docs de design de agosto na raiz → `docs/archive/design-2026-08/`.
5. `TODO.md` (1.582 linhas) → ≤ 200 + arquivo.
6. `docs/proxima-sessao.md` (601 linhas de 28-09) → ≤ 80 a partir do STATE.
7. Imports mortos nas 4 classes de `fabric/event`.
8. `ColonyFocus` e `PlanAffordability` (mortos em produção).
9. `searchRadius` repetido em Farmer/Shepherd/TreeChoice; `speedOf` em 2 lugares.
10. Famílias de GameTest copiadas (arco da mina, copa da árvore, terreno do lote).

## TOP 10 coisas que NÃO devem ser tocadas agora

1. Separação core × Fabric e as regras ArchUnit estritas.
2. A política de 2 mixins sem `@Overwrite`.
3. `ColonyGoals` / `StockRules` (regra pura, bem testada, com PIT).
4. O orçamento do planejador (`PlanningBudget`, `SweepDeadline`).
5. Formato do save e `SaveMigration` — só mexer junto com P0-01 (SHAPE_VERSION).
6. CI bloqueante.
7. Arquitetura do ciclo (planejador central + execução): **não** migrar para Behavior Trees/GOAP.
8. Lógica de fuga (`ClimbOut`, `PenEscape`, `MineReturn`): vista funcionando em jogo em 01-10.
9. Versões de Minecraft/Fabric/Loom.
10. Os 564 GameTests — nenhum é apagado antes da integração das linhas e de uma rodada verde.

---

## Estado do projeto

```text
ESTADO: AMARELO
```

**Motivo.** O código é disciplinado (núcleo puro, CI bloqueante, nenhuma
classe gigante, versões fixas) e não há crise de performance medida. Mas três
coisas impedem o verde: (1) o trabalho real está espalhado em duas linhas que
colidem em lógica, então "o estado do projeto" hoje não é um só; (2) um crash
apaga o cérebro da colônia; (3) mais de 20 correções esperam jogo, e a
documentação deixa um agente ler 400 arquivos para achar os 10 que valem.
Não é vermelho: nada disso exige reescrita, e todas as correções são pequenas.

**Próxima ação única recomendada:** ~~o autor toma as três decisões de
fusão~~ — tomadas em 06-10 (D-01 salões, D-02 motor da ADR-031, D-03 painel do
Claude, D-04 emendas, D-05 código atual). Agora: **integrar as duas linhas na
`main` aplicando as decisões** (ROADMAP §7, Etapa 1).

---

## Cobertura do pedido (o que foi feito e o que não foi)

Os dois prompts pediam mais do que cabe numa auditoria só de leitura. Esta
tabela diz, sem arredondar, o que saiu.

| Pedido | Onde está | Situação |
|---|---|---|
| Fase 0 — reconhecimento, mapa de diretórios, dependências, versões | SYSTEM-MAP §1-§2 | feito |
| Fase 1 — inventário com todos os campos | SYSTEM-MAP §4 e §4b | feito; "funcionamento real" por sistema é resumo, não leitura linha a linha |
| Fase 2 — explicar para leigo | GUIA-DO-LEIGO | feito (complementado em 06-10, depois da pergunta do autor) |
| Fase 3 — regras por categoria | RULE-RECONCILIATION §1-§8 | feito |
| Fase 4 — dependências, ciclos, estado global, classes que sabem demais | SYSTEM-MAP §5 | feito |
| Fase 5 — o que saiu do controle (código e arquitetura) | SYSTEM-MAP §5, §8; ROADMAP §1 | feito; "comentários que contradizem o código" só por amostra (C-14) |
| Fase 6 — classificar **cada** teste A-G | TEST-STRATEGY §2 | **parcial: por grupo/arquivo**, não teste por teste (são 1.857) |
| Estratégia e pirâmide de testes | TEST-STRATEGY §3-§6 | feito |
| Auditoria de performance | SYSTEM-MAP §6 | feito por leitura de código e perfis antigos |
| **Linha de base nova: 1 × 4 colônias, ms por fase** | — | **NÃO FEITO** — exige playtest com Spark; vira a Etapa 3 do roadmap |
| Pesquisa externa e alternativas | ROADMAP §5 | feito; MineColonies lido só na wiki, nenhum código de mod lido |
| Metodologia própria + protocolo para agentes | ROADMAP §6 (Protocolo PASSO) | feito |
| Árvore de decisão PROBLEMA→…→PRIORIDADE | ROADMAP §4 | **parcial**: completa só para P0-02; os demais em forma de tabela |
| Opções A-E (manter → reescrever) | ROADMAP §4 | feito |
| Candidatos à remoção | SYSTEM-MAP §8; ROADMAP §3; TEST-STRATEGY §2 | feito, nada removido |
| Documentação 01-21 em arquivos separados | — | **consolidada em 6 arquivos** (o segundo prompt limitou a 5 e pediu para não criar dezenas) |
| Registro de erros com REPRODUÇÃO | ROADMAP §1 e §1b | feito |
| Classificação de cada documento Markdown | ROADMAP §3 | por grupo; ≈160 arquivos de skill tratados como dois grupos |
| Conflitos com FONTE A / B / CÓDIGO / DECISÃO | RULE-RECONCILIATION §10 | feito |
| Matriz de profissões | RULE-RECONCILIATION §1; GUIA §4 | feito |
| Análise individual das maiores classes com proposta de divisão | SYSTEM-MAP §5 | **parcial**: 5 classes, sem plano de divisão detalhado (de propósito: ROADMAP M-12 recomenda não dividir) |
| Links quebrados após reorganização | — | não se aplica ainda: nada foi movido |
