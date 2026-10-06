# ADR-036 — Decisões do autor de 2026-10-06

**Status:** Accepted
**Date:** 2026-10-06
**Decision Type:** Gameplay / Process
**Origem:** a lista de 31 decisões levantada depois da ADR-035, respondida pelo
autor no mesmo dia, mais quatro esclarecimentos (pedreiro, felicidade, mina,
limpeza do save).

## Decisões

Numeração da lista apresentada ao autor. "Código" diz o que muda; "Nada"
registra decisão que confirma o que já existe.

### A. Projeto

| # | Decisão | Código |
|---|---|---|
| 1 | Juntar o PR #9 na `main` | merge pelo autor (o modo automático não permite merge sem revisão) |
| 2 | Apagar as branches antigas | pelo autor; `codex/bighousemod` fica: o `Barn_Majest.nbt` dele difere do da integração |
| 3 | Versão 0.3.1 | `gradle.properties` |

### B. Regras do jogo

| # | Decisão | Código |
|---|---|---|
| 4 | Toda casa — da vila original ou construída — tem baú ao lado da cama, **encostado na parede**, nunca na frente de porta ou escada | colocação do baú de cama |
| 5 | **Desfaz a Regra 25** (a maior planta que couber) | ordem das plantas deixa de ser por tamanho; a primeira casa continua a menor |
| 6 | Peça que depende de profissão: na **4ª** tentativa sem sucesso aparece **no baú da profissão** e o erro vai ao log. Peça cuja profissão devolve erro **5 vezes** é pulada | suprimento da obra e barreira |
| 7 | Os conflitos regra × código são listados e decididos pelo autor, um a um | lista na reanálise |
| 8 | Pode haver desempregado. **O pedreiro não fica parado:** sem pedido, faz estoque de peças de pedra da vila (degrau, laje, tijolo) até um mínimo | meta de pedra lavrada |
| 9 | Todo baú guarda no máximo **3 compartimentos do mesmo item**. Baú lotado: os últimos **10 compartimentos** vão para um baú **sem profissão** livre da vila. Sem baú livre na vila, a obra prioritária é um **`storage_majest`** | baús e planejador |
| 10 | Vila abandonada **para de trabalhar**; as profissões dela se desativam; o conteúdo dos baús fica | ciclo de vida da colônia |
| 11 | A vila só trabalha com o jogador **dentro dela há mais de um minuto** | foco da vila |
| 12 | Limpar o mundo de teste: **só os dados do mod**, com backup antes | fora do repositório |
| 13 | Combustível da fornalha e mudas do viveiro **surgem do nada** | Nada (ADR-028) |
| 14 | Baú da vila fora de casa **é** da colônia | Nada (Regra 45) |
| 15 | Aldeão preso que tenta se soltar **5 vezes** sem sair do lugar, ou que à noite não acha caminho para casa, **é teletransportado para a cama dele** | fuga |
| 16 | Levar material para perto do construtor: já funciona | Nada |

### C. Mina e buscas

| # | Decisão | Código |
|---|---|---|
| 17 | No fundo, a mina anda para o lado **oposto ao centro da vila** e abre uma **rampa que sobe** no mesmo ritmo da descida, minerando no caminho | geometria da mina |
| 18 | Busca de recurso vai até **10 blocos além da borda** da vila, **alternando** uma busca a partir do centro e outra a partir de uma borda | varredura |
| 19 | Blocos com mais de uma posição (eixo, metade, formato) saem **na posição da planta** | construtor |

### D. Ideias

| # | Decisão | Código |
|---|---|---|
| 20 | **Felicidade** medida na reunião da vila (30 s): comida por pessoa, camas sobrando, obras concluídas. Feliz → mais filhos; infeliz → nenhum filho novo. Aparece no `/vc log` | ciclo da colônia |
| 21 | Fornalha de verdade: **não por enquanto** | Nada |
| 22 | Colônia decide pela cadeia de fontes: **depois** (ADR-035 §3, fase 2) | Nada |
| 23 | O mineiro **recolhe o veio inteiro** | mineração |

### E. Arrumação

| # | Decisão | Código |
|---|---|---|
| 24 | Arquivar documentos antigos (`git mv`, histórico preservado) | docs |
| 25 | Apagar `agent/` e `mods/` | repositório |
| 26 | Tirar o site e o JAR do repositório; JAR vai para a página de versões do GitHub | repositório |
| 27 | Apagar testes que só conferem texto de mensagem | testes |
| 28 | E43 (descanso vale sempre): **o agente avalia** | reanálise |
| 31 | Viveiro **só do lenhador** | ADR-020 emendada |

## Consequências

- Cada item de código é um commit com teste que falha antes; GameTest quando
  depende do mundo; nada disso é visto em jogo até o próximo playtest.
- A Regra 25 sai da tabela de regras; a Regra 8 é reescrita (item 4); o
  número de tentativas da ADR-022/ADR-034 passa de 3 para 4 (item 6).

## Estado

Preenchido a cada item aplicado.

| # | Commit | Verificação |
|---|---|---|
| 3 | este commit | versão 0.3.1; JAR `2C2F849F…3180` em `downloads/` e na pasta `mods` |
| 4 | `36891a42` | baú de cama em toda casa; 2 GameTests, escada por mutação |
| 5 | `55ed3b2d` | Regra 25 desfeita; HousePlansTest 27/27 |
| 6 | `dcaed4ac`, `afce8d76` | 4 tentativas no baú da profissão; 5ª falha ao pôr pula; barreira removida |
| 8 | `18c1bef5` | estoque do pedreiro; pedido nominal protegido no ciclo |
| 9a | este commit | teto de 3 compartimentos por item no depósito e na leitura; baú nomeado sem teto; 615/615 ×3 |
| 10 | `a3cbe44e` | vila abandonada para; dispensa provada por mutação |
| 11 | `1bb32380` | foco com 1 minuto de presença |
| 12 | fora do repositório | dados do mod apagados do "Novo mundo", backup em `.minecraft/saves-backup` |
