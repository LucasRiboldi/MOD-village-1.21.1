# Workflow — corrigir um bug

A regra que organiza tudo: **não corrija sintoma antes de encontrar a causa.**

Corrigir sintoma é atraente porque funciona — o comportamento visível some. O que
não some é a causa, que reaparece noutro lugar, agora com um patch por cima
disfarçando o rastro.

---

## 1. Documentar antes de tocar em código

Preencha `templates/bug-report.md`:

```text
EXPECTED        o que deveria acontecer
ACTUAL          o que acontece
REPRODUCTION    passos mínimos
ENVIRONMENT     singleplayer / servidor dedicado / cliente+servidor
VERSION         MC, Fabric, mappings, versão do mod
MODS INVOLVED   outros mods presentes
LOGS            trecho relevante
SUSPECTED       qual sistema, e por quê
```

Escrever isto força a separar **observação** de **interpretação** — que é onde a
depuração costuma sair do trilho.

## 2. Reproduzir

**Não corrija o que você não conseguiu reproduzir.** Sem reprodução você não tem
como saber se consertou; só como saber que o comportamento mudou.

Se não reproduzir, o trabalho agora é reproduzir:

```text
Qual ambiente? singleplayer esconde bug de side.
Quantos jogadores?
Quanto tempo de sessão?
Quais outros mods?
Quantas entidades?
O mundo é novo ou tem save antigo?
```

Bug que só aparece com save antigo é quase sempre migração de formato.
Bug que só aparece em multiplayer é quase sempre autoridade ou sincronização.
Bug que só aparece depois de horas é quase sempre acúmulo, vazamento ou expiração.

## 3. Minimizar

Reduza até o menor caso que ainda falha:

```text
menos mods → menos entidades → menos passos → mundo novo → uma feature
```

Cada elemento removido que **não** faz o bug sumir é um suspeito eliminado. Cada
um que faz o bug sumir é o rastro.

## 4. Identificar o sistema

```text
É lógica do mod?
É integração com Vanilla?
É Mixin?
É persistência?
É client/server?
É performance?
É outro mod?
É versão/mapping?
```

Se o sistema Vanilla envolvido não é conhecido, **pare e pesquise**
(`minecraft-code-research`). Depurar contra um sistema que você não entende é
tentativa e erro cara.

## 5. Rastrear estado, não código

A pergunta que mais resolve: **o que o programa acha que sabe?**

```text
Qual o valor real do estado no momento da falha?
Quem escreveu esse valor? Quando?
Alguém deveria ter escrito e não escreveu?
O valor está certo e a leitura está errada?
```

Muito bug "de lógica" é, na verdade, estado que nunca foi escrito. Ver
`references/debugging.md`.

Sinais rápidos por sintoma:

| Sintoma | Suspeite primeiro |
|---|---|
| esquece tudo ao reabrir o mundo | falta `markDirty`, ou par escrita/leitura ausente |
| funciona em SP, quebra em MP | autoridade ou sincronização |
| `NoClassDefFoundError` no servidor | classe de cliente em código comum |
| trava ao carregar chunk | `getBlockState` forçando carga dentro do tick |
| some ao trocar de save | estado em `static` |
| some quando o jogador se afasta | chunk descarregado tratado como ausência |
| aldeão parado sem fazer nada | memória do Brain nunca escrita |
| some após atualizar o MC | mapping/assinatura/semântica mudou |

## 6. Causa raiz

Escreva a causa em uma frase, **em termos de mecanismo**:

```text
✗ "o registro estava errado"
✓ "WorkerService.remove nunca era chamado, porque a única fonte considerada
   (varredura) não distingue morte de chunk descarregado"
```

A segunda diz o que consertar **e** o que mais pode estar quebrado pela mesma
razão. A primeira não diz nada.

## 7. Correção mínima

A menor mudança que resolve a causa.

```text
[ ] resolve a CAUSA, não o sintoma
[ ] não mistura refatoração
[ ] não mistura feature nova
[ ] não muda comportamento além do bug
```

Se a correção certa exige refatoração, faça em **dois passos**: corrija primeiro,
refatore depois (`refactor.md`). Misturado, ninguém sabe qual metade quebrou o
que vier a quebrar.

### Corrigir parcialmente é armadilha

Se a causa tem dois caminhos, cobrir um só produz uma correção que **passa nos
testes e falha em jogo**. Exemplo real: escutar `AFTER_DEATH` sem
`MOB_CONVERSION` corrige a morte por dano e deixa a zumbificação — que é o caso
mais comum.

Pergunte sempre: **por quantos caminhos essa causa se manifesta?**

## 8. Testar

```text
[ ] o caso reportado não acontece mais
[ ] todos os caminhos da mesma causa foram cobertos
[ ] teste automatizado do caso, quando possível
[ ] o comportamento correto anterior continua
[ ] save antigo continua carregando
```

```bash
./gradlew build && ./gradlew runGametest
```

Um gametest que **falha antes da correção e passa depois** é a melhor prova
disponível. Escreva-o antes de corrigir, quando der.

## 9. Regressão

O que mais depende do que você tocou?

```text
[ ] outras features do mesmo sistema
[ ] save/load
[ ] multiplayer
[ ] os comportamentos Vanilla que deveriam continuar
```

## 10. Documentar

```text
[ ] bug-report.md completo, com causa raiz
[ ] se revelou algo do Vanilla → docs/research/
[ ] se a causa era arquitetural → ADR
[ ] se é um risco conhecido que voltará → registrado
```

Bug corrigido sem causa registrada volta. A pessoa que o corrige da segunda vez
faz a mesma investigação inteira.

## Fechamento

Relate: **o que era, por que acontecia, o que mudou, o que foi verificado
rodando.** Se a correção deixou algum caminho descoberto, diga qual.
