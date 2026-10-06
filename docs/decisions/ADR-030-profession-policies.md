# ADR-030 — Políticas de profissão por mundo

**Status:** aceita
**Data:** 2026-10-02

## Contexto

As oito profissões de `ProfessionType` são catálogos fixos. Antes desta decisão,
o número de trabalhadores e a ordem de contratação vinham apenas de
`ProfessionAssigner`; não havia configuração persistida. O Mod Menu é cliente,
mas essas decisões pertencem ao mundo/servidor.

## Decisão

Uma política central por profissão será guardada em `PersistentState` do
Overworld. Ela contém: habilitada, máximo de trabalhadores e, quando aplicável,
raio de busca. A prioridade é uma única ordenação completa das oito profissões,
e não oito números independentes. O cliente somente recebe um snapshot e envia
pedidos validados ao servidor.

Os valores padrão preservam a regra anterior: todas habilitadas, sem teto
adicional, ordem `[MINER, LUMBERJACK, MASON, SMELTER, CARPENTER, FARMER,
SHEPHERD, BUILDER]` e raio automático. A fase fundacional usa a projeção dessa
ordem sobre seus seis papéis históricos; a fase de crescimento usa a projeção
sobre os sete produtores. Assim Carpinteiro/Fazendeiro continuam fora do piso
fundacional e Construtor continua fora do crescimento automático.

> **Superado em 2026-10-06 (ADR-034, D-05):** vale o código e a Regra 35 — sete
> titulares na fundação, com o carpinteiro e sem o agricultor, e o construtor
> também no crescimento. O resto desta ADR continua valendo.

## Escopo de raio

- Lenhador, Fazendeiro e Pastor podem receber raio configurável.
- Mineiro fica com a geometria de mina atual: os limites de `MineSite` são
  guardas de segurança e ganchos de teste, não preferência de coleta.
- Pedreiro, Fundidor, Carpinteiro e Construtor não pesquisam um alvo de mundo
  por raio configurável.

## Consequências

- Uma profissão desabilitada não recebe novas atribuições. Trabalhador já
  atribuído mantém profissão e trabalho até uma futura política de realocação,
  que não entra nesta versão.
- O teto limita novas atribuições; não remove nem desaloja trabalhadores já
  existentes.
- Reordenar muda apenas o desempate de vagas futuras; não reassina aldeões já
  empregados.
- Não há controle de velocidade, frequência ou multiplicador de produção.
