# Rede hierárquica de caminhos e currais — desenho aprovado

Data: 2026-11-01. Estado: desenho aprovado pelo autor; implementação em andamento. A aprovação não substitui os testes nem o playtest.

## Decisões do autor

- Substituir o planejamento de caminhos por um grafo hierárquico, com ADR e migração do índice existente.
- Corrigir reconhecimento, acesso e condução dos currais e adicionar água, doze cultivos, crescimento local em dobro e replantio automático.
- Preservar jornada, reserva de seis adultos por espécie, pastor fisicamente próximo para alimentar/cruzar/abater e um modelo de curral para todos os biomas.
- Preservar o terreno do mundo atual. Regras novas de terreno e vegetação se aplicam somente a vilas novas, conforme o plano de playtest de 01-11.

## Problemas e evidência

O índice cresceu de 966 para 3.709 posições durante o jogo. Isso não prova sozinho excesso de pavimentação. O diário de `PathPaving` registra três blocos por passo apesar de executar uma coluna, impedindo usar sua contagem como volume físico confiável. Entradas elevadas precisam de reprodução controlada.

O pastor entrou e saiu repetidamente do mesmo curral com o mesmo destino. A fuga genérica de cercados e a condução precisam de um teste integrado; a relação causal ainda não está provada. O GameTest de saída pelo portão falhou em bateria combinada e passou isolado: não tratar a repetição verde como resolução.

## 1. Caminhos: proposta técnica

Responsáveis atuais: `VillagePathPlanner`, `PathLinks`, `PathPaving`, `RoadNetwork`, `RoadIndex`, `ColonyRoads` e `RoadIndexSave`. A ADR nova deverá explicar a substituição do planejamento e a compatibilidade com `ROAD_AREA` da ADR-017. Seu número será reservado somente na implementação.

Proposta do agente:

1. Representar centro, entradas, junções e extremidades como nós. Representar trechos por sequência física de colunas, cota e classe principal/secundária. O grafo é derivado e revalidável; o mundo continua fonte da verdade.
2. A rede principal conecta centro e componentes de ruas existentes. Ramais secundários conectam entradas individualmente à rede. Priorizar continuidade e reaproveitamento; escolher o menor percurso viável sob essas restrições, com desempate determinístico. Não prometer simultaneamente a menor distância geométrica absoluta.
3. Passos horizontais somente em X ou Z; curvas em 90 graus. Cada passo aceita no máximo um bloco de desnível e exige passagem física. Normalizar a metade inferior da porta: seu apoio está um bloco abaixo, sem fixar toda a vila numa altura média.
4. Manter largura atual: rua principal de três blocos e ramal de um. Bloquear corte, aterro ou pavimentação sobre estruturas, lavouras, minas, água funcional e proteções do jogador. Aceitar somente superfícies previstas nas regras do mod; não apresentar essa política como lista vanilla de `DirtPathBlock`.
5. Separar planejamento de execução. Uma ligação planejada não se torna rua física ou reserva permanente apenas por existir no grafo. Confirmar escrita e reconferir apoio antes de avançar o passo. Registrar colunas efetivamente alteradas.
6. Atualizar apenas conexões afetadas por nova construção ou alteração observada do mundo. Reutilizar cadência e alcance atuais, otimizar consultas repetidas e medir com `cost_ledger.py` antes de propor novas limitações.

## 2. Migração e retorno

O formato atual grava `roads`, com `colonyId`, origem `fromX/fromY/fromZ` e vetor `columns`. Não há versão própria de grafo. O índice também participa da reserva espacial de lotes; não pode ser descartado ao trocar o planejador.

Proposta do agente:

- Expandir a leitura para formato legado e novo, mantendo a representação legada gravada durante a transição. Adicionar versão explícita à representação nova, sem mudar o significado de `columns`.
- Construir o grafo a partir de colunas legadas e entradas confirmadas no mundo. Chunks indisponíveis ficam pendentes; não carregar toda a vila nem interpretar ausência de chunk como rua removida.
- Em chunk carregado, coluna legada sem rua física não vira aresta utilizável. Aplicar a reconciliação existente de `ROAD_AREA`, preservando reservas independentes de obra e proteção original; registrar a divergência. O índice não autoriza restaurar blocos ausentes. Divergência isolada não impede ativar componentes válidos.
- Migração idempotente, por colônia, sem escrita de blocos. Estados parciais e falhas têm motivo registrado. Só ativar o novo planejador após validar a representação da colônia; enquanto pendente, preservar índice e adiar novas ligações dessa colônia.
- Em erro de leitura do grafo novo, usar o legado válido e reconstruir. Dados de versão futura são preservados e não regravados por um leitor incapaz de entendê-los; a colônia afetada não planeja novas ruas.
- Retorno consiste em reabrir uma cópia de teste com o binário anterior, que ainda encontra o índice legado. Não afirmar que downgrade preserva todo campo desconhecido. Manter backup do save antes da troca e testar ida/volta sem alterações físicas.
- Não retirar o formato legado nesta entrega. Isso exigirá decisão e validação separadas.
- No mundo atual, a migração apenas representa ruas existentes; não remove pavimentação nem aciona novo nivelamento. Execução de novas ruas nessa vila fica suspensa até autorização específica de teste. Outras atividades continuam.
- A proteção aprovada alcança todas as vilas preexistentes: descoberta tardia não significa geração nova. Somente vila efetivamente gerada com a versão nova recebe a política nova de terreno e execução de ruas. A integração deve identificar essa origem no evento de geração, sem marcar retroativamente vilas descobertas.

## 3. Currais: proposta técnica

Primeiro corrigir acesso e condução com o modelo atual; depois alterar o modelo e os cultivos em tarefa separada. Manter reconhecimento geométrico de cercados existentes. Registrar água ou farmland como POI não cria condução nem comportamento animal; farmland não é o POI vanilla do fazendeiro.

Proposta do agente:

- Criar um teste com pastor, animal, destino interno e recuperação de encalhe funcionando juntos. Distinguir permanência legítima para trabalho de aprisionamento real, sem isentar todo pastor de recuperação.
- Exigir entrada real pelo acesso e chegada do animal. Dentro do curral não deixar o animal preso por laço. Não usar teletransporte no teste como substituto de condução; qualquer remoção de teletransporte da produção deve ser explicitada na ADR aplicável.
- Atualizar o modelo único de 15×15: um bloco de água central seguro, doze posições de farmland com trigo inicialmente, restante interno com grama e tochas em postes. Validar hidratação, acesso, colisão e ausência de pedra/terra obstruindo o interior. Terra de apoio e farmland funcional não são obstáculos a remover.
- Aplicar o modelo somente a novas construções. Não substituir automaticamente currais salvos. Currais antigos continuam utilizáveis pelo pastor.
- Manter os doze cultivos fora das tarefas de colheita dos aldeões. Colheita pelo jogador mantém os drops físicos normais. Proposta: replantar no mesmo local consumindo uma semente da própria colheita, antes de liberar os demais drops; sem semente, não inventar recurso e registrar a impossibilidade. Não introduzir consumo automático de plantações por animais nem alimentação remota.
- Crescimento em dobro somente nos cultivos e na recuperação de grama desse curral. Não mudar `randomTickSpeed` global. A técnica deve manter uma média de 2× sobre o processo normal, com teste determinístico da regra e medição estatística do resultado.
- Reserva mínima conta adultos por espécie. Lavouras não contam como animais nem justificam abate. Manter alimentação, reprodução e abate condicionados à presença física do pastor.

## 4. Prova e ordem de entrega

1. Estabilizar e explicar a falha combinada de `PenEscape` antes de usar a bateria como gate. Demonstrar o defeito correto em vermelho antes da correção.
2. Build de referência, ADR da rede e testes unitários do grafo. Testar obstáculos, portas em cotas distintas, normalização de portas, desníveis e caminhos sem apoio.
3. Testes de migração: legado, formato novo, tentativa repetida, colônia desconhecida, chunk indisponível, dado inválido, versão futura e retorno ao formato legado. Migração não altera blocos.
4. Integrar execução e índice espacial. Testar reconciliação após edição do jogador e ausência de pavimentação fora dos trechos escolhidos. Medir custo com entrada comparável.
5. Corrigir condução integrada; provar pastor e animal atravessando acesso, sem ciclo de fuga e sem laço interno. Cobrir curral antigo e novo, distância de interação e seis adultos de reserva.
6. Implementar modelo e crescimento local em tarefa própria, com testes de hidratação, replantio, escopo local e preservação de save antigo.
7. Executar build, unitários, GameTests focados e bateria integrada. Atualizar STATE, TODO, regras, README gerado, índice de ADRs e registro de implementação junto das respectivas tarefas.
8. Após cada feature commitada, atualizar o JAR local conforme a instrução permanente. Playtest em mundo novo valida aparência, comportamento e custos. Usar cópia do mundo antigo para validar exclusivamente compatibilidade antes de qualquer autorização de reparo.

## Limites

Este documento não resolve o crash OpenAL, o giro do mineiro ou o ciclo do lenhador. Esses problemas permanecem no diagnóstico. Não instala novas dependências nem duplica sistemas de IA por um novo POI. Nenhum sucesso de compilação substitui reprodução em mundo e ledger de tempo das profissões.
