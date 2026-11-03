# MAE Fase 2 — terreno local e vegetação baixa controlada

Data: 2026-11-02. Estado: **desenho ampliado aprovado pelo autor; implementação pendente dos gates**.

Revisão solicitada pelo autor: incluir conexão com ruas existentes, em vez de
adiá-la integralmente. O autor também exige terreno transitável por aldeões.
Na revisão seguinte, escolheu ampliar a prova para ruas abertas pelo mod após a
geração. Em 2026-11-02, aprovou a alternativa A do desenho ampliado, incluindo
os contratos, limites e integração supervisionada dos sete pontos de pavimentação.

A aprovação inclui corte/aterro até dois blocos, duas camadas de apoio, transição
até seis, prova encadeada de ruas e a ampliação delimitada de `FOUNDATION_GROUND`.
Não libera testes, não integra a Fase 1 nem dispensa os gates de branch e modelo.
Nenhuma implementação ou alteração de save foi executada ao registrar a decisão.

Base: [diagnóstico](../../technical/MAE-Fase-2.md),
[ADR-044](../../decisions/ADR-044-mae-region-safety.md) e plano mestre MAE, Fase 2.
Execução futura: [plano de implementação](../plans/2026-11-02-mae-terrain.md).

## Resultado esperado

Preparar o solo de um novo lote elegível e de um corredor de acesso já fornecido,
com plano revisável antes de qualquer escrita. Reutilizar o scanner, a referência
de altura da rua, `LotScore`, `Building`, `WorkArea` e `MaeWorldWriter`.
Não criar nivelador global, grafo, scanner espacial ou diário concorrente.

O caminho recebe preparação de terreno e conexão física local a uma âncora de
rua existente nesta fase, sem repavimentar a rua ou escolher outra rota. A construção
da casa, os acessos definitivos da edificação e a ativação automática permanecem
nas fases posteriores. Não redirecionar `SiteOpening`, `VillageGround`,
`SiteSurroundings`, `BlockShaping` ou a construção de edificações para o MAE.
Exceção de escopo aprovada nesta revisão: integrar os pontos de escrita de ruas
às operações supervisionadas descritas abaixo, somente em regiões novas com
política explícita. Isso substitui a exclusão anterior de toda integração legada.

## Decisões já tomadas pelo autor

- Prova capturada na geração e revalidação antes da escrita; sem prova, bloquear.
- Reserva de região supervisionada e aprovação individual de cada obra.
- Alternativa A: vegetação baixa explicitamente aprovada pode ser recolhida;
  árvores, água, lavouras, minas, estruturas e blocos protegidos ficam preservados.
- Vilas antigas não recebem reparo retroativo ou novas permissões por carga.
- Incluir no desenho conexão com ruas existentes; não adiar todo esse contrato.
- Toda superfície de circulação preparada deve permitir deslocamento de aldeões.
- Ampliar a prova para ruas abertas pelo mod após a geração; não aceitar autoria
  alegada pelo índice como substituto de um registro anterior à escrita.

As escolhas numéricas e os contratos seguintes foram aprovados nesta revisão;
não constituem evidência de implementação ou validação em jogo.

## Geometria e proteção

Manter três conceitos separados: `Building` descreve a estrutura, `WorkArea`
delimita todo o trabalho de terreno, e a margem de dois blocos é somente inspeção.
O volume de trabalho inclui solo, apoio, plantas removíveis e transições, sem
alterar os limites da estrutura registrada. Toda mudança deve estar na lista
aprovada, dentro do volume de trabalho e da região elegível.

O novo contrato será uma entrada explícita de terreno no writer existente.
A entrada antiga continua conservadora e compatível com a Fase 1. Proibir a
ampliação implícita do volume pela execução ou o encadeamento dos escritores
legados para completar transições que não couberam no plano.

Também criar uma reserva explícita de terreno, `reserveTerrain`, que admite
somente a lista de vegetação baixa na região inspecionada e persiste a política
versionada. A reserva atual `reserve` mantém o filtro antigo. Sem essa distinção,
uma flor impediria reservar a região antes mesmo da aprovação da obra.
Reservar terreno não autoriza remover planta, escrever bloco ou iniciar trabalho.
Região antiga não ganha política nova silenciosamente; pedidos de terreno exigem
região reservada para essa finalidade. Sobreposição continua bloqueada. Quando
houver conexão a uma rua já presente, `reserveTerrain` recebe também o contexto
de âncora descrito abaixo. Para uma rua futura, a reserva delimita previamente
o volume elegível de rua e terreno, ainda sem autorizar execução. Após a rua
certificada, a aprovação explícita do terreno fixa sua âncora dentro desse mesmo
volume, validando o diário. Não ampliar região, política ou contexto durante execução.

Continuam os limites da ADR-044: 4.096 células no volume de trabalho, oito seções
inspecionadas contando a margem, 1.024 mudanças de plano mais histórico por região,
quatro tentativas de escrita por tick e dois milissegundos acumulados entre
operações indivisíveis. Região: até 128 × 128 colunas, 262.144 células e 128 seções;
até 128 regiões. Não dividir uma obra automaticamente para contornar os tetos.

Água, lavoura, árvore, BlockEntity, estrutura, reserva de mina ou proteção
encontrada no volume ou margem bloqueia o candidato. Na operação de terreno, rua
permanece proibida para escrita, inclusive suporte e clearance. Propor uma exceção contextual somente de
leitura para a âncora na margem; ela não amplia o volume gravável. Vegetação baixa
segue a lista abaixo. Nenhuma mudança de hash passa a ser aceita por parecer
natural. Não recapturar recibos nem normalizar plantas ou ruas no hash.

## Conexão com rua existente — contrato aprovado

Usar uma única âncora terminal, fornecida pelo planejamento existente, com posição
X/Y/Z, face de chegada e faixa de contato. Consultar o grafo hierárquico já persistido
e o índice da mesma colônia, confirmando o piso no mundo. `RoadIndex` sozinho não
comprova autoria, conectividade ou elegibilidade; uma coluna de outra colônia ou
um bloco de caminho colocado manualmente não vira âncora por aparência.
A altura precisa coincidir com o segmento do grafo e com a superfície física;
o índice por X/Z não permite escolher outro pavimento ou andar na mesma coluna.

Não chamar `RoadGraphActivation.roads` para uma consulta: essa entrada pode atualizar
o store. Ler o grafo existente sem migrar, reconstruir ou persistir outro grafo.
Grafo ausente, versão desconhecida, divergência local com o índice ou ausência de
ligação topológica ao componente da vila bloqueia a conexão. Validar o componente
com os dados existentes; isso não comprova que toda a rede física está transitável.

O corredor termina em uma coluna de terreno adjacente à rua, por uma face X/Z.
Seu piso, apoio e dois blocos de clearance são preparados somente do lado MAE.
A rua, seu suporte e espaço de passagem ficam no contexto somente de leitura,
dentro da região reservada e da margem já limitada. Não colocar degrau, terra,
decoração ou pavimento na rua. Não cobrir, escavar ou cortar seu suporte.

O contexto enumera as colunas de rua necessárias na margem, vinculadas à mesma
âncora e confirmadas como trecho contíguo. Não autoriza qualquer rua encontrada
depois. Célula de escrita nessa lista, trecho não previsto, outra colônia ou mina
causa recusa. As demais células da margem mantêm o filtro conservador. Uma reserva
aceita preserva o volume e a política. Cada obra fixa seu contexto aprovado;
uma âncora futura precisa da certificação abaixo, sem ampliar essas permissões.

A inspeção de terreno distingue esse contexto do volume gravável. Nele, conferir
material de rua reconhecido, apoio firme, colisão e ausência de proteção do jogador,
fluido, BlockEntity, mina ou edificação. Não aplicar `mayBreak` como autorização
de leitura: uma rua pode estar protegida justamente para nunca ser quebrada.
O veto por `StreetSurface.isColonyStreet` e a lista de materiais passivos não
podem impedir a leitura de uma rua válida; continuam obrigatórios para qualquer
escrita. Não liberar outro bloco protegido só por estar perto da âncora.

A exceção à recusa `reserved_road` vale somente para esse contexto. A proteção do
bounding box completo de estruturas originais continua vigente, inclusive na
margem: estar no índice não permite remover essa proteção. Portanto, uma rua
dentro desse bounding box pode continuar sem candidato aceito. Não alterar a
proteção de estruturas para completar a conexão nesta entrega.

Todos os hashes de seção continuam íntegros e obrigatórios, inclusive nas seções
da âncora. Ao reservar, exigir coincidência com o recibo de geração. Em uma região
de terreno já reservada, uma obra seguinte pode usar somente hashes confirmados
pelo MAE nessa própria região, como na Fase 1. Não importar hashes de outra região,
contornar sobreposição, copiar permissões antigas ou aceitar histórico incompleto.

Ruas presentes no recibo ou abertas pelo protocolo certificado abaixo podem ser
âncoras, fora de outras proteções. Ruas já alteradas sem esse registro continuam
bloqueadas: índice, grafo e aparência não permitem recuperar a história perdida.
Não mascarar células de rua no hash nem substituir a prova original por captura atual.

Aprovação fixa identidade da âncora, vínculo ao componente, estados locais e
geometria de contato como precondições, não como novo estado autoritativo da rede.
Não copiar todo o grafo no trabalho ou invalidá-lo por crescimento não relacionado
que preserve essas precondições. Antes de cada escrita
e ao retomar, revalidar os estados e a relevância topológica da âncora. Fazer leituras
sem carregar chunks e dentro dos limites existentes. Se a âncora mudar, bloquear;
não trocar destino ou replanejar silenciosamente uma obra aprovada.

Concluir com `prepared_access_to_anchor` somente após conferir continuidade física
do terreno preparado até a superfície da rua. Esse resultado não declara acesso
à futura porta, navegação de toda a vila ou atualização do grafo. Pedido de conexão
sem âncora válida fica bloqueado, sem executar apenas metade da solicitação. Um
pedido separado de lote sem conexão pode informar `external_access_unverified`.

## Ruas posteriores à geração — prova encadeada aprovada

Reutilizar o recibo imutável, `MaeWorldWriter`, `MaeWorkStore` e seu diário limitado.
Não criar outro grafo ou ledger. Introduzir operação tipada de rua, separada da
operação de terreno: o terreno continua sem permissão para alterar ruas. A reserva
explícita v2 deve declarar a política de terreno e rua antes da primeira mudança.
Uma reserva antiga não ganha essa permissão por carga ou por conter um caminho.

### Registro antes da primeira escrita

1. Reservar a região enquanto suas seções ainda coincidem com o recibo de geração.
   Delimitar o volume de rua e terreno; reservar não aprova nem inicia calçamento.
2. Aprovar separadamente uma operação de rua da mesma colônia: rota fornecida pelo
   planejamento existente, posições X/Y/Z, estados antes/depois, plantas, cortes,
   apoio, piso final, materiais e publicação pretendida no índice/grafo existentes.
   Não representar essa operação como uma casa `Building` com limites fictícios.
3. Antes de cada mudança, exigir coincidência integral da seção com a referência
   original ou a última referência confirmada dessa própria região. Registrar
   intenção, identificador da operação e revisão esperada antes de tocar o mundo.
4. Aplicar somente a mudança aprovada pelo writer. Confirmar o estado e os hashes
   completos esperados, calculados com essa mudança exata, antes de avançar cursor
   ou `ownedSections`. Planta removida e chão trocado são mudanças distintas.
   Atualização indireta não enumerada causa bloqueio; não incorporar seu hash atual.
5. Após conclusão física, validar apoio, colisão e continuidade. Somente então
   confirmar a publicação do trecho no índice e grafo existentes. Registrar intenção
   e confirmação dessa publicação no mesmo diário; publicação incerta bloqueia.
6. A obra de terreno seguinte usa os hashes confirmados da mesma região e fixa
   uma âncora nesse trecho publicado. Revalidar antes de cada escrita e na retomada.

O registro comprova a cadeia de estados autorizados desde a geração, sob as
limitações do hash da ADR-044. Não prova toda interação histórica nem transforma
`PersistentState` e chunk em transação atômica. Divergência, intenção pendente ou
falha de persistência bloqueia; não repetir escrita ou publicação incerta sozinho.
Confirmar por seção impede aceitar uma edição vizinha apenas porque o piso da rua
parece correto. Crescimento natural não registrado também pode bloquear a seção.

### Pontos de integração e ausência de bypass

A auditoria local encontrou sete pontos diretos de pavimentação:

| Entrada atual | Mudanças que precisam passar pelo contrato |
| --- | --- |
| `RoadPaving.pave` / `layEdges` | Centro, bordas, plantas e adoção de coluna existente no índice |
| `PathPaving.pave` | Corte, clearance, piso e índice |
| `WorkPath.pave` | Plantas, piso e índice |
| `BuilderPaving.pave` e seu chamador | Plantas, piso, índice e confirmação da ação do trabalhador |
| `SiteOpening.pavePathTo` | `CorridorCut.cut`, plantas, piso e índice; interceptar antes do corte |
| `StreetGaps.pave` | Plantas, preenchimento da falha e índice |
| `StreetSurface.unworn` via `StreetGaps` | Manutenção de piso já certificado |

Antes de qualquer efeito dessas entradas numa seção supervisionada, encaminhar
para uma operação já aprovada ou recusar. Chamadores não executam corte, limpeza,
depósito, atualização do índice ou log de sucesso antecipadamente. Sem aprovação,
sem prova, com outro trabalho ativo ou erro do writer, não usar fallback direto.
Aplicar o bloqueio também à escrita fora do volume que compartilhe uma seção
supervisionada; isso não concede permissão para trabalhar fora da região.

Também proteger os caminhos de reconstrução de metadados: `RoadNetwork.indexIfMissing`,
`RoadIndex.indexWhatWasSeen` e `RoadGraphActivation.roads` não podem redescobrir e
publicar um trecho supervisionado parcial ou incerto a partir dos blocos visíveis.
Filtrar esses trechos pelo estado persistido da operação antes da reconstrução;
se a reconstrução não puder excluí-los com segurança, adiar sua publicação.
Não guardar outra lista autoritativa de ruas: derivar a exclusão do diário MAE.

Fora das seções supervisionadas, manter o comportamento legado. Suas ruas não
ganham certificação. Não adicionar agendamento, reserva automática, listener ou
aprovação implícita: esta fase entrega a integração supervisionada, não ativa
toda a produção de ruas. Outros escritores e mudanças naturais continuam capazes
de invalidar uma seção; a revalidação detecta isso, sem inventar recibo substituto.

Criar rua só altera terreno elegível e vegetação baixa permitida. Manutenção de
rua exige operação explicitamente aprovada sobre piso certificado pela própria
região. A exceção ao veto de rua vale apenas para essa célula e finalidade;
nenhuma proteção do jogador, estrutura, mina, fluido ou BlockEntity é dispensada.
Sem estado prévio comprovado, `unworn` não pode emitir um certificado retroativo.
Não assumir como própria uma rua de outra colônia ou construída pelo jogador.

### Limites, concorrência e publicação

Rua e terreno compartilham os tetos da ADR-044 e uma única obra ativa por região.
Manter até 4.096 células, oito seções com margem e 1.024 mudanças de plano mais
histórico por região. Contabilizar também recibos de publicação no limite de
histórico; bloquear antes de ultrapassá-lo, sem truncar ou dividir automaticamente.
Plano e intenção referenciam os mesmos registros, sem duplicar histórico para
contornar o teto. Uma operação de rua não pode furar os limites de corte, aterro,
apoio e circulação deste desenho; larguras e rotas vêm do planejador existente.

Serializar trabalhos com seções compartilhadas. Hash confirmado por uma região
não é importado para outra: sem prova válida própria, a segunda bloqueia mesmo
que ambas pertençam à mesma vila. Rua e lote precisam caber na mesma região para
usar essa cadeia nesta entrega. Não criar transferência entre regiões ou nova
reserva sobreposta; conexão nesses limites fica pendente.

Persistir apenas referência, revisão local esperada e resultado da publicação,
sem duplicar a rede. Revalidar antes de publicar; não sobrescrever revisão
incompatível. Fisicamente parcial não significa aresta concluída: não publicar
atalho ou ligação inexistente. Após interrupção, revalidar o mundo; divergência
de índice/grafo ou resultado incerto bloqueia e exige análise supervisionada.
O terreno consulta a rede sem alterá-la; só a operação de rua publica seu trecho.

O calçamento conserva a origem material já prevista pelas Regras 15/69 e
`SHAPED_GROUND`, com contabilização da Regra 111. A extensão de
`FOUNDATION_GROUND` abaixo cobre somente o solo e apoio MAE, não pavimento.
Na implementação, conferir os chamadores e sincronizar os comentários/regras
com essa separação; não criar suprimento novo ou contar a mesma colocação duas vezes.

### Disponibilidade e compatibilidade

O protocolo serve para futuras ruas certificadas em regiões novas elegíveis.
Não recupera automaticamente ruas já abertas sem recibo, não altera vilas antigas
e não libera a proteção do bounding box original. A existência de um trecho
conectado ao componente da vila, fora desses impedimentos, ainda precisa ser
demonstrada nas fixtures. Se não houver, recusar e relatar o motivo; não declarar
que toda vila já ganhou acesso. Revisar granularidade da prova ou proteção
estrutural seria outra decisão do autor, fora desta proposta.

## Vegetação permitida

Lista inicial fechada: `SHORT_GRASS`, `FERN`, `DANDELION`, `POPPY`, `BLUE_ORCHID`,
`ALLIUM`, `AZURE_BLUET`, `RED_TULIP`, `ORANGE_TULIP`, `WHITE_TULIP`, `PINK_TULIP`,
`OXEYE_DAISY`, `CORNFLOWER` e `LILY_OF_THE_VALLEY` de `Blocks`.

Uma planta dessa lista pode permanecer na inspeção sem ser removida. Só poderá
ser recolhida quando estiver numa célula explicitamente alterada pelo plano,
com suporte e proteção conferidos. Se o suporte mudar, planejar a remoção antes;
se o suporte permanecer e a planta continuar válida, preservar a planta.

Não usar uma tag ampla, `isReplaceable` ou qualquer sólido como autorização.
Rebentos, troncos, folhas, raízes, cipós, cultivos, cogumelos, roseira, flores
altas, grama de duas metades e rosa-do-wither continuam bloqueados. Preservar
plantas fora do plano e impedir atualizações indiretas que destruam vegetação
protegida. A checagem final inclui validade das plantas baixas remanescentes.

## Planejamento e escolha

1. Receber footprint, apoio/clearance do template, cota da entrada e, quando
   houver, corredor explícito com âncoras e largura. Não inferir uma rota atravessando
   regiões sem prova. Sem corredor, preparar apenas o lote e informar acesso pendente.
2. Inspecionar somente chunks completos carregados. Registrar uma amostra imutável
   de alturas, apoio, vegetação e impedimentos; conservar estados Minecraft no
   adaptador Fabric, sem importá-los no núcleo puro.
3. Comparar terreno existente, adaptação de base compatível com o template,
   corte/aterro e deslocamento entre candidatos do scanner. Não deformar piso,
   porta, escada ou template para aparentar uma adaptação. Fundação escalonada
   só é viável quando os requisitos de apoio do template a admitirem; pilares e
   contenções que exijam uma nova estrutura são recusados com motivo nesta entrega.
4. Avaliar até oito candidatos por solicitação, incluindo cotas válidas da rua
   e um nível acima, conforme o scanner atual. Uma janela limitada não promete
   o ótimo global da vila. Ausência de alternativa válida resulta em recusa.
5. Rejeitar antes de pontuar qualquer candidato que viole proteção, elegibilidade,
   apoio, acesso fornecido, volume, orçamento de histórico ou limites abaixo.
6. Ordenar os válidos pela tupla: número de posições alteradas, volume total de
   corte/aterro, maior alteração vertical, `LotScore.of(...)`, coordenadas de origem.
   A comparação é lexicográfica, sem pesos novos arbitrários. Relatar também a
   contagem de plantas; não usar um contador de execução como estimativa de impacto.

Um lote sem mudanças é resultado válido de planejamento, não uma lista vazia
enviada a `MaeWorldWriter.approve`, que atualmente a recusa. A revalidação de
uma futura construção continua obrigatória; o resultado não é permissão permanente.

### Limites aprovados para o MVP

| Critério | Limite aprovado |
| --- | --- |
| Corte e aterro por coluna | Até dois blocos cada, contados contra a superfície natural amostrada |
| Apoio sob a superfície preparada | Duas camadas contínuas de blocos sólidos estáveis, existentes ou previstas no plano |
| Transição exterior | Até seis blocos além do footprint/corredor; sempre incluída no volume autorizado |
| Desnível na faixa preparada para circulação | Até um bloco entre vizinhos ortogonais; sem saltos diagonais obrigatórios |
| Clearance da passagem | Dois blocos livres acima do piso; conferir ambos os lados das âncoras |
| Transitabilidade | Toda a superfície de circulação do terreno preparado pertence ao mesmo componente acessível; não somente o eixo do corredor |
| Seleção local | Até oito candidatos por solicitação |
| Levantamento incremental | Até 256 leituras de estado e dois milissegundos por fatia, conferidos entre operações indivisíveis |

Esses tetos são conservadores e ainda não foram medidos. O levantamento será
supervisionado e retomável, sem novo evento automático de tick. Chamadas no mesmo
tick compartilham o orçamento de levantamento do mundo; o orçamento de escrita
continua separado e não é multiplicado por solicitante. Captura/hash/aprovação
da Fase 1 continuam síncronos e limitados; não prometer latência rígida de 2 ms.

Na transição, preservar a borda natural e ajustar somente células necessárias.
Se não for possível conectar âncoras com os limites e o desnível definidos,
deslocar ou recusar. Não suavizar uma altura média que afaste o piso da porta.
Não criar paredes verticais ou cortar até cinco blocos por herdar o limite legado.

As duas camadas de apoio não autorizam aterro de cavernas. Aterro só preenche
depressão superficial aberta, até dois blocos, apoiada em terreno firme
inspecionado. Vazio sob o apoio, teto sobre a depressão, comunicação subterrânea
incerta ou área de mina bloqueia a coluna. Se não houver informação suficiente
para classificar, recusar; não buscar bedrock nem carregar chunks para decidir.

Material de apoio deve vir da política do bioma e ser estável. Não colocar areia
ou cascalho sujeitos à gravidade como sustentação. Se o material indicado pelo
bioma não for aceito pelo filtro de saída do writer, recusar até mapear uma opção
estável já utilizada pelo projeto. Não ampliar o filtro para qualquer bloco.

## Terreno transitável por aldeões

Aplicar a regra a toda a superfície de circulação do lote, transição e corredor
aprovados, inclusive células preservadas necessárias para ligar esses trechos.
Não estender a obra a terreno externo nem modificar uma vila antiga para fazer
o teste passar. As restrições de corte, aterro, proteção e elegibilidade permanecem;
conflito implica deslocar o candidato ou recusá-lo, sem aumentar limites sozinho.

Cada posição de circulação deve ter apoio estável, espaço livre compatível com
um aldeão adulto e passagem física entre vizinhos. Usar os volumes de colisão
reais no adaptador Fabric: dois blocos de ar e diferença de Y isoladamente não
comprovam passagem. Excluir bloqueios, cercas, obstáculos de cabeça, água e
pisos perigosos; não depender de pulo alto especial, teleporte ou escalar folhas.

Verificar alcance de todas as posições da superfície a partir da entrada
fornecida; quando há âncora, incluir a ligação até a rua. Desníveis de até um
bloco só são aceitos quando a transição física permite caminhada ou salto normal.
Não aceitar plataforma isolada, degrau sem clearance ou trecho acessível apenas
em diagonal. Havendo requisitos incompatíveis de mais de uma entrada, recusar.

A conectividade usada nesta checagem é uma análise transitória da amostra, não
outro grafo persistido de ruas. Enumerar somente a superfície limitada da obra.
O núcleo recebe transitabilidade já classificada pelo adaptador, sem Minecraft.
Revalidar o resultado no mundo ao concluir e impedir conclusão se restar trecho
inacessível. Validação geométrica é necessária; GameTests futuros com aldeão real
devem verificar o comportamento. Não prometer que isso elimina travamentos da IA
ou comprova áreas não inspecionadas da vila.

## Plano, recursos e execução

O plano distingue posições lidas, posições alteradas, footprint e volume total.
Cada alteração contém posição, estado anterior, estado posterior e finalidade
(planta, corte, aterro, superfície ou apoio). Metadados descrevem entrada,
âncoras, alturas finais, métricas, recusas e versão da política de terreno.
Registrar também contexto de rua somente de leitura e precondições de conexão.
Esse contexto não entra na lista de mudanças nem pode receber material de aterro.

Ordenar remoção de plantas antes de mexer no suporte; cortes de cima para baixo;
aterro e apoio de baixo para cima. Validar os estados intermediários, além do
estado final: não remover apoio de bloco sujeito à gravidade, não abrir água e
não depender de atualizações de vizinhos para corrigir o resultado depois.

Propor ampliação explícita de `FOUNDATION_GROUND`: incluir superfície, aterro e
apoio do lote, corredor local e transição de até seis blocos, somente nas células
aprovadas do terreno MAE e com materiais estáveis do bioma. A Regra 90 atual
descreve entorno de dois blocos; ela não autoriza automaticamente esse escopo.
A ampliação foi aprovada no desenho e exige ADR própria, com atualização da
Regra 90 e da descrição em `SupplyException` na implementação. Manter a contabilização da Regra 111;
não criar outra exceção ou rotina de fabricação. Sem aprovação, revisar o desenho
antes de implementar, sem obter material por uma exceção implícita.

Contabilizar somente blocos efetivamente colocados, com `VC_SUPPLY` uma vez por
evento de obra, incluindo o resultado parcial quando o trabalho bloquear. Não
emitir quantidade planejada como se já tivesse sido colocada. Para remoções,
fixar os itens devidos antes de escrever, conforme a política existente de corte
e o loot apropriado da planta, sem novo sorteio na retomada.

Reaproveitar `Cuttings.keep` e a política de baús públicos, respeitando baús
nomeados. Tornar acessível apenas a entrada compartilhada necessária; não copiar
a lógica de estoque. Sem espaço, sobras ficam como itens junto da obra, como hoje.

Adicionar ao registro da operação a intenção e o resultado da movimentação de
materiais. Não repetir entrega, consumo ou sorteio após interrupção de resultado
incerto. Material pendente impede conclusão/retomada automática e exige análise.
`PersistentState`, blocos, baús e entidades não formam transação atômica; o desenho
não promete recuperação automática sem perdas. Os GameTests devem cobrir a janela
de falha antes e depois dos efeitos de inventário e impedir duplicação automática.

Revalidar elegibilidade, contexto protegido, estados esperados e política antes
de cada alteração. Usar somente o writer MAE e seu diário. Registrar separadamente
previsto, confirmado, recusado e incerto. Uma falha não autoriza rollback que
sobrescreva edição posterior do jogador. Ao final, conferir apoio, passagem,
plantas remanescentes, recursos e comparação entre plano e diferenças confirmadas.

## Persistência e compatibilidade

O leitor novo aceita `maeWork` versões 1 e 2. Ler um save versão 1 não o modifica.
Somente a primeira reserva explícita `reserveTerrain`, aceita após todas as
validações, promove o envelope para versão 2, preservando regiões, trabalhos e
histórico da Fase 1. A reserva nova inclui a política explícita de ruas certificadas;
pedido recusado não promove versão. A reserva nova e o
trabalho recebem discriminador e versão de política; ausência desses campos
continua significando o contrato antigo, sem liberação de vegetação.
Discriminar trabalhos de terreno e rua, versão da cadeia, revisões de publicação
e referências confirmadas. Validar campos e limites ao carregar; tipo desconhecido
fica opaco e bloqueado, nunca tratado como aprovação genérica da Fase 1.

Manter versão 1 com campos novos não seria suficiente: o writer da 0.4.9 não
entende a nova política da reserva nem a intenção de materiais e poderia ignorá-las.
O envelope versão 2
faz a 0.4.9 preservar os dados opacos e recusar operações MAE, pelo contrato já
existente de `MaeWorkStore.supported`. Não rebaixar versão ao salvar ou concluir
obra. Downgrade exige restaurar cópia consistente do mundo e dos metadados;
não oferecer conversão automática de volta.

Tipo/versão de trabalho desconhecido é preservado opaco e bloqueado, mesmo em
envelope suportado. Não reinterpretar trabalhos antigos nem apagar campos
desconhecidos. Validar limites também ao recarregar. Testar leitura sem mutação,
promoção apenas na reserva aceita, manutenção de trabalhos antigos e recusa pelo contrato do leitor
antigo. Não varrer ou promover saves por carga.

Antes da implementação, registrar a extensão aprovada em ADR própria, vinculada à
ADR-044, e atualizar o índice real de decisões. Não alterar o estado da ADR-044
para fingir que a política nova já está implementada.

## Fronteiras de código propostas

- `core/construction/model/TerrainPlan.java`: resultado puro de alturas, apoio,
  alternativas, métricas e recusas. Não contém `BlockState`, NBT ou inventário.
- `core/construction/model/TerrainPlanner.java`: decisão determinística sobre
  amostra imutável, com limites e comparação definidos neste documento.
- `fabric/integration/MaeTerrainPreparation.java`: levantamento supervisionado,
  mapeamento de materiais, estados antes/depois e validação do plano no mundo.
- `MaeProtection` e `MaeWorldWriter`: entrada explícita de terreno, proteção
  contextual, execução e persistência; entrada antiga permanece conservadora.
- `fabric/integration/MaeRoadPreparation.java`: adaptação dos sete pontos de rua
  à operação supervisionada, sem novo planejador, executor ou agendador. Ajustar
  seus chamadores, `MaeGenerationEvidence` e stores existentes somente no necessário
  para validar a cadeia e publicar no grafo/índice depois da confirmação física.
- `Cuttings`, `MaeWorkStore` e fixtures existentes: somente os ajustes necessários
  para acesso compartilhado, dados opcionais e regressões. Se uma classe superar
  500 linhas, separar a responsabilidade real, sem reformatação global.

Não adicionar dependências, Mixin, listener, comando de jogador, varredura/migração
automática de saves ou configuração de ativação automática nesta fase.

## Critérios de aprovação e entrega

Testes novos devem falhar pela causa correta antes da correção. Cobrir terreno
plano sem alteração, encosta, depressão, água, árvore, caverna, proteção, planta
preservada/recolhida, planta de duas metades recusada, corredor, apoio, acesso,
limites, mudança entre fatias, compartilhamento de orçamento, recursos e retomada.
Cobrir reserva com flores, contrato antigo ainda conservador e reserva recusada
sem promoção. Para ruas: âncora comprovada aceita sem mudar piso, suporte ou
clearance; caminho falso, outra colônia, grafo ausente/divergente, seção divergente,
estrutura original e mudança de âncora entre fatias recusados. Conferir ausência
de duplicação de rua e de alteração do grafo pela operação de terreno. Para a
cadeia: gerar, reservar, aprovar rua, confirmar e conectar terreno; aceitar após
recarga. Recusar rua não registrada, edição externa na mesma seção, gravação
incerta, publicação incompleta, manutenção sem certificado e importação entre
regiões. Cobrir todos os sete pontos, inclusive corte anterior à pavimentação,
tentativa fora da região na mesma seção e recusa sem fallback ou sucesso falso.
Cobrir redescoberta por scanner, reindexação e ativação do grafo enquanto há
trecho parcial ou incerto, inclusive após recarga.
Verificar índices/grafo somente após confirmação, limites compartilhados e
conservação dos trabalhos v1. Para circulação: percorrer com
aldeão real lote, bordas da transição e conexão; cobrir degrau normal, barreira de
cabeça, ilha de terreno, piso perigoso e plano geométrico sem rota viável.
Revalidar os casos de segurança da Fase 1. Testes só serão criados/executados após
liberação explícita do autor; a aprovação deste desenho não altera essa regra.

Registrar os cenários nas suites unit e gametest já usadas pelo Sentinela. Não
criar outro runner. Atualizar `Sentinela/TUTORIAL.md` se mudar sua capacidade ou
configuração; tutorial deve indicar a nova cobertura quando ela for entregue.

Integração da Fase 1 por PR, atualização da `main` e nova branch a partir dela
precedem a implementação. Conferir branches sobrepostas antes de começar.
A próxima versão candidata será 0.4.10 se a versão base continuar 0.4.9;
reavaliar caso outra entrega seja integrada antes. Publicação, commit/push e cópia
de JAR obedecem ao fluxo autorizado da entrega; este desenho não os executa.

Mesmo com o gate aprovado, ativação automática, custo em carga e playtest seguem
pendentes. A integração restrita de ruas não equivale a corrigir ou substituir
todos os escritores legados de terreno, construção e profissões.

## Revisão do desenho

Revisão independente identificou três lacunas: reserva anterior à aprovação,
conflito entre âncora de rua e margem protegida, e escopo material do aterro externo.
O primeiro desenho propôs adiar a conexão a ruas existentes. O autor escolheu
rever esse ponto. Esta revisão propõe âncora de rua somente de leitura com a
prova existente, mantém `reserveTerrain` com envelope v2 e ampliação delimitada
de `FOUNDATION_GROUND`, e acrescenta transitabilidade de toda a superfície.
As limitações da prova por seção e de estruturas originais ficam explícitas.
São decisões propostas para aprovação; a revisão não comprova implementação.

Uma segunda revisão independente não encontrou ambiguidades bloqueantes nesses
contratos. Os casos aceitos são tecnicamente possíveis por análise estática;
disponibilidade de âncoras elegíveis, custo e comportamento de aldeões ainda
exigem validação futura. Não interpretar esse parecer como teste aprovado.

Após esse parecer, o autor pediu ampliar a prova para ruas abertas após a geração.
A proposta acima passa a incluir operação de rua encadeada no diário existente e
integração restrita de seus escritores. O parecer anterior não cobre esta ampliação;
registrar separadamente a revisão do contrato novo antes da aprovação final.

A revisão independente da ampliação não encontrou ambiguidades bloqueantes no
contrato de prova e seus limites. A inspeção complementar dos produtores de
metadados acrescentou o bloqueio de redescoberta de trechos parciais/incertos.
Ambos são resultados documentais, sem teste, build ou aprovação de implementação.

Decisão final do autor em 2026-11-02: alternativa A, aprovar o desenho ampliado,
mantendo a implementação sujeita aos gates de branch, modelo e testes. Esta decisão
encerra a revisão do desenho; os relatos anteriores descrevem seu histórico.
