# Experimentos

Chega um ponto em que ler mais código não decide. O comportamento depende de
ordem de execução, de estado de mundo, de interação entre sistemas — coisas que
o código mostra mal e a execução mostra bem.

Aí o próximo passo não é ler mais. É **medir**.

## Quando parar de ler e testar

Sinais de que a leitura já deu o que tinha:

- Duas leituras plausíveis do mesmo código e você não consegue eliminar nenhuma.
- A conclusão depende de "quem roda primeiro".
- O código é claro, mas o comportamento em jogo contradiz.
- A decisão é cara e a confiança está baixa (`evidence-and-claims.md`).

Sinal de que **não** é hora: você ainda não rodou `javap` no alvo. Verificação de
assinatura é leitura, é barata, e resolve boa parte das dúvidas sem experimento.

## Uma pergunta por experimento

**Nunca teste cinco hipóteses ao mesmo tempo.** Com cinco variáveis, um resultado
inesperado não diz qual delas errou, e você precisa de mais cinco experimentos
para descobrir — pior do que ter feito um de cada vez desde o começo.

Se você tem cinco perguntas, ordene por: qual resposta elimina mais alternativas?
Comece por essa.

## Setup mínimo

O experimento deve conter **só** o que a pergunta exige. Um experimento que sobe
o mod inteiro para testar uma prioridade de task não isola nada: qualquer parte
do mod pode ser a causa do que você observar.

Reduza até:

- um mundo de teste, não o save do jogador
- uma entidade, não uma vila
- uma task registrada, não o conjunto
- log em vez de mudança de comportamento, quando a pergunta é "isso é chamado?"

## Gametest como bancada

Em Fabric, o gametest é a melhor bancada disponível: sobe servidor headless, monta
a estrutura, roda as afirmações e falha o build. É reprodutível — o que uma sessão
de jogo manual não é.

Estrutura verificada em MC 1.21.1 (yarn `1.21.1+build.3`, Fabric API
`0.116.15+1.21.1`):

```java
public class MinhaPerguntaGameTest implements FabricGameTest {   // net.fabricmc.fabric.api.gametest.v1.FabricGameTest

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "minha_pergunta")
    public void aPerguntaEmUmaFrase(TestContext context) {        // net.minecraft.test.TestContext
        BlockPos pos = new BlockPos(2, 2, 2);
        context.setBlockState(pos, Blocks.STONE.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos absolute = context.getAbsolutePos(pos);          // relativo → absoluto

        context.assertTrue(condicao, "o que eu esperava, e não aconteceu");
        context.complete();
    }
}
```

Pontos que costumam morder:

- **Coordenadas são relativas à estrutura.** Use `context.getAbsolutePos(...)`
  antes de falar com o `ServerWorld`. Misturar os dois dá falha silenciosa.
- **O mundo de teste é vazio.** Não há vila gerada, não há bioma "de verdade",
  não há estrutura. O que depende de worldgen não é testável assim — e isso é uma
  limitação a declarar no resultado, não a esconder.
- **O entrypoint de gametest não pode ir no jar publicado.** Um servidor dedicado
  carrega `fabric-gametest` no boot; apontar para classes ausentes derruba o
  servidor. Sourceset e `fabric.mod.json` separados resolvem.
- `context.complete()` no fim, senão o teste expira.

Rodando (quando o projeto tem a run configurada no Loom):

```bash
./gradlew runGametest
```

## Quando o gametest não alcança

Nem tudo cabe no mundo vazio. Para o que depende de worldgen, de vila gerada, de
vários jogadores ou de tempo longo, o experimento é **sessão de jogo com
instrumentação**:

- log com prefixo do mod, em ponto único, dizendo estado — não "passei aqui"
- uma variável mudando por vez entre sessões
- observação escrita **antes** de interpretar

E o resultado carrega a marca: reprodutível uma vez não é reprodutível. Declare
quantas vezes você observou.

## Formato do registro

Use `templates/experiment.md`. O essencial:

```text
# Experimento

## Pergunta            uma frase, uma variável
## Versão              MC + Fabric + mappings
## Hipótese            o que você espera, e por quê
## Setup mínimo        o menor mundo/código que responde
## Implementação       o que foi feito
## Resultado esperado
## Resultado real      ← escreva antes de interpretar
## Logs                trecho, não o arquivo inteiro
## Conclusão
## Confiança           alta / média / baixa
## Impacto arquitetural
```

**Escreva o resultado real antes da conclusão.** A ordem importa: interpretar
enquanto observa é como o resultado vira o que você queria ver.

## Resultado negativo é resultado

"A hipótese estava errada" é uma das entregas mais valiosas: eliminou um caminho
e evitou uma arquitetura. Registre com o mesmo cuidado do resultado positivo, e
promova a conclusão a `[FATO]` no `research-status.md`.

O que não vale é o experimento silencioso — rodou, não deu o esperado, ninguém
registrou, e três semanas depois alguém tenta de novo.
