# Relatorio operacional e fila priorizada

**Data:** 2026-09-22
**Projeto:** Village Colony, Fabric 1.21.1 / Java 21
**Objetivo:** transformar o estado observado no jogo, os contratos de codigo e
os testes em uma fila executavel, sem confundir evidencia de log com defeito ja
reproduzido.

## Resumo executivo

O mod tem as correcoes estruturais pedidas para fundacao, profissao, suprimento
por bioma, selecao de lote e cancelamento. A entrega ainda nao e um release
verde: a bateria completa de GameTests precisa fechar sem as falhas residuais
de alternancia apos casa e coleta de superficie antes de qualquer declaracao de
estabilidade. A rodada atual confirmou 410 GameTests, 408 aprovados e duas
falhas: a alternancia apos casa e a fixture do fazendeiro fora do
`ServerWorld`.

O `latest.log` do playtest de 00:09:32 a 01:27:28 foi processado pelo novo
`scripts/analyze_village_log.py`. Ele nao persiste estado no mundo; mantem uma
memoria versionada de contagens, sem coordenadas, UUIDs ou linhas cruas do
usuario. O resultado esta em
[`Log-Stall-Statistics.md`](Log-Stall-Statistics.md) e
[`Log-Stall-History.json`](Log-Stall-History.json).

## Ordem de ataque

| Prioridade | Item | Evidencia e proxima acao | Criterio de encerramento |
|---|---|---|---|
| P0 | Fechar o gate de GameTests | `FarmPlanGameTest.thenextturnafterahouseisnonresidential` e `SurfaceGatheringGameTest.farmerGathersDirtOutsideTheSoilProtectedRadius` falharam na rodada de 410. Isolar os dois cenarios antes de alterar producao. | `runGametest --rerun-tasks` sem falhas. |
| P1 | Investigar repeticao do mineiro | `miner_no_branch_work`: 6.337 linhas na sessao. Pode significar frente realmente esgotada, mas a taxa torna o diagnostico inutil e pode esconder falta de progresso. Capturar uma colonia e provar progresso ou ausencia dele. | GameTest de frente esgotada/progresso e log limitado a transicoes. |
| P1 | Fechar esperas de obra medidas | `construction_waiting_resources`: 123; `builder_pathing_stalled`: 7. Confirmar em qual JAR/salvamento cada uma ocorreu e se o suprimento automatico atual foi exercitado. | A obra acorda ou e cancelada; nenhum caminho repete sem progresso. |
| P1 | Resolver pedidos sem mao disponivel | `missing_profession`: 11. Separar falta normal de aldeao da atribuicao que deixou trabalhador capaz ocioso. | Roster e tarefa sao correlacionados por teste e por log. |
| P2 | Tornar varreduras interpretaveis | `site_sweep_budget_exhausted`: 40, mas zero reinicios reportados. E continuacao limitada por orcamento, nao um loop confirmado. Comparar `SweepLog` de fim de sessao antes de otimizar. | Analise mostra volta completa, reinicio, ou bloqueio fisico. |
| P2 | Refatorar pontos de alta complexidade | `BuildSiteScanner`, `MinerWork`, `MineDigging` e `BuilderWork` excedem o limite local de 500 linhas. Extrair somente apos fixar contratos. | Responsabilidades separadas e testes preservados. |
| P3 | Endurance de vila | Ainda falta exercitar muitos ciclos com construcao, mina e coleta para detectar crescimento de fila/custo. | GameTest ou harness de ciclos longos com metricas estaveis. |

## Profissoes atuais e cadeia de recursos

Os nomes sobre a cabeca sao funcoes do mod, nao as profissoes comerciais
Vanilla. A fonte detalhada e
[`Profession-Responsibility.md`](Profession-Responsibility.md).

| Funcao | Responsabilidade | Recurso produzido ou recolhido |
|---|---|---|
| Lenhador | corta madeira e cuida do viveiro externo | troncos; apoio a mudas e arvores |
| Mineiro | abre escadas e galerias, coleta rocha e minerio | pedregulho, arenito, carvao, ferro cru |
| Pastor | substitui integralmente o antigo Criador | la branca e tarefas antes do Criador |
| Fazendeiro | mantem comida, coleta terra fora do nucleo e planta viveiro | cultivos e `DIRT` |
| Carpinteiro | fabrica derivados de madeira | tabuas e pecas de madeira, inclusive fermentador quando a receita tem rota local |
| Pedreiro | fabrica alvenaria | tijolos de pedra |
| Fundidor | funde e coleta superficie que exige essa cadeia | vidro/areia, grama com Toque Suave, lingotes, pedra e arenito liso |
| Construtor | executa o projeto aberto | consome blocos da obra; nao produz estoque |

`dirt_path` nao e material de estoque: estrada ou obra o assenta no mundo.
Terra comum e responsabilidade do fazendeiro, coletada longe do centro; o
viveiro usa o anel de 48 a 56 blocos para manter terra enraizada e arvores
fora das estruturas habitadas. A mina dobra a area de galerias a cada dois
lances de escada antes de aprofundar.

## Como uma construcao e escolhida e realizada

1. `ConstructionPlanner` retoma projeto salvo e, se houver dano profissional,
   oferece reparo antes de iniciar algo novo. A `BigHouseMOD` e excluida desse
   reparo.
2. Sem obra aberta, `HousePlans.plansForNext` aplica a sequencia casa -> uso
   nao residencial A -> casa -> uso nao residencial B. Todas as familias usam
   o mesmo `BuildSiteScanner`.
3. O scanner procura lote livre, plano e acessivel junto a rua; rejeita solo
   reservado, estruturas Vanilla, volume fisico, obras registradas e toda a
   pegada horizontal da `BigHouseMOD`, inclusive projeto pendente. Assim nenhum
   bloco da fundacao participa da busca de lote profissional.
4. O planejador cria o projeto e uma unica tarefa `BUILD`. O construtor retira
   o proximo bloco dos baus e o assenta. Ao faltar material, o projeto entra em
   `WAITING_RESOURCES`; quando chega, `WaitingWork` o acorda.
5. O jogador encerra uma obra profissional colocando uma Tocha das Almas dentro
   do volume dela. Sao removidos projeto, tarefa e registro parcial; os blocos
   ja assentados permanecem. A `BigHouseMOD` e intencionalmente imune.

## Regra de suprimento por bioma

Para cada bloco solicitado, a obra tenta nesta ordem: bloco exato no estoque,
alternativa Vanilla compativel e cadeia fisica local (coleta, bancada, corte ou
fornalha). `BiomeConstructionSupply` consulta receitas Vanilla e recursos do
bioma. Somente quando nenhuma rota local existe a peca final aparece no bau que
atende o construtor. Esta e a excecao para itens como fermentador sem haste de
blaze; ela nao torna madeira, pedra ou argila local em recurso gratuito.

## Observabilidade de logs

Execute apos encerrar o jogo:

```powershell
python scripts/analyze_village_log.py "$env:APPDATA\.minecraft\logs\latest.log"
```

O limiar padrao e tres ocorrencias na mesma sessao. Acima disso o relatorio
marca candidato a loop e lista a area responsavel. O proximo passo sempre e
reproduzir com contexto e criar GameTest: contagem alta e sinal de investigacao,
nao prova isolada de regressao.

## Melhorias futuras deliberadas

- Limitar por transicao as mensagens de frente de mina, preservando a primeira
  causa e um resumo por sessao.
- Incluir no relatorio uma correlacao opcional de projeto/tarefa anonimizada,
  sem gravar coordenadas do jogador.
- Adicionar uma verificacao de saude que compare `WAITING_RESOURCES` com a
  entrada posterior no bau e a retomada da obra.
- Dividir arquivos grandes por responsabilidade somente depois das regressões
  que cobrem seus contratos atuais.
- Criar cenario de endurance que exercite mineracao, coleta de solo, viveiro,
  producao e obras consecutivas na mesma vila.
