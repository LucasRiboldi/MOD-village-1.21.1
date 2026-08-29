# Checklist — análise de mod externo

> Use ao estudar um mod de referência. Fecha a Fase 7 do
> `references/research-protocol.md`.

## Antes de abrir código

- [ ] **Escrevi a pergunta** que este mod deve responder — uma frase
- [ ] Localizei a fonte (`PesquisaFabricMOD/` ou repositório clonado)

> Sem a pergunta, você lê tudo e não conclui nada.

## Inventário (5 minutos)

- [ ] `gradle.properties` — versão do MC, mappings, Fabric API
- [ ] `fabric.mod.json` — id, entrypoints, `environment`, dependências
- [ ] `*.mixins.json` — quantos e quais
- [ ] Estrutura de pastas — organizada por tipo técnico ou por domínio?
- [ ] **Licença** conferida

- [ ] **A versão do mod bate com a minha?**
      Se não: ele responde "é possível?", não "como escrevo hoje"

## Localizar a feature

- [ ] Entrei pelo que o jogador vê (lang, item, bloco, mensagem)
- [ ] Achei o entry point: registro, evento ou Mixin
- [ ] **Classifiquei o degrau da escada** que o mod usou

## Seguir o fluxo

- [ ] Do entry point até o efeito no mundo
- [ ] **Parei quando a pergunta foi respondida** — não continuei "para completar"

## Touchpoints

- [ ] Mixins listados: alvo, tipo, ponto
      `grep -rn "@Inject\|@Redirect\|@Overwrite\|@ModifyVariable\|@Accessor" src/`
- [ ] APIs Fabric listadas:
      `grep -rn "net.fabricmc.fabric.api" src/main/java | sed 's/.*import //' | sort -u`
- [ ] Registros identificados
- [ ] Persistência identificada

## Julgamento

- [ ] Anotei o que é **reutilizável** (a ideia)
- [ ] Anotei o que é **a evitar** — igualmente valioso
- [ ] Classifiquei o risco de compatibilidade do mod
- [ ] **Não tratei o mod como autoridade** — ele resolveu o problema dele, na
      versão dele, com as restrições dele

## Licença

- [ ] Se vou reusar **código** (não ideia), a licença permite e a atribuição está
      registrada
- [ ] Na dúvida: li, entendi, e vou escrever o meu

## Saída

- [ ] `templates/mod-analysis.md` preenchido em `docs/research/mods/`
- [ ] A pergunta original foi respondida
- [ ] `research-status.md` atualizado
- [ ] Se há 2+ mods resolvendo o mesmo: considerar `templates/comparison.md`
