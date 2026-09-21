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
| `Development-Log.md` | **histórico.** Só `grep` por data ou símbolo. Nunca inteiro. |
| `Project-State.md` | **histórico** desde 2026-08-26. Usar `docs/RULES.md` no lugar. |
| `Backlog.md` | **histórico** desde 2026-08-15. Usar `TODO.md` no lugar. |
| `TODO.md` | **vivo** — mas 700 linhas. Ler o topo (primeiros 100) e `grep` o resto. |
| `docs/technical/Plano-de-Correcao.md` | **vivo.** Ler a régua (§1) e o item atual. |

**Regra dura:** se você não sabe o que procura, você está lendo o
documento errado. Vá para `STATE.md` primeiro.

### 0.2 Estado em uma linha

**O núcleo do MVP está implementado, com oito funções operacionais, seis
titulares na `BigHouseMOD` e sete profissões produtoras. A rodada de 2026-09-21
ainda tem uma falha obrigatória de GameTest e playtests pendentes.** O estado
vivo e a auditoria estão em `STATE.md` e
`docs/technical/Project-Audit-2026-09-21.md`.

### 0.3 Não comece criando classes

Não suponha arquitetura. Não simplifique decisões existentes. Não escreva
código antes de responder:

```text
1. Qual problema está sendo resolvido?
2. Qual sistema é responsável?
3. Quais arquivos serão alterados?
4. Existe decisão arquitetural envolvida?
