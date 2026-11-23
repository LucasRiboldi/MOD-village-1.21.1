# Playtest da 0.4.118 em 22-11-2026

Colônia `650faf94`, bioma planície. Seis notas `VC_BIOME_NOTE` de `latest.log`. As coordenadas da coluna “Nota” registram a posição do jogador. A imagem 7 do [plano da 0.4.115](Plano-Playtest-0.4.115-2026-11-21.md) mostra o caso anterior de condução na cerca. A sessão da 0.4.118 foi analisada pelo log e pelo diário de ações.

| Nota | Evidência | Correção ou investigação |
| --- | --- | --- |
| 15:31:24 · (335, 80, 646) | Pastor tentou tirar um animal de outro cercado através de uma casa com porta fechada. | A 0.4.119 protege animais em recintos cobertos, delimitados por porta, na coleta, na separação e na seleção para abate. Falta reproduzir com a estrutura da vila. |
| 15:32:28 · (350, 80, 675) | Entrada de casa do mod sem ligação. A entrada próxima `door@351,86,675` aparece isolada no grafo. | A cópia do mundo tem piso de caminho em `(351,79,675)`. Verificar a ligação no piso da porta na 0.4.122. |
| 15:32:49 · (340, 79, 697) | Outro relato de entrada isolada. O grafo registrou seis nós isolados e portas próximas em `339,82,697` e `339,82,699`. | A cópia tem piso de caminho em `(339,78,697)` e `(339,78,699)`. Verificar as duas ligações na 0.4.122. |
| 15:48:27 · (359, 73, 634) | Pastor dentro e galinha na guia do lado de fora. Às 15:48:32 houve entrega de uma galinha em `358,71,634`. | A 0.4.119 alinhou a galinha pelo exterior. Na 0.4.120 o pastor salta a cerca nos dois sentidos e mantém a guia até a porteira fechar. Aguardando confirmação no mundo do autor. |
| 15:48:56 · (362, 72, 639) | O curral grande de 169 células (porteira `351,71,641`) oscilou de dois animais para zero (15:47:33), um (15:52:50) e zero (15:58:07). O diário registrou um abate de galinha sem posição. | A 0.4.119 registra a posição e indica se o animal estava num curral conhecido em `VC_FLOCK_HARVEST`. Cruzar com `VC_PENS`, entregas e a contagem antes e depois de reabrir o mundo. A 0.4.121 exige três adultos da espécie no curral da vítima antes de abater. A causa da queda observada segue aberta. |
| 15:56:31 · (402, 85, 690) | O jogador viu um lenhador subir numa obra. O diário registrou `HELP` nas proximidades e atribuiu a colocação de bloco ao construtor. As ações de ofício do lenhador registradas foram 63 `COLLECT_WOOD`. | Correlacionar a identidade do aldeão com o destino de `HELP` e a autoria de blocos durante a ocorrência. A colocação pelo lenhador segue sem confirmação. |

## Validação automatizada da 0.4.119

Sentinela `fast` passou com 1.886 testes unitários, uma suíte de propriedades e 1.241 GameTests (`20261123T004749Z`).
Os dois cenários de regressão passaram na candidata. A confirmação no mundo jogado segue pendente.

## Validação automatizada da 0.4.121

Os cinco GameTests de abate e a coleta do excedente solto passaram. O Sentinela `fast` encontrou duas falhas na condução e uma expectativa antiga de coleta. A expectativa foi atualizada. A falha da guia longe da porteira ocorreu também na 0.4.120. A vaca no desnível lateral falhou na bateria completa e entrou pela porteira na rodada focada. O passo 1 do roteiro cobre a confirmação em jogo.

## Roteiro da candidata 0.4.122

Na entrega 0.4.122-alpha, o cenário de guia a cinco blocos passou após receber uma porteira real. A rodada focada do pastor passou com 16 GameTests. O cenário de desnível lateral segue no roteiro de jogo.

1. Observe uma entrega com a vaca em desnível ou a galinha deslocada para o lado. O pastor abre a porteira, salta a cerca, alinha-se por dentro e segue em direção ao centro. Confira que o animal atravessa pela abertura, a porteira fecha antes da soltura e o pastor sai por salto. Com a porteira aberta, confira também o trajeto de um aldeão de outro ofício e de um aldeão sem cadastro.
2. Coloque um animal em uma construção coberta e outro do lado de fora. O pastor deve deixar o primeiro no recinto e continuar a coleta dos desgarrados.
3. Conte os animais do curral grande por vários dias do jogo e após salvar e reabrir. Se a contagem cair, registre a hora, a espécie e a porteira para cruzar com o log de abate.
4. Com mais de seis adultos da mesma espécie na área da vila, distribua dois adultos em cada curral e deixe o restante solto. O pastor não deve abater. Leve um terceiro adulto para um dos currais. Com o pastor próximo, confira o abate de um adulto desse curral e a permanência dos outros dois.
5. Com o grafo no modo `atual`, confira as três colunas das notas de 15:32. Se houver trajeto livre, o nó branco deve ficar no piso da porta e ligar à rua principal. Observe a releitura com a vila carregada. Salve, reabra o mundo e confira novamente.
6. Confira separadamente o arco da mina (N12) e a entrada da casa do mineiro (N28). Caminhe da rua até as entradas e verifique as ligações antes e depois de reabrir o mundo. Use `/vc bioma nota` para registrar a coordenada e o comportamento de cada entrada.
7. Se o lenhador subir novamente na obra, registre a hora, o local e o bloco atribuído a ele.
