# Plano aprovado — terreno, mina e profissões

Data: 2026-11-02. Categoria de auditoria: **1.9**.
Estado: aprovado pelo autor; implementação ainda pendente.

## Evidências e limites

Playtest de 02-11, `latest.log` até 13:48:23 e perfil
[Spark](https://spark.lucko.me/Z6diBIy88P).
Os três analisadores de evidência foram executados antes deste registro.

- O log registra oito avisos de atraso do servidor, chegando a 22,2 segundos.
- Spark: TPS 20; MSPT mediano 28,8, p95 40,6 e máximo 190 ms.
  RAM física 84%, swap 90,8% e disco 93,9% ocupados no instante registrado.
  Esses dados não isolam o método responsável pelo atraso.
- `cost_ledger`: nove amostras, média total 10,72 ms e máximo 37,45 ms.
  Não representa todo o processamento de chunks e geração.
- `time_ledger`: sete janelas; pastor 89% ocioso, construtor 63% ocioso,
  lenhador 37% ocioso/bloqueado e fundidor 15% bloqueado.
- Diário de ações cobre somente 4,6 minutos: um mineiro acumula 209 segundos
  sem progresso; o lenhador abandona tarefa por falta de progresso.
- `VC_BIOME`: `project=none`, `lots_ok=0`, `buildings=1`.
- Há repetição de rotas, `NO_TARGET` e espera de frente da mina.
  Frequência não prova custo nem causa; localizar chamadas antes de corrigir.

## Decisões do autor

Todas as opções A foram escolhidas. Não criar outro grafo nem persistir
reservas transitórias. Reutilizar RoadNetwork/RoadIndex e decisões existentes.

| Ordem | Escopo aprovado | Critério de aceitação no playtest |
|---|---|---|
| 1 | Processar planejamento/recalculo em lotes pequenos por tick e adiar novos cenários sob atraso | A fila progride sem perda de trabalho e sem repetir varredura completa por trabalhador |
| 2 | Integrar entrada da mina como destino no grafo; ramal do bioma com smoothing local; casa pequena do bioma junto ao pórtico e tocha laranja | Aldeão alcança mina por solo contínuo desde a vila; casa tem vínculo com o mineiro; acesso é aprendido pelos trabalhadores |
| 3 | Reserva atômica por bloco com dono e liberação na conclusão, desistência ou falta de progresso | Dois trabalhadores nunca alteram simultaneamente o mesmo bloco; reservas inválidas não bloqueiam a vila |
| 4 | Frente exclusiva por mineiro, conservada até conclusão ou impedimento comprovado; tentar outro acesso/frente | Mineiros progridem em percursos próprios, sem minerar escadarias nem disputar o mesmo alvo |
| 5 | Preferir terra; salto normal por padrão; salto maior apenas para recuperação de buraco, cerca ou barreira | Deslocamento comum usa solo e salto normal; recuperação continua possível |
| 6 | Filas e alternativas restritas ao ofício | Nenhum aldeão assume atividade exclusiva de outra profissão |

Na opção 6, fazendeiro atende outras fazendas quando a própria não tiver
trabalho. Lenhador prioriza árvores centrais e planta mudas nas bordas quando
não houver árvores maduras válidas. Pastor e fundidor recebem diagnóstico
de bloqueios e alternativas do próprio ofício, sem criar recurso fora das
exceções já aprovadas. Construtor, pedreiro e carpinteiro mantêm seus papéis.

**Regra temporária:** nenhuma profissão executa atividade de outra, nem como
ajuda de obra, fallback ou reatribuição. Coleta comum e recuperação de
locomoção continuam sujeitas às regras compartilhadas existentes.

Geometria, casa e terreno novos aplicam-se a novas vilas, conforme decisões
anteriores. Não reformar automaticamente terreno nem estruturas de saves
existentes. Correções de tarefas e navegação devem preservar saves.

## Execução e entrega

1. Publicar este planejamento antes de iniciar alterações de gameplay.
2. Conferir branches contra `origin/main`, localizar os responsáveis e ADRs.
3. Diagnosticar chamadas e implementar uma tarefa por commit, reutilizando
   mecanismos existentes. Decisão arquitetural não coberta exige deliberação.
4. Modelo recomendado: GPT-6.1 Sol, alta. Escalar para Astra, alta, somente
   quando houver impasse concreto. Tabela: [Modelos-Codex.md](Modelos-Codex.md).
5. O pedido atual autoriza registro, commit, push e início do desenvolvimento,
   nessa ordem. Manter os avisos por etapa; pedidos futuros seguem a confirmação
   manual de `CLAUDE.md` §0.2.2.
6. Testes continuam suspensos. Não criar nem executar unitários, GameTests,
   Sentinela completo ou modificar CI. Revisar diff e verificações estáticas.
7. Cada correção de gameplay incrementa o patch da versão; executar apenas
   `remapJar`, copiar para `downloads/` e TLauncher e comparar SHA-256.
8. Atualizar STATE, TODO, CHANGELOG e Development-Log, registrando limites.
   Conclusão depende do próximo playtest; compilação não prova comportamento.

## Pendências

- [ ] Localizar e corrigir custo por tick com diagnóstico mensurável.
- [ ] Integrar acesso, smoothing e casa da mina ao sistema existente.
- [ ] Verificar exclusão mútua das ações em blocos.
- [ ] Corrigir seleção e recuperação de frente dos mineiros.
- [ ] Revisar água e saltos sem cancelar IA Vanilla.
- [ ] Restringir tarefas aos ofícios e revisar fazendeiro/lenhador/pastor/fundidor.
- [ ] Registrar JAR candidato e confirmar efeito no próximo playtest.
