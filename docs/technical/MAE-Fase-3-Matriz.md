# MAE — Fase 3: matriz de modelos e contrato de acesso

Auditoria inicial: 2026-11-03. As seções 1–6 conservam essa fotografia histórica.
Atualização de implementação: 2026-11-04, na seção 7; estado vivo em `STATE.md`.

O autor escolheu a alternativa B: detalhar toda a matriz antes de implementar,
incluindo currais e depósitos sem porta. Esta escolha substitui a proposta de
começar pela casa pequena. Em seguida, o autor aprovou o contrato A, pediu
portões em todos os lados do curral grande e delegou as demais escolhas
compatíveis com o jogo. A implementação continua sujeita à confirmação de etapa;
testes e ativação automática não foram liberados por essa escolha.

Base inspecionada: `codex/mine-home-search`, `2b8d8f20`; `main`/`origin/main`
em `adb7630c`. Os quatro commits locais 0.4.12–0.4.15 permanecem preservados.
Este documento não os integra. Retomada e histórico: [Fase 3](MAE-Fase-3.md).

## 1. Universo e evidência

A auditoria inicial do [inventário por arquivo](MAE-Fase-3-Inventario.csv) continha **161 registros**:
143 templates vanilla da interseção de `VillageStructures.ALLOWED` com
`catalog/vanilla_structures.json`, mais os 18 arquivos NBT próprios presentes.
Todos os 143 ids autorizados foram encontrados no catálogo e no JAR local 1.21.1.
O JSON contém outros ids; sua presença não os autoriza para construção.

A inspeção leu os NBT sem modificar arquivos de origem ou saves. Os vanilla
foram lidos de `minecraft-merged.jar`, no cache Fabric Loom 1.21.1; os próprios,
de `src/main/resources/data/villagecolony/structure/`. Nenhum byte de template
vanilla foi copiado para este relatório. O CSV registra metadados e o SHA-256
dos bytes originais de cada arquivo NBT, incluindo sua compressão.

O CSV é uma fotografia de auditoria, **não um catálogo de produção**. Contém id,
origem, estilo, família descritiva, `DataVersion`, dimensões X/Y/Z, portas
inferiores, portões, portões prometidos por jigsaw, conectores de entrada,
chaves de payload de bloco, inventários não vazios, loot tables, tipos de
entidades, fluidos, cultivos, propriedades de paleta, tochas e hash.
Posições são relativas ao NBT original, antes da rotação. `shared` significa
arquivo compartilhado, não compatibilidade comprovada com todos os estilos.
As famílias são agrupamentos para esta auditoria, não uma atribuição de profissão.
`palette_property_occurrences` conta ocorrências entre blocos; não mede custo de execução.
`evidence=nbt_inspected_only` não certifica leitura pelo mod, colocação ou navegação.

| Estilo | Moradias | Oficinas/equipamentos | Currais | Fazendas | Decoração | Total vanilla |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Planície (`plains`) | 11 | 17 | 3 | 2 | 1 | 34 |
| Deserto (`desert`) | 10 | 13 | 2 | 3 | 1 | 29 |
| Savana (`savanna`) | 10 | 13 | 2 | 2 | 1 | 28 |
| Taiga (`taiga`) | 9 | 13 | 1 | 2 | 1 | 26 |
| Neve (`snowy`) | 10 | 12 | 2 | 1 | 1 | 26 |
| **Total** | **50** | **68** | **10** | **10** | **5** | **143** |

As oficinas/equipamentos incluem casas de ofício, bibliotecas, templos e
estábulos. Cada variante está individualizada no CSV; não se presume que
uma planta de pedreiro represente a geometria de todas as outras oficinas.
Há 40 arquivos com mais de uma porta inferior, contando vanilla e próprios.
Múltiplas portas podem formar uma entrada dupla ou ter funções diferentes;
não justificam rejeitar automaticamente o modelo nem escolher a primeira.

## 2. Matriz representativa dos cinco estilos

Os ids abaixo têm prefixo `minecraft:village/<estilo>/houses/`.
São candidatos para a primeira cobertura física, não uma nova whitelist.
`D` indica porta inferior; `J` indica conector com nome
`minecraft:building_entrance`; ambos usam coordenadas locais do arquivo.
A direção de `D` é `facing`; a de `J` é `orientation`.

| Estilo | Família / sufixo do id | X×Y×Z | D | J | Particularidade |
| --- | --- | --- | --- | --- | --- |
| plains | casa / `plains_small_house_1` | 7×7×7 | (1,1,3), east | (0,0,3), west_up | NBT de duas partes de cama |
| plains | oficina / `plains_masons_house_1` | 8×7×9 | (2,1,3), east | (0,0,3), west_up | Sem payload adicional de bloco |
| plains | curral / `plains_animal_pen_1` | 5×8×6 | — | (0,1,2), west_up | Dois portões brutos e um jigsaw com `final_state` de portão |
| desert | casa / `desert_small_house_1` | 6×6×5 | (1,1,2), west | (0,1,2), west_up | NBT de cama |
| desert | oficina / `desert_mason_1` | 7×5×8 | (2,1,3) e (2,1,4), west | (0,1,4), west_up | Duas portas; selecionar passagem, não uma peça isolada |
| desert | curral / `desert_animal_pen_1` | 10×6×7 | — | (4,0,0), north_up | Dois portões brutos |
| savanna | casa / `savanna_small_house_1` | 7×7×7 | (3,1,5), south | (3,1,6), south_up | NBT de cama |
| savanna | oficina / `savanna_mason_1` | 8×7×10 | (1,1,4) e (1,1,5), west | (0,1,5), west_up | Baú com loot table no arquivo |
| savanna | curral / `savanna_animal_pen_1` | 9×5×9 | — | (4,0,0), north_up | Um portão; três blocos de água |
| taiga | casa / `taiga_small_house_1` | 7×8×9 | (3,2,2), south | (3,1,0), north_up | Porta elevada; NBT de camas e placas |
| taiga | oficina / `taiga_masons_house_1` | 8×7×9 | (2,2,4), east | (1,1,4), west_up | Porta elevada |
| taiga | curral / `taiga_animal_pen_1` | 13×5×8 | — | (6,0,7), south_up | Um portão |
| snowy | casa / `snowy_small_house_1` | 7×5×6 | (3,1,4), south | (3,1,5), south_up | NBT de cama |
| snowy | oficina / `snowy_masons_house_1` | 10×8×9 | (4,2,6), east; (5,2,6), north | (4,0,8), south_up | Duas portas elevadas; NBT de fornalhas |
| snowy | curral / `snowy_animal_pen_1` | 8×6×9 | — | (0,0,5), west_up | Um portão |

Direções de porta e jigsaw podem divergir no mesmo modelo. Nem `facing`, nem
parede mais próxima, nem a altura do jigsaw constituem prova suficiente de saída.
Os outros modelos, inclusive todas as fazendas, precisam da mesma validação
individual antes de serem habilitados no MAE. Decoração sem interior exige
área de instalação protegida, mas não deve receber uma porta fictícia.

## 3. Matriz dos arquivos próprios

Ids têm prefixo `villagecolony:`. Variantes por estilo e genéricas são arquivos
distintos no CSV; fallback de `ColonyModels` não multiplica o total por vila.

| Modelo / grupo | Arquivos | X×Y×Z | Acesso observado | Destino proposto no MAE |
| --- | ---: | --- | --- | --- |
| `colony/storage_small` e cinco variantes de estilo | 6 | 7×5×7 | Porta inferior (3,1,6), north | Depósito pequeno; oito baús com payload vazio de itens |
| `colony/storage_majest`, plains, savanna, taiga e snowy | 5 | 11×13×12 | Sem porta; seis portões em Y=11 | Depósito grande de vão aberto a identificar; portões altos não são prova de entrada térrea |
| `colony/desert/storage_majest` | 1 | 11×13×12 | Sem porta ou portão | Mesmo contrato de vão aberto; geometria precisa de validação própria |
| `colony/large_animal_pen` | 1 | 15×3×15 | Portão (7,1,0), south | Curral grande atual, escolhido por `PenPlans`, não pelo catálogo profissional comum |
| `colony/animal_pen` | 1 | 15×3×15 | Portão (7,1,0), north | Asset anterior; não substituir o modelo atual automaticamente |
| `colony/barn_majest` | 1 | 12×13×15 | Sem porta; 12 portões em Y=6 | Legado fora do MVP automático; retomada atual o trata como modelo aposentado |
| `houses/small_house` | 1 | 7×7×7 | Porta (1,1,3), east; jigsaw (0,0,3), west_up | Cópia histórica; não cadastrar de novo como casa profissional |
| `houses/big_house_mod` | 1 | 7×10×11 | Porta (1,0,5), east | Fundação própria da vila; manter proprietário e fluxo existentes |
| `mine_entrance` | 1 | 9×7×5 | Sem porta, portão ou jigsaw de entrada | Pórtico controlado por `MineGate`; futuro conector não autoriza alterar escadaria ou fundação |
| **Total** | **18** | — | — | Nenhum modelo está certificado fisicamente por esta inspeção |

O depósito grande genérico e quatro variantes têm 37 blocos com payload;
a variante do deserto tem 36. Não há inventários não vazios nesses seis arquivos.
O celeiro legado tem 20 blocos com payload, uma lista `Items` não vazia e seis
entidades `minecraft:item_frame`. Não são seis animais.

O curral grande já contém **um bloco de água, 12 blocos de farmland, 12 de trigo
e quatro tochas**. Capacidade de 20 e limite de um curral grande por vila
dependem de `ShepherdPens` e `PenPlans`, não dessas contagens do NBT.
Preservar currais pequenos e extras legados; não executar reforma retroativa.
A inspeção não prova conclusão da construção, reconhecimento pelo pastor,
replantio, alimentação ou passagem de animais pelo portão.

Não foi comprovada uma entrada térrea dos depósitos grandes. Uma busca estática
restrita a duas células de ar sobre apoio na borda não encontrou candidatos;
isso não prova inacessibilidade, pois lajes e escadas exigem análise de colisão.
Nenhuma coordenada de entrada deve ser inventada para liberar esses modelos.

### Metadados mínimos do plano mestre

| Campo | Fonte a reutilizar | Situação desta auditoria |
| --- | --- | --- |
| Id, categoria e versão | Catálogos existentes, tipo de obra, Minecraft 1.21.1 e `DataVersion` do NBT | Id, origem, família descritiva e versão do arquivo no CSV; família não substitui a decisão de profissão |
| Dimensões, origem e pivô | Tamanho do template e transformação de `Blueprint` | Dimensões extraídas; origem de instalação e rotação continuam sendo as do projeto, não coordenadas absolutas do CSV |
| Pegada e volume ocupado | Peças da planta orientada e limites existentes de reserva | Calcular no projeto; a caixa dimensional não equivale a todos os blocos ocupados |
| Orientação e entrada | Porta, jigsaw ou declaração do vão; rotação existente | Evidência bruta extraída; nenhuma entrada declarada foi certificada |
| Fundação e terreno | `StreetLayer`, proposta de superfícies e contratos das Fases 1–2 | Sem modificar limites ou preparar terreno nesta etapa |
| Paleta e requisitos de blocos | `VillagePalette`, leitor e peças de `Blueprint` | Manter substituições aprovadas; inspeção NBT não comprova a capacidade de fabricar todos os materiais |
| Block entities e dependências especiais | Template, tratamentos existentes e receitas/estoque do projeto | Payloads inventariados; suporte e consumo precisam de comprovação por modelo |
| Conectores | Jigsaws existentes e futura declaração de passagem | Distinguir montagem vertical, acesso local e ligação ao grafo da Fase 4 |
| Origem e licença | Templates do jogo ou arquivos do repositório e seus registros de autoria | Não redistribuir NBT vanilla; licença/autoria dos próprios não foi auditada individualmente, portanto não presumir permissão adicional |

## 4. Contrato A aprovado

### Seleção de acesso

Combinar evidência do template e validação física, com três formas
de passagem: **porta, portão e vão aberto**. Reutilizar id, dimensões, peças,
rotação e seleção por bioma de `Blueprint`, `ColonyModels` e `VillageStructures`.
Não criar um segundo catálogo nem um segundo grafo.

1. Examinar conectores de entrada antes de o leitor substituir jigsaws por
   `final_state`. Reutilizar o conhecimento de `StreetLayer` sobre orientação
   horizontal; conectores verticais de montagem não são saídas para o aldeão.
2. Agrupar portas adjacentes quando formarem uma passagem; distinguir portas
   internas e externas. Validar os dois lados. Não exigir exatamente uma porta.
3. Para currais, validar portão, apoio, abertura, passagem até o interior e
   comportamento de interação. Não presumir que o pathfinding abre qualquer
   portão. Fechar após passagem conforme a rotina existente, sem deixar animais escapar.
4. Para vãos sem porta, exigir uma entrada declarada por modelo e conferida
   na geometria. Não escolher automaticamente o primeiro vão ou portão.
   A declaração ainda depende de inspeção física dos depósitos e do pórtico.
5. Havendo várias entradas válidas, preferir a ligada à circulação preparada
   do lote; desempatar por ordem estável de coordenadas locais. Recusar quando
   nenhuma puder ser comprovada, com motivo identificável no relatório.

A declaração específica de vão é um complemento de acesso ligado ao id existente,
não uma cópia da lista de blocos. Seu armazenamento e eventual extensão de
`Blueprint` devem ser detalhados no próximo plano, com este contrato aprovado.
Se o template mudar, revalidar a declaração antes de habilitá-lo novamente.
A camada Fabric interpreta NBT e colisões; o core recebe somente os dados
neutros necessários, preservando a separação da ADR-005.
Nenhuma destas alterações foi implementada.

### Curral grande com quatro portões

Pedido explícito do autor: adicionar um portão em cada lado de
`villagecolony:colony/large_animal_pen`. A inspeção do arquivo atual confirmou
um portão de carvalho ao norte e cerca de carvalho nas outras três posições.
Todas têm grama como apoio e espaço sem peça do template imediatamente acima.
Isso não comprova que o terreno exterior estará livre após colocação.

Escolhas técnicas do agente, dentro da delegação do autor:

- Manter o tamanho 15×3×15 e o portão norte existente. Substituir somente os
  três trechos centrais de cerca pelos portões correspondentes; não abrir vãos
  permanentes nem duplicar o modelo por bioma.
- Coordenadas locais dos portões: norte `(7,1,0)`, leste `(14,1,7)`, sul
  `(7,1,14)` e oeste `(0,1,7)`. Orientar cada portão de acordo com sua cerca.
  Essas posições ainda não foram escritas no NBT.
- Portões começam fechados e sem alimentação de redstone. A rotina de pastoreio
  deve abrir e fechar o portão necessário à passagem; não presumir abertura
  automática pelo pathfinding nem criar passagem permanente para fuga de animais.
- Selecionar o acesso conectado à circulação local válida. Ter quatro portões
  não exige construir quatro ramais ou liberar os quatro acessos em terreno
  bloqueado. Não cortar proteção para tornar uma saída utilizável.
- Preservar água, 12 cultivos, quatro tochas, capacidade de 20 animais e limite
  de um curral grande por vila. Não transformar currais pequenos em grandes.
- Aplicar o template novo somente a novos currais em vilas elegíveis para as
  novas regras. Não reformar currais existentes nem alterar saves nesta etapa.

O CSV permanece uma fotografia do arquivo anterior à implementação, com um
portão. Após alterar o template, atualizar sua linha, o hash e a comprovação
física; não registrar quatro portões no inventário antes de eles existirem.

### Geometria e circulação

Separar verificação da planta e verificação do mundo depois da obra. Conservar
a origem e o pivô usados pela rotação existente; não aplicar uma segunda rotação.
Dimensionar pegada, volume ocupado, apoio e reserva a partir da planta orientada;
dimensões da caixa, isoladamente, não comprovam piso nem espaço livre.

A passagem precisa ter apoio real, largura suficiente para a entidade, espaço
de corpo e cabeça e ligação contínua entre exterior e destino interno útil.
Usar a geometria de colisão de portas, cercas, escadas e lajes. Distinguir
posição do bloco de apoio, altura dos pés e topo de colisão; não comparar
somente os Y inteiros da origem e da porta. Manter salto normal e acesso por terra.

Toda superfície de circulação preparada deve ser transitável. Água funcional,
canteiros, paredes e decoração não viram superfícies de circulação por estarem
dentro da caixa. Não aterrar água do curral ou da fazenda para passar uma rota.
Escadas e plataformas devem resolver o desnível dentro dos limites já aprovados:
corte/aterro até dois blocos, duas camadas de apoio e transição até seis.
Um conector não autoriza saltar proteção, orçamento ou fronteira da região MAE.

O MVP exige uma conexão local de acesso. Não existe template independente de
ponte/escada na whitelist inspecionada. Reutilizar soluções locais existentes
somente após verificar seus chamadores e limites; não cadastrar pontes de outras
estruturas encontradas no JSON. A ligação ao grafo continua na Fase 4.

### Estados, conteúdo e block entities

102 dos 143 templates vanilla têm payload de bloco além de jigsaw.
`BlueprintBlock` não transporta NBT genérico. `PaletteStates` conserva o facing
separadamente; `ShapeStates` restringe propriedades a eixo, metade top/bottom,
tipo de laje e formato de escada. Não há cópia genérica de `hinge`, `open`,
idade de cultivo ou umidade. Algumas peças têm tratamento próprio em
`BlockShaping`; ausência no transporte genérico não prova defeito em todas elas.

Decisão A: inventariar requisitos por modelo e comprovar os tratamentos existentes
antes de habilitar cada família. Camas, duas metades de porta e baús exigem
verificação funcional específica. Texto de placas, vasos decorados e demais
dados não devem ser declarados preservados sem implementação e prova.
Não copiar arbitrariamente `Items`, loot tables, entidades ou UUIDs: os arquivos
não autorizam criar estoque gratuito ou duplicar entidades (ADR-028/ADR-034).
O template vanilla `taiga_armorer_2` contém dois armor stands; o inventário
registra sua presença, sem autorizar seu aparecimento na obra.

O recorte recomendado cobre apenas os dados necessários aos modelos selecionados,
com regras explícitas por categoria. Modelo que exija dados ainda não suportados
fica recusado no MAE; não alterar o construtor legado globalmente nesta etapa.
Suporte genérico a NBT e nova política de fornecimento de conteúdo exigem decisão separada.

### Integração e retomada

Reutilizar a evidência de geração e o diário das ADRs 044–045: área nova elegível,
mesma região, proteção revalidada antes de escrever, sem carregar chunks à força.
Ler metadados ou carregar um chunk não certifica terreno antigo retroativamente.
Modelos que ultrapassem os limites atuais ficam bloqueados; não ampliar orçamento
para acomodá-los implicitamente.

Sequência proposta: selecionar modelo; verificar contrato da planta; orientar;
avaliar terreno e corredor; reservar e aprovar; preparar; construir pelo fluxo
existente com os contratos de escrita necessários; inspecionar acesso final.
Não presumir que o writer de terreno já implementa a colocação de edifícios.

`BuildClosing.complete` e `ConstructionState.COMPLETED` já controlam conclusão;
`ColonySavedData` e `ColonySaveReader` já a persistem. Reutilizar esse estado.
A conclusão atual pode dispensar mobília; portanto, ela não certifica acesso.
Após recarga, revalidar a geometria observável e o projeto existente; não criar
um segundo estado persistido de conclusão ou reexecutar automaticamente uma obra concluída.
Destino inacessível não deve ser anunciado como acesso MAE pronto.

## 5. Matriz de comprovação futura — não executada

| Grupo | Cenários mínimos antes da habilitação | Recusa esperada |
| --- | --- | --- |
| Casa de cada estilo | Quatro rotações; porta elevada; cama e duas metades da porta; apoio e saída | Porta interna escolhida, falta de apoio, teto baixo ou saída bloqueada |
| Oficinas e entradas múltiplas | Duas portas adjacentes, portas em faces distintas, acesso ao posto e conteúdo exigido | Primeira porta arbitrária, equipamento obrigatório inacessível ou dados indispensáveis ausentes |
| Curral vanilla e grande | Portão bruto e de jigsaw; quatro portões centrais no grande nas quatro rotações; abertura/fechamento; entrada de pastor e animal; água e cultivos preservados | Rota cruzando cerca, portão decorativo, portão deixado aberto ou canteiro substituído por caminho |
| Depósito pequeno | Porta, acesso a baús e inicialização sem itens copiados | Baú inacessível, itens gerados do template ou segunda metade incorreta |
| Depósito grande, incluindo deserto | Entrada declarada comprovada, lajes/escadas reais, acesso útil aos recipientes | Nenhuma entrada comprovada; portões de Y=11 tratados como entrada térrea |
| Fazenda e decoração | Circulação até o destino útil, água e cultivo funcionais; decoração fora da faixa livre | Água funcional aterrada, plantação bloqueada ou decoração sobre passagem |
| Conexão local e pórtico | Desnível dentro dos limites; preservação de rota e estrutura da mina | Modificação da escadaria, travessia obrigatória por água ou corte protegido |
| Todos | Reserva, escrita parcial, recarga, mudanças posteriores do jogador e região inelegível | Escrita fora da reserva, prova retroativa ou reconstrução duplicada |

Estes cenários são requisitos de desenho. Não foram criados ou executados testes
nesta etapa. Ao liberá-los, cada alteração funcional deverá ter cobertura no
Sentinela Local, conforme a regra do autor, sem rodar baterias não autorizadas.

## 6. Decisão registrada e próximos gates

**A — Aprovada pelo autor:** contrato combinado: conectores e portas examinados
na planta; vãos declarados por modelo; validação física obrigatória; conteúdo
suportado por regras restritas. Mantém depósitos e currais no desenho completo,
habilitando cada modelo somente após prova. Não promete suportar todo NBT.

A alternativa B, de declarar manualmente todas as entradas, não foi escolhida.
O pedido adicional de quatro portões e as escolhas técnicas delegadas estão
detalhados na seção 4. Nenhuma mudança física foi executada nesta revisão.

Contrato formalizado na [ADR-046](../decisions/ADR-046-mae-structure-access.md),
com [plano preparado em 2026-11-04](../superpowers/plans/2026-11-04-mae-structure-access.md).
Próximo estágio: revisar esse detalhamento e conferir branch, modelo e liberação
dos testes para executar. A implementação deve nascer da `main`
atualizada, preservando os commits locais da mina. A execução de testes requer
liberação explícita para o recorte. A orquestração automática continua bloqueada
pela carga real e playtest da Fase 2; o gate histórico de navegação na água
continua valendo para a Fase 4. Não houve commit, build, teste ou alteração de save.

## 7. Implementação parcial — 2026-11-04

PR #51 integrado; linha `codex/mae-structure-access` baseada em `9dc8b4f2`.
Implementação e testes focados liberados pelo autor. O CSV agora tem 162 registros:
os 161 originais e a variante `villagecolony:mae/large_animal_pen`. A identidade
do projeto continua `villagecolony:colony/large_animal_pen`; não há novo catálogo.

| Modelo ou contrato | Prova realizada | Limite atual |
| --- | --- | --- |
| Casas pequenas plains/desert/savanna/snowy `_1` e taiga `_2` | Leitura carregada, estados completos, partes e quatro rotações | Não certifica material, instalação física ou acesso de cada casa |
| `plains_tool_smith_1` | Mesma prova, preservando dimensões declaradas e margem vazia do template | Não habilita oficina automaticamente |
| Curral grande MAE | Quatro portões físicos, rotações, água, cultivos e tochas; acesso alternativo e um único cercado de capacidade 20 | Travessia real do animal pela rotina ainda não comprovada |
| Curral grande automático | Asset anterior de um portão conservado e leitura isolada da variante MAE | Opção B do autor; ativação automática dos quatro portões adiada |
| `mine_entrance` | Leitura 9×7×5 e 164 posições distintas após correção de codificação NBT | Mesma geometria; não redesenha mina ou escada |
| `colony/plains/storage_majest` e `taiga_small_house_1` | Recusa explícita de payload funcional não tratado | Continuam bloqueados no MAE |
| Acesso físico local | Mundo previsto e real, colisões, apoio, altura, ligações e orçamento limitado | Não substitui prova de geração, materiais ou autorização do writer |

A inspeção CSV continua `nbt_inspected_only`: os testes acima são evidência
separada, não autorização global do inventário. A correção do pórtico trocou
somente `IntArray` por listas de inteiros nos campos consumidos pelo Minecraft.
O asset automático do curral permaneceu byte a byte igual ao anterior; hashes
atualizados no CSV, incluindo o novo arquivo supervisionado.

Escrita `structure_v3`, materiais, conclusão e retomada estão implementados
(ADR-046, seção `structure_v3`). Não houve alteração de saves do jogador,
nova versão ou JAR desta fase.
Comandos e limitações no [relatório da fase](MAE-Fase-3.md); contagens em `STATE.md`.

### Exercício supervisionado `structure_v3` — 2026-11-04

`MaeStructureCatalogGameTest` fixa a tabela abaixo para o contrato 1; mudou o
catálogo ou o contrato, a tabela do teste muda junto. `MaeStructureModelsGameTest`
exercita o caminho inteiro em um representante de cada família: prova prevista
do acesso, aprovação, material exato tirado dos baús, escrita, prova real do
acesso e conclusão única no registro de edifícios.

| Modelo | Contrato 1 | Exercício completo |
| --- | --- | --- |
| `plains_small_house_1` | aceito | casa construída e concluída |
| `plains_masons_house_1` | aceito | oficina construída e concluída |
| `plains_animal_pen_1` | aceito | curral construído e concluído |
| `colony/storage_small` | aceito | depósito construído e concluído |
| `savanna_small_house_1`, `snowy_small_house_1`, `desert_mason_1`, `plains_tool_smith_1` | aceito | não exercitado |
| `desert_small_house_1` | recusado: `potted_cactus` | — |
| `taiga_masons_house_1` | recusado: `potted_spruce_sapling` | — |
| `colony/large_animal_pen` | recusado: `farmland`, `water`, `wheat` | — |
| `taiga_small_house_1`, `savanna_mason_1`, `snowy_masons_house_1` | recusado na leitura: `unsafe_structure_payload` | — |

Limites do exercício: terreno concluído sintético (a cadeia terreno real →
obra escrita está em `MaeStructureWriteGameTest`); rotação 0; circulação e
destino útil derivados por `MaeStructureAccess.beginFor`, mas a superfície
preparada fora da pegada é posta pelo teste, não pela preparação de terreno;
ligação local de rua não entra, pois é `road_v2` (Fase 2).
A obra concluída não prova mobília funcional, reconhecimento pelo pastor
nem passagem de animais pelos portões.

## Fontes de integração

- [Leitor de templates](../../src/main/java/com/villagecolony/fabric/integration/StructureBlueprintReader.java), [estados de paleta](../../src/main/java/com/villagecolony/fabric/integration/PaletteStates.java) e [estados permitidos](../../src/main/java/com/villagecolony/core/construction/model/ShapeStates.java).
- [Catálogo autorizado](../../src/main/java/com/villagecolony/fabric/integration/VillageStructures.java), [modelos próprios](../../src/main/java/com/villagecolony/fabric/integration/ColonyModels.java) e [camada de rua](../../src/main/java/com/villagecolony/fabric/integration/StreetLayer.java).
- [Proposta de terreno](../../src/main/java/com/villagecolony/fabric/integration/MaeTerrainPreparation.java), [formação de peças](../../src/main/java/com/villagecolony/fabric/work/BlockShaping.java) e [conclusão](../../src/main/java/com/villagecolony/fabric/work/BuildClosing.java).
- [Plano de curral](../../src/main/java/com/villagecolony/fabric/work/PenPlans.java), [reconhecimento de currais](../../src/main/java/com/villagecolony/fabric/work/ShepherdPens.java) e [retomada](../../src/main/java/com/villagecolony/fabric/work/ConstructionResume.java).
- [ADR-043](../decisions/ADR-043-hierarchical-road-graph.md), [ADR-044](../decisions/ADR-044-mae-region-safety.md), [ADR-045](../decisions/ADR-045-mae-terrain-road-chain.md) e [evidências da Fase 2](MAE-Fase-2.md).
