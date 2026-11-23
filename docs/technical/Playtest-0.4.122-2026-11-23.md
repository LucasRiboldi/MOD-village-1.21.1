# Playtest da candidata Village Colony 0.4.122 — 23 de novembro de 2026

Colônia `4dac0976`, mundo “Novo mundo”, bioma planície. O recorte principal termina às 02:21, horário de São Paulo. O JAR do jogo já incluía o ajuste das entradas do grafo, mas precedia o commit de N26 para condução lateral. O log, o diário de ações e o snapshot das 02:21:20 sustentam os achados abaixo.

## O que funcionou

| Ofício | Evidência na colônia |
| --- | --- |
| Construtor | 1.415 registros de colocação de bloco. A vila chegou a 9 obras concluídas. |
| Carpinteiro e pedreiro | O diário registrou produção de madeira processada e de materiais de pedra. |
| Fazendeiro | 360 registros de colheita. Plantou trigo em `346,87,1432` às 02:14 e colheu na mesma coluna às 02:16. |
| Zelador | 495 registros de pavimentação e 39 de nivelamento. |
| Lenhador | 76 registros de derrubada ou retirada de madeira. |
| Mineiro | Mina com um nível abaixo da boca e 35 posições cavadas no snapshot final. |
| Fundidor | 385 registros de fundição. |
| Pastor | 33 registros de tosquia. O log registrou 11 entradas no curral, de oito vacas, dois porcos e uma ovelha. |

O snapshot final registrou 11 aldeões, 9 com ofício e 15 camas. O servidor marcou 6,7 ms por tique naquele instante.

## As sete notas do jogador

| Hora | Nota | Evidência e correção a investigar |
| --- | --- | --- |
| 02:14:04 | Aldeões aparecem no alto das casas. | O registro mostra resgates de telhado. Identificar o caminho e o destino que permitem subir na casa. |
| 02:14:32 | Dois aldeões pararam no mesmo ponto do telhado. | O resgate registrou saltos desde `338,86,1414` e `338,87,1412`. Reproduzir a geometria e verificar o ponto de aterrissagem. |
| 02:15:30 | Aldeões devem evitar colocar solo acima de qualquer camada da fazenda. | Há plantio e colheita na coluna indicada. Falta localizar uma colocação de solo por aldeão nesse trecho. Auditar os escritores de solo e testar a proteção da coluna em várias alturas. |
| 02:16:01 | Outro aldeão travou sobre a casa. | O mesmo ponto aparece em tentativas posteriores de alcançar plantações. Rastrear a rota de entrada e as tentativas após o resgate. |
| 02:17:07 | Curral quase vazio e suspeita de abate abaixo do mínimo. | O log marcou um abate de vaca no curral às 02:18:47. O registro de 11 adultos agrega a área e não demonstra a reserva daquele curral. |
| 02:18:13 | Terceiro aldeão, com nome azul, parado no telhado. | A nota cita `339,86,1414`. Pouco antes, a terceira morte de fazendeiro por queda ocorreu em `338,78,1408`. O nome azul, por si só, não identifica o aldeão nos registros. |
| 02:20:00 | Todos tentam o salto padrão primeiro. Salto maior só depois de travar. | A inspeção de `VillagerMoves` mostra que a lógica pode antecipar salto alto ao enxergar um obstáculo. Reproduzir em degraus e colisões, preservando manobras específicas de resgate e do curral. |

## Erros e melhorias ordenados por impacto

1. **Telhado e quedas.** Três fazendeiros morreram por queda junto da casa, às 01:16:42, 01:59:28 e 02:18:01. Cada morte ocorreu cerca de dois segundos após o primeiro salto do respectivo resgate, que mirava um piso de sete a oito blocos abaixo. O diário tem 23 registros de salto de resgate nessa colônia. Houve ainda uma queda fatal do pastor em outro ponto, sem ligação confirmada com esse telhado. O estado final de zero encalhados é a leitura de um instante.
2. **Abate e reprodução.** No final havia 1 ovelha, 10 vacas, 11 porcos e 4 galinhas no total da vila. Instrumentar a contagem por espécie e curral, com adultos e filhotes antes e depois de cada abate, além de registrar nascimentos e saídas. Antes do abate, exigir três adultos da espécie no curral da vítima e preservar pelo menos dois animais dessa espécie nesse curral.
3. **Acesso do mineiro e grafo.** O último grafo registrou quatro componentes isolados, além da entrada solta. A porta `306,91,1491`, próxima à casa registrada do mineiro, continuou em `loose`. Isso sustenta reabrir a verificação de N28. A mineração ocorreu, mas a conexão do arco com a rede antes e depois de recarregar ainda depende de prova específica para N12. Dois baús também ficaram fora das rotas a pé a partir do centro.
4. **Condução na busca.** O pastor abandonou nove tentativas na fase `FETCH` com destino a `304,89,1473`. Investigar tempo e rota nessa etapa. N26 trata a fase `LEAD`, com guia já presa, e continua aguardando playtest do JAR corrigido.
5. **Solo acima dos cultivos e salto padrão.** Reproduzir as duas notas com a coluna da plantação e obstáculos de diferentes alturas. Verificar se a proteção do solo afeta somente ações dos aldeões.
6. **Eficiência.** Na última janela de cinco minutos, o pastor passou 81% do tempo caminhando, o lenhador 55% e o construtor 27% bloqueado. Pedreiro e fundidor ficaram 59% e 78% ociosos. O relatório de suprimento registrou entregas travadas de troncos de carvalho, pedregulho e uma placa suspensa de carvalho para a obra, entre 02:19 e 02:21. Cruzar rota, locais de estoque e demanda antes de alterar atribuição de tarefas.

## Duas opções para o telhado

1. **Correção menor, recomendada.** Fazer o resgate escolher uma aterrissagem segura e impedir tentativas repetidas para o mesmo ponto perigoso. Reproduzir a queda da casa e verificar que o aldeão sai vivo e retoma o trabalho.
2. **Correção média.** Fazer a opção 1 e corrigir a rota que leva os aldeões ao mesmo telhado. A reprodução deve demonstrar como eles sobem, impedir o retorno e preservar subidas legítimas, como as do curral.

## Conferência do resgate de telhado

O resgate da 0.4.123 também está na candidata 0.4.124. No mesmo mundo, observe a casa junto de `338,86,1414`. Se um aldeão encalhar no telhado, confirme que sai vivo e volta ao trabalho. Num resgate com sete a oito blocos de desnível, procure `LET_DOWN` no diário de ações. Anote se o mesmo aldeão voltar ao telhado e exporte os relatórios com `/vc salvar`.

## Próximo playtest do rebanho

1. Abra a candidata 0.4.124 em um mundo com dois currais reconhecidos e um pastor. Anote a versão, o UUID curto da vila e as coordenadas das porteiras.
2. Acompanhe uma coleta até a entrega. A linha `FETCH` identifica o animal e `LEASHED` confirma a captura. `DELIVER` confirma que a porteira fechou e a guia foi solta. Se parar, salve a linha `ABANDON` com fase, motivo e posições.
3. Deixe dois adultos da mesma espécie no mesmo curral e observe alimentação e nascimento. `FED` mostra o par, a comida e a meta. `BREED_WAIT` registra espera por par pronto ao alcance ou comida disponível. `BIRTH` identifica o filhote associado ao par alimentado pelo mod. `VC_PENS` mostra adultos e filhotes por espécie no recinto e no conjunto dos currais carregados.
4. Com oito adultos da espécie nos currais e comida abaixo de 32, confirme que um adulto solto não autoriza abate. Acima de oito, o abate pode ocorrer se o curral da vítima começar com três adultos da espécie. Repita com 32 ou mais itens de alimento aceito, quando a meta sobe para 16.
5. Compare `VC_PENS` antes e depois. `VC_FLOCK_HARVEST` informa os adultos das duas reservas antes e depois e o número de filhotes preservados. `DEATH` registra mortes observadas nos currais. `EXIT` indica uma saída observada. `UNOBSERVED` indica que o animal ou o recinto ficou indisponível. Ao final, execute `/vc salvar`.

## Fontes

- Log do cliente preservado, linhas 5596, 6003, 8742, 9370–9426, 9733, 9916, 10251, 10478–10593, 10821 e 11005.
- Diário `villagecolony-actions-20261122-235126.jsonl`, filtrado pela colônia `4dac0976`.
- Snapshot `snapshot-20261123-022120.txt`, dia 7, tique 735763.
- Relatório de suprimento `supply-gaps.jsonl`, entradas entre 02:19:17 e 02:20:47 da mesma colônia.
