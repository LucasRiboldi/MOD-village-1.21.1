---
name: relatorio-commit-ritual
description: "Quando o usuário pedir \"relatorio commit\", rodar o encerramento de ciclo completo — revisão, testes, docs, pendências, commit, push e relatório final"
metadata: 
  node_type: memory
  type: feedback
  modified: 2026-08-16T01:55:09.838Z
---

Quando o usuário disser **"relatorio commit"**, ele está pedindo o
encerramento de um ciclo de desenvolvimento inteiro, e não um commit.
Rodar o ritual abaixo na ordem, sem pular etapa.

**Por quê:** o usuário definiu este modelo em 2026-08-15 depois de um
ciclo em que várias correções ficaram verdes na bateria e nunca foram
verificadas em jogo. O objetivo declarado dele é que outro
desenvolvedor — ou ele mesmo numa próxima sessão — consiga continuar
exatamente de onde o ciclo terminou.

**Como aplicar.** A ordem é fixa:

```text
código → testes → README → TODO/backlog → git diff → commit → push → verificação
```

1. **Estado atual, antes de mexer em nada.** `git status`, branch,
   remote, diferença local/remoto, e o diff do ciclo inteiro
   (`git diff --stat <base>..HEAD`). Separar o que é deste ciclo do que
   já estava lá.
2. **Revisar o código do ciclo** procurando bug, código morto,
   duplicação, nome ruim, erro de organização, validação frouxa,
   crescimento sem teto, funcionalidade pela metade. Corrigir só o que
   for do ciclo, seguro, e sem mudar comportamento à toa. O resto vira
   pendência registrada.
3. **Rodar de verdade:** build limpo, testes unitários, testes de jogo,
   lint. **Nunca dizer que passou sem ter executado**, e dizer quais
   verificações rodaram e o resultado de cada uma.
4. **README** representando o estado de hoje, não o de ontem. Conferir
   funcionalidades, limitações, instruções de execução, estrutura,
   tecnologias.
5. **Pendências por prioridade** em `TODO.md`: 🔴 crítico, 🟠
   importante, 🟡 melhoria, 🟢 futuro. Não inventar item para encher.
6. **Seção do ciclo** — Implementado / Corrigido / Melhorado / Pendente.
7. **Higiene do repositório:** nada de `.env`, segredo, token, build,
   cache, log, arquivo de IDE. Achando segredo já rastreado, **parar e
   avisar**, não empurrar.
8. **Commit** com mensagem que explica o objetivo do ciclo. Nunca
   `update`, `changes`, `final`.
9. **Push** sem `--force`. Divergência ou conflito: **parar e explicar**.
10. **Verificação pós-push** e o relatório final no formato
    `✅ CICLO ENCERRADO` — Implementado, Corrigido, Documentação
    (SIM/NÃO por arquivo), Testes (quais e resultado), Git (branch,
    commit, push), Pendências por prioridade, Próximo ciclo recomendado,
    Estado final.

**O que ele mais cobra:** não marcar como concluído o que não foi
validado, e distinguir *"tem teste"* de *"foi visto em jogo"* — são
coisas diferentes neste projeto e ele quer as duas separadas no
relatório. Etapa que não deu para concluir se declara: o que foi feito,
o que não foi, por quê, e o que fica para o próximo ciclo.

Ver [[commit-push-atalho]] para o pedido curto, que é outra coisa.
