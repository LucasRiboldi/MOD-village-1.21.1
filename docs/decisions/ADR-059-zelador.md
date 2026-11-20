# ADR-059 — O Zelador, dono do chão da vila

**Estado:** aceita pelo autor em 2026-11-19 (pacote B, nome "Zelador", sem bancada própria), sobre a pesquisa
`docs/research/2026-11-19-profissao-do-chao.md`. Regras 330 e 331. Revoga, na parte do chão, as Regras 215, 220, 242 e
247 e o "quem faz" da Regra 323.

## Contexto

Calçar a linha do grafo, devolver ao solo o caminho fora dela e nivelar a vila eram ajudas de quem estava sem tarefa do
próprio ofício: pedreiro, carpinteiro, fundidor, construtor e o adulto sem ofício. No jogo da 0.4.93 em planície, o
adulto sem ofício fez 86% do nivelamento (2.453 blocos) e 96% da calçada (590). Mesmo assim, o grafo terminou com
`toPave=24` e `offLine=211`, e a fila do terreno parou no dia 8 com `levelled=0`. O trabalho ficava sem dono quando
ninguém estava à toa. Essas ajudas também eram as exceções à regra temporária de 02-11 ("nenhuma profissão faz
atividade de outra").

## Decisão do autor

1. **Profissão própria, o Zelador** (`GROUNDSKEEPER`). Não tem bancada nem tarefa da fila (capacidade
   `MAINTAIN_GROUND`) e trabalha de mão vazia.
2. **Uma vaga por vila.** O Zelador é o adulto que passa das oito funções da primeira leva: o nono adulto, depois da
   falta de comida e antes da segunda leva. A fundação continua fazendo nascer só os oito ofícios (Regras 259 e 298).
3. **Só o Zelador cuida do chão.** Pedreiro, carpinteiro, fundidor, construtor e o adulto sem ofício deixam de calçar,
   nivelar e tirar neve. O pedreiro e o carpinteiro seguem mantendo as construções (Regra 221), o pastor alimenta o
   rebanho e o adulto sem ofício carrega.
4. **O dia do Zelador**, na ordem do que mais atrapalha quem anda:
   1. a linha do grafo a calçar (`PathPaving`, que tem a vez enquanto há trecho);
   2. o caminho fora da linha volta ao solo do bioma, para ficar uma linha só (`PathTrim`);
   3. entre as construções: o lote da obra aberta, o vão sob a peça da vila, o chão de dentro do curral, o vão entre as
      bases e a volta das casas, na altura da rua da construção e com o excesso limpo;
   4. a fila de quadrados do terreno, com desnível de até 1 (Regra 323);
   5. a neve que a obra pede (`SnowShovel`).
   Uma passada a cada 10 segundos, quando ele não está calçando.
5. **O feno no caminho** (Regra 331). O grafo atravessa o fardo de feno como ar (`WorldPathTerrain`), e o Zelador, ao
   calçar a coluna, leva o fardo para os baús. O fardo posto pela colônia numa obra fica.
6. **`VC_GROUND`**, uma linha por dia no fim do expediente, junto da `VC_TERRAIN`: Zeladores, trechos a calçar
   (`toPave`), caminho fora da linha (`offLine`), degraus maiores que 1 (`steps`) e o que ele calçou, devolveu ao solo e
   nivelou no dia.

## Consequências

- O chão tem dono, e o efeito é medido no jogo pela `VC_GROUND`.
- Vila com até oito adultos não tem Zelador, e ninguém cuida do chão até nascer o nono adulto.
- Uma versão anterior do mod lê o `GROUNDSKEEPER` do save como profissão desconhecida e recontrata o aldeão. A ordem de
  contratação salva no mundo ganha o Zelador no fim.
- O aterro continua surgindo do chão do bioma (Regra 90). O solo físico, com corte para o baú, foi a opção C e não foi
  escolhido.
