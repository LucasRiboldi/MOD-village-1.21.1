---
name: playtest-0102-vila-nova-decisoes
description: "Playtest 0.3.102 (28-10, Spark WT6mMz7jQQ, 4h16, planície, vila nova) — 8 pontos do autor, causas achadas e escolhas"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-28T11:46:52.617Z
---

**Jogo 0.3.102 (28-10, Spark WT6mMz7jQQ, 4h16):**
- MSPT 18; mod a 2,2% do tique (`ResourceType.valueOf` 0,7%: pista de desempenho);
- 22 obras (5,4/h), de 14 para 18 aldeões;
- parado: pedreiro 71%, carpinteiro 37%, mineiro 40%, construtor 41%, pastor 35%; fundidor 51% esperando.

**Causas achadas:**
1. Pedreiro à noite no morro: a tarefa de fabricar continua fora do expediente ("crafting where it stands, off hours", mais de 140×).
2. Só trabalhador com cama própria ganha baú; as camas das casas não.
3. "Farmer tilled a new plot" 32×: `FarmerTill` ara fora das fazendas.
4. `ProfessionChestOverflow` linhas 138 e 226 põem os baús VAZIOS primeiro.
5. A caixa da vila foi de 155×129 a 213×235; 6 construções sem porta legível (portão de cercado não conta em `PathLinks.entranceOf`).
6. Vila esburacada (o nivelamento é só em volta do lote).
8. `StructureBlueprintReader` descarta o AR da planta, e o miolo do cercado fica com terra; há obra abaixo da rua.
7. Ações:
   - 3 mineiros num poço só (2 "esperando ramal"; à noite "walking to nowhere");
   - fundidor com 74 tarefas soltas (argila/terracota);
   - pastor "fenced in" 13×;
   - lenhador 22 soltas "sem progresso";
   - construtor 215 SET_ASIDE por falta de material.
- `state_report.py` quebra no console cp1252 (→): corrigir com `reconfigure(utf-8)`.

**Escolhas do autor:**
- **Baús:** completar a pilha → baú com o item → baú mais cheio até ~80% → só então vazio.
- **Plantação:** só nas fazendas e hortas; comida abaixo da meta → obra de fazenda prioritária.
- **Camas:** baú ao lado de TODA cama (surge, com `VC_SUPPLY`).
- **Nivelamento:** o autor quer um //smooth "no carregamento das vilas / geração dos chunks". Ofereci:
  - (1) datapack `terrain_adaptation` mais forte (beard_box), só para vilas novas;
  - (2) smooth do mapa de alturas na adoção da vila, uma vez, em fatias;
  - recomendei os dois; resposta pendente.

**Mais escolhas do autor (28-10):**
- **Smooth, os dois:**
  - datapack com `terrain_adaptation` mais forte (só vilas novas);
  - smooth do mapa de alturas UMA vez na adoção, em fatias, sem tocar construção, rua, plantação nem bloco do jogador.
- **Compacta:**
  - lote procurado do centro para fora, perto da rede;
  - só abre com o traçado porta→rede, que é assentado junto da obra;
  - portão de cercado conta como porta;
  - troca de cama a cada meio dia.
- **Interior:** o ar da planta vira "limpar" na abertura da obra; obra não nasce abaixo do nível da rua.
- **Profissões, todas as 4:**
  - à noite largar a tarefa e dormir;
  - mineiros só quantos ramais abertos;
  - terracota só abre com argila no baú;
  - pastor não entra no cercado (laço pelo portão).
- **Ferramentas, todas:** skill minecraft-world-generation, bancada de jogo automático (biome_sim + /tick sprint), revisão de arquitetura (java-architecture-review/static-analysis), perfil (minecraft-performance-profiling, ex.: `ResourceType.valueOf`).

**Ferramentas novas aprovadas pelo autor (todas só para desenvolvimento e teste):**
- Carpet (`/tick warp`, `/player` falso para manter a vila atendida, `/log`);
- client game tests e MC-Runtime-Test no CI (print da tocha, do nome e do painel);
- World Inspector / SGUProfiler (custo por entidade);
- Amulet / nbtlib para auditar o save (terreno, baús, obras).

**Teto por item (28-10):**
- Nos baús PÚBLICOS o teto de 3 compartimentos por item (ADR-036 9, `ChestSlotCap`) sai: o item enche o baú até ~80%.
- No baú do TRABALHADOR o teto fica, e o alívio tira dele as pilhas cheias (64 de um só item), não os 10 últimos slots.
- A distribuição completa a pilha → baú com o item → baú mais cheio até 80% → vazio. Hoje `ProfessionChestOverflow` linhas 138 e 226 põem o vazio primeiro.

**FOCO NOVO do autor (28-10):** "criação e crescimento das ruas" é ponto sensível e vem antes do resto: estudar e perguntar.

**RUAS (decisões de 28-10, estudo em `docs/research/2026-10-28-ruas-naturais.md`):**
- rua junto com a obra;
- rua e caminho separados (rua de 3, mesmo traçador rente ao chão);
- adensar antes de esticar;
- curvas pelo relevo;
- suspiro: quintal de 2, folga lateral de 4 a 6, lote verde a cada ~5 casas e praça em cruzamento, árvores e luz;
- chão em mistura gasta natural, e esses blocos, tudo o que a vila cria e todo material de estrutura NÃO são minerados nem recolhidos por profissão;
- lote pelo mapa de interesse, com a nota no `VC_STATE`.
- Ordem: proteção → `StreetPlan` (lote+rua) → curvas/quintal → verde/praça/luz → medida.

**MINA (decisões de 28-10, estudo em `docs/research/2026-10-28-mina-natural.md`):**
- portal: boca de mina de madeira com decoração extra (vigas, lanternas, trilho e vagonete, guindaste, barris), por bioma;
- descida: escada direta até as camadas de minério (y 48/16/0/-16/-59) + espinha de peixe (corredor de 2, ramais a cada 3), veio inteiro; os salões saem;
- obstáculos, todos: contornar, ponte de pedra em caverna, selar água/lava, caminho salvo `MineRoute` iluminado até y -59;
- coleta: TUDO vai para os baús;
- o jogo da 0.3.102 mostrou a mina presa em 1 nível e a Regra 137 a mandando de volta à boca.
- Ordem: `MineRoute` → `MinePlan` → obstáculos → mineiros por frente → portal → medida.

**Ruas — etapa 1 FEITA (efca3b0e):** `StreetSurface` (borda gasta) + `mayBreak` protege a rua da colônia; 901/901.

**Ordem planejada (um commit por marco):**
1. `state_report` utf-8;
2. baús (encher);
3. baú em toda cama;
4. fazenda;
5. noite/mineiros/fundidor/pastor;
6. interior/ar e abaixo da rua;
7. portão como porta e anéis com estrada;
8. smooth (datapack + adoção);
9. ferramentas.

**How to apply:** um commit por marco. Ligado a [[playtest-0399-ociosos-decisoes]] e [[caminho-rente-ao-chao]].
