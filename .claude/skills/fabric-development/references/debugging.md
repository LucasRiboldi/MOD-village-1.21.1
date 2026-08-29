# Depuração

A regra: **não corrija sintoma antes de encontrar a causa.** Processo completo em
`workflows/bug-fix.md`; aqui estão as técnicas.

## Comece pelo estado, não pelo código

A pergunta que mais resolve:

> **O que o programa acha que sabe?**

```text
Qual o valor real do estado no momento da falha?
Quem escreveu esse valor? Quando?
Alguém deveria ter escrito e não escreveu?
O valor está certo e a leitura está errada?
```

Muito bug "de lógica" é estado que nunca foi escrito. Ler a lógica primeiro faz
você conferir dez vezes um código correto.

## Sintoma → suspeito

Tabela de triagem. Não é diagnóstico; é onde olhar primeiro.

| Sintoma | Suspeite |
|---|---|
| esquece tudo ao reabrir o mundo | `markDirty` ausente, ou par escrita/leitura faltando |
| funciona em SP, quebra em MP | autoridade ou sincronização |
| `NoClassDefFoundError` no servidor | classe de cliente em código comum |
| trava ao carregar chunk | `getBlockState` forçando carga dentro do tick |
| some ao trocar de save sem reiniciar | estado em `static` |
| some quando o jogador se afasta | chunk descarregado tratado como ausência |
| entidade parada sem fazer nada | memória do Brain nunca escrita |
| conexão cai no handshake | registro condicional / ids divergentes |
| cubo preto e rosa | resources faltando |
| `block.meumod.foo` na tela | lang faltando |
| bloco não dropa nada | loot table faltando |
| some após atualizar o MC | mapping / assinatura / semântica mudou |
| trava periodicamente com TPS normal | pressão de GC (alocação) |
| corrupção intermitente | mutação fora da thread do servidor |
| mixin não faz nada | injector não aplicou — veja o log de boot |

## Ler o log

```bash
ls run/logs/ build/gametest/logs/ 2>/dev/null
```

**Procure de baixo para cima até a primeira coisa estranha** — o topo do
stacktrace é onde estourou, não onde começou.

O que importa:

```text
[ ] a PRIMEIRA exceção, não a última (as seguintes costumam ser consequência)
[ ] avisos de Mixin no boot          ← nunca é ruído
[ ] "Mixin apply failed" / "Critical injection failure"
[ ] o mod citado no topo do stack pode não ser o culpado
[ ] o que aconteceu ANTES da exceção
```

Crash report em `run/crash-reports/` traz a seção "Affected level" e a lista de
mods — útil para separar "é meu" de "é interação".

## Instrumentar

```java
LOGGER.info("[meumod] colheita: aldeão={} alvo={} estado={}", uuid, pos, estado);
```

```text
✓  log de ESTADO — os valores que explicam a decisão
✗  log de PASSAGEM — "entrei aqui", "cheguei ali"
```

Log de passagem prova que o código rodou, que quase nunca é a dúvida. Log de
estado mostra **por que** ele decidiu o que decidiu.

```text
[ ] prefixo do mod → filtrável num log de trinta mods
[ ] NUNCA no tick sem controle de frequência
[ ] remova ou baixe para debug antes de publicar
```

Um log por tick por entidade enche o disco e **muda o timing**, escondendo bugs
de concorrência.

## Bissecção

Quando não se sabe nem por onde começar:

```text
o bug existe na versão anterior?     → git bisect
existe sem os outros mods?
existe com metade da feature desligada?
existe em mundo novo?
```

Cada resposta corta o espaço de busca pela metade. Mais rápido que ler tudo.

## Depurar Mixin

```text
[ ] o aviso está no log de BOOT, não no momento do uso
[ ] "injection failure" = o alvo mudou → verifique com javap
[ ] require = 0 esconde a falha → use 1
[ ] TAIL vs RETURN: rodou uma vez ou n vezes?
[ ] o alvo é lambda? o @Inject no método externo não pega
```

```bash
javap -s -cp "$MC_JAR" <fqn> | grep -A1 "<metodo>"
```

## Depurar IA

Estado antes de código. Ordem eficiente:

```text
1. qual Activity está ativa?      deveria ser essa?
2. quais memórias estão preenchidas?  alguma vencida?
3. qual sensor deveria escrever?  rodou?
4. o gate de memórias da task passa?
5. qual task está bloqueando a que você quer?
6. o WALK_TARGET está posto e sendo mantido?
```

Quase sempre a task está correta e a memória que ela exige nunca foi escrita. Ver
`ai-development.md`.

## Depurar persistência

```text
1. o campo tem par escrita/leitura?    grep writeNbt/readNbt
2. markDirty é chamado após mutação?
3. o dado é gravado no arquivo certo?  (dados relacionados juntos)
4. o KEY mudou entre versões?
5. o estado é limpo no start E no stop?
```

Teste decisivo: **criar → usar → fechar o mundo → reabrir.** Se o estado não
voltou, é um dos cinco acima.

## Depurar client/server

```bash
./gradlew runServer
grep -rn "net.minecraft.client" src/main/java
```

```text
[ ] reproduz em servidor dedicado?  (se só em MP, é side)
[ ] quem tem o estado verdadeiro?
[ ] o cliente está exibindo estado velho, ou decidindo indevidamente?
[ ] o sync inicial existe?
```

## Ferramentas do jogo

```text
F3            coordenadas, chunk, bioma
F3+B          hitboxes
F3+G          bordas de chunk
/debug start|stop   profiler do tick
/reload             recarrega datapack (recipes, loot, tags)
```

## Antes de dizer que consertou

```text
[ ] reproduzi o bug ANTES da correção
[ ] entendi a CAUSA, não só o sintoma
[ ] cobri todos os caminhos da mesma causa
[ ] o caso reportado não acontece mais
[ ] escrevi um teste que falhava antes
[ ] o comportamento correto anterior continua
[ ] verifiquei RODANDO, não só compilando
```

O terceiro é o que evita a correção parcial que passa nos testes e falha em jogo.
