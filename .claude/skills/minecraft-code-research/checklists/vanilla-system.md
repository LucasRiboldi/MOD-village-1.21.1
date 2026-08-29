# Checklist — análise de sistema Vanilla

> Use ao investigar como o Minecraft faz alguma coisa. Fecha a Fase 4 do
> `references/research-protocol.md`.

## Ambiente

- [ ] Versão do Minecraft e mappings anotados
- [ ] `$MC_JAR` e `$MAPPINGS` localizados
- [ ] Fontes gerados, se precisar ler corpo de método (`./gradlew genSources`)

## Localização

- [ ] Entrei pelo que o jogo **mostra** (id, chave de lang, som), não pelo nome
      que eu imaginei
- [ ] Classe principal encontrada e **confirmada nesta versão**
- [ ] Classes de apoio identificadas

## A cadeia

- [ ] **Entry** — o que dispara o sistema
- [ ] **Core** — onde a decisão mora
- [ ] **Supporting** — o que ela usa
- [ ] **Data** — o que ela lê
- [ ] **State** — o que ela guarda, e onde
- [ ] **AI** — se houver
- [ ] **Events** — o que ela emite
- [ ] **Persistence** — o que sobrevive
- [ ] **Integration** — quem mais toca nisso

## Callers

- [ ] Mapeei quem chama o método central
- [ ] Sei por **quantos caminhos diferentes** ele é alcançado
- [ ] Verifiquei se o retorno é usado

## Sinais de perigo

- [ ] `static` mutável no caminho
- [ ] chamado no tick de todas as entidades
- [ ] carrega chunk (`getBlockState`/`getBlockEntity` em posição arbitrária)
- [ ] assinatura com `RegistryWrapper.WrapperLookup` (1.20.5+)
- [ ] lambda/classe anônima no alvo pretendido
- [ ] sobrecargas com o mesmo nome

## Extension points

- [ ] Verifiquei registro, data-driven, `protected`, interface e Fabric API
- [ ] Registrei quais são viáveis e quais não, **com o motivo**

## Saída

- [ ] Documento preenchido (`templates/system-analysis.md`)
- [ ] Uma `templates/vanilla-class-analysis.md` por classe importante
- [ ] Afirmações etiquetadas, com fonte
- [ ] Nenhum número de linha inventado
- [ ] `research-status.md` atualizado
- [ ] A pergunta original foi **respondida**, não resumida
