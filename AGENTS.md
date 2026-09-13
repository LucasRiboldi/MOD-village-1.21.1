# AGENTS.md — Codex no Village Colony

`CLAUDE.md` é a fonte canônica do projeto. Este arquivo concentra apenas o fluxo do Codex e as regras que ele deve aplicar neste repositório; em conflito, `CLAUDE.md` vence.

## Contexto mínimo

1. Leia `STATE.md` — somente as primeiras 60 linhas.
2. Leia `CLAUDE.md` e use `docs/PATTERNS.md` na seção correspondente ao sintoma.
3. Consulte a ADR ou o documento do subsistema que será alterado.

Não leia documentos históricos inteiros. Use `rg` para localizar trechos em `Development-Log.md`, `Project-State.md` e `Backlog.md`. Em `TODO.md`, leia o topo (100 linhas) e pesquise o item específico.

## Antes de alterar

- Identifique o problema, o sistema responsável, os arquivos envolvidos e se há decisão arquitetural.
- Se a mudança exigir decisão não coberta por ADR, pare e peça deliberação; não invente arquitetura.
- Antes de editar código, rode `./gradlew build` (Windows: `./gradlew.bat build`). Se a linha de base falhar, registre e pare.
- Para defeito, reproduza primeiro com teste que falhe pela causa correta; depois implemente e rode o teste novamente.

## Invariantes

- Mod Fabric para Minecraft 1.21.1, Java 21. Dependências têm versões fixas; nunca use `latest`, `+` ou SNAPSHOT.
- `core/` não importa Minecraft, Fabric, `data/` nem outros domínios do core. A exceção entre domínios é `core/coordination`.
- O mundo é a fonte da verdade: não persista estado que o Minecraft já guarda e não invente recursos sem origem física.
- Nunca use `@Overwrite`. Mixin novo exige ADR; mixins delegam a lógica e não cancelam IA Vanilla.
- Prefira uma responsabilidade por classe. Arquivos acima de 500 linhas exigem justificativa e divisão por responsabilidade quando apropriado.
- Código e identificadores em inglês; documentação e Javadoc em português.

## Verificação e conclusão

- `./gradlew test`: unitários; `./gradlew runGametest`: testes Fabric no servidor.
- Alterações em `fabric/` exigem GameTests. Teste verde não substitui evidência em jogo; declare explicitamente o que ainda depende do playtest do autor.
- Teste novo deve demonstrar a falha antes da correção. Rode os testes proporcionais ao escopo e relate exatamente o que foi executado.
- Atualize `STATE.md` quando o estado vivo mudar, `TODO.md` quando uma pendência abrir/fechar e `docs/technical/Development-Log.md` para registrar uma sessão de implementação. Decisão arquitetural nova também exige ADR.
- Cada commit deve conter uma tarefa. Formato: `P0.x: descrição (teste que prova)`. O JAR em `downloads/` é atualizado manualmente conforme `docs/proxima-sessao.md`.

## Skills

- Código Fabric/aldeões: use `fabric-development`, `minecraft-modding` e, conforme o escopo, `minecraft-villager-systems`, `minecraft-code-research` e `minecraft-testing`.
- CI/CD e release: use `minecraft-ci-release` e mantenha a classificação **categoria 1.9** nas auditorias relacionadas.
- Carregue skills sob demanda; não abra instruções de áreas que a tarefa não toca.
