# Trabalho, suprimento e expansão de vila — desenho

Data: 2026-11-02. Estado: aguardando aprovação do autor.

## Intenção

Eliminar procura interminável, ociosidade improdutiva e trocas de profissão. Cada aldeão mantém seu ofício, abandona tarefas sem progresso útil em cinco minutos e escolhe a próxima ação útil de seu próprio ofício. A política física nova vale somente para vilas efetivamente geradas depois desta versão; vilas existentes são preservadas.

## Decisões do autor

- Toda feature criada ou alterada recebe uma prova registrada no Sentinela Local.
- Uma vila nova recebe no máximo um curral grande. Currais pequenos existentes continuam utilizáveis.
- O curral grande comporta no máximo 20 animais, contém água, doze cultivos, grama interna e tochas em postes.
- O fundidor atende pedidos ativos primeiro. Sem pedido, usa obras da fila; sem obra conhecida, mantém somente cada lingote minerável disponível até 64. Outros produtos seguem as metas existentes do mod.
- O mineiro trabalha na frente planejada. Sem obra pedindo material, minera alvos alcançáveis escolhidos de forma variada, até 64 de pedra e 64 de cada minério encontrado por dia, sem quebrar escadas, degraus, corredores, apoios ou teto estrutural.
- Profissões não trocam. Atividades secundárias não podem executar construção de outro ofício.
- A casa pequena do mineiro nasce ao lado do pórtico da mina, no estilo do bioma.
- Caminhos usam o `RoadGraph` existente; não será criado outro grafo.

## Escopo e limites

Inclui contrato de progresso de tarefas, rotas de suprimento, produção de fundidor, trabalho de mineiro, curral grande, identidade profissional, casa do mineiro, ativação gradual do grafo e relatório operacional.

Não inclui remodelar vilas existentes, alterar o formato global da mina, mudar `randomTickSpeed`, criar POI artificial para água/farmland, teletransportar animais, nem substituir o índice legado de ruas nesta entrega.

## Contrato de tarefa

`Task` ganha estado efêmero de execução: início, último progresso útil, alvo/etapa atual e tentativas. Progresso útil é só coleta entregue, bloco quebrado permitido, item fabricado/fundido entregue, animal atendido ou bloco da obra colocado. Caminhar, trocar alvo, reservar e liberar não renovam o prazo.

Após 300 segundos sem progresso, o executor libera a tarefa, desfaz reserva inválida, registra causa e escolhe outra tarefa do próprio ofício. O alvo bloqueado recebe recuo para evitar retomada imediata. A demanda é identificada por colônia, tipo de tarefa e recurso; a contagem sobrevive à troca de executor e ao recarregamento do mundo, e reinicia no próximo dia do mundo. O segundo fracasso da mesma demanda usa a exceção de suprimento já existente, limitada ao déficit real e ao depósito que atende a tarefa; nunca cria lava nem material acima do pedido.

`IdleWatch` deixa de ser a única proteção: sua regra de espera permanece como observabilidade e recuperação de encalhe. O novo contrato cobre também procura em movimento.

## Produção e mineração

`SmelterWork` mantém prioridade de pedidos abertos. Sem pedido, consulta demanda de construções na fila e fabrica intermediários já previstos por suas receitas. Sem demanda conhecida, produz apenas lingotes de minérios disponíveis até 64 por tipo no estoque público. Falta de matéria-prima abre uma única rota física ou usa a exceção limitada após duas execuções vencidas; não recria busca indefinidamente.

`MinerWork` mantém frentes e reivindicações existentes. Quando não houver demanda de pedra/minério, abre uma tarefa de suprimento limitada, com alvo alcançável selecionado por rotação determinística entre frentes planejadas. Em cada dia do mundo, pode entregar até 64 de pedra e 64 de cada minério encontrado. O seletor exclui escadaria, degraus, corredores, apoios e teto estrutural. Não faz varredura aleatória ilimitada nem altera a geometria da mina nesta entrega.

## Profissões e curral

`Worker.giveUpProfession` não desatribui profissão após falha operacional. `ProfessionAssigner` preenche somente aldeões ainda sem profissão. `BuildHelper` não empresta pastor, carpinteiro ou pedreiro para tarefas `BUILD`; cada rotina secundária permanece restrita às capacidades de seu próprio ofício.

`PenPlans` agenda o modelo grande apenas se a vila nova ainda não tiver um curral grande concluído ou em construção. A identidade do modelo vem da construção registrada, não da simples geometria de cercas. `ShepherdPens` continua reconhecendo currais pequenos e cercados antigos para condução. O modelo novo contém água segura, 12 `FarmBlock` com cultivo, grama nos demais pisos internos e tochas; a capacidade do curral grande é limitada a 20 mesmo que sua área seja maior.

## Casa do mineiro e caminhos

O planejador da mina agenda uma casa pequena compatível com o bioma junto ao pórtico, antes de trabalho mineiro distante. A casa usa o catálogo estrutural existente e respeita as proteções de lote, terreno e estruturas.

`RoadGraphMigration` permanece leitura/migração sem escrita física. A ativação usa o marcador de geração nova da ADR-043. Para essas vilas, o planejador escolhe ligações pelo grafo, ruas principais de três blocos e ramais de um, cota da porta pelo apoio inferior, passos cardinais e desnível máximo de um. Estruturas, minas, lavouras, água funcional e proteção do jogador bloqueiam alteração. A política física fica desligada para vilas preexistentes.

## Provas e relatório

Cada alteração ganha teste unitário ou GameTest focado e uma entrada no manifesto do Sentinela Local: feature, teste, perfil mínimo e regra provada. O Sentinela não roda automaticamente.

Provas mínimas: prazo inclui caminhada improdutiva; suprimento respeita déficit; fundidor prioriza pedido/fila/lingotes; mineiro nunca corta escada; profissão não muda nem constrói fora do papel; um só curral grande por vila nova; composição e limite 20; casa ao lado do pórtico; grafo não escreve em vila antiga e conecta somente passos válidos em vila nova.

O relatório operacional agrega tarefas abertas, duração sem progresso, liberações, motivo, estoque, produção, frentes da mina e tempo do planejador. Ele lê o diário já existente, sem gravar estado paralelo no mundo.

## Ordem de entrega

1. Contrato de progresso, registro e relatório.
2. Identidade profissional, fundidor e mineiro.
3. Curral grande e casa do mineiro.
4. Ativação do grafo para vilas novas, após reprodução mínima da navegação na água.
5. Atualização de documentação, versão, JAR local e playtest manual.

## Sucesso

Nenhum trabalhador repete a mesma procura sem progresso por mais de cinco minutos. Fundidor não fica sem ação por falta de pedido; mineiro não fica ocioso sem obra e não danifica acesso; vila nova tem no máximo um curral grande funcional; profissões permanecem estáveis; ruas novas usam somente o grafo e não alteram vilas antigas. Cada afirmação tem prova local rastreável pelo Sentinela.
