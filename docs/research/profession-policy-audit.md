# Auditoria — políticas de profissão (Minecraft 1.21.1)

## Fatos

- [FATO] `ProfessionType` declara MINER, LUMBERJACK, MASON, SMELTER,
  CARPENTER, FARMER, SHEPHERD e BUILDER.
- [FATO] `ProfessionAssigner` calcula vagas pela população adulta; não há
  máximo editável nem configuração persistida.
- [FATO] `PRODUCER_ORDER` resolve empates para as sete profissões produtoras;
  `FOUNDATION_ORDER` garante o piso de fundação.
- [FATO] Lenhador, Fazendeiro e Pastor procuram alvos em torno do centro da
  colônia. Seus raios atuais são 64, 32 e 32 respectivamente.
- [FATO] Os mutadores de raio de Fazendeiro/Pastor e os limites do Mineiro são
  usados por GameTests; não são opções de usuário persistidas.

## Decisão

Aplicar política no servidor antes de atribuir uma profissão. O máximo nunca
remove trabalhador existente. Uma única ordem de oito papéis é projetada para
as fases fundacional e de crescimento; sua ordem padrão reproduz as duas listas
históricas. Valores padrão mantêm o algoritmo anterior.
