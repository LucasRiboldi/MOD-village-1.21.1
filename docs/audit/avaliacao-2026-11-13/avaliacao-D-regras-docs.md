# Avaliação — Bloco D: regras, experiência do jogador, documentação e processo

Snapshot: worktree `zen-joliot-f9313c`, HEAD `7e86e031` (conteúdo igual a `origin/main` `ac2f1977`, 0.4.74). Data: 2026-11-13.
Só leitura. Nada foi editado, nada foi commitado, Gradle não foi rodado. Rodado de verdade nesta sessão: `git` (log, branch,
rev-list, worktree, ls-files), `gh pr list`, `gh release list/view`, `gh run list`, `python scripts/rules_to_readme.py --check`
(só confere) e scripts Python próprios, em memória, para contar linhas do `docs/RULES.md` e conferir links. Uma busca na web
para a regra de IA do Modrinth. Tudo o mais foi lido.

---

### D2 — Lógica e regras de negócio (ISO 25010: adequação funcional — completude, correção, pertinência)

Nota proposta: **4/10** — há 269 regras com teste para quase todas, mas o índice se contradiz, a numeração tem ambiguidades, e
o que o mod promete (vila que trabalha sozinha) ainda não aparece no jogo: a maior parte do expediente segue fora da Regra 51.

Evidências:
- CONFIRMADO — `docs/RULES.md` tem 284 linhas de tabela em "Regras ativas" (268 números distintos de 1 a 269; a 52 não tem
  linha), mais 22 emendas em "Regras emendadas" e 1 em "Provisórias". Pelo estado: 192 ✅, 4 ❌, 2 ⚠️, 2 🟡, 1 🔒, 1 ↪️. **81
  linhas (29%) não têm as colunas Estado e Implementação**: todas as regras de 197 em diante, mais 54–58, entre outras. O
  arquivo se declara "fonte única para esta regra existe, está ativa, e onde está implementada" (`docs/RULES.md:13-14`),
  e para um terço delas não diz nem uma coisa nem outra. (Script Python sobre o arquivo, nesta sessão.)
- CONFIRMADO — Revogações sem estado ou com estado contraditório: 201 a 205 dizem "Revogada" no texto e não têm estado
  (`docs/RULES.md:247-251`); a 187 diz "Revogada" e o estado diz "✅ feita (0.4.26)" (`:233`). A correção existe só na branch
  local `fix/varredura-falhas` (`68a47bc0 docs(rules): revogadas 201 a 205 e a Regra 52 com estado na tabela`), fora da `main`.
- CONFIRMADO — **"Regra 51" tem dois sentidos.** Na tabela, a 51 é "peça que falta aparece na 4ª tentativa" (`docs/RULES.md:91`,
  10-06), com três emendas sobre peças (`:349,353,355`). Em `CLAUDE.md:128`, no README e no `/vc bioma`, "Regra 51" é o teto de
  15% de ócio + bloqueio + encalhe (10-08), que não tem linha própria na tabela (só a menção em `:350`). A métrica central de
  qualidade do projeto não está no índice de regras.
- CONFIRMADO — Dos 15 conflitos da reconciliação de 06-10 (`docs/audit/RULE-RECONCILIATION.md:176-197`), 5 foram marcados
  resolvidos no próprio documento (C-01, C-04, C-06, C-10, C-11); C-02 e C-03 foram resolvidos depois no `RULES.md` (Regras 21,
  25, 32). **Seguem abertos e verificados hoje:** C-12 (o arquivo ainda diz "onde o código discordar, a regra está certa",
  `:15`, e "quando o código divergir, a tabela está errada", `:417`); C-13 (`ColonyHut` ainda citado, `:47`); C-15 (o corpo das
  regras continua remetido a `docs/archive/technical/Project-State.md §18`, `:19` e `:415`, documento arquivado e congelado em
  08-26 — as regras 70 a 269 não têm corpo lá). C-05, C-07, C-08, C-09, C-14: não reconferidos (NÃO CONFIRMADO).
- CONFIRMADO — O cabeçalho do `RULES.md` (`:5-10` e `:25-28`) ainda diz que a candidata 0.4.8 suspende a terraplanagem pelo
  pedreiro; as Regras 215, 246 e 247 devolveram o nivelamento ao pedreiro e ao construtor. A varredura de 11-06 (§9,
  `docs/audit/2026-11-06-conflitos-e-redundancia.md`) listou larguras de rua contraditórias (Regras 70, 123, 186) e a 127; a
  escolha 9A ("uma regra por assunto") foi aceita, mas a 70 continua com estado "↪️ substituída… o corredor do lote segue"
  (`:115`). São 33 menções "emenda a Regra N" no arquivo: a cadeia de emendas exige ler 3 a 4 linhas para saber o que vale.
- CONFIRMADO — Regra contra código: a mensagem `"half of the colony's wood stays in logs"` (`CraftingSteps.java:366`) ainda diz
  metade; a Regra 248 diz um terço. Achado já registrado no jogo da 0.4.68 (A3) e não corrigido.
- CONFIRMADO (pelos relatórios, não reproduzido) — A promessa central falha no jogo. Jogos da 0.4.43 (15 h, 5 colônias): **só
  11% do expediente é trabalho**, e 8 de 9 grupos passam de 15% parado (pastor 77%, fundidor 68%, carpinteiro 56%)
  (`docs/audit/2026-11-10-qualidade-das-profissoes.md:14-31`). Jogo da 0.4.68 (2 h): fundidor 80% parado, fazendeiro 0% de
  trabalho, pastor 2%, construtor e mineiro 20% (`docs/audit/2026-11-13-jogo-0468.md`, tabela "Tempo por ofício"). Recurso
  que surge sem origem física num jogo de 2 h: 467 trigo, 144 lã, 43 toras, 20 tábuas (mesmo arquivo, "Blocos surgidos").
- CONFIRMADO — Ciclo de "candidatas" sem fechamento: o `STATE.md` tem **57 blocos "Candidata"**, 40 dizem "Não jogada", e só um
  diz "confirmado em jogo" (curral, `STATE.md:155`). O jogo da 0.4.68 confirmou as Regras 254, 255, 259 e 260, mas nenhum
  documento vivo muda o estado delas de candidata para confirmada; a 0.4.69 a 0.4.74 (6 versões, 7 regras) não foram jogadas.
  Sistemas nunca vistos funcionando em jogo: obra MAE pedida do `queued` ao `done` e o socorro até o fim (`HANDOFF.md`, "O que
  falta no MAE"), poste de luz (0 `VC_LAMP_POST`), estruturas por bioma fora da planície.
- CONFIRMADO (lido, não rodado) — Cobertura por teste: `docs/testing/regras-e-testes.md` (gerado em `1a5bf0f2`): 161 regras
  citadas por teste, 94 só pela classe, 7 sem teste, 6 fora de vigor. O próprio relatório avisa que citar não prova medir.

Riscos para a 1.0:
- O jogador encontra uma vila que fica parada a maior parte do tempo e materializa recursos; é o oposto da descrição.
- Regras que se sobrepõem (269 números, emendas em cadeia, regra temporária de 02-11 ainda ativa) tornam qualquer correção
  arriscada: uma mudança numa regra quebra a premissa de outra (o padrão das 15 contradições de 06-10 continua).
- Sem estado "confirmada em jogo", não há como dizer quais funções estão prontas para a 1.0.

O que falta para 7/10 e para 9/10:
- 7/10: preencher Estado e Implementação das 81 linhas e trazer o corpo das regras para o `RULES.md` (M); dar número próprio à
  regra dos 15% e desfazer a ambiguidade da 51 (P); fechar C-12, C-13, C-15 e o cabeçalho da regra temporária (P); criar o
  estado "confirmada em jogo" e marcar as já vistas (P); dois playtests seguidos com todos os ofícios abaixo de 40% parado (G).
- 9/10: consolidar as 269 regras por sistema, uma regra viva por assunto, emendas fundidas no texto (G); Regra 51 cumprida
  (≤ 15%) em três biomas (G); "recurso que surge" reduzido a exceções declaradas e medidas por jogo (M).

Bloqueia a 1.0? **Sim.** A função principal (vila autônoma produtiva) não está demonstrada em jogo, e as regras ainda mudam a
cada jogo (7 regras novas em 13-11).

---

### D8 — Experiência do jogador (ISO 25010: usabilidade — reconhecibilidade, aprendizagem, operabilidade, proteção contra erro)

Nota proposta: **3/10** — o jogador vê o mod trabalhar (overlay, painel da bancada, `/vc log`), mas não há guia, o chat é
em português fixo, a configuração prometida não existe e o próprio README manda usar um mundo de teste.

Evidências:
- CONFIRMADO — Não existe guia do jogador. `git ls-files` só acha `docs/audit/GUIA-DO-LEIGO.md` (de 06-10, era 0.3.x, escrito
  "para quem nunca viu o código", sem MAE, grafo, bancadas novas) e `Sentinela/TUTORIAL.md` (ferramenta de teste). Nenhum
  livro, avanço, Patchouli ou wiki em `src/main/resources`. Nenhuma captura de tela no repositório.
- CONFIRMADO — Mensagens ao jogador em português fixo: 49 `Text.literal` contra 25 `Text.translatable` em `src/main/java`.
  `/vc log`, `/vc bioma`, `/vc mae` e o aviso de aldeão preso (`StrandedNotice.java:25`) não passam pelo arquivo de idioma.
  Os três arquivos de idioma (`en_us`, `pt_br`, `pt_pt`, 91 chaves cada) cobrem só blocos, overlays, Mod Menu e o painel.
- SUSPEITO — O painel da bancada é montado no servidor com `Text.translatable` (`BenchPanel.java:53-99`); o README diz que o
  cliente não precisa do mod em servidor dedicado (`README.md:653-660`). Cliente sem o mod veria as chaves cruas. Falta medir
  (e conferir se cliente sem mod sequer entra com blocos do mod registrados — Bloco C).
- CONFIRMADO — Os comandos são de diagnóstico de desenvolvimento: `/vc log`, `/vc bioma`, `/vc bioma nota`, `/vc dados`
  (`BiomeCheck.java:63-69`, `VillageLogCommand.java:37-38`) e `/vc mae …` com `hasPermissionLevel(2)` (`MaeCommand.java:56`).
  O README diz "Todos os comandos são abertos a qualquer jogador" (`README.md:667`) e não lista o `/vc mae`.
- CONFIRMADO — Controle do jogador: só a tela de políticas de profissão no Mod Menu (ligar, limite, raio, ordem; operador nível
  2) e o cancelamento de obra com Tocha das Almas, mecânica que só se descobre lendo o README. A configuração por mundo
  (`/vc config`, 14 ajustes) está "desenhada, falta implementar" (`README.md:886-891`). Nenhum arquivo em `config/`.
- CONFIRMADO — Proteção contra erro: "Use um mundo de teste: o mod corta árvores, minera e coloca blocos no mundo"
  (`README.md:661`). Para uma 1.0 pública é a admissão de que o mod pode estragar o mundo do jogador.
- CONFIRMADO — Pontos bons: overlay de profissão e de obra com cor por estado (Regra 129), painel da bancada por Shift+clique,
  oito bancadas com receita e função para o jogador, tocha e caminho de areia (`README.md:596-631`); `/vc log` explica quem
  está travado e por quê.
- CONFIRMADO — O README abre com instruções de troca entre Claude Code e Codex (`README.md:3-5`) e mais 60 linhas de mudanças
  por versão antes de "O que o mod faz" (`:77`). Um jogador novo não entende o mod pelo README sem atravessar texto de
  desenvolvimento e a tabela de 380 linhas de regras.

Riscos para a 1.0:
- Público do Modrinth e do CurseForge lê inglês; tudo o que o jogador lê no chat e no README é português.
- Jogador não sabe o que esperar, nem como desligar ou ajustar; reclamações virarão "o mod quebrou minha vila".

O que falta para 7/10 e para 9/10:
- 7/10: guia do jogador curto em inglês e português (o que acontece, o que o jogador pode fazer, como parar) (M); passar todas
  as mensagens para chaves de idioma com tradução inglesa (M); implementar o `/vc config` já desenhado, com um interruptor
  por vila (M); README de jogador separado do de desenvolvimento (P); screenshots/GIF (P).
- 9/10: guia dentro do jogo (livro ou avanços) (M); comando para devolver a vila ao Vanilla ou pausar a colônia (M); mensagens
  pensadas para o jogador, não para triagem (M).

Bloqueia a 1.0? **Sim.** Sem guia, sem inglês e sem forma de controlar ou desligar, o mod não cumpre a meta "guia do jogador".

---

### D11 — Documentação e organização (ISO 25010: manutenibilidade — analisabilidade; e a "documentação de uso" da 25010)

Nota proposta: **4/10** — há muita documentação, com cuidado e links íntegros, mas os documentos vivos estouraram os
próprios tetos, se repetem e se contradizem, e um agente novo precisa de muito contexto para começar.

Evidências:
- CONFIRMADO — Tamanho dos documentos vivos (`wc -l`): `STATE.md` **1.095** linhas, sendo que ele mesmo diz "Sobrescreve, não
  acumula. Se passar de 150 linhas, algo está errado" (`STATE.md:1007`); `TODO.md` 671 (o `CLAUDE.md` o chama de "vivo e
  curto"; tem 214 itens abertos e 89 já marcados `[x]`, organizados por jogo e não por prioridade); `README.md` 919;
  `CHANGELOG.md` 2.430; `docs/RULES.md` 418. O `STATE.md` ainda tem a seção "🔴 P0 — … a mina é a raiz" de 14-10
  (`STATE.md:1015`).
- CONFIRMADO — Contradições e documento velho: `HANDOFF.md` (fotografia de 13-11) manda ler "as mais recentes, 243 a 262"
  (agora 269) e traz "Verificado e não verificado na 0.4.53"; o README diz no mesmo topo "toda rede pelo grafo, em toda vila"
  e "o grafo é ativado somente em vilas novas… vilas existentes preservam o índice anterior"; a seção "Estado e nota da
  auditoria" do README ainda é a de 10-10 (6,9/10). `src/main/resources/data/villagecolony/structure/houses/README.md:86-90`
  diz seis camas e seis titulares na BigHouseMOD; o `CLAUDE.md` diz sete e a Regra 259 diz oito ofícios na fundação.
- CONFIRMADO — Contagens fora do lugar e divergentes: o `CLAUDE.md` diz que a contagem de testes fica só no `STATE.md`, mas o
  `HANDOFF.md` (1.085 GameTests), as notas de release (1.112) e o `TODO.md:58` (1.123 GameTests em 11-05) trazem números
  próprios. 1.123 em 11-05 contra 1.112 hoje sugere perfis diferentes sem dizer qual (SUSPEITO de drift).
- CONFIRMADO — Uma fonte por assunto, em parte: a tabela de regras está no `RULES.md` e copiada no README por
  `scripts/rules_to_readme.py`; o `--check` rodado agora diz "README em dia". Bom mecanismo, mas é 40% do README. O
  `AGENTS.md` (129 linhas) carrega um bloco de 60 linhas de uma ferramenta externa ("Fabric Bootstrap", `:68-129`), e o
  `CLAUDE.md` importa `@.fabric/AGENTS.md`, que não existe (`.fabric/` está no `.gitignore`).
- CONFIRMADO — `docs/` tem 563 arquivos versionados, 293 em `docs/archive/`; 54 ADRs com dois `ADR-025` (`ADR-025-client-side-
  pixel-art-overlays.md` e `ADR-025-mineiro-autonomo.md`). Na raiz: `PROJECT_AUDIT.md` (auditoria de 10-28) e
  `PROJECT_CONSTITUTION.md` (339 linhas, emendada por ADR-034) ao lado dos documentos vivos; `docs/TODO-archive.md.txt` fora do
  padrão; `.claude/` (217 arquivos) e `.codex/` (70) versionados no repositório público.
- CONFIRMADO — Pontos bons: nenhum link relativo quebrado nos sete documentos vivos (conferido contra o índice do git,
  sensível a maiúsculas); `HANDOFF.md` curto e direto (114 linhas) com ordem de leitura, ambiente e armadilhas; CHANGELOG no
  formato Keep a Changelog com entrada por versão; ADRs numeradas e citadas nos commits; `docs/PATTERNS.md` como catálogo de
  defeitos.
- PROVÁVEL — Custo de entrada de um agente novo: `HANDOFF` (114) + `AGENTS` (129) + `CLAUDE` (200) + `STATE` (60) + auditoria de
  profissões (111) + topo do `TODO` (100+) ≈ 700 linhas antes de tocar em código, mais o modo temporário (§0.2.2) que pede "ok"
  do autor a cada etapa.

Riscos para a 1.0:
- Ninguém de fora consegue contribuir: o estado real exige saber que STATE, TODO e README mentem em partes diferentes.
- O documento canônico de regras depende de um arquivo histórico congelado.

O que falta para 7/10 e para 9/10:
- 7/10: `STATE.md` de volta a ≤ 150 linhas, o resto para `docs/archive/` (P); `TODO.md` por prioridade, sem itens feitos (M);
  `HANDOFF` e README sem trechos velhos (P); renumerar o ADR-025 duplicado (P); tirar o import inexistente e o bloco Fabric
  Bootstrap do contexto obrigatório (P); mover `PROJECT_AUDIT.md`/`PROJECT_CONSTITUTION.md` para `docs/` (P).
- 9/10: README de jogador em inglês + `CONTRIBUTING`/guia de desenvolvimento separado (M); verificação automática de tetos de
  tamanho e de contagens com fonte (M); índice de `docs/` com um dono por assunto (M).

Bloqueia a 1.0? **Não por si**, mas o guia do jogador e o README em inglês (D8) dependem dela.

---

### D12 — Processo, Git e prontidão para publicar (ISO 25010: manutenibilidade/modificabilidade do processo; portabilidade — instalabilidade)

Nota proposta: **4/10** — o processo de engenharia é disciplinado (commit com o teste que prova, CI verde, release com
SHA-256), mas nada da publicação pública existe: nem página, nem declaração de IA, nem procedência dos assets, e o ritmo de
versões não é o de uma linha de lançamento.

Evidências:
- CONFIRMADO — Histórico: 1.447 commits sem merge desde 2026-08-06; 408 nos últimos 30 dias, **56 com mais de 500 linhas e 10
  com mais de 2.000** (soma do `--numstat`). PRs enormes: #54 +11.556 em 171 arquivos, #61 +7.308/207, #52 +5.897/61, #65
  +4.396/59. Nenhum commit "update/changes/final"; os recentes seguem `P0.x: descrição (teste)` e citam o teste que prova.
  Duas convenções convivem: Conventional Commits (`CLAUDE.md` §2 item 6) e `P0.x:` (`AGENTS.md`).
- CONFIRMADO — Branches: 65 remotas, só 4 à frente da `main` (`codex/local-test-sentinel` 2, `codex/release-handoff` 3,
  `codex/mae-structures` 1, `ccr-e0fb7319-jp46gw` 2). PR #45 aberta desde 02-11 sem atividade. Locais e sem upstream, de
  hoje: `fix/varredura-falhas` (6), `docs/varredura-falhas` (4) e `claude/varredura-pre-jogo-b46f91` (9), esta empilhada sobre
  a primeira — contra a ADR-035 §2 ("não se abre branch a partir de outra branch de trabalho"). 14 worktrees (Claude e
  Codex), uma `prunable`. A `main` local está 12 commits atrás de `origin/main`.
- CONFIRMADO — Versionamento: `mod_version=0.4.74` (`gradle.properties:32`); de `v0.4.0-alpha` (31-10) a `v0.4.74-alpha`
  (13-11) foram 74 versões em 13 dias; 10 releases no GitHub, todas pre-release, 6 delas desde 06-11. O número de patch é
  contador de JAR, não semântica. O release é compilado pelo workflow "só com `remapJar`: os testes não rodaram nesta
  compilação" (`gh release view v0.4.74-alpha`) e tem SHA-256 diferente do JAR jogado (`HANDOFF.md`, "Publicar").
- CONFIRMADO — CI: últimos runs de `CI` e `Release` na `main` com `success` (13-11). JAR fora do git (nenhum `.jar` versionado
  além do wrapper); `.gitignore` cobre `build/`, `run/`, `logs/`, `*.jar`.
- CONFIRMADO — Metadados (`src/main/resources/fabric.mod.json`): id, nome, descrição em inglês, autor, contato (homepage =
  repositório, sources, issues), `license: MIT`, ícone, dependências com faixa. Faltam: link de página de mod, `suggests` além
  do Mod Menu, e a descrição promete "without player intervention", o que os jogos ainda não mostram. Descrição do
  repositório no GitHub: "MOD for Minecraft 1.21.1".
- CONFIRMADO / SUSPEITO — Licença e assets: código MIT (`LICENSE`); `NOTICE` declara só `houses/small_house.nbt` como cópia da
  Mojang. Mas `houses/README.md:86` diz que `big_house_mod.nbt` é "cópia editada da big house Vanilla" e, 20 linhas depois
  (`:109`), "nenhum byte de arquivo da Mojang mora no repositório" — contradição CONFIRMADA, e a BigHouseMOD não está no
  `NOTICE`. Procedência das outras 22 estruturas `colony/*` não documentada (NÃO CONFIRMADO). As 64 texturas de bancada saem
  de "mockups 1254px" recortados por script (`0ac5a474`), e o ícone tem 1254×1254 e veio de "artwork the author supplied" com
  o texto "Village++" (`815c7403`); origem das imagens não registrada (SUSPEITO de imagem gerada por IA, pelo tamanho típico).
  Nenhuma licença declarada para os assets.
- CONFIRMADO (web, `modrinth.com/news/article/ai-policy-and-disclosures`) — Desde 13-08-2026 o Modrinth exige o rótulo
  "Contains AI-generated content" para código ou assets substancialmente gerados por IA, proíbe imagens primariamente geradas
  por IA na página (ícone, galeria, descrição) e pode tirar da busca projetos "totalmente gerados por IA"; a carência acabou
  em 27-09. Aqui: **939 de 1.447 commits** trazem `Co-Authored-By: Claude`, mais os de Codex sem rodapé. Não há declaração de
  uso de IA no README, nem página no Modrinth ou no CurseForge, nem automação de publicação (`grep` sem `minotaur`,
  `cursegradle`, `mc-publish`). A política do CurseForge não foi conferida.

Riscos para a 1.0:
- Rejeição ou remoção da busca no Modrinth se o projeto for visto como "totalmente gerado por IA", ou se ícone e texturas forem
  imagens geradas por IA.
- Reclamação de licença por estrutura da Mojang não declarada.
- Versão "1.0" sem semântica: o jogador não distingue correção de mudança de regra.

O que falta para 7/10 e para 9/10:
- 7/10: declaração de uso de IA no README e no Modrinth, e decisão sobre ícone/texturas (refazer à mão ou declarar) (P/M);
  `NOTICE` completo com a procedência de toda estrutura e textura (P); páginas no Modrinth e no CurseForge com descrição em
  inglês, ícone, galeria, changelog (M); release que roda os testes e publica o mesmo JAR jogado (M); fechar ou integrar a PR
  #45 e as branches locais (P); versão semântica a partir de um corte de funcionalidades (P).
- 9/10: publicação automatizada nas duas plataformas a partir da tag (M); PRs pequenas, uma por regra (M); canal beta e
  canal estável separados (P); política de suporte de versão do Minecraft (P).

Bloqueia a 1.0? **Sim.** Sem declaração de IA, procedência de assets e páginas nas plataformas, não há publicação; o risco de
rejeição no Modrinth é real.

---

## Top 5 problemas do bloco (por gravidade)

1. **A vila não trabalha sozinha no jogo** (D2): 11% do expediente é trabalho nos jogos da 0.4.43; na 0.4.68, fundidor 80%
   parado e fazendeiro 0% de trabalho; centenas de itens surgem sem origem. É a promessa da descrição do mod.
2. **Publicação no Modrinth em risco por IA e procedência** (D12): 939 commits com co-autoria de IA e nenhuma declaração;
   ícone e texturas de origem não registrada; BigHouseMOD derivada da Mojang fora do `NOTICE`.
3. **Sem guia, sem inglês, sem controle** (D8): chat em português fixo, nenhum guia do jogador, `/vc config` não implementado,
   README que pede "mundo de teste".
4. **Regras sem fechamento** (D2/D11): 81 regras sem estado, "Regra 51" com dois sentidos, 57 candidatas e só uma confirmada
   por escrito, contradições de 06-10 (C-12, C-13, C-15) ainda abertas.
5. **Documentos vivos que mentem por tamanho** (D11): `STATE.md` com 1.095 linhas contra teto de 150, `TODO.md` de 671 linhas
   por jogo e não por prioridade, `HANDOFF` e README com trechos de versões passadas.

## O que eu não consegui verificar

- Nada foi jogado nem compilado; os números de tempo por ofício vêm dos relatórios de 11-10 e 13-11, não de logs relidos.
- Os conflitos C-05, C-07, C-08, C-09 e C-14 da reconciliação de 06-10 não foram reconferidos no código.
- Se um cliente sem o mod entra num servidor dedicado com os blocos do mod, e o que ele vê no painel (D8, deixado ao Bloco C).
- Origem real do ícone, das texturas das bancadas e das 22 estruturas `colony/*.nbt` (se foram geradas por IA ou derivadas da
  Mojang); não comparei bytes com o JAR do Minecraft.
- A política de IA e de conteúdo do CurseForge; a do Modrinth foi lida por busca na web, não pela página de regras inteira.
- Se `scripts/rule_coverage.py` daria hoje os mesmos números (não rodei: ele escreve `docs/testing/regras-e-testes.md`).
- O conteúdo das branches `codex/*` à frente da `main` e da PR #45, além do título.

Fontes externas: [Modrinth — AI policy and disclosures](https://modrinth.com/news/article/ai-policy-and-disclosures/),
[Modrinth — Content Rules](https://modrinth.com/legal/rules).
