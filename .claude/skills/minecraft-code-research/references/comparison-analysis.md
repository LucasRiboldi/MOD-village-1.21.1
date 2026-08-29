# Análise comparativa

Quando vários mods resolveram um problema parecido com o seu, comparar rende mais
que estudar um a fundo. Um mod mostra **uma** solução; três mostram o **leque** —
e o leque é o que permite escolher com critério.

## O que a comparação é e o que não é

**Não é** eleger o melhor mod. Cada um resolveu o problema dele, na versão dele,
com restrições que você não tem.

**É** responder: quais abordagens existem, o que cada uma custa, e qual serve ao
meu caso?

## Quando vale

- Existem 2+ implementações acessíveis do mesmo problema.
- A decisão é de arquitetura (vai durar) e não local.
- As abordagens diferem em algo que importa — não só em estilo.

Quando **não** vale: uma implementação só (não é comparação, é análise —
`mod-analysis.md`); a decisão é trivial e reversível.

## Método

**Uma pergunta comparativa, dita antes de abrir o código.**

> "Como diferentes mods fazem o aldeão trabalhar sem quebrar a IA Vanilla?"

Sem isso, você compara arquitetura em geral e não conclui nada.

**Uma linha por implementação, incluindo o Vanilla e o projeto atual.** O Vanilla
é a linha mais importante: é a linha de base e frequentemente a resposta.

```text
SISTEMA: <a pergunta>
├── Vanilla          o que o jogo já faz
├── Mod A
├── Mod B
├── Mod C
└── Nossa conclusão
```

**As mesmas colunas para todos.** Comparação com critérios variáveis é opinião
com tabela.

| Dimensão | O que olhar |
|---|---|
| Versão / mappings | vale a comparação? |
| Abordagem | em uma frase |
| Degrau da escada | onde parou |
| Touchpoint Vanilla | o que toca |
| Fabric API | o que usa |
| Mixins | quantos, que tipo, onde |
| Dados | JSON, NBT, componentes |
| Persistência | o que sobrevive |
| Client/Server | como divide |
| Performance | custo aparente |
| Compatibilidade | risco (`compatibility-analysis.md`) |
| Vantagens | o que ganha |
| Riscos | o que custa |

## Registrar

Use `templates/comparison.md`. Uma tabela real, com uma linha por mod:

```markdown
| Dimensão | Vanilla | Mod A | Mod B | Nosso projeto |
|---|---|---|---|---|
| Versão | 1.21.1 | 1.20.1 | 1.21.1 | 1.21.1 |
| Abordagem | Brain + Schedule | bloco captura o aldeão | task extra no Brain | ? |
| Degrau | — | Mixin + BlockEntity | registro + task | ? |
| Mixins | — | 4 (2 Redirect) | 1 (Inject TAIL) | ? |
| Risco compat. | — | ALTO | BAIXO | ? |
```

A coluna do seu projeto começa com `?` e é preenchida na conclusão. É ela que
transforma tabela em decisão.

## Ler a tabela

Padrões que aparecem quando a tabela fica pronta:

- **Todos usam o mesmo mecanismo Vanilla** → esse é o caminho previsto. Divergir
  exige motivo forte.
- **Cada um resolveu diferente** → não há caminho canônico; a escolha é sua e
  vale documentar a decisão.
- **Um usa muito menos Mixin que os outros** → provavelmente achou um extension
  point que os demais não viram. Vale investigar **esse** a fundo.
- **O de versão mais nova é mais simples** → a API ganhou suporte no meio do
  caminho, e os mais antigos carregam contorno histórico.
- **Todos evitam a mesma coisa** → há uma armadilha ali. Descubra qual antes de
  ser o primeiro a cair nela.

A quarta é a que mais economiza trabalho: muito contorno em mod antigo é dívida
de época, não sabedoria a copiar.

## Ponderação honesta

Nem toda dimensão pesa igual **para você**. Declare o peso antes de concluir:

```text
Este projeto prioriza: compatibilidade > performance > simplicidade > completude.
Logo, a abordagem do Mod B ganha apesar de cobrir menos casos.
```

Sem isso, a conclusão parece objetiva e é preferência não declarada — que é como
se justifica a escolha que já se queria fazer.

## Conclusão

Termina com:

1. **A escolha**, em uma frase.
2. **Por quê**, ligado às dimensões que você declarou pesar.
3. **O que você está abrindo mão** — toda escolha tem um custo; nomeá-lo é o que
   permite revisitar depois.
4. **O que faria mudar de ideia** — o gatilho para reabrir a decisão.

O item 4 é o mais valioso e o mais esquecido. "Se precisarmos que funcione com o
chunk descarregado, esta escolha não serve mais" é o que evita que a decisão seja
defendida por inércia dois anos depois.

Uma escolha registrada assim vira `[DECISÃO]` e vai para `docs/decisions/` com o
`templates/architecture-decision.md`.
