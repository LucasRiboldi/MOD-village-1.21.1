# TODO

**Atualizado:** 2026-10-09. Só o que está **aberto**. O histórico até hoje está em
`docs/archive/technical/TODO-ate-2026-10-07.md` (consultar por `grep`, nunca inteiro).
Estado vivo: `STATE.md`. Decisões: `docs/decisions/` (a última é a ADR-039).

Prioridade: 🔴 bloqueia · 🟠 importante · 🟡 melhoria · 🟢 futuro.

## 🔴 Playtest da 0.3.7

Nada das ADR-037, 038 e 039 foi visto em jogo. Rodar depois: `python scripts/time_ledger.py`
e `python scripts/cost_ledger.py` no `latest.log`, e Spark.

- [ ] `walk` aparece no `time_ledger` (A3: o braço do aldeão agora desce).
- [ ] Carpinteiro com escada/laje/cerca **da madeira do bioma** no baú dele; pedreiro com as peças
      das casas da vila; fundidor com vidro e a pedra lisa que as casas usam.
- [ ] Peça feita entra no **baú da profissão** de quem fez, não no do vizinho.
- [ ] Mineiro encalhado **continua mineiro**; ramal novo abre para o minério que falta.
- [ ] Fundidor fica abaixo de 15% de ociosidade quando houver minério no fluxo; no playtest
      `GqK0MLwvTw`, a causa provável foi o mineiro preso numa descida longa e segurando o único ramal.
- [ ] Caminho calçado até obra afastada da rua.
- [ ] Fechar e abrir o mundo: lenhador vai direto à árvore conhecida; mineiro volta ao ramal e ao veio;
      animal na corda continua sendo levado à cerca.
- [ ] Pastor trazendo animal solto até a cerca perto da cama; ovelha solta também procria.

## 🟠 Erros abertos

- [ ] **KF-002** (aldeão de coleta some no tique 1): não reapareceu em ~27 baterias de 07-10.
      Correção recomendada: montar o cenário dentro da própria arena, com o raio protegido encurtado
      por gancho de teste (como `MineDigging.shortenSurfaceRadiusTo`), em vez de 65–97 blocos fora.
- [ ] **KF-003**: causa achada e corrigida (fusão de colônias no tique de teste). Fecha com
      `python scripts/gametest_battery.py 30` limpo; até agora 7 + 6 limpas.
- [ ] **`lumber_stall_guard`** caiu 1 vez em ~6 baterias de 07-10 (tarde), sem relação com a mudança
      da hora. Medir com o script antes de investigar.
- [ ] **Tique do servidor de teste** ainda roda o ciclo de vida das colônias (`updateLifecycles`).
      Ver decisão abaixo (item 3 de 07-10).

## 🟠 Plano: testes que não provam o que deveriam

Regra (vale para todo teste novo ou alterado): **o commit diz a mutação feita e que o teste caiu
com ela**. Teste que passa com a regra desligada não entra como prova.

- [ ] **D2 (caminho até a obra):** a mutação da parada na borda sobreviveu. Achar a segunda guarda
      (desligar as duas e ver qual segura) e montar o cenário que só a primeira segura.
- [ ] **B1:** mineiro da superfície não invade o poço (a mutação sobreviveu em 07-10).
- [ ] **B3:** carpinteiro/pedreiro em volta do sino sem bancada (sino de outra arena interfere: montar
      com bancada ausente e sino a 3 blocos, conferir `CraftStation.spotFor`).
- [ ] **B4:** ofício parado cede vaga — testar o gancho em `WorkTime`, não só `IdleYield`.
- [ ] **B5:** pastor **andando** com o animal na corda (hoje o teste teleporta o pastor).
- [ ] **B6:** boca da mina prefere morro separado de água.
- [ ] **Save de verdade:** `WorkMemorySavedData` gravado e relido pelo NBT (hoje só o conteúdo
      `WorkMemory` é provado); fechar/abrir mundo fica no playtest.

## 🟡 Estrutura

- [ ] Seis arquivos de produção em 497–500 linhas (`SmelterWork`, `Mine`, `ClimbOut`,
      `SurfaceGatheringWork`, `Worker`, `BuilderWork`): dividir antes da próxima mudança neles.
- [ ] Branch local do worktree do Codex (`codex/village-visuals-logistics-mine-sweep`): já toda na
      `main`; remover o worktree.

## Decisões do autor que faltam

- [ ] **Tique do servidor de teste** (item 3 de 07-10): alternativas em `docs/decisions/ADR-039-*`.
- [ ] Aldeões sem ofício 100% parados (cota da ADR-011): vagas para todos, ou ajudantes?
- [ ] Dois construtores na mesma obra.
- [ ] E5/E6: limites da felicidade e do rebanho (números do agente). E7: texto da Regra 33.
- [ ] Números do agente na ADR-039 (carvão < 32, ferro < 16, caminho 4/ciclo até 24).
- [ ] `docs/workers-analysis/`: mover para `docs/research/`, arquivar ou apagar.

## Decidido, não fazer

- Obra antiga fica como está: mudanças valem daqui para frente (casas antigas sem piso não são refeitas).
- Peça sem rota: depois de 4 tentativas aparece no baú de quem precisa (`BiomeConstructionSupply`,
  já em vigor, testado em `LocateFallbackGameTest`).
