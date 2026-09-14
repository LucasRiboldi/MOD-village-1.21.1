# Ciclo controlado apos auditoria - 2026-09-14

## Objetivo

Converter os achados de `AUDIT_REPORT.md` em lotes pequenos, reversiveis e
testaveis. Nenhum lote altera regras de jogo antes de ter uma regressao que
falha contra a regra desligada e uma validacao em mundo controlado.

## Portao de inicio

O commit `2a0a4b7` esta com CI vermelho. Nenhum lote de comportamento deve ser
considerado pronto enquanto `surfacegatheringgametest.smeltergathersdirtoutsidetheprotectedvillageradius`
nao estiver reproduzido, explicado e verde em Linux.

## Lote 0 - Conter a falha de CI (AUD-001)

**Problema:** uma regressao obrigatoria falha no GitHub e passa localmente.

**Arquivos provaveis:** `SurfaceGatheringGameTest`, `FarthestVillageSector`,
`SurfaceGatheringWork`, workflow de CI e teste de GameTest correspondente.

**Procedimento:**

1. Repetir localmente o GameTest isolado e a suite; registrar sistema, seed,
   setor escolhido, posicao do bloco, inventario e tempo do mundo.
2. Fazer o teste falhar de modo controlado retirando a regra de coleta; confirmar
   que a assercao denuncia exatamente a ausencia de coleta.
3. Eliminar apenas a variacao comprovada, sem aumentar timeouts cegamente.
4. Rodar `build`, `runGametest` e o mesmo job no GitHub.

**Saida exigida:** CI verde com o diagnostico do teste preservado.

## Lote 1 - Politica de obra parcialmente pronta (AUD-004)

**Decisao necessaria antes do codigo:** uma casa parcial deve ser retomada
automaticamente quando a condicao faltar deixa de existir, ou deve permanecer
abandonada com uma recuperacao explicita do jogador/administrador?

**ADR proposta:** registrar ciclo de vida do projeto, condicao de retomada,
protecao do lote, limite de tentativas e efeito de reload.

**Regressoes:**

1. A obra entra em espera por recurso ausente.
2. A paciencia expira sem liberar o lote indevidamente.
3. O recurso aparece no bau ou cadeia produtora.
4. A politica escolhida e observada apos reload, sem duplicar blocos nem abrir
   um segundo projeto no mesmo lote.

**Validacao no mundo:** uma casa curta deve avançar, esperar, receber o recurso
e seguir a politica da ADR por tres ciclos de trabalho.

## Lote 2 - Cobertura integral de materiais (AUD-002)

**Decisao necessaria antes do codigo:** cada material de estrutura Vanilla deve
ter produtor, substituicao declarada ou bloqueio antecipado e explicavel. O
contrato atravessa blueprint, core e adaptador Fabric; requer ADR.

**Regressoes:**

1. Catalogar todos os `ResourceId` dos templates habilitados.
2. Falhar o teste para qualquer ID sem destino explicito.
3. Para cada destino, provar demanda, tarefa, retirada do mundo/receita e
   deposito no bau certo.
4. Provar que um blueprint impossivel e recusado antes de iniciar obra.

**Validacao no mundo:** escolher uma casa real por estilo e verificar que a
lista de faltas sempre aponta para uma profissao ou para motivo explicito.

## Lote 3 - Lotes e revarredura (AUD-003)

**Decisao necessaria antes do codigo:** fechar P0.7 sobre terreno alterado pelo
jogador. Sem ela, relaxar `isLotGround` seria inventar politica.

**Regressoes depois da decisao:**

1. Solo permitido encontra lote sem invadir construcao, estrada ou bloco do
   jogador protegido.
2. Solo proibido registra a razao e nao recebe bloco de obra.
3. Uma edicao local do jogador invalida/reconcilia apenas a area afetada.
4. Depois de N varreduras sem lote, o cursor muda de estrategia e volta a
   verificar locais anteriormente inadequados.

**Validacao no mundo:** vila controlada com tres tipos de terreno; registrar
tempo ate a primeira obra, recusas por motivo e TPS durante revarredura.

## Lote 4 - Mineracao continua e recuperavel (AUD-005)

**Problema:** ha trabalho real, mas ocupacao irregular de varios mineiros em
ramais escassos ou bloqueados.

**Regressoes:**

1. Tres mineiros e quatro ramais; cada mineiro deve ter alvo, motivo de espera
   ou proxima revalidacao verificavel.
2. Ramal sem pedra em 64 cortes libera a posse e oferece alternativa justa.
3. Alvo fora de alcance e agua/lava provocam rota/fechamento definidos, sem
   repetir a mesma tentativa indefinidamente.
4. Carvao, ferro e pedra entregues reduzem demanda e mantem os baus separados
   por profissao.

**Validacao no mundo:** acompanhar um dia completo de trabalho, com contagem
de entregas por mineiro e nenhuma espera sem razao no log.

## Lote 5 - Harness e diagnostico do jogador (AUD-006, AUD-009)

Depois de Lotes 0-4, criar uma vila de GameTest controlada com mediciones de:

- lotes examinados/recusados por motivo;
- projeto aberto, blocos colocados e recurso faltante;
- tarefas abertas, trabalhador atribuido e motivo de inatividade;
- inventarios por profissao;
- alvo/ramal de cada mineiro e proxima revalidacao.

Em seguida, expor uma versao somente administrativa no jogo com texto e
particulas temporarias. O diagnostico nao deve criar estado global, alterar
blocos nem substituir a verificacao por log.

## Validacao de cada lote

1. Fazer o novo teste falhar com a regra desligada.
2. Rodar testes unitarios afetados e `./gradlew.bat build`.
3. Rodar `./gradlew.bat runGametest` quando tocar adaptador Fabric/mundo.
4. Rodar o CI remoto e guardar o link do run verde.
5. Testar em mundo de desenvolvimento; registrar TPS, comportamento, log e
   captura de antes/depois em `STATE.md` e no Development Log.
6. Um lote por commit; quando solicitado commit/push, copiar o JAR verificado
   para `downloads/` e para `%APPDATA%\\.minecraft\\mods/`.

## Ordem de execucao

`Lote 0 -> ADR de Lote 1 e 2 -> Lote 1 -> Lote 2 -> decisao P0.7 -> Lote 3 -> Lote 4 -> Lote 5`.

O ciclo para entre os lotes que exigem decisao de arquitetura ou revisao em
jogo. Isto evita transformar um sintoma observado em uma mudanca ampla sem
prova e sem a escolha do autor.
