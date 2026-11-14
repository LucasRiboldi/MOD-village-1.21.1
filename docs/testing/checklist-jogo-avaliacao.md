# Lista de testes em jogo — para lapidar o mod e subir a nota

Pedido do autor (2026-11-14). A avaliação de 13-11 (`docs/audit/2026-11-13-avaliacao-qualidade.md`) deu **5,0/10**.
As dimensões que mais pesam, e que **só o jogo prova**, são D2 regras (4, peso 15%), D8 jogador (3), D7 compatibilidade
(5), D4 desempenho (5) e D3 save (6). Cada sessão abaixo diz qual dimensão ela alimenta.

Esta lista complementa o `roteiro-por-bioma.md` (como criar o mundo de um bioma e a régua de 0 a 10), e não o
substitui.

## Como anotar (vale para todas as sessões)

- **Todo problema vira nota na hora e no lugar:** `/vc bioma nota [A3] pastor deixou a vaca fugir`. O código entre
  colchetes é o do item desta lista; a nota vai ao log com posição e bioma.
- **Item que deu certo também vale nota:** `/vc bioma nota [A3] ok`. Sem isso, "não vi problema" e "não olhei" ficam
  iguais no relatório.
- Antes de fechar o jogo: `/vc bioma` e `/vc dados`.
- **O log rota a cada sessão:** gere os relatórios (seção "Depois de cada sessão") antes de abrir o jogo de novo.
- Fique **dentro da vila**: ela só trabalha com jogador dentro (ADR-003, Emenda 6).

## Sessão A — A vila trabalha sozinha (D2, F1) · planície, vila nova · 3 dias de jogo

Mundo novo, semente anotada. Ache a vila (`/locate structure #minecraft:village`), entre nela e não mexa em nada no
primeiro dia. Acelere com `/tick sprint 1d`, um dia por vez.

| Código | O que fazer | O que deve acontecer | Regra |
|---|---|---|---|
| A1 | Esperar o primeiro ciclo | `Colony created` no log; casa fundadora assentada sem cortar a vila | — |
| A2 | `/vc bioma` no fim de cada dia | `lots_ok` acima de 0 e pelo menos 1 obra concluída por dia | 282 |
| A3 | Olhar o curral quando o pastor trouxer animal | o animal entra pelo portão sem fugir para longe; nenhum "gave up: the cow did not walk into the pen" | 284 |
| A4 | Soltar 1 vaca a uns 5 blocos do curral | o pastor recolhe; animal fora da vila (além da caixa) não é recolhido | 283 |
| A5 | Seguir o mineiro até a mina | chega pela rua; o ramal até a mina está calçado; nenhum aldeão nadando com caminho seco ao lado | 278, 280 |
| A6 | Seguir o lenhador numa árvore | ele encosta no tronco antes de derrubar; derruba a árvore inteira | 277 |
| A7 | Procurar aldeão parado na soleira de casa ou entre alçapões | nenhum "fenced in" nesses lugares | 275, 276 |
| A8 | Procurar aldeão parado no mesmo bloco por mais de 1 min | nenhum encalhe repetido no mesmo ponto; o solto sai do lugar | 279 |
| A9 | Anotar tronco que surgir onde não havia árvore | cada um aparece numa linha `VC_LOG_PLACED`, com quem o pôs | 281 |
| A10 | Fim do dia 3 | todo ofício com menos de 40% do expediente parado; meta, até 15% (Regra 51) | 51 |

## Sessão B — Segundo bioma (D2, portão Beta) · taiga ou deserto · 2 dias

O portão Beta pede "todo ofício abaixo de 40% parado em **dois biomas, dois jogos seguidos**". Repita A2, A3, A5, A8 e
A10 num bioma diferente, com os códigos B2, B3, B5, B8 e B10. Na taiga, olhe também a mata no caminho da rua (Regra
280). No deserto, olhe o arenito da rua e o teto da mina.

## Sessão C — O jogador mexe na vila (D2 e D8) · na vila da sessão A

O mod precisa conviver com quem joga. Faça um item por vez e anote a reação.

| Código | O que fazer | O que deve acontecer |
|---|---|---|
| C1 | Quebrar 3 blocos de uma obra em andamento | o construtor repõe; a obra não trava |
| C2 | Tirar todos os itens do baú de um ofício | o ofício busca de novo ou espera; nada some do baú |
| C3 | Construir uma casinha sua (4×4, porta, cama) perto da vila | o mod não derruba nem reforma a sua casa; anote se algum aldeão passa a usar a sua cama |
| C4 | Pôr um bloco no meio da rua | a rua desvia ou o pedreiro tira; ninguém fica preso |
| C5 | Matar 1 aldeão de ofício | nenhum erro no log; anote se e quando outro aldeão assume o ofício |
| C6 | Negociar com um aldeão de ofício | a troca do jogo continua funcionando |
| C7 | Passar uma noite com zumbis (sem tocar o sino) | os aldeões se abrigam; voltam ao trabalho de manhã |
| C8 | Tocar o sino de dia | todos se escondem e voltam depois |

## Sessão D — Save e reabertura (D3, F2)

Antes de começar, **copie a pasta do mundo** (`%APPDATA%\.minecraft\saves\<mundo>`) para outro lugar.

| Código | O que fazer | O que deve acontecer |
|---|---|---|
| D1 | Salvar e sair com uma obra pela metade; reabrir | a obra continua do ponto em que parou |
| D2 | Reabrir e conferir 3 aldeões | cada um com o mesmo ofício, baú e cama de antes |
| D3 | Reabrir um mundo salvo na 0.4.82 ou na 0.4.83 | abre sem erro e a vila segue trabalhando |
| D4 | Fechar o jogo pelo Gerenciador de Tarefas com a vila trabalhando; reabrir | o mundo abre; a vila não volta vazia nem perde as obras |

## Sessão E — Desempenho (D4, F2)

| Código | O que fazer | O que deve acontecer |
|---|---|---|
| E1 | `/spark profiler start`, `/tick sprint 1d`, `/spark profiler stop` com 1 vila | TPS perto de 20; o mod em poucos % da thread |
| E2 | O mesmo com 2 ou mais vilas carregadas (fique entre duas vilas próximas) | nenhum travamento visível; picos do `VC_COST` abaixo de 50 ms |
| E3 | Jogar 30 min sem acelerar | nenhuma engasgada perceptível |

Mande o link do Spark junto com o log.

## Sessão F — Compatibilidade (D7, F3)

| Código | O que fazer | O que deve acontecer |
|---|---|---|
| F1 | Instalar Lithium, FerriteCore e ModernFix (versões para 1.21.1) junto do mod; jogar 20 min | o jogo abre; a vila trabalha igual; nenhum erro de mixin no log |
| F2 | Abrir o mundo para LAN e entrar com uma segunda conta (se tiver) | os dois veem os aldeões trabalhando; nada dessincroniza |
| F3 | Subir um servidor dedicado com o mod e entrar | a vila trabalha; os comandos `/vc` respondem |

## Sessão G — Primeira impressão (D8, F4)

Jogue como quem nunca viu o mod.

| Código | O que observar |
|---|---|
| G1 | Dá para entender, só olhando, que os aldeões estão trabalhando sozinhos? |
| G2 | As mensagens no chat ajudam ou poluem? Anote cada uma que confundir. |
| G3 | Com o jogo em inglês, alguma mensagem aparece em português? Anote quais. |
| G4 | Falta algum controle (desligar a vila, pausar uma obra, ver o que cada um faz)? |

## Depois de cada sessão

Com o jogo fechado:

```bash
python scripts/biome_report.py --out docs/testing/relatorios/<bioma>-<data>.md
```

```bash
python scripts/time_ledger.py
```

```bash
python scripts/action_report.py
```

```bash
python scripts/cost_ledger.py
```

Mande o relatório, o link do Spark e as notas. A triagem segue o modo de correção por playtest (`CLAUDE.md` §0.2.2):
log primeiro, depois as opções para você escolher.

## O que cada sessão move na nota

| Sessão | Dimensão (nota de 13-11) | O que sobe |
|---|---|---|
| A, B | D2 regras (4) | regras candidatas viram "confirmadas em jogo"; o portão Beta exige dois biomas |
| C, G | D8 jogador (3) e D2 | defeitos de convivência com o jogador e lista do que falta ao jogador |
| D | D3 save (6) | primeira prova de reabertura e de save antigo |
| E | D4 desempenho (5) | o número que autoriza (ou dispensa) repartir o ciclo |
| F | D7 compatibilidade (5) | primeira matriz de mods e de multijogador |
