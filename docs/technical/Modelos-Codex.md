# Modelos do Codex por atividade

Revisado em 2026-11-02, conforme modelos disponíveis na interface do autor.
Recomendações iniciais, não resultados de benchmark do projeto.

| Atividade | Modelo | Força |
|---|---|---|
| Consulta simples, localizar arquivo, status Git | 6 Luna | baixa |
| Executar comando conhecido e resumir relatório | 6 Luna | média |
| Triagem de logs/relatórios | 6.1 Terra | média |
| Analisar tela e defeito visual | 6.1 Terra | alta |
| Correção isolada com causa conhecida | 6.1 Terra | média; alta se necessário |
| Diagnóstico em várias classes de um subsistema | 6.1 Sol | alta |
| Implementação e revisão entre subsistemas | 6.1 Sol | alta |
| Travamento intermitente ou divergência entre jogo e log | 6.1 Sol | alta |
| Impasse arquitetural, migração difícil ou diagnóstico inconclusivo | 6 Astra | alta |
| Documentação de alterações conhecidas | 6 Luna ou 6.1 Terra | média |
| Commit/push/JAR por procedimento definido | 6 Luna | média |

O conjunto atual de caminhos, terreno, mina e profissões usa **6.1 Sol, alta**.
Terra alta pode iniciar uma investigação delimitada; não é impedimento técnico.
Não recomendar versões 5.6 ou 6 anteriores quando não houver motivo concreto.
Força extra alta/máxima exige ganho observado que compense tempo e consumo.

Fontes consultadas:
- [Seleção oficial de modelos](https://developers.openai.com/api/docs/guides/model-selection).
- [Orientação de esforço](https://developers.openai.com/api/docs/guides/deployment-checklist).

A interface confirma disponibilidade; não confirma preço, limite ou consumo
do plano. Nomes dos perfis `village-*` não provam qual modelo está configurado.
Antes de cada etapa, anunciar atividade, modelo e força; respeitar a confirmação
manual do autor conforme `CLAUDE.md` §0.2.2. Não afirmar força selecionada sem
evidência. Não editar configuração global ou trocar modelo automaticamente.
