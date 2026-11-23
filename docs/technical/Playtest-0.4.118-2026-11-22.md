# Playtest da 0.4.118 em 22-11-2026

Colônia `650faf94`, bioma planície. Seis notas `VC_BIOME_NOTE` de `latest.log`. As coordenadas da coluna “Nota” registram a posição do jogador. A imagem 7 do [plano da 0.4.115](Plano-Playtest-0.4.115-2026-11-21.md) mostra o caso anterior de condução na cerca. A sessão da 0.4.118 foi analisada pelo log e pelo diário de ações.

| Nota | Evidência | Correção ou investigação |
| --- | --- | --- |
| 15:31:24 · (335, 80, 646) | Pastor tentou tirar um animal de outro cercado através de uma casa com porta fechada. | A 0.4.119 protege animais em recintos cobertos, delimitados por porta, na coleta, na separação e na seleção para abate. Falta reproduzir com a estrutura da vila. |
| 15:32:28 · (350, 80, 675) | Entrada de casa do mod sem ligação. A entrada próxima `door@351,86,675` aparece isolada no grafo. | Identificar a entrada exata, registrar o motivo da recusa em cada lado e verificar se o piso permite a ligação. |
| 15:32:49 · (340, 79, 697) | Outro relato de entrada isolada. O grafo registrou seis nós isolados e portas próximas em `339,82,697` e `339,82,699`. | Identificar qual porta corresponde à nota e testar a conexão após reler a região. Confirmar N12 e N28 separadamente na mina e na casa do mineiro. |
| 15:48:27 · (359, 73, 634) | Pastor dentro e galinha na guia do lado de fora. Às 15:48:32 houve entrega de uma galinha em `358,71,634`. | A 0.4.119 dá à galinha um caminho até a frente da porteira, ainda na guia. Confirmar a entrega com o pastor dentro e a ave deslocada para o lado. |
| 15:48:56 · (362, 72, 639) | O curral grande de 169 células (porteira `351,71,641`) oscilou de dois animais para zero (15:47:33), um (15:52:50) e zero (15:58:07). O diário registrou um abate de galinha sem posição. | A 0.4.119 registra a posição e indica se o animal estava num curral conhecido em `VC_FLOCK_HARVEST`. Cruzar com `VC_PENS`, entregas e a contagem antes e depois de reabrir o mundo. A causa da queda segue aberta. |
| 15:56:31 · (402, 85, 690) | O jogador viu um lenhador subir numa obra. O diário registrou `HELP` nas proximidades e atribuiu a colocação de bloco ao construtor. As ações de ofício do lenhador registradas foram 63 `COLLECT_WOOD`. | Correlacionar a identidade do aldeão com o destino de `HELP` e a autoria de blocos durante a ocorrência. A colocação pelo lenhador segue sem confirmação. |

## Validação automatizada

Sentinela `fast` passou com 1.886 testes unitários, uma suíte de propriedades e 1.241 GameTests (`20261123T004749Z`).
Os dois cenários de regressão passaram na candidata. A confirmação no mundo jogado segue pendente.

## Roteiro da candidata 0.4.119

1. Observe uma galinha conduzida de lado para uma porteira com o pastor já dentro. A galinha deve andar até a abertura ainda na guia, cruzar e deixar a porteira fechar.
2. Coloque um animal em uma construção coberta e outro do lado de fora. O pastor deve deixar o primeiro no recinto e continuar a coleta dos desgarrados.
3. Conte os animais do curral grande por vários dias do jogo e após salvar e reabrir. Se a contagem cair, registre a hora, a espécie e a porteira para cruzar com o log de abate.
4. Registre a coordenada do bloco da porta nos dois casos do grafo. Se o lenhador subir novamente na obra, registre a hora, o local e o bloco atribuído a ele.
