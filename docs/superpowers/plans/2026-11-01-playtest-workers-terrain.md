# Correções do playtest: terreno, mina e trabalhadores

## Decisões aprovadas

- Limpar vegetação e blocos soltos no lote e na margem externa de um bloco; encaminhar drops ao estoque público.
- Após o arco da entrada da mina, enfileirar casa pequena do bioma no mesmo nível, em lote lateral válido entre 8 e 16 blocos.
- Aplicar prioridade: ofício próprio, estoque próprio até 16, ajuda segura após 1.200 ticks sem tarefa.
- Fundidor permanece móvel apenas em área construída.
- Pastor mantém busca ampla.
- Rotas nunca usam folhas ou troncos como piso; aldeões removem folhas de corpo e cabeça na rota.
- Mineiro avança frente somente após quebra confirmada; falha libera alvo e busca acesso alternativo.

## Limites

- Não alterar estado de vila existente além do comportamento de trabalho.
- Não criar nem executar testes até nova liberação do autor, conforme `CLAUDE.md` §0.2.2.
- Não tocar em mixins, grafo de ruas ou migração persistida.
- Preservar água funcional, estruturas, estradas, obras abertas e blocos protegidos do jogador.

## Etapas

1. Estender abertura de lote com margem de um bloco e coleta segura de flora/decoração.
2. Reusar catálogo e planejamento de casas para criar destino residencial do mineiro junto ao arco.
3. Corrigir confirmação de progresso e recuperação de alvo do mineiro.
4. Restringir piso de rota e remoção gradual de folhas ao caminho real.
5. Aplicar prioridades de profissão e área móvel do fundidor sem mudar o contrato do pastor.
6. Revisar diff, compilar sem testes, atualizar estado e diário da sessão; gerar JAR local apenas após commit solicitado.
