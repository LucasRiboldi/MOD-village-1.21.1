# ADR-052 — As pontas azuis levam a vila para fora

**Estado:** aceita pelo autor em 2026-11-10. Regras 229 e 230. Emenda as Regras 185 e 206 e a caixa da vila da Emenda 6.

## Contexto

Até a 0.4.45 a ponta azul era a mais distante de cada quadrante, nas diagonais, e só as 2 mais distantes cresciam, 1 bloco por dia,
seguindo a direção em que a linha já ia, mesmo para dentro ou de lado. A caixa da vila só crescia com construção nova.

## Decisão do autor

1. Uma ponta azul por lado da caixa: norte, sul, leste e oeste.
2. Toda ponta tenta sempre ir para fora da vila, e o construtor calça o bloco seguinte a ela.
3. Ritmo: as 4 pontas, 1 bloco por dia de jogo cada.
4. A zona da vila alcança a ponta quando ela sai 7 blocos da caixa.
5. Limite: 128 blocos.

## Como

- **Lado (Regra 229):** `RoadSides` dá o lado da coluna pelo eixo em que ela mais se afasta do centro. `RoadTips` fica com a ponta
  mais distante de cada lado; as outras perdem o trecho morto, como antes.
- **Para fora:** `RoadShape.growTips` pede, uma vez por dia, a coluna seguinte de cada ponta na direção do lado dela, e não na
  direção da linha. A linha que dobrou faz uma curva e volta a sair.
- **Limite:** a ponta não passa de 128 blocos além da vila gerada; sem vila gerada, da caixa da colônia.
- **Ramal sem entrada não é ponta:** a ponta que termina numa entrada retirada (poste, poço, quina de plantação na rua) sai, como
  manda a Regra 223, mesmo sendo a mais distante do lado.
- **Zona (Regra 230):** `RoadShape.claimTips` leva a caixa até a ponta calçada que está 7 blocos ou mais fora dela
  (`Colony.grow`). Com a caixa maior, a busca de lote, os baús e a releitura do grafo acompanham a rua. Log: `VC_ROAD_TIP ... result=claimed`.

## Consequências

- A vila cresce sozinha: cada lado anda até 1 bloco por dia, e a caixa salta quando a rua sai 7 blocos.
- Toco que é o único de um lado vira a ponta daquele lado e cresce; antes ele era podado.
- O registro do `RoadGeometryPinTest` foi refeito: os cenários com pontas mudaram.
- **Não medido em jogo:** o custo da caixa maior (releitura, busca de lote) e o desenho das ruas que saem.
