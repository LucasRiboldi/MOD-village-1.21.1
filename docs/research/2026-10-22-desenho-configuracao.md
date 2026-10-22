# Desenho — configuração do mod (R-153/R-154 do estudo v2) — 2026-10-22

**Pedido do autor:** planejar o sistema de configuração, só o desenho e sem código, escolhido entre os sistemas
grandes do "Regras v2" (avaliação: `2026-10-22-avaliacao-regras-v2.md`).

## 1. O problema

Hoje todo limite do comportamento é uma **constante no código**. Mudar o rebanho de 8 para 10, ou o teto de
estoque de 64 para 128, exige editar o código, compilar e instalar um JAR novo. O único ajuste que existe em jogo
é a **política de profissões** (`ProfessionPolicy`: ofício ligado e ordem), salva por mundo
(`ProfessionPolicySavedData`) e protegida por permissão de operador nível 2 (`ProfessionPolicyNetworking`).

O v2 pede três coisas:
- valores padrão seguros;
- configuração inválida rejeitada ou trocada pelo padrão documentado (R-153);
- mudança só por operador, registrada e persistida com segurança (R-154).

## 2. O que seria configurável (fase 1, candidatos)

Todos têm hoje o valor de uma regra. A faixa proposta é o intervalo em que o comportamento continua seguro; fora
dela, o valor é recusado.

| Chave | Hoje | Faixa segura | Regra | Onde está |
|---|---|---|---|---|
| `stock.crafted` | 64 | 16–256 | emenda 1/5 | `StockCeiling.CRAFTED` |
| `stock.raw` | 256 | 64–1024 | emenda 1/5, 42 | `StockCeiling.RAW` |
| `flock.breedUpTo` | 8 | 2–32 | 68 | `ShepherdFlock.FLOCK_TARGET` |
| `flock.keepAlive` | 6 | 2–`breedUpTo` | 68 | `FlockHarvest.HARVEST_KEEP` |
| `pen.floorPerAnimal` | 4 | 2–16 | 72 | `ShepherdPens.FLOOR_PER_ANIMAL` |
| `bed.minGain` | 16 | 4–64 | 74 | `BedPlan.MIN_GAIN` |
| `houses.spareBeds` | 4 | 0–16 | 49 | `ConstructionPriority.HOUSE_SURPLUS` |
| `farms.adultsPerFarm` | 15 | 5–50 | 67 | `ConstructionPriority.ADULTS_PER_FARM` |
| `focus.perimeter` | 48 | 16–128 | 62 | `VillageFocus.PERIMETER` |
| `terrain.lotCut` / `terrain.lotFill` | 5 / 2 | 0–8 / 0–4 | 90 | `LotTerraform.MAX_CUT/FILL` |
| `terrain.corridorCut` | 12 | 0–16 | 70 | `Corridor.MAX_CUT` |
| `founding.animalsPerSpecies` | 2 | 0–4 | 102 | `VillageFarmAnimals.EACH` |
| `search.absentFor` (tiques) | 24.000 | 6.000–96.000 | 98 | `KnownAbsent.FOR_TICKS` |

**Ficam de fora, de propósito:**
- os orçamentos de desempenho (`PlanningBudget.DEADLINE_MS`, o teto de 50 ms do corredor), porque mudá-los
  troca travamento por paralisia sem que o jogador veja a causa;
- as proteções (Regra 3, lava): não são negociáveis.

## 3. Arquitetura proposta

1. **Core — `ColonySettings`:** registro imutável com todas as chaves, os valores padrão (as constantes de hoje)
   e a faixa de cada uma.
   - `ColonySettings.with(key, value)` devolve as configurações novas ou o motivo da recusa.
   - Puro e testável no Core, então conta no PIT.
   - **Regras entre chaves:** `keepAlive ≤ breedUpTo`, `raw ≥ crafted`.
2. **Persistência:** `ColonySettingsSavedData`, um `PersistentState` como o da política de profissões, com
   `saveVersion` e migração pelo `SaveMigration`.
   - Valor inválido no save (editado à mão, ou de versão antiga) é trocado pelo padrão, com aviso no log
     (R-153).
3. **Leitura:** as constantes viram o **padrão**, e o código lê `Settings.current()`.
   - São cerca de 15 pontos de leitura; a troca é mecânica.
   - Os testes usam os padrões; há um `Settings.withOverrides(...)` que desfaz no fim, para testar um valor
     sem vazar para os vizinhos (a lição dos lotes de GameTest).
4. **Edição:** o comando `/vc config list | get <chave> | set <chave> <valor> | reset [chave]`, com
   permissão de operador nível 2 (o mesmo da política).
   - Respostas: o valor aceito, ou a faixa permitida.
   - Uma tela no cliente pode vir depois, sobre o mesmo pacote de rede da política.
5. **Registro (R-154, R-164):** toda mudança escreve
   `VC_CONFIG version=1 key=… from=… to=… by=<jogador>` e entra no diário da vila. O `/vc config list` mostra
   o que difere do padrão.

## 4. Riscos e como o desenho os trata

| Risco | Tratamento |
|---|---|
| Mudar no meio de uma operação (baixar o rebanho com animais acima do limite) | Os sistemas já leem o limite a cada ciclo; o efeito é o comportamento normal (o abate começa), sem estado preso. A mudança só vale no ciclo seguinte. |
| Valor que trava a vila (casa sem margem e sem lote, estoque zero) | Faixas mínimas que não zeram nada essencial, e as regras entre chaves. |
| Configuração diferente entre mundos confundindo o diagnóstico | O `VC_CONFIG` no log e uma linha `VC_CONFIG_ACTIVE` no início de cada sessão com o que difere do padrão; os relatórios a mostram. |
| GameTests dependendo de valores | Testes rodam com os padrões; quem testa um valor usa `withOverrides` e desfaz. |
| Save antigo sem configuração | Ausente vale o padrão; nada a migrar além do `saveVersion`. |

## 5. Plano de testes

- **Unidade, Core:**
  - cada faixa: o limite aceito e o limite+1 recusado;
  - as regras entre chaves;
  - `reset`;
  - mutação: entra no PIT.
- **GameTest:**
  - `set` sem permissão é recusado;
  - `set` válido muda o comportamento (ex.: `flock.breedUpTo` 4 faz o pastor parar de criar em 4);
  - o valor sobrevive ao salvar e recarregar;
  - valor inválido gravado no save volta ao padrão.
- **Python:** os relatórios leem `VC_CONFIG_ACTIVE`.

## 6. Fases

1. **F1:** `ColonySettings`, persistência, comando e as chaves de estoque, rebanho, curral, cama e casas.
2. **F2:** terreno, foco, fundação e ausência; a linha `VC_CONFIG_ACTIVE` nos relatórios.
3. **F3 (opcional):** a tela no cliente.

## 7. Decisões que dependem do autor

1. **Escopo:** por mundo (cada save tem a sua, como a política de profissões), global (um arquivo em
   `config/` para todos os mundos), ou global com ajuste por mundo.
2. **Por colônia:** cada vila do mesmo mundo pode ter valores próprios, ou é um valor para o mundo todo.
3. **Como editar:** só o comando, comando e arquivo, ou já com a tela.
4. **Lista da fase 1:** a tabela da §2 inteira, ou só um subconjunto.
