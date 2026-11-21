---
name: vc-log-driven-fix
description: >-
  Use quando o autor fornecer evidência de uma falha do Village Colony (Fabric 1.21.1)
  em latest.log, crash-report, saída de teste ou relatório VC_* e pedir diagnóstico
  ou correção. Features, refatorações e ajustes de documentação sem falha associada
  ficam fora deste fluxo.
---

# Correção orientada a logs do Village Colony

Siga `CLAUDE.md` §§0.2.2, 2 e 2.1 e o fluxo de playtest do `AGENTS.md`. Antes de cada etapa coberta pelo §0.2.2, recomende modelo e esforço e aguarde `ok`. A escolha numerada autoriza o escopo da correção.

## 1. Recortar a evidência

Use trechos do `logs/latest.log` mais recente, `crash-reports/crash-*.txt`, saída de testes ou relatórios `VC_*`. Em playtest, comece pelas notas `VC_BIOME_NOTE` e pelos dados de `/vc dados`, `/vc bioma` e `/vc log`. Confira a versão, o horário e o cenário. Isole as linhas do sintoma e a cadeia `ERROR` / `Exception` / `Caused by` relevante.

Liste cada achado com prioridade, tipo de falha, trecho literal e regra ou sistema afetado. Cite o logger `villagecolony` ou o marcador `VC_*` quando presente. Para `runGametest`, inclua o nome do teste. Se o material estiver incompleto, peça só o recorte que falta.

## 2. Diagnosticar

Correlacione o recorte com código, testes e regras vigentes. Siga o §2.1 para levantar hipóteses com arquivo e linha. Identifique a causa mais bem sustentada e confira se explica o recorte. Se faltar evidência, escreva `EVIDÊNCIA_INSUFICIENTE` e indique qual observação distinguiria as hipóteses. Se a solução exigir uma decisão arquitetural sem ADR aplicável, pare e peça deliberação.

## 3. Apresentar opções e esperar a escolha

Ofereça 2 ou 3 opções. Se a causa continuar incerta, inclua investigação adicional e indique quais opções dependem de confirmação. Use este formato para cada uma:

```text
🔧 Opção [N] — [título curto]
📍 Evidência: [trecho do log ou relatório]
🛠️ O que muda: [arquivo ou classe e alteração proposta]
🧪 Verificação: [teste ou observação que comprova o resultado]
⚠️ Risco: [baixo, médio ou alto e possível efeito colateral]
```

Aguarde o número da opção antes de executar qualquer uma delas.

## 4. Executar a opção escolhida

Para alterar código, parta de uma branch nova sobre a `main` atualizada conforme a ADR-035 §2. Se houver outra branch ativa no mesmo sistema, consulte o autor. Rode `./gradlew.bat build` antes da edição e pare se a linha de base falhar. Para bugfix, crie um Fabric GameTest que falhe pela causa correta, aplique a menor correção escolhida e confirme que ele passa.

Preserve Fabric 1.21.1, Java 21, versões fixas e o logger compartilhado `LoggerFactory.getLogger(MOD_ID)` para logs novos. Siga as invariantes de `AGENTS.md`, incluindo a proibição de `@Overwrite`, a exigência de ADR para mixin novo e o mundo como fonte da verdade. Mantenha o CI fora do ciclo.

## 5. Verificar e entregar a candidata

Rode `./gradlew.bat build` e `./gradlew.bat test` após implementar. Rode `./gradlew.bat runGametest` para bugfix ou alteração em `fabric/`. Revise o diff e execute o Sentinela com `scripts/test_lab.py`, seguindo `CLAUDE.md` §2. Registre os comandos e as saídas decisivas. Se a falha persistir ou surgir outra, volte ao diagnóstico com o novo recorte.

Atualize `STATE.md`, `TODO.md` e o log de desenvolvimento conforme o `AGENTS.md`. Após os gates, faça o commit atômico no formato `P0.x: descrição (teste que prova)`. Rode `./gradlew.bat remapJar`, copie o JAR para `downloads/` e para `mods` do TLauncher e confira o SHA-256 das três cópias. Push, merge e release exigem pedido explícito.

## 6. Reportar

```text
✅ Correção: [opção escolhida e resultado]
📁 Arquivos alterados: [lista]
🧪 Testes executados: [comando + resultado]
🧱 Invariantes tocadas: [Regra N, ADR-NNN ou nenhuma]
📌 Pendente de playtest: [o que observar em jogo]
```

Em um ciclo com JAR, reporte a conferência dos hashes e trate o resultado como candidato até o playtest do autor. Para uma opção só de investigação, reporte a evidência obtida e a próxima decisão.
