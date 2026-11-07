---
name: website-sem-bundler
description: Website/ (com W maiúsculo) não tem package.json nem vite config; o index.html virou HTML estático autocontido em 2026-09-12
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-12T04:49:14.425Z
---

**A pasta é `Website/`, com W maiúsculo.** Vale saber porque o Windows acha o
arquivo de qualquer jeito, mas `git log -- website/` (minúsculo) devolve
**vazio** — pathspec do git é sensível a maiúscula. Caí nisso e concluí errado
que a pasta era untracked; ela tem três commits (`786bcda`, `0a2aff7`,
`18a0047`) e rastreia `Website/index.html`, `Website/src/main.jsx` e
`Website/src/style.css`.

Era um shell React: o `index.html` carregava `/src/main.jsx` como módulo, com
JSX dentro e 1.525 linhas de CSS ao lado.

**Não havia `package.json` nem `vite.config` em lugar nenhum do repositório** —
conferido com `find -maxdepth 2`. JSX não roda no navegador sem build, então
aquela página **nunca pôde ter funcionado** como estava.

Em 2026-09-12 o autor pediu *"edite a pasta website, sendo um html que
apresenta o projeto, liste todas as profissoes, levante o que cada uma ja
executa o que tem erro e o que falta fazer"*. O `index.html` passou a ser
**HTML estático autocontido** — CSS inline, zero build, abre clicando — com a
ficha das oito profissões em três colunas: Executa / Falha / Falta.

**`Website/src/` ficou órfã e não foi apagada.** É trabalho do autor e a
decisão de remover é dele. Se alguém for reintroduzir React ali, vai precisar
de toolchain — não existe nenhuma.

**Why:** a próxima sessão que abrir `Website/` vai ver `main.jsx` e supor que
há um build por trás. Não há, e o `index.html` já não o referencia.

**How to apply:** a página é a fonte pública do estado por profissão, e os
números dela (780 unitários, 304 gametests, 4 erros abertos) envelhecem —
conferir contra os XML de `build/test-results/` e a tabela de erros do
`TODO.md` antes de republicar. A tabela de ofícios do `README.md` (ferramenta,
tarefa, onde guarda) é a referência canônica dos nomes; o site seguiu ela.
