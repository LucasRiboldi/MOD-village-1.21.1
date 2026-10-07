# Correcoes dos bloqueios observados em jogo

**Objetivo:** corrigir os bloqueios confirmados do mineiro, crescimento da estrada, moradia do fundidor e leitura do `/vc log`, preservando as regras fisicas atuais.

## 1. Desvio do mineiro

- Adicionar teste unitario que prove que um desvio local mira o trecho intermediario da rota, nao a pedra distante.
- Fazer `MinerDetours` usar `WorkTargets` como objetivo imediato e manter a pedra protegida.
- Rodar os testes unitarios do mineiro.

## 2. Estrada que cria acesso ao lote

- Adicionar GameTest com estrada sem ponta utilizavel, mas com trecho reto capaz de receber uma ramificacao.
- Registrar ramificacoes como candidatos secundarios; pontas verdadeiras continuam prioritarias.
- Rodar o lote de GameTests de extensao de estrada.

## 3. Moradia segura para trabalhador legado

- Adicionar GameTest em que o trabalhador tem cama externa e existe cama livre numa estrutura concluida.
- Migrar `HOME` com bilhete Vanilla para a cama valida antes de criar o bau.
- Suprimir repeticao do mesmo aviso enquanto o estado nao muda.
- Rodar os GameTests de `ChestSpawner`.

## 4. Diagnostico e custo de replanejamento

- Adicionar teste unitario para mostrar um unico estado atual por profissao e separar o ultimo bloqueio historico.
- Ajustar o apresentador do `/vc log` sem alterar o armazenamento do trace.
- Usar a ramificacao persistente da estrada para evitar varreduras completas repetidas sem progresso geometrico.

## 5. Verificacao e estado vivo

- Rodar testes unitarios, `build` e `runGametest`.
- Atualizar `STATE.md`, `TODO.md` e `docs/archive/technical/Development-Log.md` com resultados exatos e playtests ainda pendentes.
