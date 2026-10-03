# ADR-025 — Overlays pixel art no cliente

**Status:** aceita
**Data:** 2026-10-02
**Minecraft:** 1.21.1 · **Mappings:** Yarn 1.21.1+build.3 · **Fabric API:** 0.116.15+1.21.1

## Contexto

`WorkerNameplate` e `SiteMarker` já comunicam profissão e obra a clientes Vanilla.
O novo painel com ícones e materiais precisa de renderização e, portanto, de um
cliente com o mod. A lógica da colônia continua exclusivamente no servidor.

## Decisão

`[DECISÃO]` O servidor publica snapshots mínimos e periódicos; o cliente os
guarda e desenha overlays por `WorldRenderEvents`, sem mixin nem estado de mundo
cliente persistido.

## Consequências

- Clientes Vanilla continuam vendo as placas existentes.
- Clientes com o mod recebem somente IDs de entidades e dados de apresentação.
- Renderização não consulta baús, mundo do servidor ou rede a cada frame.
- O painel usa recursos próprios; itens de materiais continuam sendo itens Vanilla.

## Validação

Compilação, testes unitários do codec/cache, GameTest do fluxo servidor e
playtest cliente para distância, billboard e leitura dos sprites.
