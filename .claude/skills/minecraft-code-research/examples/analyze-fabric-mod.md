# Exemplo — analisar um mod Fabric externo

**Pedido:**

> "Tem uma pasta `PesquisaFabricMOD` com uns mods de aldeão. Dá uma olhada e me
> diz como eles fazem o aldeão trabalhar."

O pedido é largo. A primeira coisa a fazer é estreitá-lo — largo demais produz
leitura infinita sem conclusão.

---

## Fase 0 — Estreitar o pedido

"Como eles fazem o aldeão trabalhar" pode significar cinco coisas. A pergunta que
vale é a que muda uma decisão nossa:

```text
PERGUNTA   Como outros mods fazem o aldeão executar trabalho customizado sem
           quebrar a IA Vanilla — e qual degrau da escada eles usaram?
VERSÃO     nossa: 1.21.1 · Yarn 1.21.1+build.3
MODO       FEATURE
```

Escrita antes de abrir qualquer arquivo. É ela que diz quando parar.

## Localizar as fontes

```bash
ls -d PesquisaFabricMOD */Pesquisa* ../Pesquisa* 2>/dev/null
find . -maxdepth 4 -name "fabric.mod.json" -not -path "*/build/*"
```

Se a pasta não existir, o método é o mesmo para qualquer repositório clonado —
a skill não depende dela.

## Inventário — 5 minutos por mod

Para **cada** mod, antes de ler uma linha de lógica:

```bash
cd <mod>
grep -E "minecraft_version|yarn_mappings|loader_version|fabric_version" gradle.properties
cat src/main/resources/fabric.mod.json
find . -name "*.mixins.json" -not -path "*/build/*" -exec cat {} \;
ls LICENSE* 2>/dev/null
```

O resultado típico é uma tabela assim:

| Mod | MC | Mappings | Mixins | Licença |
|---|---|---|---|---|
| Fabric Tutorial | 1.21.x | Yarn | 0 | MIT |
| VillagerConfig | <ver> | <ver> | <n> | <lic> |
| Easy Villagers | <ver> | <ver> | <n> | <lic> |

**A coluna de versão decide o peso de cada linha.** Um mod de 1.20 responde "é
possível?" mas não responde "como escrevo isso hoje".

## O comando que mais rende

A lista de imports da Fabric API é o resumo honesto do que o mod usa — e portanto
de quais APIs existiam e serviram:

```bash
grep -rn "net.fabricmc.fabric.api" src/main/java | sed 's/.*import //' | sort -u
```

E os mixins são o mapa do que ele realmente faz ao Vanilla:

```bash
grep -rn "@Inject\|@Redirect\|@Overwrite\|@ModifyVariable\|@Accessor\|@WrapOperation" src/main/java
```

Dois comandos, e você já sabe o degrau da escada em que o mod parou e o risco de
compatibilidade que ele carrega.

## Localizar a feature

Entre pelo que o jogador vê, não pelo nome que você imagina:

```bash
grep -rn "work\|job\|profession" src/main/resources/assets/*/lang/en_us.json | head
grep -rln "VillagerEntity\|VillagerProfession\|PointOfInterest" src/main/java | head
```

Do arquivo, ache o **entry point**: registro, evento ou Mixin. Isso já classifica
a abordagem.

## Seguir o fluxo — e parar

```text
TRIGGER → ENTRY → VALIDATION → DECISION → ACTION → STATE
```

**Pare quando a sua pergunta estiver respondida.** Não continue "para completar":
um mod grande tem centenas de classes e você precisa de cinco.

## Padrões que costumam aparecer

Ao comparar mods de aldeão, três abordagens distintas costumam surgir:

**A — Capturar o aldeão num bloco.** O aldeão vira estado de uma BlockEntity e a
IA Vanilla deixa de rodar. Simples e previsível; incompatível com qualquer mod que
espere um aldeão normal no mundo.

**B — Acrescentar task ao Brain.** Mixin mínimo em `initBrain`, task própria,
Vanilla intacto. Mais trabalho, muito mais convivência.

**C — Substituir o Brain inteiro.** Controle total, risco máximo. Congela a versão
e briga com todo mundo.

A leitura da matriz é o produto real desta análise:

> `[INFERÊNCIA]` Mods que priorizam convivência escolhem B. Mods que priorizam
> controle total escolhem A ou C — e pagam em compatibilidade.

## Registrar

Um `templates/mod-analysis.md` por mod, em `docs/research/mods/`. Havendo três ou
mais resolvendo o mesmo problema, feche com `templates/comparison.md` — a matriz
vale mais que as análises individuais somadas.

## Licença

```bash
ls LICENSE* COPYING* 2>/dev/null && head -5 LICENSE*
```

Ideia, arquitetura e abordagem são livres para aprender. **Trecho de código
carrega a licença da origem** — MIT e Apache-2.0 exigem atribuição; GPL/LGPL têm
exigências sobre o resultado.

Na dúvida: leia, entenda, escreva o seu. Além de resolver a licença, é o que
garante que você entendeu.

## Conclusão típica

**Resposta:** as três abordagens existem; B (task acrescentada ao Brain) é a que
preserva o Vanilla e convive com outros mods.

**Recomendação:** B, pelo critério declarado do projeto (compatibilidade acima de
controle total).

**Do que abrimos mão:** controle total sobre a rotina do aldeão. Ele continua
dormindo, comendo e socializando quando a agenda Vanilla mandar.

**O que faria mudar de ideia:** se o design exigir que o aldeão ignore a agenda
Vanilla por completo, B deixa de bastar.

---

## O que este exemplo demonstra

1. **Estreitar o pedido é o primeiro trabalho.** "Dá uma olhada" não tem critério
   de parada; a pergunta escrita tem.
2. **Dois comandos entregam o essencial** — imports da Fabric API e lista de
   Mixins dizem o degrau e o risco antes de ler lógica.
3. **A versão de cada mod pondera cada linha da tabela.**
4. **Nenhum mod é autoridade.** Cada um resolveu o problema dele, com restrições
   que você não tem.
5. **A matriz é o produto**, não as análises individuais.
