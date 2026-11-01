# Correções aprovadas — playtest de 31-10, decisões de 01-11-2026

Estado: preparação e reprodução. Este plano não declara correções implementadas.

## Contrato aprovado pelo autor

1. Mineiro mantém a frente até conclusão ou impedimento comprovado; tenta acesso alternativo quando travar.
2. Lenhador pode colher a árvore inteira a partir da base, sem alcançar cada tora. Preservar tempo de corte e origem física dos itens.
3. Árvores centrais têm prioridade. Proibir replantio até quatro blocos das portas e outras formas de crescimento até três. Preservar a proibição de árvores sobre caminhos.
4. Todos os aldeões em deslocamento removem folhas da passagem de corpo e cabeça na rota real, uma por segundo, preservando folhas protegidas do jogador.
5. Novas vilas recebem terraços conectados, com desníveis de um bloco, duas camadas de suporte abaixo da superfície e aterro dos lagos naturais internos. Preservar construções, lavouras, minas e água funcional.
6. Grama flutuante significa plantas sem bloco válido embaixo. Não remover blocos de terra com grama apenas por isolamento.
7. Recuperação de trabalhadores tenta outro acesso, libera reservas inválidas e escolhe tarefa útil alternativa após limite de tentativas, com motivo registrado.
8. Reatribuir temporariamente profissões conforme a demanda. Manter jornada atual e estoque alvo de 16 por tipo.
9. Otimizar consultas repetidas primeiro, mantendo frequência e alcance. Medir antes de introduzir orçamento de processamento.
10. Aplicar novas regras de terreno e vegetação apenas em novas vilas. Não executar reparo retroativo do terreno da vila salva.

## Evidência e ressalvas

- A linha de partida é `codex/terrain-stock-repair`, commit `013c8919`; as alterações anteriores ainda não estão na `origin/main`.
- Havia alteração preexistente em `docs/technical/Log-Stall-History.json`; na integração solicitada pelo autor, o JSON foi validado e incluído como evidência do relatório, sem substituir o histórico.
- `MinerSteps.step` aguarda assentamento após quebrar um bloco. A mensagem de assentamento após um tick não prova, sozinha, defeito: deve ser correlacionada com ausência de progresso, alvo e navegação.
- `LumberjackWork.step` já mede alcance contra `job.plan.base()` com raio de quatro blocos. Implementar alcance da copa novamente seria redundante. Investigar saída de alcance e concorrência com navegação.
- `LeafPaths.clearOne` usa eixo cardinal do destino; ainda não acompanha o próximo nó da rota.
- O preenchimento raso existente não equivale à fundação contínua aprovada.

## Sequência de execução e prova

1. Executar build de referência antes de editar comportamento. Investigar falhas anteriores separadamente.
2. Reproduzir saída do lenhador e giro do mineiro em GameTests; validar alvo estável, progresso e ausência de deslocamentos repetidos desnecessários.
3. Cobrir folhas na rota, deslocamento sem tarefa profissional, diagonais e proteção do jogador.
4. Testar prioridade central, limites exatos de três/quatro blocos e todos os pontos de plantio/crescimento.
5. Definir e testar identificação de vila nova antes de aplicar terreno/vegetação; reabrir save antigo não pode ativar reparo.
6. Testar terraços, duas camadas de suporte, água funcional, lagos naturais e plantas sem apoio.
7. Testar recuperação e reservas por profissão; testar reatribuição sem abandonar trabalho ativo nem alternar indefinidamente entre profissões.
8. Medir consultas repetidas e comparar custo antes/depois com mesma entrada.
9. Atualizar regras, ADRs afetadas, STATE, TODO e registro da sessão junto das implementações; rodar build, unitários, GameTests e sincronização do README.
10. Depois de cada feature commitada, atualizar JAR local conforme instrução permanente do autor. O playtest seguinte deve medir trabalho, caminhada, espera, bloqueio e ócio por profissão.

As escolhas estão aprovadas. Detalhes de implementação devem reutilizar os sistemas existentes; novas decisões incompatíveis com estes contratos precisam voltar ao autor.
