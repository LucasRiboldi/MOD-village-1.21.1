# Roteiro de jogo da 0.4.107 — o que testar, em linguagem simples

Para conferir no jogo tudo o que mudou da 0.4.94 à 0.4.107 (Regras 326 a 343). Nada disso foi visto em jogo ainda:
os testes automáticos provam o mecanismo, e só o jogo prova o efeito.

## Antes de começar

1. Feche o Minecraft, se estiver aberto.
2. No TLauncher, a pasta `mods` deve ter **só** o `village-colony-0.4.107.jar` ativo (os antigos ficam como
   `.disabled`).
3. Crie **mundos novos**: várias mudanças só valem para o terreno gerado agora. O ideal são dois:
   - um em **montanha** (perto de uma vila no morro);
   - um em **planície**.
4. Jogue pelo menos **3 dias do jogo** (cerca de 1 hora) perto da vila: os relatórios saem no fim de cada expediente.
5. Sempre que vir algo estranho (ou bom), anote na hora:

   ```text
   /vc bioma nota <o que você viu>
   ```

   Exemplo: `/vc bioma nota mineiro parado na boca da mina`. Cada nota vira um item da próxima análise.

6. No fim, antes de sair: `/vc dados`. Depois feche o jogo normalmente (sem forçar).

## O que olhar

Marque cada item: ✅ deu certo, ❌ deu errado (anote com `/vc bioma nota`), ➖ não deu para ver.

### No mundo de montanha

| # | O que fazer | Deu certo se… | Versão |
|---|---|---|---|
| 1 | Chegue à vila nova e olhe o céu acima das casas | **Não há** terra, pedra ou caminho flutuando em cima das casas | 0.4.101 |
| 2 | Ache a casa grande do mod (BigHouseMOD) | Ela está **perto das casas e na mesma altura**, não no topo do morro | 0.4.102 |
| 3 | Ache a boca da mina | Ela fica perto da casa grande, e o mineiro chega até ela | 0.4.102 |
| 4 | Siga o mineiro por alguns minutos | Ele **desce cavando uma escada larga** (3 blocos de largura e de altura), em degraus; não fica parado na boca | 0.4.100 |

### Em qualquer mundo

| # | O que fazer | Deu certo se… | Versão |
|---|---|---|---|
| 5 | Olhe a roupa dos trabalhadores da vila | Nenhum trabalhador do mod **ganha roupa de ofício Vanilla** (avental de fazendeiro, chapéu de bibliotecário etc.) | 0.4.105 |
| 6 | Observe o fundidor (fornalha) e o carpinteiro (bancada de madeira) | Eles ficam **ocupados**: fazem peças, buscam material, cuidam das casas; não ficam muito tempo parados de pé | 0.4.103 |
| 7 | Digite `/vc blocos` | Aparece uma tabela com os blocos da vila, a reserva e o estoque; em vermelho o que falta | 0.4.106 |
| 8 | Abra alguns baús da vila | Não há pilhas enormes de escada, laje ou cerca: o estoque de peças prontas é pequeno (4, 12 ou 1 de cada), e a madeira fica em tora | 0.4.104 |
| 9 | Veja o curral e o pastor | O pastor **não tira** o animal que já está dentro do curral, e o nome dele não fica vermelho (travado) | 0.4.95 |
| 10 | Veja árvores tiradas do caminho ou de uma obra | A árvore sai **inteira**, sem sobrar tronco no ar | 0.4.94 |
| 11 | Ache o Zelador (o aldeão que cuida do chão) | Ele calça os caminhos, nivela o chão e tira a neve; ninguém mais faz isso | 0.4.96 |
| 12 | Veja o fazendeiro | Ele **não planta** dentro do curral; sem tarefa, faz a ronda da roça; com tudo pronto, a vila pede a plantação grande | 0.4.97 |
| 13 | Cave um buraco (ou ponha um bloco) no chão da vila e espere um dia | O que você mudou **fica**: os aldeões não tapam nem nivelam a sua coluna | 0.4.98 |
| 14 | Olhe os caminhos até as casas de dois andares | O caminho chega à **porta de baixo**, nunca à porta de cima | 0.4.99 |

## Depois de jogar

Me avise ("joguei"). Eu leio o log e os relatórios:

- `time_ledger.py`: o tempo de cada ofício, agora com a espera por motivo;
- `action_report.py`: o que cada aldeão fez e por que desistiu;
- `stock_report.py`: se a reserva de cada bloco sobe ou desce, e se cada trava de cru serve;
- as linhas `VC_VANILLA_JOB` (roupa Vanilla) e as suas notas.
