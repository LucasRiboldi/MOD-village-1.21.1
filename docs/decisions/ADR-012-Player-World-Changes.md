# ADR-012 — Releitura do mundo após alterações do jogador

**Data:** 2026-09-13
**Estado:** Aceita, emendada em 2026-09-14
**Escopo:** Fabric 1.21.1; construção, frentes da mina e depósitos elegíveis.

## Contexto

Os índices de ruas e cursores de varredura tornam a busca de lotes incremental.
Uma rua ou terreno alterado pelo jogador pode tornar esses dados obsoletos.
Do mesmo modo, uma frente da mina encerrada não reconsidera automaticamente
um trecho que o jogador abriu. O inventário de baús elegíveis já é lido do
mundo a cada ciclo; o problema de estoque é a elegibilidade do baú, não uma
cópia de inventário desatualizada.

## Decisão

1. Observar interações de bloco do jogador no servidor e comparar os estados
   antes e depois no fim do tick. Só uma mudança efetiva invalida dados; não
   persistir cópias do terreno nem forçar chunks.
2. Reconciliar a coluna alterada nos índices de estrada das colônias
   próximas, sem apagar o cursor de varredura nem os resultados parciais já
   acumulados. Se o bloco final for pavimentação elegível, acrescentá-lo ao
   índice. Reiniciar apenas o cursor de consulta das estradas para que os
   candidatos sejam reavaliados com o estado atual do mundo. A elegibilidade
   de terreno, volume e acesso continua sendo lida ao vivo; não há cache de
   decisão de lote.
3. Se uma posição alterada estiver sobre ou imediatamente ao lado de uma
   posição já percorrida de um braço da mina, reabrir aquele braço a partir
   do primeiro índice afetado, limpando estado transitório de bloqueio e veio.
   Não mudar a geometria persistida da mina nesta decisão.
4. Baús acessíveis aos profissionais continuam sendo os depósitos de
   trabalhador registrados e o baú da boca da mina. Esses inventários são
   consultados ao vivo; um baú arbitrário colocado no mundo não passa a ser
   depósito da colônia sem ser descoberto pelo vínculo de armazenamento.

## Consequências

- Alterações do jogador não substituem a verdade do mundo nem geram varredura
  global; chunks descarregados permanecem fora da consulta.
- Alterações repetidas não fazem a varredura incremental voltar ao centro.
  Cada edição corrige somente a coluna de rua correspondente e reinicia a
  consulta limitada do índice, preservando o avanço da varredura em curso.
- Terreno liberado ao lado de uma rua existente é reavaliado pelo cursor de
  consulta; pavimentação nova também entra no índice imediatamente. Chunks
  descarregados continuam fora da consulta.
- Padrões alternativos de escavação e migração de formato de save ficam fora
  deste escopo e continuam sujeitos a decisão própria (E45).

## Verificação

Testes devem demonstrar que uma alteração reabre apenas frentes próximas e
que a busca reconsulta estradas/terreno após a edição. Alterações sem estado
final diferente não podem invalidar. GameTests confirmam a integração com o
mundo Fabric; a confirmação visual ainda depende de sessão em jogo.

## Emenda 1 — 2026-09-14

O verbo "invalidar" da decisão original apagava também os cursores e a
acumulação incremental. Com várias alterações do jogador, a busca podia
recomeçar repetidamente sem terminar. A emenda limita a invalidação aos dados
afetados: remove ou acrescenta a coluna de pavimentação alterada e reinicia o
cursor de consulta, mantendo a varredura parcial. As condições do lote são
recalculadas contra o mundo em cada consulta; nenhuma decisão de elegibilidade
fica em cache.
