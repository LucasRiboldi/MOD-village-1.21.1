# Testes comportamentais — auditoria e rede de segurança

Este diretório existe para uma pergunta só: **o aldeão continua trabalhando
depois da próxima alteração?**

Auditoria de 2026-09-09, commit `3e367d5`. O que está aqui foi **medido**, não
presumido; onde não houve medição, está escrito que não houve.

---

## 1. Baseline executada

| suíte | comando | resultado |
|---|---|---|
| unidade | `./gradlew test --rerun-tasks` | **669 testes, 59 classes, 0 falhas** |
| gametest | `./gradlew runGametest --rerun-tasks` | **269 testes, 30 classes registradas** |

**A bateria de gametest não é confiavelmente verde.** Em 12 execuções seguidas na
mesma máquina e no mesmo commit, **3 falharam** — sempre o mesmo teste. Ver
[known-failures.md](known-failures.md), KF-001.

Isto é o achado mais importante da auditoria. Uma rede de segurança que passa
75% das vezes não distingue "eu quebrei algo" de "a bateria oscilou", e é
exatamente essa distinção que o resto do sistema depende para existir.

Ambiente: Minecraft 1.21.1, Fabric Loader 0.19.5, Java 21 (JDK 21.0.12),
mod 0.3.0.

---

## 2. Arquitetura encontrada

165 arquivos em `src/main`, divididos em duas camadas com uma regra de dependência
verificada por teste (`architecture/DependencyRuleTest`):

```
core/     modelo e regras, sem Minecraft — testável fora do jogo
fabric/   integração: Brain, mundo, baús, blocos, eventos
```

`core` não conhece `net.minecraft`. É por isso que 669 testes de unidade rodam
sem subir servidor, e é o ativo mais valioso do projeto para regressão barata.

---

## 3. As sete profissões

Declaradas em `core/worker/model/ProfessionType`, com capacidade e ferramenta em
`ProfessionRegistry`:

| profissão | executor | o que produz |
|---|---|---|
| `LUMBERJACK` | `fabric/work/LumberjackWork` | tora |
| `MINER` | `MinerWork` | pedra, areia, minério, carvão |
| `FARMER` | `FarmerWork` | colheita e plantio |
| `SHEPHERD` | `ShepherdWork` | lã |
| `SMELTER` | `SmelterWork` | vidro, lingote |
| `MANUFACTURER` | `ManufacturerWork` | tábua, porta, tocha, vidraça, descascado |
| `BUILDER` | `BuilderWork` | assenta a obra |

---

## 4. O ciclo autônomo real

O modelo do mandato foi comparado com o código. O que existe:

```
VillageDetectionHandler (evento de tick do servidor)
  └─ por colônia, a cada CYCLE_TICKS = 600
       ColonyGoals.of(...)          o que a colônia quer ter
       ResourceDemand.deficit(...)  o que falta
       ColonyCycle.run
         ├─ cancelSatisfied         tira da fila o pedido sem motivo
         ├─ requestMissing          abre tarefa por mão capaz
         └─ WorkAssignment.assign   reserva tarefa para trabalhador
       ConstructionPlanner.plan     abre obra, se houver lote e construtor
       <Profissao>Work.run          despacha o trabalho do ciclo
```

E por **tique**, não por ciclo:

```
<Profissao>Work.tick(world)
  └─ por trabalho aberto
       sem alvo   → busca (orçamento global, ver §6)
       com alvo   → anda / executa / deposita
       guardas    → WorkStall (imobilidade) e stall (travamento)
```

**Diferença relevante para o mandato:** não há máquina de estados explícita com
os nomes `IDLE/SEARCHING/...`. O estado vive em campos do `Job` de cada
profissão (`target`, `progress`, `stalled`, `stall`) e no `TaskState` do
`core`. Criar uma segunda máquina de estados só para os testes é o que a
seção 4 do mandato proíbe — a observabilidade já existe pelo relatório por
profissão (`MinerReport`, `LumberjackReport`, ...).

---

## 5. Recuperação de falhas — o que já existe

O projeto já tem os mecanismos que o mandato pede, e eles são a razão de vários
defeitos terem sido diagnosticáveis:

| mecanismo | onde | o que impede |
|---|---|---|
| guarda de travamento | `stalled >= STALL_LIMIT` (2400) | andar para sempre sem chegar |
| guarda de imobilidade | `WorkStall` (300) | ficar parado com tarefa na mão |
| memória de recusa | `TreeMarks`, `MineDigging` | reencontrar o mesmo alvo ruim |
| escada de prazos | `TreeMarks.memoryFor` | a segunda recusa custar como a primeira |
| paciência da obra | `PatienceClock` (20 ciclos) | obra parada segurar a vila |
| `IdleLog` | por assunto e motivo | log repetir o mesmo silêncio |

**O ponto cego medido em jogo (2026-09-09):** os dois guardas não pegam quem
**gira no próprio eixo**. `still 0/300` com `stall` subindo até 2400 — ele se
mexe, então a imobilidade não conta; ele não chega, então só o travamento o
solta, dois minutos depois. Corrigido na origem (`MinerReach.IN_THE_PASSAGE`),
mas a **assinatura** vale como padrão de diagnóstico.

---

## 6. Riscos de regressão identificados

Ordenados por quanto custam quando quebram.

1. **Orçamento global de busca.** `SEARCHES_PER_TICK = 1` no mineiro e no
   lenhador é do **servidor inteiro**, não da colônia. Vale em jogo e distorce
   a bateria, que roda ~18 cenários de mineiro em paralelo. É a raiz do KF-001.

2. **Ordem de mapa como contrato.** `Map.copyOf` devolve mapa sem ordem e
   embaralhado por execução. Já causou um defeito real (a prioridade do
   fabricante virou sorteio). Onde a ordem importa, `Collections.unmodifiableMap`.

3. **Estado estático entre testes.** `TreeMarks`, `MineClaims`, `TestBarrier`,
   `FarmPlans` e os registros do mod são estáticos e vivem enquanto o servidor
   vive. Gametest que não limpa contamina o seguinte.

4. **Relógio do mundo compartilhado.** 26 chamadas a `setTimeOfDay` na bateria;
   `WorkHoursGameTest` usa horários diferentes dos demais. Os guardas só contam
   em expediente.

5. **Registro silencioso de gametest.** Classe fora de
   `src/gametest/resources/fabric.mod.json` some da bateria, e ela continua
   dizendo "todos passaram".

6. **Uma passagem, um recurso.** Vários executores param no primeiro item que
   conseguem produzir/coletar, então a ordem da lista é a prioridade real.

---

## 7. Lacunas de cobertura

Medido, não estimado.

- **Persistência (§22 do mandato): não coberta.** Não há teste de
  salvar → parar → carregar → continuar. O `data/save` existe e o log de jogo
  mostra `Saved 19 colonies with 338 workers`, mas nada afirma que a tarefa em
  curso sobrevive ao restart.
- **Ciclo longo (§23): não coberto.** O teste mais longo é de centenas de
  tiques. Nenhum mede degradação ao longo de muitos ciclos.
- **Deadlock entre profissões (§19): não coberto** por teste. Dois casos reais
  foram achados **em jogo**, não pela bateria: a roça sem lote parando toda
  construção, e o fabricante que nunca descascava.
- **Multi-agente (§18): parcial.** Há testes com dois trabalhadores
  (`lumber_two_workers`, disputa de ramal na mina), mas nenhum com dezenas.

Estas lacunas **não devem ser preenchidas antes do KF-001**. Teste novo sobre
bateria instável mede ruído.

---

## 8. Próximo passo recomendado

Nesta ordem, e o motivo é a dependência entre eles:

1. **Corrigir o KF-001.** Sem bateria confiável, nada abaixo é verificável.
2. **Teste de persistência**, que é a maior lacuna e protege o que o jogador
   mais percebe — voltar ao mundo e a vila continuar trabalhando.
3. **Teste de ciclo longo**, uma profissão de cada vez.
4. Só então escalar para muitos aldeões.
