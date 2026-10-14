# Fluidez, mina que não trava e revisão das regras (2026-10-13)

> Pedido do autor: *"a mina tem que ser repensada para não travar … pesquise alternativas de
> melhorar o projeto e deixar o jogo fluido e sem travamentos ou bloqueios; as profissões devem
> trabalhar em ritmo contínuo e a vila expandindo aos poucos, rua aumentando e construções nas
> bordas aumentando a área da vila; rever as regras, os conflitos e propor melhorias"*.
> Base: [`2026-10-13-reanalise-estrutural-profissoes.md`](2026-10-13-reanalise-estrutural-profissoes.md).
> Já aplicado na 0.3.46: rodízio casa ↔ outro tipo (1A), reserva do artesanato (2), ruas como rede
> de caminhos conectados (4).

## 1. A mina: por que todo desenho travou

[FATO] Em todas as versões da mina, o motivo dominante de parada é o mesmo — **"out of reach"**: o
mineiro não acha um lugar de pé de onde alcançar a pedra. 30.362 recusas numa sessão da 0.3.29
(ramais); 387 na 0.3.37 (rede 1×2); "unable to climb", "no work position" em outras.
[INFERÊNCIA] O que trava não é a geometria em si, é escolher o alvo **antes** de saber se o aldeão
consegue chegar até ele. A navegação do aldeão Vanilla (`MobNavigation`) não sobe escada de mão
direito, não atravessa buraco, e no túnel estreito desiste com facilidade. Cada redesenho mudou o
formato e manteve o defeito.

### Como outros fazem
- **MineColonies** ([wiki](https://minecolonies.com/wiki/workers/miner/)): poço até uma
  profundidade fixa pelo nível da cabana, depois uma rede de galerias a partir dele; segue o veio
  inteiro quando acha minério; tapa água, lava e caverna com pedregulho; e tem **"Recall Citizen"**,
  que teleporta o mineiro perdido de volta à cabana — o próprio projeto assume que ele se perde.
- **Mineração em ramais** (prática comum,
  [Dummies](https://www.dummies.com/article/home-auto-hobbies/games/online-games/minecraft/mining-efficiently-in-minecraft-168906/)):
  corredor principal e ramais retos 1×2 com dois blocos de pedra entre eles (um a cada três) — toda
  pedra fica à vista de um túnel, sem cavar à toa, e o chão é **plano**.

### Proposta M — "cavar pela frente, num nível plano, com socorro"
1. **Um nível de trabalho plano por vez.** A escadaria em hélice (que já existe) desce até o nível;
   nele, um corredor principal 2×2 e ramais retos 1×2 a cada 3 blocos. Sem degrau dentro do nível:
   a navegação do aldeão anda em plano sem falhar.
2. **O alvo é sempre a frente do próprio túnel.** O próximo bloco é o da face do ramal que ele está
   cavando, alcançado andando pelo túnel que ele mesmo abriu. Minério só entra se estiver ao alcance
   do braço **a partir de uma célula já aberta do túnel** (o `NetworkVeins` já mede assim). Nada de
   alvo que exija ficar de pé num lugar que ainda não existe — some o "out of reach" por construção.
3. **Socorro, como o Recall da MineColonies.** Sem bloco cavado em 30 s de expediente, o mineiro é
   levado à última célula andável do ramal dele (ou ao salão), e a face que travou é marcada pronta;
   três socorros no mesmo ramal fecham o ramal. Ele nunca fica parado mais que 30 s.
4. **Nível esgotado, desce.** Ramais no comprimento máximo e corredor principal no fim: a escadaria
   desce ao próximo nível e recomeça. No fundo, volta pela escadaria (já existe).
5. **Congelar depois.** Feito isso, nenhuma mudança de geometria sem um playtest de 30 min medindo
   minério/min e recusas — a reanálise mostrou que as trocas seguidas foram parte do problema.

Alternativa mais barata e menos imersiva — **mina abstrata**: o mineiro fica na boca e a mina avança
sozinha num ritmo fixo, tirando blocos da sequência planejada. Nunca trava, mas ninguém vê o mineiro
cavando. Não recomendo; fica registrada.

## 2. Fluidez: o que mantém todo mundo trabalhando

| # | Mudança | Por quê |
|---|---|---|
| F1 | **Reserva fixa em vez de só demanda da obra** (feito no item 2) | O artesão tinha trabalho só enquanto havia obra. |
| F2 | **A obra nunca fica sem candidato** (feito no 1A e na Regra 60) | Uma porta fechada parava a vila inteira. |
| F3 | **Busca de lote pela rede de ruas** (feito no item 4) e **lote recuado** (0.3.45) | A volta em anéis levava 20 min e o terreno atrás das casas não contava. |
| F4 | **Socorro geral contra parada**: a mesma regra de 30 s da mina para todo ofício que anda (lenhador, fazendeiro, pastor, construtor) — sem progresso, volta ao baú/posto e o alvo sai de castigo | Hoje cada ofício tem a sua guarda, com prazos diferentes (`WorkStall`, `TreeChoice.STALL_LIMIT`, …). |
| F5 | **Expansão aos poucos pela rua**: com a vila cheia, a rua cresce um trecho de 16 pelo melhor lado (feito na 0.3.44) e o lote recuado nasce dos dois lados dela | A caixa cresce só com rua e obra, como o autor quer. |
| F6 | **Uma regra por assunto** — a tabela de regras tem 98 linhas, várias contraditórias (ver §3) | Regra contraditória vira código contraditório. |

## 3. Conflitos nas regras (docs/RULES.md)

| Conflito | Hoje | Proposta |
|---|---|---|
| **Ordem das obras** — Regras 49, 55, 60 e o 1A | A 49 diz "casa com falta de cama, depois oficina de cada ofício, depois as demais com casa no rodízio"; a 55 diz "casa só com falta de cama, uma oficina de cada tipo"; o 1A trouxe o rodízio com teto | Uma regra só: **falta de cama → casa; senão oficina que falta (uma de cada tipo); senão rodízio casa ↔ outro tipo, casa até camas = adultos + 4; tipo sem lote sai da vez 30 min** |
| **Quem ajuda a obra** — Regra 59 × emenda da 51 (0.3.32) | A emenda diz que o fazendeiro com a roça descansando ajuda a obra; a 59 diz que só carpinteiro e pedreiro ajudam | Riscar a emenda da 51; vale a 59 |
| **Mina** — Regra 54 × emenda 0.3.36 | A 54 descreve corredor 3×3 a cada 7 blocos; o código é 1×2 a cada 4 | Reescrever a 54 com a proposta M quando aplicada |
| **Expansão** — Regra 56 × emendas 0.3.43/0.3.44 | O texto principal ainda diz "anexa 16 blocos"; a emenda diz "sem faixa vazia" | Reescrever a 56: **a caixa cresce só com rua e obra; a vila cheia puxa a rua pelo melhor lado; uma vez a cada 30 min** |
| **Foco** — Emenda 6 da ADR-003 × Regra 62 | Emenda 6: só com o jogador dentro da caixa; a 62 abriu 48 blocos em volta | Emendar a ADR-003 com a 62 |
| **Teto do estoque** — P1 (64/256) × Regra 58 × reserva de 32 | Três números para o mesmo estoque | Uma regra: **fabricado até a maior casa + 32 (máx. 64); cru sem teto, excedente nos baús públicos** |
| **Numeração** | Linhas fora de ordem, número 54 e 56 com emendas espalhadas | Reordenar e consolidar emendas na própria linha |

## 4. Proposta de ordem de trabalho

1. **Mina M** (itens 1–4) com GameTests de "nunca fica mais de 30 s sem cavar" e playtest de 30 min.
2. **F4 — socorro geral de 30 s** para os ofícios que andam.
3. **Consolidar a tabela de regras** (§3) — sem mudar comportamento, só o texto e o número.

Cada um depende de decisão do autor; o 1 é o maior.
