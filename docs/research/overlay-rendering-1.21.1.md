# Pesquisa — overlays no mundo, Fabric 1.21.1

**Objetivo:** adicionar apresentação client-side sem mudar a autoridade do servidor.

- `[FATO]` `WorldRenderEvents.AFTER_ENTITIES` é a extensão Fabric indicada para
  anexar renderização ao mundo sem injetar `WorldRenderer`.
- `[FATO]` `WorkerNameplate` e `SiteMarker` existem para clientes Vanilla; não
  serão removidos.
- `[FATO]` `WorkerService` fornece profissão e `ConstructionProject` fornece
  estado, planta, blocos restantes e materiais restantes.
- `[DECISÃO]` snapshots são enviados a cada 20 tiques e o renderer lê apenas o
  cache cliente.
- `[RISCO]` shaders e oclusão exigem playtest; nenhum mixin será introduzido
  para compensar comportamento visual sem evidência.
