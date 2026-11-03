# MAE — Fase 2: implementação e validação local

## Estado em 2026-11-03

Implementação autorizada em `codex/mae-terrain`, sobre a Fase 1 integrada pelo
PR #48 (`main` `b57063162165b7d78c4657cb571ced734b862fea`). Modelo e testes focados
liberados pelo autor. [ADR-045](../decisions/ADR-045-mae-terrain-road-chain.md)
registra o desenho ampliado aprovado; [plano](../superpowers/plans/2026-11-02-mae-terrain.md)
acompanha execução. Versão candidata 0.4.10; integração e distribuição dependem do PR e do CI.

## Contrato implementado

- `TerrainPlanner` compara até oito propostas por posições alteradas, movimentação,
  desnível, `LotScore` e coordenadas. Não faz busca global ou altera `Building`.
- `MaeTerrainPreparation` levanta somente o volume autorizado, até 256 leituras
  incrementais por tick/mundo. Exige prova íntegra, suporte de duas camadas,
  corte/aterro até dois, transição até seis, clearance e circulação conectada.
- `reserveTerrain` aceita explicitamente 14 plantas baixas e promove o envelope
  v1 para v2 somente na reserva aceita. Reserva antiga continua conservadora.
  Margem de inspeção não autoriza escrita; árvores, água, lavouras e proteções ficam vetadas.
- Terreno e rua são contratos distintos do writer existente. A rua é publicada
  no índice/grafo somente após confirmação física e material do trecho inteiro.
  Extensão preserva `ENTRANCE` e alcança o hub. O terreno não altera a âncora.
- `MaeRoadAccess` bloqueia os sete pontos legados antes de corte, loot, índice ou
  sucesso. Produtores legados de metadados adiam na dimensão com reserva, porque
  suas varreduras podem cruzar outra colônia sem filtro espacial comprovado.
- Uma obra ativa por região; inspeções em seções compartilhadas são serializadas.
  Obra pausada, bloqueada ou desconhecida mantém a exclusão após recarga.
  Escrita ou publicação incerta não é retomada automaticamente.
- Loot é fixado antes da escrita e entregue pela rotina existente `Cuttings.keep`.
  `Cuttings` e `PublicChests` foram movidos de `work` para `integration`, sem mudar
  sua lógica, evitando dependência cíclica. Baús nomeados ficam preservados e
  sobra do estoque público cheio cai no chão. Exceções contam colocações reais confirmadas.

## Evidência local

Passaram **37 testes unitários/estáticos**: planejador 16, store 6, volume 2,
arquitetura/dependências 10, exceções de materiais 2 e registro de memória 1.
Passaram **48 GameTests focados**: segurança 10, terreno 11, cadeia de rua 1,
contratos 8, gates legados 11, materiais 4 e helpers físicos 3. Sem falhas ou
casos ignorados. A fixture da cadeia usa um aldeão
real para percorrer todas as superfícies preparadas, a rua e o degrau de um
bloco, com navegação e salto Vanilla. Só o destino é controlado pelo teste.
Essa prova local não demonstra a navegação de toda vila ou o desempenho em carga.

A revisão reproduziu quatro problemas antes de corrigi-los: adoção indireta
por outra colônia, seção liberada por contrato concluído desconhecido, envelope
de inspeção do corte excessivamente alto e plantas incorporadas ao aterro
omitidas da métrica. O teste de arquitetura detectou a chamada entre camadas;
a correção reutiliza os helpers físicos sem ampliar a lista de exceções.

Comandos focados e limites operacionais:
[Sentinela/TUTORIAL.md](../../Sentinela/TUTORIAL.md#segurança-mae--fases-1-e-2).
Relatórios locais: `build/gametest-report.xml` e `build/reports/tests/test/index.html`.
A última execução GameTest concluiu em 29 segundos; a execução unitária/estática,
em 41 segundos. Esses tempos incluem preparação Gradle, não são tempos do mod.

A fixture de rua/terreno registrou `VC_MAE_METRICS`: 520 leituras incrementais,
máximo de 256 por fatia; dez escritas, máximo de uma por fatia. Maiores durações
observadas: 2.556.700 ns no levantamento e 4.967.000 ns no writer. As 520 leituras
não incluem as consultas síncronas de hashes e proteção. O teto do writer é quatro
tentativas compartilhadas por tick; uma escrita observada não altera esse teto.
As medições são de uma fixture sintética, sem equivalência com carga real.

O contrato da âncora também foi exercitado sem grafo, sem índice próprio,
na altura errada, com índice estrangeiro sobreposto, grafo divergente e alteração
física do piso/clearance. A recusa preserva reserva, diário e rede existente.

`rules_to_readme.py --check` e `git diff --check` passaram. A conferência dos
documentos alterados verificou 90 destinos de links locais, sem destino ausente;
não verificou respostas HTTP nem todos os fragmentos de seção. A comparação dos
helpers movidos confirmou corpos e comentários preservados, exceto pacote,
imports e visibilidade necessários. A revisão independente cobriu gates e
exclusão de seções; não constitui revisão independente de todo o algoritmo.

## Limites e próxima decisão

Nenhum save do jogador foi aberto. Não há certificação retroativa, reparação de
vila antiga, ativação automática, integração dos construtores legados de edifícios
ou planejamento global da vila. A meta de dois milissegundos por fatia não é SLA:
hashes, proteção e uma escrita indivisível podem excedê-la. Medir em carga antes
de ativar orquestração. Restam decisão de integração/entrega e playtest do autor.
Não foi executado Sentinela completo, PIT ou CI nesta fase.

## Diagnóstico histórico — 2026-11-02

O trecho abaixo preserva o diagnóstico anterior à implementação. Seus estados
pendentes, propostas e referências de linha descrevem aquela base histórica;
o estado atual é o registro acima.

Data: 2026-11-02. Base auditada: `b8f501fa29597bac5673b3536f950588a109c148`,
branch `codex/mae-region-safety`, candidata 0.4.9.
O autor aprovou o diagnóstico desta fase e escolheu a alternativa A para detalhamento.
O [desenho](../superpowers/specs/2026-11-02-mae-terrain-design.md) e o
[plano](../superpowers/plans/2026-11-02-mae-terrain.md) tiveram o desenho ampliado
e seus limites aprovados pelo autor em 2026-11-02 (alternativa A). Implementação
continua sujeita aos gates de branch, modelo e liberação dos testes focados.

Revisão posterior solicitada: incluir conexão a ruas existentes e transitabilidade
de todo terreno preparado. O desenho agora propõe contexto de âncora somente de
leitura, mantendo a prova por seção e proteção estrutural. O autor solicitou ampliar
a prova para ruas abertas pelo mod após a geração: o desenho propõe reserva prévia,
operação tipada de rua no writer/diário existentes e integração restrita de sete
pontos de pavimentação. Não há captura retroativa, transferência entre regiões ou
ativação automática. Ruas sem prova válida continuam bloqueadas; conectar localmente
não comprova navegação de toda a vila. Ampliação aprovada no desenho, ainda não implementada.

## Escopo e evidência

Auditoria estática do código e dos contratos existentes, conforme a Fase 2 do
plano mestre MAE. Nenhuma funcionalidade implementada, teste executado, save
aberto ou terreno modificado durante este diagnóstico. Não há reprodução em jogo
dos riscos encontrados; este relatório não comprova a causa de sintomas históricos.

Referências vigentes: [Fase 1](MAE-Fase-1.md),
[ADR-044](../decisions/ADR-044-mae-region-safety.md),
[regras](../RULES.md) e [fluxo do projeto](../../CLAUDE.md).

## Sistemas existentes e reutilização

Os caminhos de código abaixo são relativos à raiz do repositório.

| Sistema | Evidência | Reutilização na Fase 2 |
| --- | --- | --- |
| Seleção de lote | `fabric/integration/LotLevel.java:307–318`: tenta a cota da rua, um nível acima e depois terraplanagem; `levels` consulta `LotTerraform.columnLevels`. | Reaproveitar leitura, recusas e referência da rua; devolver alternativa e impacto antes de escrever. |
| Busca espacial | `fabric/integration/BuildSiteScanner.java:115,248,347,394`: `Site`, busca pelo índice e varredura com cursor/orçamento. | Reaproveitar o scanner. O índice classifica candidatos; o fallback aceita o primeiro viável. Não criar busca paralela. |
| Custo de localização | `core/construction/model/LotScore.java:22–40`; chamado por `fabric/integration/RoadIndex.java:199,238`. | Já considera distância, diferença de altura e comprimento de acesso. Complementar com impacto físico; não substituir por outra nota equivalente. |
| Smoothing da geração | `core/construction/model/VillageBlend.java:27–30,110`; `fabric/integration/VillageGround.java:139,300,341`. | A matemática é pura, mas o domínio é a vila inteira, com transição de 16–64 blocos e preenchimento de vazios até oito blocos. Não chamar esse escritor para preparar um lote MAE. |
| Corte/aterro ao abrir obra | `fabric/integration/LotTerraform.java:39–45,146,180,253`. | Reaproveitar os predicados compatíveis, sem executar o escritor legado no MAE. Corte até cinco, aterro até dois e anel de seis são contratos legados, não limites automaticamente aprovados para o MAE. |
| Suporte da fundação | `fabric/integration/FoundationPreparation.java:21,30,41,68,126`. | Separar a inspeção da escrita. Exige 50% de apoio inicial e permite preencher uma camada; isso não prova duas camadas contínuas de suporte. |
| Entorno da obra | `fabric/integration/SiteSurroundings.java:36–42,107,153`. | Existe um segundo smoothing local: anel de dois e rampa de três. Reaproveitar critérios compatíveis no plano, sem encadear dois escritores sobre o mesmo entorno. |
| Apoio durante a montagem | `fabric/work/BlockShaping.java:267,285,329,379`. | Faz partes de portas/camas e apoio de terra para cultivo/caminho. Não é planejador de terreno; suas escritas precisam entrar no escopo autorizado quando a montagem for integrada ao MAE. |
| Segurança e persistência MAE | `fabric/integration/MaeProtection.java:83–108`; `MaeWorldWriter.java:90–121,129–176,235`. | Manter prova de geração, aprovação individual, estados antes/depois, revalidação, histórico, pausa, retomada e orçamento. Não criar outro escritor ou diário paralelo. |

Prefixo dos fontes: `src/main/java/com/villagecolony/`.

## Achados prioritários

### P0 — Escrita legada fora do volume aprovado

**Fato:** `LotTerraform` alcança um anel de seis blocos; `SiteSurroundings`
alcança cinco. `BlockShaping.groundFor` pode escrever abaixo da peça.
Nenhum desses métodos recebe aprovação MAE. `MaeWorldWriter.approve` aceita
somente alterações dentro dos limites do `Building` aprovado.

**Risco de integração:** aprovar apenas a caixa da estrutura não autoriza solo,
fundação e transição externos. A margem de inspeção de dois blocos não é uma
permissão de escrita. O volume de trabalho precisa incluir explicitamente todas
as alterações e continuar dentro da região elegível. Exceder os limites bloqueia
ou exige outro projeto aprovado; nunca amplia a autorização silenciosamente.

### P0 — Mutação antes da aceitação final da obra

**Fato:** `fabric/work/SiteOpening.java:238–264` executa `LotTerraform.level`
e guarda os materiais em `Cuttings.keep` antes de consultar a recusa da fundação
e verificar obra aberta/pendente. Só depois registra o projeto. O mesmo método
também chama `SiteSurroundings.level`, na linha 286.

**Risco:** uma recusa posterior pode deixar terreno e materiais alterados.
Chamadores fazem verificações próprias, mas o método não garante ausência de
efeitos na recusa. Não foi reproduzido um caso em jogo nesta auditoria.
No novo fluxo, aprovar plano e condições antes de qualquer efeito no mundo.

### P1 — Critérios de apoio e transição não representam um plano único

**Fato:** `LotTerraform` permite aterro de dois blocos; a preparação da
fundação admite um. O anel de `LotTerraform` só corta e só roda quando o
footprint mudou; `SiteSurroundings` também faz aterro e tem outra extensão.

**Consequência:** não basta somar as duas rotinas para garantir uma transição
com degraus de um bloco, suporte e acesso. Os limites distintos atendem contratos
legados; não são, isoladamente, prova de defeito. A Fase 2 precisa comparar o
estado final inteiro antes da execução e impedir preenchimento de cavernas/minas.

### P1 — Vegetação comum bloqueia a elegibilidade atual

**Fato:** `MaeProtection.terrain` não admite flores, plantas de grama, árvores,
água, lavoura, areia ou cascalho. A inspeção aplica esse filtro ao volume inteiro
e à margem, inclusive a posições que não serão alteradas. Ruas existentes também
são protegidas na inspeção.

**Consequência:** um lote intocado com flor na margem pode ser recusado. É uma
restrição deliberada da Fase 1, não uma falha reproduzida de smoothing. Qualquer
distinção entre contexto somente lido e células alteráveis exige decisão registrada
e prova de proteção. Não liberar vegetação, areia ou água por filtro genérico.

### P1 — Contadores legados não são o histórico de diferenças reais

**Fato:** `LotTerraform.java:211,283,295–302` escreve superfície e remove planta
fora da contagem de blocos movidos; as chamadas não condicionam a contagem ao
retorno de `setBlockState`. Colunas inválidas podem ser puladas durante a execução.

**Consequência:** esses números não servem como prova de execução integral do
plano. Usar o histórico do writer MAE e separar previsto, confirmado e bloqueado.
Recursos recolhidos/consumidos precisam de origem e contabilização física conforme
as regras 110–111; o escritor de blocos sozinho não comprova essa integração.

### P1 — Orçamento do escritor não cobre automaticamente o planejamento

**Fato:** o writer limita quatro tentativas por tick e orçamento flexível de
dois milissegundos. A aprovação limita o volume a 4.096 células, a inspeção a oito
seções e plano mais histórico a 1.024 entradas por região. O scanner já tem cursor
e orçamento de colunas. A aprovação e as operações indivisíveis não têm garantia
de latência máxima de dois milissegundos.

**Consequência:** levantamento, comparação de alternativas e inspeção também
precisam caber em trabalho limitado. Uma transição extensa pode não caber na
reserva ou no histórico restante; recusar antes de escrever. Medição em carga
continua pendente; não aumentar limites sem evidência.

## Alternativas avaliadas — A escolhida para detalhamento

**A — Integração local com vegetação baixa controlada, recomendada.** Reaproveitar
scanner, referência da rua, nota e escritor MAE. Produzir plano sem escrita e
comparar manter/adaptar, deslocar e corte/aterro local. Permitir somente plantas
baixas explicitamente aprovadas no plano, com prova de origem, proteção e destino
dos itens. Água, árvores, lavouras, minas, estruturas e blocos protegidos continuam
intocados; mudar o lote se houver conflito. Distinguir contexto inspecionado de
células autorizadas sem enfraquecer a prova de integridade. Registrar a decisão
em ADR antes de alterar esse contrato. Nenhum redirecionamento do legado nesta fase.

**B — Mesmo fluxo local, mantendo integralmente o filtro da Fase 1.** Implementar
plano, custo, execução e verificação apenas nos lotes sem vegetação ou materiais
recusados no volume inspecionado. Evita ampliar a política agora; muitas áreas
naturais continuarão indisponíveis e o problema das flores permanece como bloqueio.

Para ambas: manter os limites atuais como tetos, preservar vilas antigas e não
ativar nivelamento global. Preferir alternativa válida com menos intervenção;
pesos numéricos e limites de corte/aterro MAE ainda devem constar do desenho aprovado.
Não escolher altura média que invalide a entrada da estrutura.

O diagnóstico não aprova uma nova arquitetura. A implementação exige escolher
a alternativa, registrar o desenho e resolver a integração da Fase 1 por PR.
Não abrir uma segunda branch sobreposta nem uma branch filha da branch de feature.

## Critérios da próxima entrega

- Plano enumera cada posição antes/depois, solo de apoio e transições autorizadas.
- Comparação registra corte, aterro, profundidade máxima, área alterada, custo do
  acesso e motivo de rejeição. Restrição de segurança elimina candidato antes da nota.
- Superfície e entrada têm continuidade de passagem e desníveis de um bloco
  quando necessários; suporte e clearance são verificados no estado final previsto.
- O MAE executa somente plano aprovado; revalida antes de escrever e confere
  resultado real. Interrupção não repete uma escrita de resultado incerto.
- Mudança do mundo entre planejamento e escrita bloqueia o trabalho. Não há
  promessa de rollback automático nem remoção retroativa de alterações do jogador.
- Testes focados cobrem plano, encosta, depressão, água, árvore, caverna,
  proteção, acesso, suporte, limites e retomada. Reaproveitar fixtures existentes
  de `LotTerraform`, `FoundationPreparation`, `VillageBlend`, terreno e `MaeSafety`.
- Registrar cobertura no Sentinela e atualizar tutorial somente se sua configuração
  ou capacidade mudar. Executar testes apenas com a liberação do autor.

O gate da Fase 2 continua aberto. Esta auditoria não substitui esses testes,
medição em carga ou playtest. Grafo, construção final e orquestração permanecem
nas fases posteriores do plano mestre.
