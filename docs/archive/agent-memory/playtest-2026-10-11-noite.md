---
name: playtest-2026-10-11-noite
description: Playtest 0.3.26 de 6 min (Spark HGyazYR1VW) — vila presa 3 min numa vidraça que o pastor pegou; escolhas do autor para o 3º ciclo
metadata:
  node_type: memory
  type: project
  modified: 2026-10-11T21:11:51.246Z
---

Sessão 17:50–17:58 de 2026-10-11, JAR 0.3.26. TPS 20, mod 2,5%. Uma janela VC_TIME só (enviesada: caiu na trava).

Causa dominante: obra única com 1 glass_pane faltando; o construtor terminou a tarefa ("every remaining piece is waiting for physical support"), o WorkAssignment deu a BUILD ao pastor (ordem da lista, sem preferência) que andou 2400 ticks e parou a 51 blocos; mineiro pôs em 30 s. Construtor 92% parado.

Escolhas do autor (3º ciclo, branch claude/autossuficiencia, PR #26 ainda aberta):
- Obra: ajudante só se ≤32 blocos e desiste em 30 s; peça faltando → obra pede à oficina e o construtor espera sem largar (manutenção enquanto isso).
- Areia: britador primeiro; fundidor sem areia ajuda outro ofício; lembrar a mancha de areia.
- Tetos: peças fabricadas 64, crus 256 (geral da vila); artesão no teto vira ajudante.
- Coletores: bosque perto da vila (+ árvore inalcançável largada em 30 s), fazendeiro lavra lote novo, mineiro abre 2º ramal cedo. ("Something else" veio sem texto.)

**How to apply:** ao retomar, conferir no git o que já entrou; nada disso foi jogado. Ver [[autossuficiencia-escolhas-2026-10-11]].

**0.3.27 entregue** (ba4d6224, JAR C3B1F7E3…, 729/729 ×2, 1467 unit): todos os itens acima implementados.

**4º ciclo (pedido do autor, 11-10 noite) — cobertura de blocos:** fonte = `build/gametest/structure-coverage.txt`
(StructureCoverageGameTest; 187 itens, só 49 com ResourceType). Escolhas: divisão **por material**
(madeira→carpinteiro; pedra/barro/terracota/fornalha→pedreiro; metal/vidro/vidraça/lampião→fundidor;
lã colorida/tapete/cama/corante→pastor; papel/livro/farinha de osso/feno→fazendeiro; gelo/neve→fundidor
superfície); estoque **8 de cada item das plantas** em rodízio (carpinteiro e fundidor, abaixo do teto 64,
tabela por item fora do ResourceType); fazendeiro colhe flores/grama/samambaia/bambu/cana e replanta;
fundidor recolhe gelo e neve; drops seguem Regra 41; 4ª tentativa continua como rede.

**0.3.28 entregue** (27d5d234, JAR 5E53CE56…, 732/732 ×2, 1473 unit): CraftFamily + 3 TaskTypes novos
(CRAFT_METAL/TEXTILE/FARM), ItemStock (ordem nominal alvo DIRT), PlantPatch + 11 ResourceTypes, TaskChain
não encadeia nominal. Nada jogado. Pendente: construtor sem preferência na obra (autor não escolheu), ramais
cedo podem não alcançar a galeria, fundidos sem ResourceType fora do estoque.
