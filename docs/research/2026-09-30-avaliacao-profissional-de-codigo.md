# Avaliação profissional de código: métodos para Java e mods de Minecraft

**Data:** 2026-09-30
**Pergunta:** quais métodos profissionais avaliam lógica, arquitetura e design
de projetos Java, e de mods Fabric em particular, e quais melhorariam o
Village Colony.

**Resposta curta:** o projeto já usa o que muitos projetos profissionais não
usam (teste de mutação, Error Prone, regra de dependência como teste, GameTest
no CI, ADRs). O ganho agora está em quatro coisas:

1. **ArchUnit** para vigiar a camada `fabric`, que tem ciclos entre pacotes e
   nenhuma regra sobre isso.
2. **NullAway + JSpecify** para o erro que o compilador ainda não pega.
3. **Teste de propriedade com estado (jqwik)** para as regras de contratação,
   metas e tarefas.
4. **Funções de aptidão de mod:** orçamento de tique, servidor dedicado e
   estado estático.

---

## 1. Onde o projeto está hoje (medido em 2026-09-30)

| Medida | Valor | Como foi obtido |
|---|---|---|
| Classes de produção | 329 (196 `fabric`, 122 `core`) | `find src/main` |
| Testes | 1.196 unitários, 528 GameTests | `gradlew test`, `runGametest` |
| Teste de mutação (PIT, só `core`) | 1.576 mutações, **88% mortas**, força dos testes 95%, 113 sem cobertura | `gradlew pitest` |
| Cobertura por unitários (JaCoCo) | 34% das instruções: a camada `fabric` só roda nos GameTests, que o JaCoCo não mede | `build/reports/jacoco` |
| Error Prone | **0 avisos** na compilação | `compileJava --rerun-tasks` |
| Regra de dependência `core` ↛ `fabric` | vigiada por `DependencyRuleTest` | teste lendo `import` |
| Arquivos acima do teto de 500 linhas | **8** (o maior tem 568) | `wc -l` |
| Mapas e conjuntos estáticos mutáveis | **63** | `grep "private static final Map<… = new"` |
| Ciclos entre pacotes em `fabric` | `work ↔ integration` (58 e 10 arquivos), `work ↔ event`, `integration ↔ event`, `adapter ↔ integration` | `grep import` por pacote |

O núcleo (`core`) está em nível profissional: 88% de mutação é resultado
forte, e a literatura põe 80–85% como bom alvo para código de produção. O
risco se concentra na camada `fabric`: é onde está a maior parte do código, é
onde estão os ciclos e o estado estático, e é onde a verificação é só de
comportamento (GameTest), sem regra de estrutura.

---

## 2. Os métodos, por dimensão

### 2.1 Lógica e correção

| Método | O que mede | Estado no projeto |
|---|---|---|
| **Teste de mutação (PIT)** | se os testes detectam um erro plantado; mede o teste, não a cobertura | ✅ no CI, só `core`, sem limite mínimo |
| **Análise estática no compilador (Error Prone)** | padrões de erro conhecidos | ✅ mas como aviso (`allErrorsAsWarnings`) |
| **Análise de nulidade (NullAway + JSpecify)** | o `NullPointerException` antes de rodar | ❌ |
| **Teste de propriedade (jqwik)** | invariantes sobre entradas geradas; na versão com estado, sequências aleatórias de ações contra uma máquina de estados | ❌ |
| **SpotBugs / PMD** | padrões no bytecode e no fonte; o PMD mede complexidade cognitiva | ❌ (o Error Prone cobre boa parte) |

### 2.2 Arquitetura

| Método | O que mede | Estado |
|---|---|---|
| **ADR** (Architecture Decision Record) | a decisão e o porquê | ✅ 29 ADRs |
| **Regra de arquitetura como teste (ArchUnit)** | camadas, ciclos, nomes, "quem pode chamar quem", no bytecode | ⚠️ parcial: há um teste caseiro por `import` só para `core` |
| **Métricas de pacote de Robert Martin** (instabilidade *I*, abstração *A*, distância da sequência principal; ferramenta JDepend) | se o pacote estável é abstrato e o instável é concreto | ❌ |
| **Funções de aptidão** (*fitness functions*, da arquitetura evolutiva) | uma meta de qualidade expressa como verificação automática que roda sempre | ⚠️ implícitas: o teto de 500 linhas e o de 50 ms por tique existem como regra, não como teste |
| **ATAM** (Architecture Tradeoff Analysis Method, SEI) | trocas entre atributos de qualidade num workshop com cenários | ❌, e desproporcional para um autor só (ver §5) |

### 2.3 Design e legibilidade

| Método | O que mede | Estado |
|---|---|---|
| **Complexidade cognitiva** (SonarSource, G. Ann Campbell) | quão difícil é *entender* um método; aninhamento pesa mais que ramificação | ❌ |
| **Guia de revisão do Google** (*eng-practices*) | desenho primeiro; depois funcionalidade, complexidade e excesso de engenharia | ⚠️ o `/code-review` existe, sem roteiro próprio do projeto |
| **ISO/IEC 25010** (modelo de qualidade) | vocabulário comum: manutenibilidade = modularidade, reusabilidade, analisabilidade, modificabilidade, testabilidade | ❌; útil só como vocabulário |

### 2.4 Específico de mod Fabric

| Prática | Fonte | Estado |
|---|---|---|
| Evento antes de mixin; mixin só quando não há evento | Fabric Wiki | ✅ ADR-004 |
| `@WrapOperation` e `@ModifyExpressionValue` (MixinExtras, embutido no Loader 0.15+) no lugar de `@Redirect` e `@ModifyConstant`, que não encadeiam com outros mods | MixinExtras, Fabric Wiki | ✅ o projeto não usa `@Redirect` |
| Exportar e auditar alvos de mixin (`-Dmixin.debug.export`) | Fabric Wiki | ❌ |
| Separar cliente e servidor; nada de classe de cliente em lógica de mundo | Fabric Wiki, `fabric.mod.json` | ⚠️ sem teste de servidor dedicado no CI |
| GameTest | Mojang/Fabric | ✅ 528 |
| Perfil com Spark contra o orçamento de 50 ms por tique | Spark | ⚠️ manual, por sessão |
| Compatibilidade com mods de otimização (Lithium etc.) | prática da comunidade | ❌ |
| Regras de publicação (Modrinth: licença, vantagem injusta, **declaração de conteúdo feito com IA**) | Modrinth Content Rules | ⚠️ relevante no dia de publicar |

---

## 3. As melhores opções para este projeto, em ordem

A ordem é ganho sobre esforço, contado sobre o que o projeto **já sofreu** (as
memórias e o `Development-Log` registram os defeitos).

### 1º — ArchUnit com regras congeladas

**O que é.** Uma biblioteca de teste JUnit que lê o bytecode e verifica
regras de arquitetura: camadas, ciclos entre fatias, dependências proibidas.
O `FreezingArchRule` grava as violações que já existem num arquivo versionado
e só reprova as **novas**; a lista encolhe sozinha quando alguém corrige.

**Por que primeiro.** É a lacuna estrutural medida acima: os ciclos
`work ↔ integration ↔ event` em `fabric` crescem sem ninguém ver. Hoje o
`DependencyRuleTest` só protege `core`, e lendo texto.

**Como aplicar aqui:**

- `slices().matching("com.villagecolony.fabric.(*)..").should().beFreeOfCycles()`,
  congelada: registra os ciclos de hoje e barra os novos.
- A regra da ADR-006 (`core` não conhece Minecraft) reescrita em ArchUnit,
  pegando também nome totalmente qualificado. É o caso que o teste por
  `import` deixa passar.
- A ADR-004 como regra: toda classe em `fabric.mixin` só chama
  `fabric.brain` ou `fabric.integration` ("mixin só delega").
- "Toda classe com campo estático mutável registra `ServerMemory`". Os 63
  mapas estáticos são o risco de estado que sobrevive entre mundos, que a
  memória `estado-que-sobrevive-ao-dono` já registrou.

**Esforço:** baixo (uma dependência de teste e um arquivo de teste).

### 2º — NullAway + JSpecify, começando pelo `core`

**O que é.** Um verificador do Error Prone que reprova a compilação quando um
valor possivelmente nulo é usado sem checagem, guiado pelas anotações
`@Nullable` e `@NullMarked` da JSpecify, que o Spring 7 adotou como padrão em
2025.

**Por que.** O projeto usa `Optional` com disciplina, mas o código fala com o
Minecraft, que devolve `null` a toda hora (`world.getEntity`, chunk
descarregado, `getBlockEntity`). O modo `OnlyNullMarked` permite ligar pacote
por pacote: primeiro `core`, que já tem 95% de força de teste, e depois
`fabric.integration`.

**Esforço:** médio. O plugin `net.ltgt.errorprone` já está no build.

### 3º — Transformar os avisos do Error Prone em erro

Hoje são 0 avisos, com `allErrorsAsWarnings = true`. **Travar agora custa
nada** e impede que o primeiro aviso volte calado, como o PIT ficou parado
com o CI verde entre `78e7efc` e `7619b1d`.

**Esforço:** uma linha.

### 4º — Teste de propriedade com estado (jqwik) nas regras de colônia

**O que é.** Em vez de um cenário escrito à mão, o jqwik gera sequências
aleatórias de ações (contratar, dispensar, morrer, abrir obra, faltar
material) e confere invariantes depois de cada passo. Quando falha, reduz o
caso à menor sequência que ainda falha.

**Por que aqui.** Os defeitos mais caros deste projeto foram de **transição**,
não de estado: o rodízio de castigo por ofício, a vaga que se reabre para
quem a largou, o teste em repouso que não viu defeito de transição (memória
`castigo-por-oficio-vira-rodizio`). O jqwik é feito para isso. Invariantes
candidatos:

- a fundação nunca fica sem titular enquanto houver adulto;
- nenhuma profissão passa de uma cabeça acima da cota por causa da demanda
  (`ProfessionDemand`, de hoje);
- toda tarefa aberta tem pelo menos uma profissão capaz;
- meta de fundido sem cadeia de cru nunca é aberta (a regra de hoje).

**Esforço:** médio; roda no `core`, sem Minecraft.

### 5º — Funções de aptidão de mod

Metas que o projeto já tem por escrito, transformadas em verificação:

| Meta | Verificação |
|---|---|
| Arquivo ≤ 500 linhas | teste (ou regra ArchUnit) congelado nos 8 atuais |
| Ciclo da colônia cabe no tique | `scripts/analyze_village_log.py` reprovando quando a mediana de `Colony cycle took` passa de um teto, rodado sobre o log do playtest |
| Carrega em servidor dedicado | passo de CI que sobe `runServer` com EULA aceita e para depois do "Done", pegando classe de cliente em código comum |
| Mutação não regride | `mutationThreshold` do PIT em 85% (hoje 88%) com `withHistory` para rodar só o que mudou |

### 6º — Roteiro de revisão do projeto

Um checklist curto no `docs/` que junta o guia do Google (desenho primeiro,
depois funcionalidade, complexidade e excesso de engenharia) com as armadilhas
que este projeto já pagou:

- teste que copia o predicado de produção;
- condição impossível que deixa o teste vazio;
- "tem teste" tratado como "visto em jogo";
- chunk não carregado;
- estado estático sem `ServerMemory`.

O `/code-review` e o `gauntlet-verifier` passam a revisar contra ele.

### 7º — Complexidade cognitiva, só como relatório

PMD (regra `CognitiveComplexity`) no CI, **sem reprovar**, para apontar onde
quebrar os 8 arquivos grandes. Entra depois do ArchUnit porque o teto de
linhas já dá o sinal principal.

---

## 4. Plano de adoção

| Onda | Itens | Critério de pronto |
|---|---|---|
| 1 (1 sessão) | ArchUnit com ciclos congelados, regra do mixin e regra do `ServerMemory`; Error Prone como erro; `mutationThreshold` + `withHistory` | CI verde com as regras novas; o arquivo de violações congeladas versionado |
| 2 (1–2 sessões) | NullAway no `core` com `@NullMarked`; roteiro de revisão; servidor dedicado no CI | `core` compila sem aviso de nulidade |
| 3 (contínuo) | jqwik nas regras de contratação e metas; teto de tique no script de log; PMD como relatório; desfazer os ciclos de `fabric` um a um | a lista congelada encolhe a cada ciclo |

---

## 5. O que não vale a pena aqui

- **SonarQube servidor próprio:** caro de manter para um autor só. Se quiser o
  painel, o SonarCloud é gratuito para repositório público; mas Error Prone,
  PIT, ArchUnit e PMD já cobrem as mesmas perguntas dentro do Gradle.
- **ATAM completo:** foi feito para vários interessados negociando atributos.
  As ADRs com a seção "Consequências" já são a versão enxuta que cabe aqui.
- **Métricas CK e painéis de complexidade por classe:** geram número sem
  decisão. O que decide aqui são as regras que reprovam (ArchUnit, PIT,
  NullAway).
- **Cobertura como meta:** o JaCoCo diz o que rodou, não o que foi
  verificado. O projeto já sabe disso, e o PIT é a métrica certa.

---

## 6. Aplicado em 2026-09-30

Tudo o que roda localmente foi aplicado e executado no mesmo dia. Fica de
fora só o SonarCloud (serviço externo).

| Item | Como ficou | Resultado da execução |
|---|---|---|
| ArchUnit 1.5.1 | `ArchitectureRulesTest`: core sem Minecraft, core sem ciclo, **ciclos da `fabric` congelados**, mixin só delega, **coleção estática mutável exige `ServerMemory`** (congelada). Registro em `src/test/resources/archunit_store` | 5/5 verdes. Congelados: 10 ciclos entre pacotes da `fabric` e 10 classes com coleção estática sem registro, **todas revisadas: nenhum vazamento** (limpas por dona registrada ou tabelas fixas). Uma classe nova com `HashMap` estático plantada de propósito foi **reprovada** |
| Error Prone como erro | `allErrorsAsWarnings = false` e `-Werror` no código do mod | 0 avisos, build verde |
| NullAway 0.14.2 + JSpecify 1.0 | `@NullMarked` em todos os pacotes do `core`; modo padrão (o `JSpecifyMode` exige JDK 22+ ou Temurin) | 31 acusações no `core`: campos e parâmetros legitimamente nulos anotados `@Nullable`, `Map.get` desembrulhado trocado por `getOrDefault`, e três guardas tornadas explícitas (`Colony.observe`, `ConstructionProject.retryIfSupportChanged`, `HiringLog.record`). Nenhum defeito em produção encontrado |
| jqwik 1.9.3 (propriedade com estado) | `HiringProperties`: roteiros aleatórios de chegada, morte, demanda e contratação, com três invariantes. Tarefa própria `propertyTest` no `check` | 300 roteiros verdes. Uma mutação plantada (demanda até 3 acima da cota) foi **pega**. Achado de processo: com o `fabric-loader-junit` no classpath, o jqwik **não executa nada e a suíte fica verde**; por isso a tarefa separada |
| Teto de 500 linhas | `FileSizeRuleTest` com os 8 arquivos de hoje congelados em `architecture/oversized-files.txt` | verde. `ColonyGoals` (529) e `LumberjackWork` (523) passaram do teto com a revisão das profissões do mesmo dia e ficaram congelados assim |
| PIT com limite | `mutationThreshold = 85`, análise incremental local | 1.579 mutações, **88%** mortas, força 95% |
| PMD 7.28 (complexidade cognitiva, relatório) | `config/pmd/complexity.xml`, limite 15, sem reprovar | 44 métodos acima de 15. Piores: `BuildSiteScanner.findForFootprints` (42), `MineCuts.nextCut` (41), `ChestScanner.findFreeChest` (37), `LotLevel.flatGroundAt` (36) |
| Orçamento de tique | `analyze_village_log.py --max-slow-cycles --max-cycle-ms`, saída 3 quando estoura; 2 testes Python | No log real de 30-09 03:13 (JAR anterior): **50 ciclos acima de um tique, pior 497 ms**, mediana dos lentos 125 ms |
| Servidor dedicado | já coberto: o `runGametest` sobe um servidor sem cliente (`server()`), e classe de cliente em código comum derruba a bateria | 528/528 GameTests |
| Auditoria de mixin | já existia: `mixin.debug.export`, `verify`, `checks.interfaces` em todo run | — |
| Roteiro de revisão | `docs/technical/Roteiro-de-Revisao.md` | — |
| CI | comentários e artefatos (`pmd`, `propertyTest`) atualizados; o `build` já roda tudo | — |

**Próximos passos, pela ordem do plano:** NullAway em `fabric.integration`;
desfazer os ciclos da `fabric` um a um (o registro congelado mostra quanto
falta); quebrar os quatro métodos acima de 35 de complexidade; mais
propriedades jqwik (`ColonyGoals`, estados de `Task`).

---

## Fontes

- ArchUnit — [User Guide](https://www.archunit.org/userguide/html/000_Index.html),
  [Freezing Architecture Rules](https://deepwiki.com/TNG/ArchUnit/2.3.2-freezing-architecture-rules),
  [Loiane Groner, 2026](https://loiane.com/2026/07/architecture-testing-java-archunit/)
- NullAway e JSpecify — [gradle-nullaway-plugin](https://github.com/tbroyer/gradle-nullaway-plugin),
  [jspecify-nullaway-demo](https://github.com/sdeleuze/jspecify-nullaway-demo/blob/main/build.gradle),
  [Scott Logic](https://blog.scottlogic.com/2024/12/18/taming-nullness-in-java-with-jspecify.html)
- jqwik — [User Guide](https://jqwik.net/docs/current/user-guide.html),
  [Stateful testing](https://blog.johanneslink.net/2018/09/06/stateful-testing/)
- PIT — [Gradle plugin](https://gradle-pitest-plugin.solidsoft.info/),
  [Loiane Groner, 2026](https://loiane.com/2026/06/mutation-testing-java-pit/),
  [JAVAPRO](https://javapro.io/2026/01/21/test-your-tests-mutation-testing-in-java-with-pit/)
- Complexidade cognitiva — [whitepaper SonarSource](https://www.sonarsource.com/docs/CognitiveComplexity.pdf),
  [validação empírica](https://arxiv.org/pdf/2007.12520)
- Métricas de Martin — [Software package metrics](https://en.wikipedia.org/wiki/Software_package_metrics)
- ISO/IEC 25010 — [arc42 quality](https://quality.arc42.org/standards/iso-25010),
  [SonarSource](https://www.sonarsource.com/resources/library/iso-iec-25010-explained/);
  fitness functions: [Architectural Fitness in Practice](https://www.rw-it.consulting/wp-content/uploads/2026/01/26-01-28_SWA_Architectural-Fitness-in-Practice.pdf)
- Revisão — [Google eng-practices](https://google.github.io/eng-practices/review/),
  [What to look for](https://github.com/google/eng-practices/blob/master/review/reviewer/looking-for.md)
- Fabric e Mixin — [Modding Tips](https://wiki.fabricmc.net/tutorial:modding_tips),
  [Mixin export](https://wiki.fabricmc.net/tutorial:mixin_export),
  [Registering Mixins](https://wiki.fabricmc.net/tutorial:mixin_registration),
  [MixinExtras](https://github.com/LlamaLad7/MixinExtras)
- Desempenho — [spark](https://modrinth.com/mod/spark),
  [Spark profiler guide](https://www.sparkanalyzer.io/blog/spark-profiler-guide)
- Publicação — [Modrinth Content Rules](https://modrinth.com/legal/rules),
  [AI rules and disclosures](https://modrinth.com/news/article/ai-policy-and-disclosures/)
