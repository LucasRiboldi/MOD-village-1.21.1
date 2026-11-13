# ADR-044 — Elegibilidade e escrita supervisionada do MAE

Estado: opção de elegibilidade A aprovada pelo autor em 2026-11-02; integrada na `main` (`MaeBuildQueue`, `MaeClearing`,
`MaeWorkStore`). Emendada pela ADR-047 (fila do construtor).

## Contexto

`VillageOrigin` identifica uma vila gerada sob a política atual. Não comprova que
um lote de expansão está intocado. O autor escolheu registrar evidência durante
a geração e revalidar antes de cada escrita; sem evidência, bloquear. A região
pode ser maior, supervisionada, mas cada obra exige aprovação individual.

## Decisão

1. Reutilizar `Building` para identidade e limites de lote. `WorkArea` representa
   somente um volume inclusivo; não concede permissão nem cria outro planejador.
2. Capturar impressões SHA-256 de estados por seção de chunk em
   `ServerChunkEvents.CHUNK_GENERATE`. Usar o attachment persistente Fabric
   `villagecolony:mae_generation`, versão 1. Nunca capturar por `CHUNK_LOAD`;
   nunca substituir recibo existente, inclusive versão desconhecida.
   Emenda (autor, 2026-11-05, Regra 202): a reserva `terrain_v2` substitui o recibo das
   seções da região pela impressão daquele momento, depois da conferência bloco a bloco.
   Medido no save: o recibo sai antes de o mundo assentar, e seção com água corrente
   diverge em 100% dos casos, e com folha em 92%, sem ninguém mexer. Perde-se a detecção
   de terra ou pedra posta pelo jogador antes da reserva.
3. Consultar somente chunks completos já carregados. Origem da vila não substitui
   a prova local. Referência estrutural cuja origem não esteja carregada bloqueia
   sem resolver `StructureAccessor` remotamente. Toda estrutura preexistente
   preserva o bounding box inteiro, incluindo espaços entre suas peças.
4. Persistir reservas, aprovação, progresso e histórico na seção opcional
   `maeWork` versão 1 do `ColonySavedData`. Manter payload futuro ou malformado
   opaco e impedir atualização. Não inferir permissões de saves legados.
5. Reservar a região não inicia obra. Aprovar um `Building` fixa lista de posições
   e estados antes/depois. Lote e margem de dois blocos devem caber na região.
   Uma obra pendente por região; regiões não se sobrepõem.
6. `MaeWorldWriter` é a única entrada de escrita **do MAE**. Antes de cada bloco,
   revalidar lote, margem, recibo, hashes esperados, `BlockProtection`, edificações,
   reservas de ruas, passagens de minas, fluidos, árvores, cultivos, BlockEntities
   e borda do mundo. Na dúvida, bloquear. A implementação é restrita ao Overworld,
   onde o registro atual de colônias opera.
7. Nesta fundação, aceitar somente ar e materiais passivos explicitamente listados
   em `MaeProtection.terrain`. Não autorizar remoção de vegetação, trabalho com
   fluidos ou blocos ativos. Esses comportamentos exigem a fase correspondente.
8. Orçamento compartilhado por mundo: até quatro tentativas por tick e dois
   milissegundos acumulados, conferidos entre operações indivisíveis. Uma operação
   pode ultrapassar dois milissegundos; isto não constitui limite de latência rígido.
9. Registrar intenção antes de escrever e confirmar estado posterior antes de
   avançar cursor. Falha bloqueia com causa; retomada é explícita e revalida.
   Falha sem escrita por confirmar entrega a região, sem desfazer nada (autor, 2026-11-05, Regra 205). Intenção sem confirmação nunca é repetida automaticamente. Só hashes confirmados
   pelo MAE podem substituir a referência de geração para uma obra seguinte.
   Emenda (autor, 2026-11-05, Regras 200 e 202): a limpeza de árvore, folha e mato que o
   próprio MAE faz antes da reserva também substitui a referência das seções da região.
   Emenda (autor, 2026-11-05, Regra 201): o volume aceita o chão natural do bioma e
   minério, além dos materiais passivos da primeira lista.
10. Limitar região a 128 × 128 colunas, 262.144 células e 128 seções verificadas;
    lote a 4.096 células e oito seções com margem. Limitar 128 regiões e 1.024
    mudanças históricas por região. Ao atingir limite, bloquear sem truncar.

## Consequências e limites

- A comparação é conservadora por seção completa: crescimento, queda de blocos
  ou edição vizinha pode invalidar uma área mesmo fora do lote. Isso não autoriza
  recapturar prova. Hash prova coincidência do estado atual com a captura, não
  autoria nem histórico completo de todas as interações anteriores.
  Emenda (autor, 2026-11-06, ADR-047): a obra pedida por `/vc mae obra` não usa esta prova durante a obra.
- O attachment exige percorrer seções durante a geração. Reserva e aprovação são
  chamadas supervisionadas síncronas e limitadas; o orçamento por tick cobre a
  execução das mudanças. Medir custo em carga real antes de ativar orquestração.
- Marcar `PersistentState` como sujo não é uma transação de disco entre chunk e
  metadados. Divergência ou intenção pendente bloqueia para análise. Histórico
  não implica rollback automático; não existe executor de rollback nesta fase.
- Nenhum planejador, construtor legado ou loop de aldeão chama o novo writer.
  A Fase 1 não centraliza escritores legados, não altera o grafo hierárquico e
  não implementa a Fase 2. A integração física exige aprovação posterior.
- As poucas linhas em `ColonySavedData` apenas conectam o proprietário persistente;
  codec e política ficam em classes próprias. Não dividir o proprietário só para
  retirar essas conexões do arquivo já próximo do limite de 500 linhas.

## Fontes e validação

Pesquisa nos sources das dependências fixadas do projeto: Minecraft 1.21.1,
Yarn `1.21.1+build.3`, Fabric API `0.116.15+1.21.1`; especialmente
`ServerChunkEvents` (`fabric-lifecycle-events-v1:2.6.0+0865547519`),
`AttachmentRegistry`, `AttachmentTarget` e `StructureAccessor`.

Casos, comandos, resultados e pendências: [MAE — Fase 1](../technical/MAE-Fase-1.md).
