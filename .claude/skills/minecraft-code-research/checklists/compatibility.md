# Checklist — compatibilidade

> Use antes de fechar qualquer recomendação técnica. Compatibilidade não se
> conserta depois: é consequência do degrau da escada que você escolheu.

## Mixin

- [ ] Listei todos os Mixins e seus tipos
- [ ] **Nenhum `@Redirect` ou `@Overwrite`** — ou há justificativa escrita
- [ ] Considerei `@WrapOperation` (encadeável) no lugar de `@Redirect`
- [ ] Sei quais mods conhecidos miram as mesmas classes
- [ ] Nenhum Mixin assume índice de lista ou estado prévio
- [ ] Nenhum Mixin remove comportamento Vanilla sem justificativa

## Registro

- [ ] **Namespace próprio** — nada de `minecraft:`
- [ ] Registro **incondicional** (nada de `if (config)`)
- [ ] Ordem **determinística**
- [ ] Registro na janela certa (entrypoint, antes do freeze)

> Registro condicional produz ids diferentes entre cliente e servidor, e a
> conexão cai no handshake.

## Eventos

- [ ] A lógica **não depende de ordem** entre listeners de mods diferentes
- [ ] Se depende, isso está registrado como `[RISCO]`
- [ ] O resultado é o mesmo em qualquer ordem (idempotente, quando possível)

## Dados

- [ ] Tags usam `"replace": false`
- [ ] **Não substituo loot table / recipe Vanilla** — ou sei que substituo e
      declarei o risco
- [ ] Nenhum arquivo Vanilla é sobrescrito silenciosamente

## Estado

- [ ] Nenhum estado de mundo em campo `static`
- [ ] Se há ponto de acesso global, ele é **limpo no start e no stop** do servidor
- [ ] Estado não vaza entre saves

## Client / Server

- [ ] `environment` no `fabric.mod.json` bate com o que o código assume
- [ ] Nenhuma classe de cliente referenciada em código comum
- [ ] Testado em servidor dedicado (`runServer`), não só `runClient`
- [ ] Funciona se o cliente não tiver o mod — ou a dependência está declarada

## Versão

- [ ] Assinaturas verificadas nesta versão
- [ ] Nenhum código copiado de outra versão sem conferir mapping e semântica
- [ ] Formato de dados tem número de versão gravado (migração futura)

## Vizinhos prováveis

Se o alvo for aldeão / IA / POI, verifiquei o risco frente a:

- [ ] mods de aldeão (VillagerConfig, Easy Villagers, Guard Villagers…)
- [ ] mods de performance (reescrevem caminhos quentes, inclusive IA e POI)
- [ ] mods grandes com muitos touchpoints (Create)

## Classificação final

- [ ] Classifiquei: **LOW / MEDIUM / HIGH**
- [ ] Se HIGH: há justificativa escrita **e** plano de degradação
- [ ] Conflito conhecido está **documentado**, não escondido

> `[RISCO]` sem classificação é decorativo. Com classificação, é decisão.

## Degradação

- [ ] Se a minha integração falhar, o resultado é **comportamento Vanilla**
- [ ] Nenhuma exceção minha escapa de dentro de método Vanilla
