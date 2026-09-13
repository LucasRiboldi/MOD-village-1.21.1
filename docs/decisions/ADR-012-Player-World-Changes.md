# ADR-012 — Releitura do mundo após alterações do jogador

**Data:** 2026-09-13
**Estado:** Aceita
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
2. Invalidar os cursores e índices do scanner de construção para colônias
   próximas da posição alterada. A varredura limitada volta a consultar o
   mundo e pode indexar estradas novas ou terrenos liberados.
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
- A busca de construção pode levar várias passagens limitadas para reindexar a
  área, mas não precisa aguardar o cursor antigo terminar.
- Padrões alternativos de escavação e migração de formato de save ficam fora
  deste escopo e continuam sujeitos a decisão própria (E45).

## Verificação

Testes devem demonstrar que uma alteração reabre apenas frentes próximas e
que a busca reconsulta estradas/terreno após invalidação. Alterações sem estado
final diferente não podem invalidar. GameTests confirmam a integração com o
mundo Fabric; a confirmação visual ainda depende de sessão em jogo.
