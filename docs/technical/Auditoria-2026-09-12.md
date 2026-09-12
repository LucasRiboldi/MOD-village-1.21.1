# Auditoria de 2026-09-12

Diagnóstico executado depois do pedido para manter a categoria **1.9
CI/CD** e usar a skill `minecraft-ci-release`.

## Escopo

- Código do mod em `src/main`.
- Unitários em `src/test`.
- GameTests em `src/gametest`.
- Configuração de build, manifesto Fabric e CI/CD.
- Registro do estado de P0.7 contra `TODO.md` e `Plano-de-Correcao.md`.

Fora de escopo: `build/`, `run/`, `Website/`, `.claude/`, `graphify-out/`,
`.agents/` e mudanças de regra de jogo que dependem de decisão do autor.

## Resultado por severidade

| severidade | resultado |
|---|---|
| CRÍTICO | Nenhum novo confirmado. |
| ALTO | Categoria 1.9: o repositório não tinha workflow de CI/CD e o jar publicado aceitava qualquer Fabric API por `fabric-api: "*"` no manifesto. |
| MÉDIO | P0.7 não foi executado automaticamente: a medição chegou e mostrou que o gargalo principal não é terraplanagem, mas pedra recusada como solo natural. Alterar isso muda regra de projeto. |
| BAIXO | `actionlint` não está instalado localmente; o YAML foi validado por inspeção, pinagem por SHA e execução dos comandos equivalentes na máquina. |

## Itens executados no lote ALTO

1. Criado `.github/workflows/ci.yml`.
   - Roda em `push` para `main`, `pull_request` e `workflow_dispatch`.
   - Usa Java 21, porque este projeto é Minecraft 1.21.1.
   - Roda `git diff --check`, `python -m unittest discover -s tests`,
     `./gradlew build --rerun-tasks` e `./gradlew runGametest --rerun-tasks`.
   - Faz upload dos jars em sucesso e dos relatórios em falha.
   - Todos os actions externos estão pinados por SHA completo.

2. Corrigido `src/main/resources/fabric.mod.json`.
   - Antes: `fabric-api: "*"`.
   - Depois: `fabric-api: ">=${fabric_api_version}"`.
   - O valor processado vem de `gradle.properties`: `0.116.15+1.21.1`.

3. Atualizado `build.gradle`.
   - `processResources` agora expande `fabric_api_version`.
   - O `build/resources/main/fabric.mod.json` processado declara
     `fabric-api >=0.116.15+1.21.1`.

4. Criado `ModMetadataTest`.
   - Confirma que o manifesto publicado não usa curinga em dependências.
   - Confirma que a exigência da Fabric API vem da matriz de versões.
   - A fase vermelha foi conferida: os dois testes falharam antes da
     correção e passaram depois.

## Estado de P0.7

O P0.7 foi mantido no diagnóstico, mas não virou lote automático. A sessão
já entregou o número que ele pedia:

```text
6.583 recusas de lote
4.578 NOT_NATURAL_GROUND
905 OFF_ROAD_LEVEL
```

Conclusão: a terraplanagem automática não é o gargalo principal. A vila do
autor é rochosa, e o código hoje trata pedra exposta como montanha, não
como solo natural. Mudar isso toca a Regra 3 e a Regra 19; portanto é
decisão de projeto do autor.

## Verificação executada

```text
./gradlew.bat test --tests com.villagecolony.architecture.ModMetadataTest --rerun-tasks
git diff --check
python -m unittest discover -s tests
./gradlew.bat build --rerun-tasks
./gradlew.bat runGametest --rerun-tasks
```

Resultados:

- `ModMetadataTest`: falhou antes da correção e passou depois.
- Python: 74 testes, OK.
- Build Gradle: `BUILD SUCCESSFUL`.
- GameTests: 308/308.
