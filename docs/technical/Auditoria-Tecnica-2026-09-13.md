# Auditoria técnica do projeto — 2026-09-13

## Escopo e regras aplicadas

Inventário completo da árvore `docs/` e leitura direcionada dos arquivos
vivos (`STATE.md`, `TODO.md`, `docs/RULES.md`, `docs/PATTERNS.md`), plano
atual, ADRs pertinentes, arquitetura, salvamento, testes, CI e log recente.
Os históricos extensos não foram lidos por inteiro, conforme `CLAUDE.md` e
`AGENTS.md`; foram pesquisados por sintoma e subsistema. Categoria **1.9
(CI/CD)** e a skill `minecraft-ci-release` foram mantidas nesta auditoria.

Regra de execução: CRÍTICO/ALTO corrigível sem decisão arquitetural vai
para um lote; a revisão do autor acontece entre lotes. Nenhum CRÍTICO foi
confirmado. Um ALTO corrigível foi executado neste lote. O próximo ALTO
minerador para na revisão porque altera contrato geométrico persistido e
não há ADR que autorize a mudança.

## Mapa tecnico

- Mod Fabric para Minecraft 1.21.1 / Java 21, Gradle com versoes fixas.
- 175 classes Java de produção: 77 em `core`, 92 em `fabric` e 6 em
  `data/save`. A separação é protegida pelos testes arquiteturais; não foi
  alterada neste lote.
- Testes locais após a correção: 798 testes unitários, 312 GameTests e 74
  testes Python; build e os tres grupos passaram.
- CI categoria 1.9: workflow de build/testes, Java 21, Python, GameTests,
  artefato e relatórios de falha. Actions estão fixadas por SHA e as
  permissões são somente leitura. O workflow não publica uma GitHub Release;
  `downloads/` continua sendo atualização manual. `git diff --check` em
  checkout limpo não compara arquivos alterados e, por isso, é uma guarda
  efetiva apenas quando a CI receber um diff/base para comparar.
- Dívida estrutural, não bloqueadora deste lote: há classes acima da meta
  indicativa de 500 linhas, por exemplo `BuildSiteScanner` (1087),
  `VillageDetectionHandler` (1009), `MinerWork` (907) e `MineDigging` (887).
  Não recomendo refatorar sem um item focado e cobertura que preserve o
  comportamento.

## Achados

### ALTO, lote 1 concluido: andaime de estrutura contado como material

O log local `latest.log` registra a obra em
`(2716, 69, -1316)` parada com 435 blocos restantes, `WAITING_RESOURCES`,
esperando `minecraft:structure_void` (por exemplo, 01:40:04). Esse bloco é
marcador de estrutura, não um material a fabricar ou procurar nos baús.

Em `StructureBlueprintReader.isScaffolding`, `STRUCTURE_VOID` não era
filtrado. O leitor passava a exigência falsa para o inventário de materiais.
Foi incluído no filtro, e `BlueprintReaderGameTest` ganhou um teste que lê
a casa média Vanilla e afirma que o marcador não aparece no BOM.

O teste novo foi executado primeiro contra a regra antiga e falhou com a
mensagem esperada; depois da correção, os 312 GameTests passaram. `build`,
798 unitários e 74 testes Python também passaram. Falta ver a casa média
construir no cliente real.

Arquivos: `src/main/java/com/villagecolony/fabric/integration/StructureBlueprintReader.java`;
`src/gametest/java/com/villagecolony/gametest/BlueprintReaderGameTest.java`.

### ALTO, proximo lote: a mina repete uma frente bloqueada no limite

No mesmo `latest.log` há 10.781 linhas `hit stone with nowhere to stand` e
10.780 `no miner branch work`; a mensagem `went one level deeper` aparece
11 vezes no arquivo inteiro. Isso corresponde ao relato de mineiros sem
progresso, embora o log também mostre progresso em outros momentos.

O caminho de código explica uma repetição concreta: quando os quatro
braços fecham e a mina não pode descer mais, `Mine.deepenIfEveryArmIsDone`
chama `MineShaft.turned`. Esse método preserva `entry` e `descent` e gira
somente `gallery`. `MineShaft.positionAt` calcula a hélice inicial a partir
da descida; logo, uma barreira na hélice/descida reaparece mesmo depois do
giro da galeria. O teste existente verifica a mudança do rumo da galeria e
que os braços reabrem, mas não compara as posições bloqueadas do novo
padrão. O sintoma é observado e a limitação do padrão é verificável no
código; um novo GameTest precisa provar a repetição antes de implementar a
mudança.

`MineFlooding` já sela faces de fluido sem sobrescrever blocos protegidos,
e `MineDigging.flooded` encerra o braço afetado. O log atual não demonstra
que água/lava seja a causa desta sessão.

**Ponto de revisão:** separar orientação estável da boca da orientação da
rota, definir o que fazer com minas existentes e elevar a versão do formato
de `MineSave` (`SHAPE_VERSION` atual 4) exige decisão/ADR. Não foi alterado
neste lote. E44 (escada de recusas) continua sendo outro problema, já
marcado como decisão do autor; não deve ser confundido com esta geometria.

Arquivos: `src/main/java/com/villagecolony/core/construction/model/Mine.java`;
`src/main/java/com/villagecolony/core/construction/model/MineShaft.java`;
`src/main/java/com/villagecolony/data/save/MineSave.java`;
`src/test/java/com/villagecolony/core/construction/model/MineTest.java`.

### ALTO aparente, sem defeito de código provado: falta material real

O mesmo log tem uma obra parada em `WAITING_RESOURCES` por
`minecraft:smooth_stone_slab`, com 369 blocos restantes. Isso é distinto do
falso `structure_void`: não há evidência de erro no leitor nesse caso. As
regras do projeto proíbem inventar recursos. A obra deve aguardar enquanto
não houver origem/receita/produtor definido; não transformar esse estado
previsto em sucesso artificial.

Uma obra anterior, `plains_small_farm_1`, retomou com 45 blocos restantes
em 01:24:34. Sem identidade/posição que a vincule à casa relatada pelo
jogador, isso prova que uma obra antiga retomou, não que a obra específica
do relato foi recuperada.

### MÉDIO: estado documental e cobertura

- `STATE.md` ainda declara que a mina funciona; a sessão atual contradiz
  essa frase. O estado vivo foi atualizado para distinguir verificação
  anterior de falha atual e espera de teste em jogo.
- `CLAUDE.md` termina dentro do bloco de código do §0.3 e não contém os §1
  e §2 que ele próprio indica. `AGENTS.md` primeiro declara `CLAUDE.md`
  canônico, mas depois inclui uma pergunta sem resposta sobre qual vence.
  `docs/RULES.md` abre um fence Markdown sem fechá-lo. Esses documentos já
  tinham alterações locais; foram preservados para revisão, sem edição nesta
  auditoria.
- Cobertura Java é extensa, mas a mina é verificada em grande parte por
  modelo e cenários isolados. O sintoma real justifica um GameTest que
  atravesse o fechamento dos braços, a troca de padrão e o próximo alvo.
  Os itens E41/E42 continuam abertos.

## Pendencias e proximo passo

- **P0.7**: decisão do autor sobre pedra como solo; não executar
  automaticamente.
- **E45**: padrão alternativo de escavação no fundo; desenhar geometria,
  compatibilidade do save e critério do teste antes do código.
- **E44/E43**: decisões do autor já registradas; sem mudança neste lote.
- **Em jogo**: validar retomada da casa específica, casa média após excluir
  `structure_void`, quatro correções anteriores de 09-13 e a mina. `build`
  verde não substitui essa sessão.
- **CI/CD 1.9**: sem novo crítico. Avaliar depois se `git diff --check`
  precisa de checkout/base explícito e se o projeto quer publicação formal
  de releases além do artefato de CI.

Commit, push e copia manual do JAR para `downloads/` ficam para depois da
revisão deste lote e das correções pendentes; nenhum deles foi feito aqui.
