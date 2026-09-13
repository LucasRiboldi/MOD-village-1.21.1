# Plano — Releitura de mudanças feitas pelo jogador

**Status: implementado; validação em jogo pendente.** `build` e 314 GameTests verdes.

## Resultado esperado

Após o jogador abrir ou alterar terreno perto da colônia, a busca de lote
reavalia ruas e espaços em passagens limitadas. Uma frente encerrada da mina
volta ao trecho afetado. Baús de profissão elegíveis seguem sendo lidos ao
vivo, sem persistir snapshots do mundo.

## Sequência

1. **Modelo da mina:** teste vermelho para reabrir braço concluído no índice
   afetado; implementar reset local do cursor e do estado transitório.
2. **Construção:** teste vermelho para invalidação dos índices/cursor; expor
   invalidação por colônia e garantir nova leitura limitada.
3. **Eventos Fabric:** capturar interação do jogador e comparar candidatos
   após o tick; encaminhar apenas posições cujo estado mudou para a mina e
   para as colônias próximas. Registrar e limpar fila no ciclo de vida.
4. **Documentação:** anotar elegibilidade e releitura dos baús, atualizar
   `STATE.md`, `TODO.md` e `docs/technical/Development-Log.md`.
5. **Verificação:** `./gradlew build`, `./gradlew runGametest`; revisar diff e
   parar para validação do usuário antes de iniciar outro lote. A invalidação
   desligada fez somente `invalidatingAfterAPlayerRoadChangeReindexesTheWorld`
   falhar (313 passaram); reativada, todos os 314 passaram.

## Limites

Sem mudança de geometria/save da mina (E45), sem varredura de chunks
descarregados, sem inventário global de baús e sem commit/push neste lote.
