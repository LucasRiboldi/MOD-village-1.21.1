---
description: Roda o Gauntlet Loop sobre as alterações atuais — verificação por subagente independente até PASS ou até o limite de iterações
argument-hint: [requisito em uma linha] [--max-iterations N]
---

# Gauntlet Loop

Verifica a alteração que está na árvore de trabalho agora, com o
`gauntlet-verifier` como único juiz. Você é o **Builder**: implementa e
corrige, e não libera o próprio trabalho.

Requisito e opções: `$ARGUMENTS`

Se o requisito vier vazio, deduza-o da conversa e **escreva-o em uma
linha antes de começar** — é contra ele que o Verifier vai medir, e um
requisito vago produz uma verificação vaga.

`MAX_ITERATIONS` padrão é **5**.

## O laço

**1 — Prepare.** Antes de chamar o Verifier, faça a conferência barata
você mesmo, para não gastar uma iteração com o que já dá para ver:

```bash
git status --porcelain
git diff --stat HEAD
python scripts/gauntlet.py --requirements "<o requisito>"
```

Sem `--deep` isso não roda a bateria de gametest — é a passagem rápida.
Se ela já acusar FAIL, conserte antes de chamar o Verifier.

**2 — Chame o Verifier.** Use a ferramenta Agent com
`subagent_type: "gauntlet-verifier"`, e passe no prompt:

- o requisito original, na íntegra;
- o número da iteração e o `MAX_ITERATIONS`;
- o que mudou desde a iteração anterior, quando houver;
- o que a iteração anterior reprovou, quando houver;
- a instrução de rodar `python scripts/gauntlet.py --deep --iteration N`.

Não resuma o seu próprio trabalho de forma favorável, e **não diga que
está correto**. Descreva o que você fez; quem julga é ele.

**3 — Leia o veredito.**

| | o que fazer |
|---|---|
| **PASS** | entregue. Relate ao autor o que foi implementado, os testes executados, o resultado, o número de iterações e as ressalvas do Verifier |
| **FAIL** | conserte **o que ele apontou**, e nada além. Volte ao passo 1 com `iteration + 1` |
| **BLOCKED** | pare. Não conserte às cegas: diga ao autor o que impediu a verificação |

**4 — Ao corrigir**, rode primeiro o teste que falhou, sozinho, e só
depois a bateria — a ordem está na saída do Verifier.

**5 — No limite.** Se a iteração passar de `MAX_ITERATIONS` sem PASS, o
resultado é **BLOCKED**. Nunca diga que ficou pronto. Relate:

- o que ainda está quebrado;
- por que não foi aprovado;
- o que foi tentado em cada iteração (está em `build/gauntlet/ledger.json`);
- qual é o próximo passo.

## Regras do laço

1. **O Builder nunca declara sucesso sozinho.** Só PASS do Verifier libera.
2. **Não conserte o teste para conseguir PASS.** Se um teste existente
   está errado, isso se argumenta no relatório, não se edita em silêncio.
3. **Não alargue o escopo entre iterações.** Corrigir o que foi apontado
   é uma coisa; aproveitar a viagem é outra, e faz o Verifier ter de
   revisar o que ninguém pediu.
4. **BLOCKED não vira PASS.**
5. **Não commite** — este projeto commita quando o autor pede, e o jar de
   `downloads/` entra junto. Ver `CLAUDE.md`.
6. **Depois do Verifier, confira `git status`**: nada em `src/` pode ter
   mudado por conta dele.

## Quando este laço não se aplica

Alteração só de documentação, de comentário ou do `TODO.md` não precisa
do laço — mas **passe pela conferência rápida do passo 1** mesmo assim,
que custa segundos e pega arquivo fora do escopo.
