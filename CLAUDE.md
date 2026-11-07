@AGENTS.md

# CLAUDE.md — Village Colony

> Mod Fabric para **Minecraft 1.21.1** que transforma vilas Vanilla em
> colônias autônomas. Os aldeões trabalham, produzem e constroem sozinhos
> — o jogador acha a vila e vai embora.
>
> **Este arquivo é o ponto de entrada.** Em divergência com qualquer outro
> documento, **este vence** — exceto onde ele delega explicitamente.

---

## 0. Contexto — leia isto antes de qualquer coisa

### 0.1 Quanto contexto você deve carregar

**O erro mais comum é ler demais.** O projeto tem documentação excelente
e longa, e o histórico tem mais de 4.600 linhas que **não** precisam ser
lidas para trabalhar. O tempo gasto lendo é tempo que não sobra para
implementar.

**Ordem canônica de leitura, para qualquer tarefa:**

| Ordem | Documento | Por quê |
|---|---|---|
| 0º | `HANDOFF.md` | **onde o trabalho parou** — só ao retomar de outro agente ou sessão |
| 1º | `STATE.md` | **o estado vivo** — P0 aberto, sessão em curso, verificação pendente |
| 2º | Este arquivo, §1 e §2 | as regras e o workflow |
| 3º | `docs/PATTERNS.md` | a assinatura do defeito que você está caçando |
| 4º | ADR ou doc específico do subsistema | a decisão que governa o que você vai tocar |

**Depois disso, procure por trecho.** Nunca leia inteiro:

| Documento | Regra |
|---|---|
| `docs/archive/technical/Development-Log.md` | **histórico.** Só `grep` por data ou símbolo. Nunca inteiro. |
| `docs/archive/technical/Project-State.md` | **histórico** desde 2026-08-26. Usar `docs/RULES.md` no lugar. |
| `docs/archive/technical/Backlog.md` | **histórico** desde 2026-08-15. Usar `TODO.md` no lugar. |
| `TODO.md` | **vivo e curto** — só o que está aberto. Histórico em `docs/archive/technical/TODO-ate-2026-10-07.md` (`grep`). |
| `docs/technical/Plano-de-Correcao.md` | **vivo.** Ler a régua (§1) e o item atual. |

**Regra dura:** se você não sabe o que procura, você está lendo o
documento errado. Vá para `STATE.md` primeiro.

### 0.2 Estado em uma linha

**O núcleo do MVP está implementado: oito profissões, sete titulares na
`BigHouseMOD`. A auditoria de 2026-10-06 (`docs/audit/`) e a ADR-035 definem
a direção técnica.** Contagem de testes, JAR e o que espera playtest: só no
`STATE.md` — número escrito em outro lugar envelhece e mente.

### 0.2.1 Uma linha de desenvolvimento por vez (ADR-035 §2)

- Todo trabalho sai de uma branch criada a partir da `main` atualizada e
  volta por PR para a `main`. Não se abre branch a partir de outra branch de
  trabalho, e dois agentes (Claude, Codex) não trabalham em paralelo em
  branches que tocam o mesmo sistema.
- Antes de começar: `git fetch` e `git log main..origin/main`. Se há outra
  branch viva tocando o mesmo sistema, pare e pergunte ao autor qual segue.
- Por que: em 03-10 e 04-10 duas linhas resolveram os mesmos três pedidos de
  formas incompatíveis (mina, fluidos, painel) e um GameTest ficou fora do
  registro; a integração custou um dia (`docs/audit/`, P0-01).

### 0.2.2 Modo temporário de correção por playtest

Ativo desde 2026-11-01. **Testes liberados pelo autor em 2026-11-05:** valem de
novo as exigências das seções 2 e 2.1. O resto deste modo continua: log primeiro,
escolha do autor, modelo por etapa, um JAR por ciclo.

Antes de iniciar cada etapa de triagem, diagnóstico, implementação ou análise
complexa, informe ao autor a etapa, o modelo e a força recomendados e aguarde
um "ok" explícito. O autor faz a troca manual; não afirme que ela ocorreu sem
confirmação. Recomendações atuais: triagem `gpt-6.1-terra`/`medium`;
diagnóstico delimitado `gpt-6.1-terra`/`high`; implementação entre subsistemas
`gpt-6.1-sol`/`high`; impasse complexo `gpt-6-astra`/`high`.
A tabela por atividade está em `docs/technical/Modelos-Codex.md`.
No Claude Code: triagem Sonnet 5.5/Médio; diagnóstico e implementação entre
subsistemas Opus 5.5/Alto; impasse complexo Opus 5.5/Extra. Tabela em
`docs/technical/Modelos-Claude.md`.
Indique se o perfil local ainda não corresponde à recomendação. A cada mudança
de etapa, peça novo "ok", mesmo que o modelo recomendado seja o mesmo.

1. Ler primeiro as notas do autor (`VC_BIOME_NOTE`, de `/vc bioma nota`) e os
   dados de `/vc dados`, `/vc bioma` e `/vc log`; cada nota é um achado. Contar
   os blocos que surgiram sozinhos e registrar no `TODO.md` para buscar fluxo de
   fabricação. Ler somente o `latest.log` mais recente e os relatórios `VC_*` gerados no
   jogo. Usar `time_ledger.py`, `action_report.py`, `cost_ledger.py`, `rg` e
   recortes por horário; não despejar logs inteiros no contexto.
2. Listar violações de regra, inconsistências, ócio, bloqueio, encalhe,
   repetição e custo anormal. Cada achado deve ter evidência e prioridade.
3. Localizar a causa e apresentar ao autor 2 ou 3 correções pequenas, com
   efeitos e riscos. Não editar comportamento antes da escolha.
4. Implementar somente a opção escolhida, com teste que falhe sem ela.
5. Revisar o diff e rodar o Sentinela (`scripts/test_lab.py`). Marcar a correção
   como candidata, pendente de confirmação no próximo playtest.
6. Fazer um commit atômico e gerar o JAR com `./gradlew.bat remapJar`. Copiar
   o JAR para `downloads/` e para a pasta `mods` do TLauncher; conferir o
   SHA-256 das três cópias.
7. O autor joga novamente. O próximo ciclo começa pelo novo log e pelos novos
   relatórios.

Push, merge e release continuam exigindo pedido explícito.

### 0.3 Não comece criando classes

Não suponha arquitetura. Não simplifique decisões existentes. Não escreva
código antes de responder:

```text
1. Qual problema está sendo resolvido?
2. Qual sistema é responsável?
3. Quais arquivos serão alterados?
4. Existe decisão arquitetural envolvida?
```

### 0.4 Toda verificação mede o tempo dos aldeões (Regra 50)

Pedido do autor, 2026-10-02. Depois de qualquer playtest, rode
`python scripts/time_ledger.py` (lê as linhas `VC_TIME` do `WorkTime`). Por
profissão, ele dá a proporção de trabalho, caminhada, espera, bloqueio, ócio
e encalhe no expediente.

- **Mais de 40% sem trabalhar** (espera + bloqueio + ócio + encalhe): o fluxo
  da profissão pede melhoria.
- **Mais de 10% bloqueado + encalhado:** há travamento a corrigir.

- **Regra 51 (autor, 2026-10-08): ocioso + bloqueado + encalhado ≤ 15%** por
  profissão no expediente (sem ofício fica fora). Quem fica sem tarefa entra na
  cadeia de alternativas: tarefa do ofício → manutenção do próprio ofício
  → coleta comum permitida. Pela regra temporária de 02-11, nenhuma profissão
  assume atividade de outra, inclusive ajuda de obra e reatribuição. Exceções do autor: o pedreiro ocioso calça o caminho (Regra 215), e quem não tem ofício calça e tira neve (Regra 220).
  Espera acima de 40% é alerta. Análise e alternativas:
  `docs/research/2026-10-08-tempo-ocioso.md`.

Uma correção de fluxo só está provada quando a proporção cai no playtest
seguinte. O GameTest prova o mecanismo; o `time_ledger` prova o efeito.

No mesmo playtest, rode `python scripts/action_report.py` (o diário de ações por aldeão) e
`python scripts/cost_ledger.py` (linhas `VC_COST`,
ADR-035 §5): média, p95 e máximo de cada fase do ciclo por faixa de colônias.
Nenhuma otimização de desempenho começa sem esse número.
Toda análise de Spark mostra ao autor também a saída do `action_report.py`, com os motivos de
recusa e desistência por profissão (pedido do autor, 2026-10-08).

### 0.5 Comentário diz a regra, não a história (ADR-035 §6)

- O javadoc diz **o que vale hoje** e cita a fonte: Regra N, ADR-NNN, KF-NNN.
- Sessão, playtest, número de log e quem pediu ficam no commit e no ADR —
  o git guarda a história melhor que o comentário, e comentário velho mente.
- Mudou uma regra? No mesmo commit, `grep` pelo número dela e corrija todo
  comentário e documento que a descreve.
- Comentários históricos que já existem não são reescritos em massa: enxugue
  o do método que você estiver tocando.

---

## 1. Stack

Fabric 1.21.1 · Java 21 · Gradle + Loom

## 2. Regras de desenvolvimento

1. Antes de modificar comportamento vanilla, use `minecraft-code-research`
   para localizar a classe Yarn correspondente. Cite o arquivo.
2. Todo bugfix vem com Fabric GameTest de regressão (red → green).
3. Toda nova regra de negócio entra como teste em `src/gametest`.
4. Não reescrever classes de domínio sem antes rodar `graphify`.
5. Após implementação, rodar `./gradlew build && ./gradlew test`.
6. Commits atômicos (Conventional Commits).
7. Se encontrar bloqueio arquitetural, PARAR e apresentar hipóteses
   antes de improvisar.
8. Quando houver testes, alimente o Sentinela (autor, 2026-11-05): rode por
   `python scripts/test_lab.py <perfil>` e depois `python scripts/sentinel_analysis.py`,
   com o Minecraft fechado. Mudou regra ou teste: `python scripts/rule_coverage.py`.

### 2.1 Workflow por etapa

| Etapa | Skill | O pedido |
|---|---|---|
| Antes de tocar em comportamento vanilla | `minecraft-code-research` | Mostrar como o vanilla 1.21.1 faz (ex.: como `VillagerEntity.tick()` e o `Brain` chamam `Sensor.tick()`), na ordem exata, antes de decidir o que entra no Mixin. |
| Antes de corrigir bug | `debugging-strategies` | Listar as 5 hipóteses mais prováveis dado o código (arquivo e linha). Não corrigir ainda — só diagnosticar. |
| Depois de entender o bug | `minecraft-testing` | Criar um Fabric GameTest que reproduz o bug. Ele deve falhar agora. |
| Depois do teste falhar | `fabric-development` | Corrigir o bug. O GameTest deve passar. |
| Ao finalizar feature ou bugfix | `clean-code` + `code-review-excellence` | Revisar o diff do PR contra as regras do mod. |

### 2.2 Comportamento que não se aceita

- "Vou implementar uma solução para X" sem antes pesquisar como o vanilla faz.
- "Corrigi o bug, teste manualmente" — sempre exija teste automatizado.
- Reescrita de classe de domínio sem antes mapear dependências.

## 3. Skills a priorizar

- `minecraft-code-research` (sempre antes de tocar em vanilla)
- `minecraft-testing` (todo bugfix)
- `fabric-development` + `minecraft-villager-systems` (implementação)
- `debugging-strategies` (diagnóstico)

@.fabric/AGENTS.md
