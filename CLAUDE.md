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
| `TODO.md` | **vivo** — mas 700 linhas. Ler o topo (primeiros 100) e `grep` o resto. |
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

Uma correção de fluxo só está provada quando a proporção cai no playtest
seguinte. O GameTest prova o mecanismo; o `time_ledger` prova o efeito.

No mesmo playtest, rode `python scripts/cost_ledger.py` (linhas `VC_COST`,
ADR-035 §5): média, p95 e máximo de cada fase do ciclo por faixa de colônias.
Nenhuma otimização de desempenho começa sem esse número.

### 0.5 Comentário diz a regra, não a história (ADR-035 §6)

- O javadoc diz **o que vale hoje** e cita a fonte: Regra N, ADR-NNN, KF-NNN.
- Sessão, playtest, número de log e quem pediu ficam no commit e no ADR —
  o git guarda a história melhor que o comentário, e comentário velho mente.
- Mudou uma regra? No mesmo commit, `grep` pelo número dela e corrija todo
  comentário e documento que a descreve.
- Comentários históricos que já existem não são reescritos em massa: enxugue
  o do método que você estiver tocando.
