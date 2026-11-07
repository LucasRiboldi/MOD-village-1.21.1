---
name: decisoes-autor-2026-10-27-sete-itens
description: "Escolhas do autor (27-10) para os 7 pontos abertos depois da 0.3.96 — cama perto, ajudas, atalho 2×, lote sem mudança, neve pela pá, remover rede de túneis, veio ao alcance"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-28T00:03:08.830Z
---

O autor pediu para **sempre perguntar as opções antes de implementar**. Escolhas de 27-10:

1. **Item 3:** a tarefa vai para quem do ofício mora mais perto do alvo, e a troca de cama acontece quando a **soma** das duas caminhadas cai (Regra 74 afrouxada).
2. **Item 8, ajudas para quem está sem tarefa:** pastor ajuda o fazendeiro (colhe a roça madura), ilumina ruas à noite (tocha ao lado do caminho escuro), cuida do bosque (replanta e põe farinha de osso) e limpa a faixa de obra. Ele **recusou** "carregar baú", "pedra exposta", "construtor ajuda lenhador" e "levar material à obra".
3. **D5:** atalho ao chafariz quando o desvio pela rede passa de **2×**.
4. **Regra 127 (lote pela centralidade):** **não fazer**, deixar como está.
5. **Neve:** o pedreiro sem tarefa recolhe neve do chão com a pá; o diorito vem da mina para o baú; só surge no lugar o que faltar depois das 3 buscas.
6. **Rede de túneis antiga:** remover tudo (classes, testes e parte do save; o save antigo ignora o que sobrar).
7. **Veio:** só a pedra que o mineiro alcança do piso (até 2 acima, sem buraco embaixo); o resto espera a galeria chegar perto.

**Feito:**
- **D5** (fbe87350, `NetworkDetour`);
- **item 7** (8d6ea84e). **Desvio, falta confirmar com o autor:** a régua que ficou é "o veio nunca sobe acima da altura em que começou". O "até 2 acima do piso" ao pé da letra quebrou 5 testes do veio;
- **item 3** (2ac92115): soma ≥16 e `TradeSites` com o lugar fixo do ofício.

**Item 8, parte feita:**
- pastor (94932be6; a correção 398ddc75 tirou do CropPatch para o `KnownFarmland`);
- lenhador no bosque (72e5f8e8);
- a faixa de obra já era do `BuilderPaving`.

**Item 8 completo:** a tocha está em 446a13a4 (`ColonyTorchBlock`, `ColonyTorches`, `HostileSpawnMixin`, POI). Falta conferir no jogo a aparência e o lugar que o carpinteiro escolhe.

**Tudo feito e fechado na 0.3.97** (4381f840, JAR 4659B5BA… instalado, não jogado):
- neve com a `SnowShovel` (29e9859d);
- rede de túneis removida (fb3ad049).

O desvio do veio ("nunca sobe acima de onde começou") foi **confirmado pelo autor em 27-10**. Os itens abaixo ficam como histórico.

**Falta (histórico):**
- **item 5:** neve pela pá e diorito da mina;
- **item 6:** remover a rede de túneis;
- fechar a 0.3.97.

**Detalhes do item 8 (27-10):** cada ajuda tem um ofício fixo.
- **Pastor:** colhe e replanta a roça madura.
- **Lenhador:** cuida do bosque.
- **Construtor:** limpa a faixa de obra (o `BuilderPaving` já faz).
- **Carpinteiro:** põe a **tocha especial do mod**.
  - Aparece sem custo, ao lado do caminho, só onde a luz de bloco é 0, no máximo uma a cada 8 blocos.
  - Luz 15, mais blocos `minecraft:light` invisíveis num anel em volta (área ~3×); eles saem quando a tocha quebra.
  - Visual da tocha comum, com chama e partícula laranja; a luz não tem cor (limite do jogo, sem shader).
  - Impede o nascimento natural de monstro num raio de 30 blocos. Ideia: registrar a tocha como POI e usar um mixin no nascimento.

**Why:** fechar os pontos abertos da 0.3.96 do jeito que o autor escolheu.
**How to apply:** implementar um por vez, com teste e commit. A ordem sugerida é D5, 7, 3, 8, 5 e 6. Ligado a [[decisoes-autor-2026-10-27-pendentes]] e [[rede-de-caminhos-decisoes]].
