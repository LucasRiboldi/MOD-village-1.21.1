# Avaliação de qualidade do Village Colony — 2026-11-13

**Snapshot:** `main` em `ac2f1977` (0.4.74, `v0.4.74-alpha`). **Meta avaliada:** 1.0 pública no Modrinth e no CurseForge —
estável, sem perda de save, servidor dedicado e multijogador, guia do jogador, declaração de uso de IA.
**Método:** régua ISO/IEC 25010:2023 (modelo de qualidade de produto), revisão de arquitetura no estilo ATAM leve, portões de
prontidão de lançamento e as marcas de evidência do método VILA-RESET (`PROJECT-RECOVERY-AUDIT.md`). Quatro levantamentos
em paralelo, só diagnóstico, com as ferramentas rodando de verdade; os relatórios completos estão em
[`avaliacao-2026-11-13/`](avaliacao-2026-11-13/) (régua e blocos A a D).

Marcas: **CONFIRMADO** (visto no código, no git ou rodando), **PROVÁVEL** (mecanismo visto, efeito não reproduzido),
**SUSPEITO** (indício), **NÃO CONFIRMADO**. Faixas: 0–2 não existe · 3–4 frágil · 5–6 funciona com ressalvas · 7–8 sólido ·
9–10 exemplar.

## 1. Veredito

**Nota geral: 5,0 / 10 — alpha funcional com ressalvas.** A engenharia é disciplinada (cada commit cita o teste que prova,
build reprodutível, CI que reprova de verdade, 1.804 unitários e 1.112 GameTests verdes no Sentinela), o núcleo puro e os
mixins são exemplares. O que separa o mod de uma 1.0 pública é o que o jogador vê: a vila ainda passa a maior parte do
expediente parada nos jogos, não há guia nem inglês, o save não tem prova de recarga nem backup, o ciclo da colônia trava um
tique inteiro, nada foi testado com outros mods ou multijogador, e a publicação esbarra na declaração de IA e na procedência
dos assets.

## 2. Notas por dimensão

| # | Dimensão | ISO 25010 | Nota | Peso | Bloqueia a 1.0 | Em uma frase |
|---|---|---|---|---|---|---|
| D1 | Arquitetura e modularidade | manutenibilidade | **6** | 6% | não | `core` isolado e mixins finos, vigiados por ArchUnit; a camada fabric (77% do código) é um ciclo só, preso a serviços estáticos |
| D2 | Lógica e regras de negócio | adequação funcional | **4** | 15% | **sim** | 269 regras com teste para quase todas, mas a vila não trabalha sozinha no jogo e o índice de regras se contradiz |
| D3 | Confiabilidade e save | confiabilidade | **6** | 10% | **sim** | versão, migração e leitura defensiva; sem backup, sem gravação atômica, sem teste de recarga nem de save antigo |
| D4 | Desempenho | eficiência de desempenho | **5** | 10% | **sim** | média boa com 1 vila (mod ~2,4% da thread); o ciclo roda num tique só: até 93 ms com 1 vila e 458 ms com 4+ |
| D5 | Testes | confiabilidade/testabilidade | **6** | 10% | com D3 | bateria grande e registrada por inteiro, PIT 85,2% no core; instáveis crônicos e nenhum teste de recarga de save |
| D6 | Qualidade do código | manutenibilidade | **6** | 5% | não | 0 aviso do Error Prone e pouca duplicação; NullAway só no core, 148 métodos complexos, 1.588 linhas de comentário datado |
| D7 | Compatibilidade | compatibilidade | **5** | 10% | **sim** | servidor dedicado sobe; multijogador e outros mods nunca testados; corrida provável na geração de mundo; README errado sobre o cliente |
| D8 | Experiência do jogador | usabilidade | **3** | 10% | **sim** | o jogador vê o trabalho, mas não há guia, o chat é português fixo, `/vc config` não existe |
| D9 | Segurança | segurança | **6** | 5% | não | escrita no mundo exige op; dois comandos gravam em disco sem op, diários sem teto (31 MB) |
| D10 | Ferramentas, build e CI | manutenibilidade | **7** | 5% | não | versões e ações fixadas, 305 dependências verificadas, CI reprova; cobertura publicada falsa (7,3% vs 79%) e release sem amarra ao CI |
| D11 | Documentação e organização | manutenibilidade | **4** | 6% | não | muita documentação com links íntegros; `STATE.md` 1.095 linhas (teto 150), `TODO.md` 671, trechos velhos |
| D12 | Processo e prontidão para publicar | portabilidade (instalabilidade) e processo | **4** | 8% | **sim** | processo disciplinado; sem declaração de IA (939 commits co-autorados), sem procedência de assets, sem páginas nas plataformas |

Média simples 5,2; **média ponderada pela meta 1.0: 5,0**. Os pesos favorecem o que o jogador sente (função, save, desempenho,
compatibilidade, experiência).

## 3. Dez problemas que mais pesam

1. **A vila não trabalha sozinha** (D2, CONFIRMADO em logs): 11% do expediente é trabalho nos jogos da 0.4.43; na 0.4.68 o
   fundidor ficou 80% parado e o fazendeiro 0%. Há 57 versões "candidatas" no `STATE.md` e uma só confirmação em jogo escrita.
2. **Ciclo da colônia num tique só** (D4, CONFIRMADO em `VC_COST`): picos de 57–93 ms com 1 vila, 458 ms com 4+; o overlay
   manda todos os trabalhadores a todo jogador a cada segundo.
3. **Perda de save com arquivo truncado** (D3, PROVÁVEL): escrita não atômica, sem backup; se a leitura falha, o autosave
   seguinte grava o estado vazio. Nenhum teste reabre o servidor nem lê o `.dat` de uma versão publicada.
4. **Estado sem trava acessado da geração de mundo** (D7, PROVÁVEL): `HostileSpawnMixin` → `ColonyTorches.suppresses` em
   `CHUNK_GENERATION` usa `HashMap` estático nas threads `Worker-Main`.
5. **Publicação bloqueada** (D12, CONFIRMADO): sem declaração de IA (exigida pelo Modrinth desde 08-2026), procedência do
   ícone, das texturas e da `big_house_mod.nbt` (cópia editada da Mojang) fora do `NOTICE`, nenhuma página nas plataformas, e o
   JAR da release é recompilado sem rodar os testes.
6. **Sem guia, sem inglês, sem controle** (D8, CONFIRMADO): 49 mensagens em `Text.literal` contra 25 traduzíveis, nenhum guia,
   `/vc config` desenhado e não feito, nenhum jeito de pausar uma colônia.
7. **Instáveis crônicos** (D5, CONFIRMADO): 41 de 83 baterias locais com alguma falha; 2 de 3 baterias no mesmo commit caíram
   por testes diferentes; 3 de ~10 pushes na `main` vermelhos por instável.
8. **Compatibilidade nunca verificada** (D7, CONFIRMADO como lacuna): 7 mixins com `require 1` em pathfinding, mundo e cérebro
   do aldeão; nenhum teste com Lithium, C2ME, FerriteCore ou mod de aldeão; o README diz que o cliente não precisa do mod, mas
   o mod registra blocos e itens.
9. **Camada fabric acoplada** (D1/D6, CONFIRMADO por `jdeps`): `VillageColonyMod.COLONIES/WORKERS/...` em 259 arquivos;
   `fabric/work` com 250 classes, 146 num ciclo; NullAway fora de ~89 mil linhas, inclusive a leitura de save.
10. **Documentos vivos estourados e regras ambíguas** (D11/D2, CONFIRMADO): `STATE.md` 1.095 linhas, `TODO.md` por jogo e não
    por prioridade, 81 regras sem estado, "Regra 51" com dois sentidos, ADR-025 duplicada, `CLAUDE.md` importa um arquivo que
    não existe.

## 4. Critérios de saída (portões)

| Portão | Versão | O que precisa ser verdade |
|---|---|---|
| **Beta** | 0.5.0 | Dois playtests seguidos, em dois biomas, com todo ofício abaixo de 40% parado e nenhum travado; regras sem contradição (estado em todas, "51" desfeita); documentos vivos nos tetos; instáveis corrigidos ou em quarentena declarada |
| **Versão candidata** | 0.9.0 | Save com backup e teste de recarga e de save antigo; ciclo repartido com p95 < 20 ms por tique em 6 vilas; servidor dedicado com 2 jogadores por 30 min sem erro; matriz mínima de mods; comandos com permissão e diários com teto |
| **1.0 pública** | 1.0.0 | Guia do jogador e mensagens em inglês e português; `/vc config` e pausa; declaração de IA e procedência de assets; páginas no Modrinth e no CurseForge; release publica o JAR do mesmo SHA com CI verde; Regra 51 (≤ 15% ocioso) cumprida em três biomas |

## 5. Cronograma até a 1.0

Estimativa para o ritmo atual (autor + agentes, um ciclo de correção por jogo). Tamanhos: P até 1 dia, M 2 a 4 dias,
G uma semana ou mais. As fases 2 e 3 podem correr em paralelo com a 1, porque tocam outros sistemas.

| Fase | Semanas | Entregas | Dimensões |
|---|---|---|---|
| **F0 — Sinal limpo** | 1 | `STATE.md` ≤ 150 linhas e `TODO.md` por prioridade (P/M); índice de ADRs, ADR-025 renumerada, import inexistente fora do `CLAUDE.md` (P); `rule_coverage.py` corrigido e com teste (P); relatório JaCoCo da bateria separado da auditoria e "Check whitespace" com base (P); quarentena declarada dos 5 instáveis (M); README corrigido sobre o cliente (P); fechar PR #45 e branches locais (P) | D5, D10, D11, D12 |
| **F1 — A vila trabalha** | 3 a 5 | Estado nas 81 regras, "Regra 51" desfeita, C-12/C-13/C-15 fechados, estado "confirmada em jogo" (M); ciclos de playtest até todo ofício < 40% parado em dois biomas (G); "recurso que surge" reduzido a exceções medidas (M) | D2 |
| → **Portão Beta (0.5.0)** | | | |
| **F2 — Save e desempenho** | 3 a 4 | Backup ou leitura de recuperação, fixtures `.dat` de 2 ou 3 versões publicadas, versão de esquema nos 5 arquivos, GameTest de parar e reabrir (M); NullAway em `data/save` e `fabric/adapter` (P/M); ciclo repartido entre tiques com orçamento e retomada (G); overlay recortado por vila do jogador (P); gravação dos ledgers fora da thread do servidor (P); perfil "carga" no Sentinela (M) | D3, D4, D5, D6 |
| **F3 — Servidor e compatibilidade** | 2 | `ColonyTorches` seguro para threads ou fora da geração (P); sessão de servidor dedicado com 2 clientes por 30 min (M); matriz Lithium, FerriteCore, ModernFix e um mod de aldeão (M); permissão em `/vc bioma nota` e `/vc dados`, teto e rotação dos diários, interruptor dos logs `VC_*` (P) | D7, D9, D4 |
| → **Portão versão candidata (0.9.0)** | | | |
| **F4 — Experiência do jogador** | 2 a 3 | Todas as mensagens em chaves de idioma com inglês (M); `/vc config` com interruptor por vila e pausa da colônia (M); guia curto do jogador em inglês e português, README de jogador separado do de desenvolvimento, capturas e GIF (M/P) | D8, D11 |
| **F5 — Publicação** | 1 a 2 | Declaração de uso de IA no README e nas páginas; decisão sobre ícone e texturas (refazer ou declarar) e `NOTICE` completo (P/M); páginas no Modrinth e no CurseForge com descrição em inglês, ícone, galeria e changelog (M); release que roda os testes, amarra ao CI do mesmo SHA e publica o JAR jogado (M); versão semântica e canais beta e estável (P) | D12, D10 |
| → **Portão 1.0 (1.0.0)** | | Regra 51 cumprida em três biomas e dois playtests limpos na candidata | |

**Total estimado: 12 a 17 semanas.** O caminho crítico é a F1: só o jogo prova que a vila trabalha, e cada ciclo depende de
um playtest do autor.

## 6. Depois da 1.0 (dívida declarada, não bloqueia)

Contexto por servidor no lugar dos serviços estáticos de `VillageColonyMod` (G); `fabric/work` em grupos por profissão com
dependência num só sentido (G); NullAway em toda a camada fabric (G); nenhum método acima de complexidade 25 e teto por
complexidade no lugar do de 500 linhas (M); comentários datados abaixo de 300 (contínuo); PIT estendido a pacotes puros de
`fabric/work` e 10 a 20 propriedades jqwik nos sistemas com mais sobreviventes (M); migração da fronteira da mina entre
formas (G); publicação automatizada a partir da tag (M); `doMobGriefing` ou gamerule própria para alteração de terreno (M).

## 7. O que esta avaliação não verificou

Multijogador real e cliente sem o mod entrando no servidor; coexistência com outros mods; a corrida em `ColonyTorches`
acontecendo de fato; save truncado na prática; heap em sessão longa; as versões 0.4.69 a 0.4.74 em jogo; SpotBugs (o plugin
foi recusado pela verificação de dependências); PIT local (usado o relatório do CI do mesmo commit); a origem real do ícone,
das texturas e das estruturas `colony/*.nbt`; a política de IA do CurseForge.

Fontes do método: [ISO/IEC 25010:2023](https://www.iso.org/standard/78176.html),
[ATAM e atributos de qualidade](https://www.infoq.com/articles/atam-quality-attributes),
[Regras do Modrinth](https://modrinth.com/legal/rules), [Política de IA do Modrinth](https://modrinth.com/news/article/ai-policy-and-disclosures/).
