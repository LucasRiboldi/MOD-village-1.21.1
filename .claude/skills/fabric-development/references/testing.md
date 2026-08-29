# Testes

**"Tem teste" e "foi verificado rodando" são coisas diferentes.** As duas entram
no relato, separadas. Um mod com cobertura alta e nunca aberto no jogo não está
testado.

## Os sete níveis

Escolha proporcionalmente ao risco. Nem toda mudança precisa de todos.

```text
1. COMPILE       ./gradlew build
2. GAME START    ./gradlew runClient · runServer
3. FEATURE       a funcionalidade faz o que promete
4. SAVE / LOAD   fechar e reabrir o mundo
5. MULTIPLAYER   cliente conectando a servidor dedicado
6. REGRESSION    o que funcionava continua
7. PERFORMANCE   com carga realista
```

| Mudança | Níveis mínimos |
|---|---|
| texto, config | 1, 2 |
| item novo | 1, 2, 3 |
| bloco com estado | 1–4 |
| Mixin | 1–3, 6 + gametest |
| persistência | 1–4, 6 |
| networking | 1–3, **5** |
| IA | 1–3, 6, 7 |
| migração de versão | todos |

O nível 4 pega o bug de persistência mais comum — `markDirty` esquecido. É barato
e quase nunca é feito.

## Testes de unidade

Valem para lógica que **não depende do Minecraft**. Se o projeto separa um núcleo
de domínio (`project-architecture.md`), ele é testável em milissegundos:

```groovy
dependencies {
    testImplementation platform("org.junit:junit-bom:${project.junit_version}")
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test { useJUnitPlatform() }
```

```bash
./gradlew test
```

Regra de domínio complexa testada assim é incomparavelmente mais barata de
evoluir do que a mesma regra acoplada a `ServerWorld`.

## Gametest — a bancada de verdade

Sobe um servidor headless, monta a estrutura, roda as afirmações e **falha o
build**. É reprodutível, o que uma sessão de jogo manual não é.

### Sourceset separado — e por quê

```groovy
sourceSets {
    gametest {
        compileClasspath += sourceSets.main.compileClasspath + sourceSets.main.output
        runtimeClasspath += sourceSets.main.runtimeClasspath + sourceSets.main.output
    }
}

loom {
    runs {
        gametest {
            server()
            name = 'Game Test'
            source sourceSets.gametest
            vmArg '-Dfabric-api.gametest'
            vmArg "-Dfabric-api.gametest.report-file=${layout.buildDirectory.get()}/gametest-report.xml"
            runDir 'build/gametest'
        }
    }
}
```

Com `fabric.mod.json` **próprio** para o sourceset de teste:

```json
{
  "id": "meumod-gametest",
  "entrypoints": { "fabric-gametest": ["com.exemplo.gametest.MeuGameTest"] }
}
```

**O motivo é concreto:** um servidor dedicado carrega o entrypoint
`fabric-gametest` no boot. Se ele apontar para classes que não estão no jar
publicado, o servidor cai antes de o mod iniciar. Sourceset separado mantém o
teste fora do jar de release.

### Escrever um

```java
public class MinhaGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "meu_lote")
    public void oQueDeveAcontecer(TestContext context) {
        BlockPos pos = new BlockPos(2, 2, 2);
        context.setBlockState(pos, Blocks.STONE.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(pos);      // relativo → absoluto

        context.assertTrue(condicao, "o que eu esperava, e não aconteceu");
        context.complete();
    }
}
```

```bash
./gradlew runGametest
```

### O que morde

```text
[ ] coordenadas são RELATIVAS à estrutura — use context.getAbsolutePos antes de
    falar com o ServerWorld. Misturar dá falha silenciosa.
[ ] o mundo de teste é VAZIO — sem vila gerada, sem estruturas, sem bioma real.
    O que depende de worldgen NÃO é testável assim.
[ ] context.complete() no fim, senão o teste expira
[ ] o nome do batchId agrupa; testes do mesmo lote compartilham o mundo
```

**Declare o que o gametest não cobre.** "A metade 'vila original' da regra não é
exercitada, porque o mundo de teste não tem vila gerada" é informação honesta —
esconder isso faz um teste verde significar menos do que parece.

## O teste que prova uma correção

Para bug: escreva um gametest que **falha antes da correção e passa depois**. É a
melhor prova disponível de que você consertou a coisa certa — e a proteção contra
o mesmo bug voltar.

## O que o gametest não alcança

```text
worldgen, vila gerada, estrutura
sessões longas, acúmulo, vazamento
vários jogadores
interação com outros mods
```

Para isso, sessão de jogo com instrumentação: log com prefixo do mod, em ponto
único, dizendo **estado** — não "passei aqui". Uma variável por vez, observação
escrita antes de interpretar.

E o resultado carrega a marca: **reproduzido uma vez não é reprodutível.** Diga
quantas vezes observou.

## Verificar em jogo

```text
[ ] runClient — o jogo abre, o conteúdo aparece
[ ] runServer — o servidor dedicado sobe        ← não é opcional
[ ] cliente conecta ao servidor dedicado
[ ] criar mundo → usar a feature → fechar → reabrir → o estado voltou
[ ] mundo da versão anterior do mod abre
```

## Relatar honestamente

```text
VERIFICADO RODANDO   o que você executou e observou, com o resultado
TEM TESTE ESCRITO    coberto, mas não executado agora
NÃO VERIFICADO       o que ficou de fora, e por quê
```

**Nunca diga que passou sem ter executado.** Se o build falhou, diga com a saída.
Se um nível foi pulado, ele entra em "não verificado" — não some da lista.

## Checklist

```text
[ ] níveis escolhidos proporcionalmente ao risco
[ ] ./gradlew build passa
[ ] ./gradlew runGametest passa
[ ] runClient e runServer verificados
[ ] save/load verificado
[ ] o que o gametest não cobre está declarado
[ ] correção de bug tem teste que falhava antes
```

Detalhamento em `checklists/testing.md`. Plano em `templates/test-plan.md`.
