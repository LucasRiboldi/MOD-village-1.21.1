# ADR-058 — O pastor trabalha como um pastor: o curral é um lugar, o desgarrado tem prazo e o dia tem ronda

**Estado:** aceita pelo autor em 2026-11-19 (pacote B), depois do jogo da 0.4.93 em planície. Emenda a Regra 72 (a
separação do curral misto) e a Regra 309 (a coleta rumo ao curral sem prazo). Regras 327, 328 e 329.

## Contexto

No jogo da 0.4.93 (colônia ccac8347, cerca de 3 h), o pastor ficou bloqueado de 62% a 96% do tempo em 13 das 21
janelas de 5 minutos, e o nome dele ficou vermelho. Ele fez 23 entregas no curral, 16 delas de porco, e o curral grande
continuou com 2 ou 3 animais. Em 3 horas ele não desistiu de nenhuma coleta. O autor deixou duas notas: "o pastor não
consegue levar o animal para dentro do curral" e "pastor com nome vermelho e travado".

As causas:
- **O curral era recalculado, não lembrado.** Para o mod, um animal estava guardado só se houvesse cerca a até 6 blocos
  e nenhuma saída aberta. Isso falhava em dois casos. Primeiro, a porteira fica aberta por 30 segundos a cada entrega, e
  nesse tempo todos os animais de dentro viravam desgarrados. Segundo, no meio do curral de 13 × 13, a cerca fica a mais
  de 6 blocos. A releitura dos currais também desfazia o curral com a porteira aberta.
- **A ida não tinha prazo.** Pela Regra 309, a coleta rumo ao curral não tinha prazo, e isso incluía a ida até o animal.
  Quando o animal estava fora de alcance, o pastor ficava parado para sempre.
- **Não havia rotina.** O pastor emendava uma coleta na outra e nunca olhava o rebanho como um todo.

## Como um pastor trabalha (a referência do autor)

O pastor cuida de um rebanho. Ele conta os animais na porteira e vai atrás só do desgarrado, o que está do lado de fora.
Desiste do que não alcança e volta depois. Alimenta e tosquia, e fecha a porteira. O curral é um lugar fixo: o que está
dentro da cerca está guardado, com a porteira como estiver.

## Decisão do autor

1. **O curral é um lugar** (Regra 327). Na leitura do curral, todo portão conta como parede, aberto ou fechado. Todo
   animal no chão de dentro de um curral conhecido está guardado e nunca é escolhido para a coleta. Porteiras da mesma
   cerca formam um curral só, que guarda todos os seus acessos (ADR-046).
2. **O desgarrado tem prazo** (Regra 328). Na ida até o animal, o pastor desiste se ficar 30 segundos sem chegar um bloco
   mais perto. Rumo ao curral, desiste também se o animal não entrar pela porteira 3 vezes. O animal largado fica 5
   minutos fora da coleta, e o pastor passa ao próximo. Com o animal na corda, a condução segue sem prazo, como manda a
   Regra 309.
3. **A ronda do dia** (Regra 329). Uma vez por dia, no expediente e antes de buscar desgarrados, o pastor passa pela
   frente de cada curral da vila, conta o rebanho de dentro e fecha a porteira que ficou aberta. Ele só fecha se ninguém
   estiver passando e se nenhuma passagem do mod a segurar aberta. O curral que ele não alcança em 30 segundos fica de
   fora da ronda. No fim, a linha `VC_ROUND` registra os currais, os visitados, os pulados, os animais de dentro, os
   desgarrados, as porteiras fechadas e a conta por espécie. A tosquia espera a ronda acabar.
4. **A separação do curral misto** (Regra 72) acontece no máximo uma vez por dia em cada vila.

### Emenda de 22-11-2026 — entrega escolhida pelo autor

O pastor abre a porteira, pula a cerca, alinha-se diante da abertura pelo lado de dentro e caminha em direção ao centro do curral. O animal atravessa na guia, pela porteira. Com o animal dentro e a passagem desocupada, a porteira fecha, o pastor solta o animal e sai pulando a cerca. Os demais aldeões contornam o curral, inclusive quando a porteira está aberta.

### Emenda de 22-11-2026 — reserva para abate

Em qualquer curral da vila, o pastor só abate um adulto quando há pelo menos três adultos da mesma espécie dentro daquele cercado. Restam pelo menos dois adultos da espécie no curral. A reserva de seis adultos por espécie na área da vila continua valendo (Regra 167).

## Consequências

- O pastor deixa de levar de novo o animal que já está no curral, e a contagem do curral passa a subir com as entregas.
- O animal fora de alcance deixa de prender o pastor. O nome vermelho só aparece quando ele está de fato parado.
- A ronda acrescenta uma caminhada por dia e uma linha de log, que mede o rebanho no jogo.
- **Não decidido aqui:** conduzir vários animais com a comida na mão (opção C do autor), porque exige um Mixin novo na IA
  do animal.
- `ShepherdHerding` passou para a classe `PenRegroup` a separação do curral misto, para não passar de 500 linhas.
