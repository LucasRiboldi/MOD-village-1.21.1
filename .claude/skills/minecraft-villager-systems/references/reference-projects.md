# Projetos de referência

Como usar mods existentes como fonte de aprendizado — sem copiar arquitetura nem
tratar nenhum deles como autoridade.

## A pasta de pesquisa

O projeto pode ter uma pasta com mods de referência. Procure antes de supor que
não existe:

```bash
ls -d PesquisaFabricMOD */Pesquisa* ../Pesquisa* 2>/dev/null
find . -maxdepth 4 -name "fabric.mod.json" -not -path "*/build/*"
```

Fontes típicas: tutoriais de Fabric, `VillagerConfig`, `Easy Villagers`,
`SimpleVillagers`, `GuardVillagers`, `Create`.

**A skill não depende dessa pasta.** Se ela não existir, o método vale para
qualquer repositório clonado.

## A regra

> **Nenhum projeto de referência é "a implementação correta".**

Cada um resolveu o problema *dele*, na versão *dele*, com restrições que você não
tem. Use como:

```text
REFERÊNCIA   uma forma que funciona
COMPARAÇÃO   o leque de abordagens
EVIDÊNCIA    "é possível fazer assim"
PADRÃO       o que se repete entre eles
CONTRASTE    o que um faz e outro evita — e por quê
```

Nunca como: "eles fazem assim, então é assim que se faz."

## Antes de qualquer coisa: a versão

```bash
cd <mod>
grep -E "minecraft_version|yarn_mappings|fabric_version" gradle.properties
```

Um mod de 1.20 responde **"é possível?"**. Não responde **"como escrevo isso
hoje?"** — as APIs de aldeão mudaram: `VillagerProfession` e
`PointOfInterestType` viraram `record`, e a serialização mudou em 1.20.5.

### Inventário verificado — `PesquisaFabricMOD/`

Levantado com o comando acima. **Reconfira antes de usar** — a pasta muda.

| Projeto | MC declarado | Vale como |
|---|---|---|
| `template-mod-template-1.21.1` | **1.21.1**, loader 0.19.3 | ✅ referência direta de estrutura |
| `Create-mc1.21.1-dev` | **1.21.1** | ✅ padrões de mod grande na sua versão |
| `Fabric-Tutorial-1.21.X-main` | **1.21.11**, yarn `1.21.11+build.2` | ⚠️ 1.21.11 ≠ 1.21.1 — confira cada API |
| `SimpleVillagers-1.20` | **1.20.4** | ⚠️ pré-1.20.5 — Data Components e serialização mudaram |
| `VillagerConfig-main` | templado (`[VERSIONED]`) | ⚠️ confira o build real |
| `easy-villagers-fabric-master` | não declarado no `gradle.properties` | ⚠️ procure em outro arquivo |
| `guardvillagers-main` | esquema próprio (`26.2.0`) | ⚠️ confirme loader e versão antes |

> Repare que **apenas dois** batem com 1.21.1. Os outros mostram **que uma
> abordagem é possível** — não como escrevê-la hoje.
>
> `SimpleVillagers-1.20` é o caso mais perigoso: é anterior a 1.20.5, então todo
> código de dados de item e de serialização está numa forma que não existe mais.
> Copiar dali compila mal ou, pior, compila e corrompe save.

## Os dois comandos que mais rendem

```bash
# quais APIs Fabric o mod usa — o resumo honesto do que existia e serviu
grep -rn "net.fabricmc.fabric.api" src/main/java | sed 's/.*import //' | sort -u

# o que ele realmente faz ao Vanilla
grep -rn "@Inject\|@Redirect\|@Overwrite\|@ModifyVariable\|@Accessor" src/main/java
```

Dois comandos, e você já sabe **o degrau da escada** em que o mod parou e o
**risco de compatibilidade** que ele carrega.

## Perguntas específicas de aldeão

Ao ler um mod de aldeão, procure por:

```text
[ ] como ele instala comportamento?     Mixin em initBrain? captura? substituição?
[ ] registra POI próprio?
[ ] registra profissão própria?
[ ] registra memórias ou sensores?
[ ] remove alguma task Vanilla?          ← se sim, risco alto
[ ] onde guarda o estado?                memória? static? PersistentState?
[ ] como trata morte E conversão?
[ ] como trata chunk descarregado?
[ ] como coordena vários aldeões?
```

```bash
grep -rn "setTaskList\|initBrain\|MemoryModuleType\|PointOfInterest\|VillagerProfession" src/main/java | head -30
```

## As três abordagens que costumam aparecer

Quando você compara vários mods de aldeão, as soluções tendem a cair em três
famílias:

**A — Capturar o aldeão num bloco.** Ele vira estado de uma BlockEntity e a IA
Vanilla deixa de rodar. Simples e previsível; **incompatível** com qualquer mod
que espere um aldeão normal no mundo.

**B — Acrescentar task ao Brain.** Mixin mínimo em `initBrain`, task própria,
Vanilla intacto. Mais trabalho, muito mais convivência.

**C — Substituir o Brain.** Controle total, risco máximo, congela a versão.

`[INFERÊNCIA]` Mods que priorizam convivência escolhem B. Mods que priorizam
controle total escolhem A ou C, e pagam em compatibilidade.

Ver `examples/complete-villager-system.md`.

## Ler a comparação

Padrões que valem mais que qualquer mod individual:

- **Todos usam o mesmo mecanismo Vanilla** → é o caminho previsto.
- **Um usa muito menos Mixin** → achou um extension point que os outros não
  viram. **Investigue esse.**
- **O mais novo é mais simples** → a API ganhou suporte no caminho; os antigos
  carregam contorno histórico, não sabedoria.
- **Todos evitam a mesma coisa** → há uma armadilha ali. Descubra qual antes de
  ser o primeiro a cair.

O último costuma ser o achado mais útil: se nenhum mod usa
`getNavigation().startMovingTo` em aldeão, é porque não funciona.

Método completo em `minecraft-code-research/references/comparison-analysis.md`.

## Licença

```bash
ls LICENSE* COPYING* 2>/dev/null && head -5 LICENSE*
```

Ideia, arquitetura e abordagem são livres para aprender. **Trecho de código
carrega a licença da origem** — MIT e Apache-2.0 exigem atribuição; GPL/LGPL têm
exigências sobre o resultado.

Na dúvida: **leia, entenda, escreva o seu.** Além de resolver a licença, é o que
garante que você entendeu — e o entendimento é o que você realmente queria.

## Compatibilidade — os vizinhos

Estes mesmos projetos são os **vizinhos prováveis** do seu mod:

```text
mods de aldeão      disputam VillagerEntity, Brain, POI, profissões, trades
mods de performance reescrevem caminhos quentes, inclusive IA e POI
mods grandes        muitos touchpoints
```

Analisar os mixins deles é, ao mesmo tempo, aprendizado e análise de risco. Ver
`fabric-development/references/compatibility.md`.

## Registrar

Uma análise por mod, em `docs/research/mods/`, usando
`minecraft-code-research/templates/mod-analysis.md`. Havendo três ou mais
resolvendo o mesmo problema, feche com uma matriz — ela vale mais que as análises
somadas.
