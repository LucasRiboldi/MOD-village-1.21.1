# Falhas e prioridades — 01-11-2026

Classificação da auditoria: **categoria 1.9**. Base: `013c8919`, linha
`codex/terrain-stock-repair`, Minecraft 1.21.1, Java 21, versão 0.4.0.

Este relatório reúne todos os problemas conhecidos nesta revisão: relato do autor,
estado vivo, backlog e testes executados. Não representa inspeção exaustiva de cada
classe. Itens históricos não revalidados permanecem identificados como tais.
Não há evidência de perda generalizada do save nem de crash fatal na sessão analisada.

## Régua e ordem de correção

- **P0:** impede trabalho e circulação ou compromete o terreno observado pelo jogador.
- **P1:** função incompleta, repetição improdutiva ou regra aprovada ainda não cumprida.
- **P2:** cobertura, desempenho, robustez e dívida com risco demonstrável.
- **P3:** organização e manutenção sem bloqueio imediato do jogo.

Ordem proposta: chegada/recuperação do mineiro → circulação dos trabalhadores →
colheita contínua → terreno de novas vilas → vegetação → produção e pastor → desempenho.
As escolhas de comportamento já aprovadas estão no
[plano](../technical/Plano-playtest-2026-11-01.md). Novas alternativas serão discutidas
com o autor antes de alterar essas escolhas.

## Falhas urgentes e requisitos ainda abertos

| ID | Prioridade | Problema e evidência | Situação real | Correção aprovada / prova necessária |
|---|---|---|---|---|
| F01 | P0 | Mineiro girando, sem progresso; relato do jogo. Histórico: 3.875 registros de falta de frente e 24 de falta de patamar. | Causa do save atual não reproduzida. As contagens são mensagens, não aldeões distintos. | Manter frente até conclusão/impedimento, buscar outro acesso; medir blocos extraídos, tempo, trocas de alvo e reservas. |
| F02 | P0 | Caminho calculado não garante chegada na arena forense. | Reprodução vermelha em 140 ticks, inclusive com adulto, expediente e `WorkTargets`; não prova vínculo com F01. | Separar colisão/geometria, critério de chegada e Brain. Reprodutor abaixo. Não aumentar tolerância para esconder falta de acesso ao bloco. |
| F03 | P0 | Aldeões presos; 225 registros de encalhe, 218 de saída cavando e 194 de construtor sem chegar. | Persistência do travamento por indivíduo ainda precisa de diário. Saídas registradas não provam solução global. | Outro acesso, liberar reserva inválida, limitar tentativas e escolher tarefa útil com motivo registrado. |
| F04 | P0 | Smoothing deixa transições ruins, vazios, lagos internos e plantas sem suporte; relato do autor. | Proteções/correções parciais implementadas nesta linha; novo desenho integral pendente. | Somente novas vilas: terraços de um bloco, superfície e duas camadas de apoio, aterro de lagos naturais internos; preservar minas, estruturas e água funcional. |
| F05 | P1 | Lenhador pega uma tora, sai e volta. | Árvore simples e alta passam; cenário com obstáculos/concorrência ainda não reproduz o relato. | Colher árvore inteira desde a base mantendo tempo físico; medir saídas antes da última tora e entregas. |
| F06 | P1 | Ruas desconectadas e acesso à mina. | Desvio por terreno natural já implementado; falta playtest de execução e reconexão posterior. | Chegar, trabalhar e voltar; não basta `Path.reachesTarget()`. |
| F07 | P1 | Folhas na altura do corpo/cabeça continuam bloqueando deslocamentos. | Rotina existente usa direção cardinal do destino, sem cobrir toda rota real e todos os aldeões. | Uma folha natural por segundo na rota real, todos os aldeões em deslocamento; preservar folhas protegidas. |
| F08 | P1 | Árvores centrais atrapalham circulação; replantio junto às portas/caminhos. | Prioridade central aprovada ainda pendente; teste antigo exige árvore mais próxima. | Árvores centrais primeiro; replantio proibido até quatro blocos das portas, outros crescimentos até três; nenhum sobre caminho. Atualizar teste e regra juntos. |
| F09 | P1 | Grama flutuante após corte. | Relato; não confundir planta com bloco de terra com grama. | Remover apenas plantas sem suporte válido, respeitando aplicação em novas vilas. |
| F10 | P1 | Árvores e copas dentro de novos lotes. | Limpeza completa de árvores naturais implementada; falta confirmação em jogo. | Provar retirada de troncos/copas e ausência de dano a árvores protegidas fora do lote. |
| F11 | P1 | Ofícios passam tempo demais sem produzir. | Diário anterior: construtor 46%, carpinteiro 11%, pastor 3%, pedreiro 15%, fundidor 57% trabalhando. Não confundir espera com ócio. | Manter jornada, estoque de 16 por tipo e obra prioritária; reatribuição temporária por demanda ainda pendente. Medir Regra 51 no novo jogo. |
| F12 | P1 | Reservas e falta de insumo podem bloquear a peça sem entrega física. | Histórico de 25 esperas por recurso; CALCITE e fallback ainda exigem prova no save. | Conciliar demanda, reserva, baú e entrega; provar produção executável antes de fallback. |
| F13 | P1 | Animal alimentado/abatido sem pastor próximo. | Limite de cinco blocos implementado, ainda sem confirmação no novo playtest. | Somente pastor próximo; preservar seis adultos por espécie e reprodução. |
| F14 | P1 | Curral pequeno, terra/pedra no interior, distribuição por espécie. | Modelo único 15×15, cercas/tochas e alternância implementados nesta linha; falta jogo. | Confirmar estrutura NBT, miolo livre, um por espécie conforme demanda e uso de outros cercados válidos. |
| F15 | P1 | Itens soltos não recolhidos e acesso desigual ao estoque. | Coleta de itens a cinco blocos por trabalhadores e baús comuns compartilhados implementados. Pedido abrange aldeões em geral; verificar cobertura. | Todos os aldeões elegíveis próximos recolhem; baús nomeados continuam reservados. Verificar queda, lotação e entrega. |
| F16 | P2 | Consultas repetidas e ciclos longos. | Histórico: 490 ciclos acima de um tick; pico de 464 ms foi relatado antes. Não demonstra sozinho queda sustentada de TPS. | Otimizar consultas repetidas sem reduzir frequência/alcance; comparar `cost_ledger` e Spark antes/depois. |
| F17 | P2 | Testes verdes com contratos fracos. | Consulta absoluta/relativa corrigida; colheita agora exige todas as toras e ausência de abandono na árvore alta. | Ampliar execução real de desvios, retomada após areia, recuperação e disputa de destinos. |
| F18 | P2 | Aplicação em mundo salvo pode alterar terreno que o autor quer preservar. | Regra de aplicação somente em novas vilas aprovada; isolamento integral ainda precisa de teste. | Reabrir save antigo sem acionar os novos reparos de terreno/vegetação. |
| F19 | P2 | Condução de vaca ao cercado falhou no CI de push do mesmo código que passou no CI de PR. | `ShepherdHerdingGameTest.theShepherdLeadsALooseCowToTheFence`, execução 41254270066; não falhou na bateria local de 955 desta revisão. Intermitência comprovada, causa aberta. | Repetir com registro de posição/rota, isolar disputa de navegação e corrigir a causa sem esconder a falha com tolerância maior. |

## Cobertura e reprodução

1. Build de referência desta sessão aprovado (`build/release-audit-baseline.log`).
2. Na etapa anterior, 147/147 GameTests selecionados passaram antes do reforço forense.
3. Mutação temporária recolocando pedra no quarto degrau e uma tora na árvore derrubou
   exatamente os três testes esperados. Mutações foram retiradas.
4. O teste forense reforçado falhou com caminho existente e sem chegada física.
   A causa ainda pode envolver o cenário; não é uma correção de produção comprovada.
5. Para integrar os testes passantes sem perder a evidência vermelha, a alteração
   forense fica preservada em [patch diagnóstico](repro/miner-forensic-arrival.patch).
   O teste original continua verificando somente planejamento. F02 permanece aberto.

Reproduzir em checkout limpo, sem aplicar sobre alterações próprias no mesmo arquivo:

```powershell
git apply --check docs/audit/repro/miner-forensic-arrival.patch
git apply docs/audit/repro/miner-forensic-arrival.patch
.\gradlew.bat runGametest -PgametestOnly=MineStallForensicGameTest
# Após guardar o relatório da falha, retirar somente o patch aplicado:
git apply -R docs/audit/repro/miner-forensic-arrival.patch
```

O patch não participa automaticamente da bateria. Não há teste desativado por anotação,
nem afirmação de que a chegada foi corrigida. Critério de fechamento de F02: reproduzir
pela causa correta, corrigir e integrar o cenário de execução à bateria obrigatória.

## Backlog histórico e riscos adicionais

Referência canônica: [TODO.md](../../TODO.md). Estes itens continuam abertos no
documento; números de versões antigas não foram revalidados nesta sessão.

| Grupo | Pontos ainda abertos | Prioridade |
|---|---|---|
| Construção por bioma | Último bloco por tronco/apoio, `UNSUPPORTED`, rotas `STALLED`, terraplanagem em ermos/taiga, cama livre na altura do trabalho. | P1 |
| Produção por bioma | Lavoura/viveiro sob neve ou no deserto; pastor sem ovelha; reserva de madeira onde não há árvore; acácia e melancia sem rota completa. | P1 |
| Suprimentos | Entrega real de CALCITE, materiais sem catálogo executável, reservas sem retirada e frequência de fallback terminal. | P1 |
| Cobertura de jogo | Alternância de obras; roteiro por bioma com múltiplas sementes; compatibilidade entre biomas e madeira vizinha. | P2 |
| Intermitências | `MinerDirectPathGameTest`, KF-002, fechamento estatístico de KF-003 e `lumber_stall_guard`. São registros históricos, não falhas observadas agora em todas as rodadas. | P2 |
| Arquitetura | AUD-09 localizador global, AUD-10 estáticos mutáveis, arquivos extensos e AUD-20; avaliar ao tocar cada sistema. | P2 |
| Testes | AUD-11 comandos/cliente com cobertura histórica baixa; suprimentos além da fundação e bateria de escassez. | P2 |
| Desempenho | AUD-04 medições novas; AUD-16 cache somente se perfil justificar. | P2 |
| Documentação | AUD-26 numeração da regra do ócio; AUD-18 tags históricas; versões/branches antigas ainda citadas no backlog. | P3 |
| Organização | AUD-15 imports antigos e AUD-24 remoção opcional de branches já integradas. | P3 |

As contagens antigas de estáticos, imports e cobertura são retratos da auditoria de
10-10, não medições atuais. Não declarar resolvido nem regressão sem nova prova.

## Requisitos futuros não são defeitos confirmados

Configuração por colônia, invalidação por evento, contrato explícito de tarefas,
estados de disponibilidade, animal inacessível fora do plantel, proteção do jogador,
transporte físico, itens de outros mods e adaptação de versão têm entradas no TODO.
Alguns exigem decisão; não serão implementados automaticamente como parte deste merge.

Decisões históricas ainda abertas no TODO: vagas/ajuda para aldeões sem ofício,
dois construtores na mesma obra, limites de felicidade/rebanho E5/E6, texto E7/Regra 33,
números da ADR-039 (carvão, ferro e caminhos) e destino de `docs/workers-analysis/`.
São deliberações pendentes, não defeitos reproduzidos. Também falta comprovar o
encerramento de branches históricas citadas no backlog; esta integração não as apaga.

## Ambiente e limites

- Fabric KB serve apenas o store pessoal nesta sessão, sem raiz de projeto resolvida
  nem destino de escrita compartilhado. Isso limita conhecimento compartilhado, não o mod.
- Serena está sem servidor de linguagem ativo nesta configuração; leituras desta revisão
  usaram ferramentas locais. `.serena/` gerado localmente não entra no commit.
- Não houve novo playtest neste relatório. O mundo existente deve ser preservado;
  validar terreno novo em vila nova, e deslocamento/profissões no save de diagnóstico.
- Histórico analisado: `Log-Stall-History.json`, sessão `latest.log`, SHA-256
  `42570ee035fe497413e633d57ca12cabc3c896dac9e1be7c4af52126f8712462`.
  Os contadores apontam candidatos a loops; não substituem causa por aldeão.
- Não reconsultamos o Spark remoto nesta sessão; métricas citadas no estado são da
  análise anterior. A atualização do JAR local não publica uma nova release no GitHub.

## Validação desta integração

`clean build` e `test runGametest` aprovados: 1.667 testes unitários, uma propriedade
jqwik e 955 GameTests obrigatórios. Python: 155/155. README gerado consistente;
`git apply --check` aprova o patch diagnóstico. Auditoria de dados e PIT são gates
do CI do PR; não foram repetidos localmente nesta rodada.

Resultados finais são registrados no topo do `STATE.md`. A atualização local mantém
versão 0.4.0 de desenvolvimento; o JAR fica fora do Git e seu SHA-256 identifica o binário.
Para fechar falhas de fluxo, executar `time_ledger.py`, `action_report.py` e
`cost_ledger.py` no próximo playtest, além dos testes automatizados.
