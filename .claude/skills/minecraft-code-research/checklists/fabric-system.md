# Checklist — verificação da Fabric

> Use antes de concluir "a Fabric não resolve isso". Fecha a Fase 6 do
> `references/research-protocol.md`.

## Antes de procurar

- [ ] Sei se o que preciso é do **Loader** (entrypoint, carga, Mixin) ou da
      **API** (eventos, registros, networking, datagen)
- [ ] Versão da Fabric API anotada

## Procura

- [ ] Localizei os sources jar do projeto:
      `find . -path "*loom-cache/remapped_mods*" -name "*-sources.jar"`
- [ ] Identifiquei o módulo provável pelo nome
      (`lifecycle-events`, `entity-events`, `networking`, `object-builder`,
      `registry-sync`, `data-generation`, `transfer`, `command`, `biome`, `gametest`…)
- [ ] **Listei as classes do módulo**, não confiei em memória:
      `unzip -l "$J" | grep "\.java$"`
- [ ] Li a classe candidata: `unzip -p "$J" <caminho>.java`

> Ausência afirmada de memória é hipótese, e costuma estar errada — a API é grande.

## Se encontrei

- [ ] **Quando dispara** exatamente — antes ou depois do efeito?
- [ ] **Em qual side** roda
- [ ] **É cancelável?** Qual o tipo de retorno?
- [ ] **Ordem entre mods** — a minha lógica depende disso? (se sim, é `[RISCO]`)
- [ ] O que acontece se o meu listener lançar exceção
- [ ] A versão de API (`v1`/`v2`/`v0`) bate com a que o resto do projeto usa

## Se não encontrei

- [ ] Escrevi **onde procurei**, não só que não achei
- [ ] Marquei como `[FATO]` com o método de busca, ou `[HIPÓTESE]` se a busca foi rasa
- [ ] Registrei qual é o próximo degrau da escada

## Entrypoints

- [ ] Sei qual entrypoint é o certo para o que vou registrar
      (`main` / `client` / `server` / `fabric-gametest`)
- [ ] Nada que precise de `MinecraftServer` ou `World` está no entrypoint
- [ ] Código de cliente está no entrypoint `client`, não no `main`

## Saída

- [ ] `research-status.md` com a linha "API Fabric investigada"
- [ ] Limitações registradas, não só a existência
