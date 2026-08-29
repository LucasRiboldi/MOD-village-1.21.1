# Workflow — desenvolver uma feature

Modo **FEATURE**. Para funcionalidade normal: nem ajuste de uma linha, nem
sistema que vai durar anos.

Se for ajuste pequeno → modo SMALL, pule para o passo 5.
Se for sistema grande → `new-system.md`.

---

## 1. Entender

```text
OBJETIVO       o que o jogador vai poder fazer, em uma frase
SCOPE          o que entra
NON GOALS      o que explicitamente não entra
SISTEMA        qual sistema do Minecraft está envolvido
VERSÃO         MC, mappings, Fabric API — do gradle.properties de hoje
DEPENDÊNCIAS   o que precisa existir antes
```

Se você não consegue escrever o objetivo em uma frase, ainda não entendeu o
pedido. Pergunte antes de escrever código — cinco minutos de conversa economizam
uma tarde de implementação errada.

## 2. Verificar a pesquisa

```bash
ls docs/research docs/decisions docs/architecture 2>/dev/null
grep -ril "<sistema>" docs/ 2>/dev/null | head
```

| Pergunta | Se "não" |
|---|---|
| O sistema Vanilla já foi pesquisado? | acione `minecraft-code-research` |
| Existe ADR cobrindo isso? | pode ser preciso criar |
| Existe implementação anterior no projeto? | procure antes de duplicar |
| Alguma hipótese não validada sustenta o plano? | valide antes |
| Há risco conhecido registrado? | leia |

**Não repesquise o que já está documentado.** E não implemente sobre hipótese
não validada — se o plano depende dela, ela é o primeiro trabalho.

## 3. Plano

Preencha `templates/feature-plan.md`. As seções que decidem o resto:

```text
DATA         o que a feature lê
STATE        o que ela guarda — e QUEM É O DONO
OWNERSHIP    servidor? entidade? bloco? mundo?
LIFECYCLE    quando nasce, quando morre
CLIENT       o que o jogador vê
SERVER       o que é autoritativo
PERSISTENCE  o que sobrevive ao restart
NETWORKING   o que precisa atravessar
```

**Corte o que não se aplica**, com o motivo. Feature sem networking é normal;
feature que "esqueceu" networking é bug.

## 4. Arquitetura

Pergunta central: **qual é a menor arquitetura correta?**

```text
[ ] Isto já é resolvido por um sistema Vanilla?
[ ] Cabe num registro existente?
[ ] É data-driven (tag, loot table, recipe)?
[ ] A Fabric API cobre?
[ ] Precisa mesmo de classe nova? Quantas?
[ ] Precisa mesmo de Mixin?  → mixin-workflow.md
```

Não crie `Manager`/`Service`/`Factory` por reflexo. Abstração se paga com
repetição, variação, extensibilidade real, testabilidade ou separação de domínio
— não com expectativa.

Onde o código mora: `references/project-architecture.md`.
Em que ponto do ciclo de vida: `references/mod-lifecycle.md`.

## 5. Implementar o núcleo mínimo

**A menor coisa que já faz algo.** Não a feature inteira.

```bash
./gradlew build
```

Compile agora, com cinco arquivos novos — não com cinquenta. O compilador aponta
o lugar quando o lugar é pequeno.

## 6. Integrar, uma peça por vez

Acrescente **só o que a feature exige**, compilando entre as peças:

```text
REGISTRIES   → build
EVENTS       → build
PERSISTENCE  → build
NETWORK      → build
RESOURCES    → build
MIXINS       → build
```

Referências por peça: `registration.md`, `fabric-events.md`, `persistence.md`,
`networking.md`, `datagen-and-resources.md`, `mixin-development.md`.

## 7. Resources

A dimensão mais esquecida. Código certo e incompleto:

```text
JAVA OBJECT → REGISTRY → IDENTIFIER → RESOURCE
```

```text
[ ] lang            senão aparece "block.mymod.foo"
[ ] modelo / blockstate / textura   senão vira cubo preto e rosa
[ ] loot table      senão o bloco não dropa nada
[ ] recipe          se aplicável
[ ] tags            ferramenta correta, comportamento condicional
```

## 8. Compilar e rodar

```bash
./gradlew build
./gradlew runClient
./gradlew runServer     # ← onde bugs de side aparecem
```

**`runServer` não é opcional.** É lá que classe de cliente vazada em código comum
lança `NoClassDefFoundError`, e onde autoridade de cliente aparece.

## 9. Testar

```bash
./gradlew runGametest
```

Escolha os níveis proporcionais ao risco (`references/testing.md`):

```text
1. compila
2. o jogo inicia
3. a feature funciona
4. save/load — fechar e reabrir o mundo
5. multiplayer
6. regressão
7. performance
```

Nível 4 pega o bug de persistência mais comum: falta de `markDirty`.

## 10. Validar

```text
[ ] FUNCIONALIDADE   faz o que o objetivo dizia
[ ] EDGE CASES       alvo sumiu, chunk descarregou, jogador interrompeu
[ ] CLIENT/SERVER    checklists/client-server.md
[ ] PERSISTÊNCIA     fechou e reabriu, o estado voltou
[ ] PERFORMANCE      frequência × população, com números
[ ] COMPATIBILIDADE  classificada LOW/MEDIUM/HIGH
[ ] LOGS             sem spam, sem exceção engolida
```

## 11. Documentar

- `templates/implementation-summary.md` como resumo de entrega
- descobriu algo do Vanilla → `docs/research/`
- decisão que dura → ADR
- `research-status.md` atualizado, se havia pesquisa aberta

## Fechamento

Rode `checklists/feature.md`.

**Relate o que foi verificado rodando, separado do que apenas tem teste
escrito.** São coisas diferentes e a diferença importa. Se alguma etapa ficou
pela metade, diga qual, por quê, e o que fica para depois — etapa não concluída
declarada é informação; etapa não concluída silenciosa é dívida escondida.
