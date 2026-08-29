# Checklist — pesquisa concluída

> Use antes de dizer que a pesquisa acabou. Vale para os modos FEATURE, DEEP e
> FORENSIC. Em QUICK, só a primeira e a última seção.

## A pergunta

- [ ] **A pergunta original foi respondida** — diretamente, não com um resumo do
      que foi lido
- [ ] Se não foi possível responder, isso está dito explicitamente, com o que
      falta

## Evidência

- [ ] Toda afirmação técnica tem etiqueta (`[FATO]`, `[INFERÊNCIA]`, `[HIPÓTESE]`…)
- [ ] Todo `[FATO]` tem fonte: arquivo · classe · método · versão
- [ ] **Nenhum número de linha inventado**
- [ ] Assinaturas citadas foram verificadas com `javap`, não lembradas
- [ ] "Não existe API para X" foi verificado nos sources, não afirmado de memória
- [ ] Confiança declarada onde a conclusão sustenta decisão

## Versão

- [ ] Versão do Minecraft, mappings e Fabric API registrados no documento
- [ ] Conclusões que dependem de versão estão marcadas com `[VERSÃO]`

## Cobertura

Aplicável ao caso, e marcado como "não se aplica" com motivo quando não:

- [ ] Sistema Vanilla mapeado
- [ ] Ciclo de vida identificado
- [ ] Estado e dono identificados
- [ ] Extension points levantados **na ordem da escada**
- [ ] Fabric API verificada
- [ ] Outros mods consultados
- [ ] Client/Server analisado
- [ ] Persistência analisada
- [ ] Performance considerada, com números
- [ ] Compatibilidade classificada

## Decisão

- [ ] Há uma **recomendação**, não só um levantamento
- [ ] O degrau da escada está nomeado
- [ ] Se subiu a escada, a justificativa está escrita
- [ ] Riscos listados e classificados
- [ ] O que faria mudar de ideia está registrado

## Artefatos

- [ ] Documento gravado em `docs/research/` (template certo)
- [ ] `research-status.md` atualizado: objetivo, fatos, hipóteses, riscos, próximo passo
- [ ] `FUTURE RESEARCH` registra o que foi deliberadamente adiado
- [ ] Se houve decisão: ADR em `docs/decisions/`
- [ ] Se houve experimento: registro em `docs/experiments/`
- [ ] **Nenhum documento duplicado** — atualizei o existente em vez de criar outro

## Honestidade

- [ ] O que **não** foi investigado está dito
- [ ] O que ficou em hipótese está marcado como tal, com o próximo passo
- [ ] Se a pesquisa não mudou nenhuma decisão nem reduziu risco, **isso está dito**

## Handoff

- [ ] Se o próximo passo é implementar, o pacote para `fabric-development` está
      completo: sistema identificado, ciclo de vida, degrau escolhido, riscos,
      caminho dos documentos
- [ ] Um agente sem o contexto desta sessão conseguiria continuar lendo só os
      arquivos
