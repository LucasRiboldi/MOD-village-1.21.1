# Decisoes de correcao pendentes

**Data:** 2026-09-23
**Estado:** escolhas do autor registradas; desenho tecnico e plano de entrega em preparacao.

Escolha `A`, `B` ou `MEDIR PRIMEIRO` para cada item. Depois da escolha, registrar motivo e data; implementacao so ocorre com uma ordem posterior.

| No. | Erro ou pendencia | Evidencia atual | Escolha |
|---:|---|---|---|
| 1 | E42 e E43: impasse e descanso | E43 confirmado por codigo; E42 sem GameTest | A |
| 2 | Bloco sem apoio e obra incompleta | decisao de regra | B |
| 3 | Mina, portal, limite e veios | contratos verdes; playtest P1.3 pendente | B, com limite pendente |
| 4 | E38/E3: residuos e capacidade; E21 | E38 confirmado; E21 suspeita | B |
| 5 | P0.9/C3/S6-S4: terreno, rua e scanner | investigacao | A |
| 6 | P2.1/E41: desempenho e endurance | medicao/cobertura pendente | A |
| 7 | P1.4: telemetria | contratos verdes; playtest pendente | B |
| 8 | P0.8/P1.2/P1.4: migracoes e estruturas | playtests pendentes | A |
| 9 | Pastor, fundidor, E4 e E21 | cobertura insuficiente | A, depois B |
| 10 | Documentacao, JAR e codigo residual | divergencia confirmada | A |
| 11 | Village Growth Planner | evolucao de produto | A |

## 1. E42 e E43: fila e descanso

**Erro:** a segunda passagem reserva capacidade em descanso; E42 ainda precisa de GameTest.

- **A. Elegibilidade unica:** descanso impede reserva nas duas passagens.
  - Positivo: local, sem estado novo e diretamente testavel.
  - Negativo: um unico profissional pode esperar quatro ciclos.
- **B. Retry com backoff:** esperar 1/2/4 ciclos e terminar em estado observavel.
  - Positivo: tentativas explicitas e menos consulta repetida.
  - Negativo: cria fila, expiracao, limpeza e risco de estado preso.

**Melhor:** A. **Prova:** GameTest E42, quatro ciclos e segundo profissional. **Decisao:** PENDENTE.

## 2. Blocos dependentes, recursos e obra incompleta

**Erro:** escada ou tocha pode nao ter apoio ou rota valida.

- **A. Fases e validacao previa:** estrutura, dependente e especial; projeto invalido antes de abrir.
  - Positivo: evita espera infinita e obra parcialmente silenciosa.
  - Negativo: exige catalogo de dependencias e estado INVALID_BLUEPRINT.
- **B. Pular bloco impossivel.**
  - Positivo: nao bloqueia a fila.
  - Negativo: obra parece concluida e reparo pode repetir a lacuna.

**Melhor:** A. **Prova:** testes de escada/tocha sem apoio e GameTest sem reserva de lote. **Decisao:** PENDENTE.

## 3. Mina, portal, limite e veios

**Erro:** portal destruido nao pode voltar; mina deve terminar e abrir nova boca oposta somente apos abandono.

- **A. Maquina de estados finita:** progresso so por bloco removido; EXHAUSTED abandona a mina.
  - Positivo: preserva geometria e impede recriacao; carvao entra como prioridade de alvo.
  - Negativo: migracao e troca de boca exigem invariantes estritas.
- **B. Replanejar/reconstruir boca e ramais ao falhar.**
  - Positivo: tenta recuperar alteracao do mundo.
  - Negativo: reintroduz portal infinito, duplicacao e escavacao duplicada.

**Melhor:** A. **Prova:** portal removido, limite, boca oposta e prioridade; playtest P1.3. **Decisao:** PENDENTE.

## 4. Residuos, capacidade e rotas: E38, E3, E21

**Erro:** varas, macas e mudas podem assorear bau; E21 ainda nao tem causa confirmada.

- **A. Consumidor por item e capacidade reservada:** definir origem, consumidor, limite e overflow.
  - Positivo: recursos fisicos e rastreaveis.
  - Negativo: catalogo cresce com os drops.
- **B. Armazem global com pedidos genericos.**
  - Positivo: centraliza transferencias.
  - Negativo: subsistema grande e risco de pedidos invisiveis.

**Melhor:** A. **Prova:** matriz de itens, bau cheio e reproducao isolada de E21. **Decisao:** PENDENTE.

## 5. Terreno, ruas, centro e scanner

**Erro:** C3 recusa camas; rua e lote usam reguas distintas.

- **A. Politicas separadas e metricas por motivo.**
  - Positivo: protege o mundo do jogador e isola causa.
  - Negativo: nao entrega nivelamento visual imediato.
- **B. Unificar regra e terraplanar/estender estrada automaticamente.**
  - Positivo: aparencia uniforme.
  - Negativo: muda mundo fora da obra e exige ADR.

**Melhor:** A. **Prova:** GameTests de camas/terreno, metricas e ADR se alterar solo. **Decisao:** PENDENTE.

## 6. Desempenho e endurance: P2.1 e E41

**Erro:** falta medida de save e cobertura de degradacao longa.

- **A. Orcamento por colonia e round-robin.**
  - Positivo: reduz picos e da latencia previsivel.
  - Negativo: uma colonia pode esperar alguns ticks.
- **B. Cache global agressivo ou trabalho assincrono do mundo.**
  - Positivo: pode melhorar media em mundo grande.
  - Negativo: invalidacao, chunks e concorrencia elevam risco.

**Melhor:** A. **Prova:** 36 colonias, justica e mediana/p95/maximo no save. **Decisao:** PENDENTE.

## 7. Telemetria: P1.4

**Erro:** falta confirmar transicoes reais e distinguir espera normal de falta de progresso.

- **A. Transicoes estruturadas, sessao efemera e progresso amostrado.**
  - Positivo: baixo volume, privacidade e analise confiavel.
  - Negativo: toda atividade precisa transicoes simetricas.
- **B. Traco por tick persistido no save.**
  - Positivo: detalha falha rara.
  - Negativo: aumenta I/O, volume e dados de posicao.

**Melhor:** A. **Prova:** parser, versao desconhecida e playtest P1.4. **Decisao:** PENDENTE.

## 8. Migracoes, BigHouse e baus

**Erro:** regras precisam sobreviver a save antigo e reabertura sem recriar estruturas.

- **A. Migracao unica, idempotente e auditavel.**
  - Positivo: protege mundo do jogador e torna reabertura testavel.
  - Negativo: aumenta fixtures por versao de save.
- **B. Auto-reparo a cada ciclo/carregamento.**
  - Positivo: tenta recuperar estado ausente.
  - Negativo: pode duplicar bau, casa e portal.

**Melhor:** A. **Prova:** fixtures anterior/atual/migrada, carga dupla e playtests. **Decisao:** PENDENTE.

## 9. Cobertura: pastor, fundidor, E4 e E21

**Erro:** cobertura desigual; E4 e E21 sao suspeitas sem reproducao.

- **A. Matriz critica com sementes fixas.**
  - Positivo: falhas reproduziveis para material, bau, agua, cancelamento e retomada.
  - Negativo: requer manter matriz pequena.
- **B. Endurance/randomicos genericos primeiro.**
  - Positivo: pode capturar vazamento e custo crescente.
  - Negativo: mascara causa e dificulta diagnostico.

**Melhor:** A, com B depois. **Prova:** matriz pastor/fundidor e endurance medido. **Decisao:** PENDENTE.

## 10. Documentacao, release e codigo residual

**Erro:** STATE.md excede teto, ha historico conflitante e ConstructionService.forget nao tem chamador conhecido.

- **A. Manifesto de release e auditoria de residuos.**
  - Positivo: verifica commit, hashes, testes e destinos; reduz deriva.
  - Negativo: exige pequena automacao e decisao sobre metodo residual.
- **B. Reconciliacao manual periodica.**
  - Positivo: nenhuma ferramenta nova agora.
  - Negativo: depende de disciplina e repete deriva.

**Melhor:** A. **Prova:** dry-run com hash divergente e busca/teste antes de remover metodo. **Decisao:** PENDENTE.

## 11. Village Growth Planner

**Pendencia:** evolucao de produto; casa para nao residencial nao deve mudar por acidente.

- **A. VillageInventory antes de NEED_SCORE.**
  - Positivo: separa observacao de decisao e permite migracao gradual.
  - Negativo: entrega em estagios.
- **B. Reescrever imediatamente o planejador por score global.**
  - Positivo: comportamento rico de uma vez.
  - Negativo: mistura lote, recursos, reparo e crescimento.

**Melhor:** A, apos playtests. **Prova:** ADR, testes e GameTest de vila existente. **Decisao:** PENDENTE.

## Registro do autor

Exemplo de resposta: 1=A, 2=A, 3=A, 4=MEDIR PRIMEIRO.

| No. | Escolha | Motivo do autor | Data | Implementacao autorizada? |
|---:|---|---|---|---|
| 1 | A | Elegibilidade unica | 2026-09-23 | Sim, apos teste de regressao |
| 2 | B | Pular bloco impossivel | 2026-09-23 | Sim, apos contrato de resultado parcial |
| 3 | B | Replanejar falha tecnica; remocao do jogador e definitiva | 2026-09-23 | Sim, sem reconstruir portal/arco/lanterna removidos pelo jogador |
| 4 | B | Armazem global com pedidos genericos | 2026-09-23 | Sim, apos ADR e limite de escopo |
| 5 | A | Politicas separadas e metricas | 2026-09-23 | Sim, apos reproducao |
| 6 | A | Orcamento por colonia e round-robin | 2026-09-23 | Sim, apos baseline de desempenho |
| 7 | B | Traco por tick persistido no save | 2026-09-23 | Sim, apos contrato de retencao e migracao |
| 8 | A | Migracao unica, idempotente e auditavel | 2026-09-23 | Sim, apos fixtures de save |
| 9 | A, depois B | Matriz critica, depois endurance | 2026-09-23 | Sim |
| 10 | A | Manifesto de release e auditoria | 2026-09-23 | Sim |
| 11 | A | VillageInventory antes de NEED_SCORE | 2026-09-23 | Sim, apos playtests de base |

## Limite confirmado para a decisao 3B

A regra ja aprovada para a mina diz que arco, lanterna ou portal removido pelo jogador
permanece removido como bloco normal. O autor confirmou em 2026-09-23 que a recuperacao
automatica e limitada a falhas tecnicas comprovadas do plano, nunca a blocos removidos
pelo jogador.
