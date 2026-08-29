# Anti-padrões de pesquisa e modding em Minecraft

Cada item traz o padrão, **por que ele é atraente** (ninguém escolhe um
anti-padrão de propósito), como ele falha e o que fazer no lugar.

---

## Mixin First

**Padrão:** achar a classe Vanilla e escrever um Mixin, sem procurar API.

**Por que atrai:** funciona na primeira tentativa e resolve exatamente o que você
queria. É a ferramenta mais poderosa disponível, e poder resolve rápido.

**Como falha:** dois mods injetando no mesmo método brigam. `@Redirect` e
`@Overwrite` são mutuamente exclusivos por construção — o segundo mod
simplesmente não roda, ou roda com estado que o primeiro já alterou. O bug chega
como "seu mod não funciona com o X", e você não consegue reproduzir.

**No lugar:** desça a escada de extensão do `SKILL.md` e pare no primeiro degrau
que resolve. Se o Mixin continuar necessário, escreva a linha que diz por quê.

---

## Version Blindness

**Padrão:** copiar código de tutorial/mod de outra versão do Minecraft.

**Por que atrai:** o código existe, aparentemente resolve, e reescrever parece
desperdício.

**Como falha:** três modos, do melhor para o pior — não compila (barato);
compila mas o método mudou de semântica (caro); compila, roda, e corrompe save
porque o formato de serialização mudou (péssimo).

**No lugar:** `mappings.md`. Verifique versão, mapping, existência e assinatura
antes de reusar. Nome igual não é comportamento igual.

---

## Mapping Blindness

**Padrão:** assumir que nome Yarn e Mojmap são a mesma coisa.

**Por que atrai:** muitos nomes coincidem, então parece que todos coincidem.

**Como falha:** `ServerLevel` não existe em Yarn; `ServerWorld` não existe em
Mojmap. Em Mixin com descriptor errado, a falha é no boot — ou pior, o injector
com `require = 0` não aplica e não avisa.

**No lugar:** `javap` e `mappings.tiny`. Sempre.

---

## Single Class Assumption

**Padrão:** concluir que entendeu o sistema porque leu a classe principal.

**Por que atrai:** a classe tem o nome do sistema. Parece o lugar certo.

**Como falha:** no Minecraft o comportamento é distribuído. O trabalho do aldeão
está em `VillagerEntity`, na `Schedule`, nas tasks do `Brain`, nas memórias, no
POI, no `PointOfInterestStorage` e num JSON de datapack. Quem leu só a entidade
vai fazer um patch no lugar errado e não vai entender por que não funciona.

**No lugar:** monte a cadeia da Fase 4 e siga os callers.

---

## Tick Abuse

**Padrão:** lógica pesada dentro do tick, para toda entidade.

**Por que atrai:** tick é o lugar mais fácil de "fazer acontecer sempre".

**Como falha:** 20 vezes por segundo × N entidades. Um scan de 32 blocos por
aldeão por tick derruba o TPS com 20 aldeões, e o autor testou com dois.

**No lugar:** ver `performance-analysis.md`. Reativo > periódico > por tick. Se
precisar de periódico, use cooldown e escalone entidades entre ticks.

---

## Chunk Load Cascade

**Padrão:** `world.getBlockState(pos)` para posição arbitrária, dentro do tick.

**Por que atrai:** é a API óbvia e funciona nos testes, onde tudo está carregado.

**Como falha:** o método **carrega o chunk que faltar**. Do tick do servidor isso
é gerar terreno dentro do laço; chamado de dentro de um evento de carga de chunk,
a thread passa a esperar por um chunk que só ela poderia produzir — e trava.

**No lugar:**

```java
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);  // null = não carregado, pule
```

Tratar `null` como "não sei agora" é quase sempre correto: o mod não deve segurar
chunk que o jogo já descarregou.

---

## Global Entity Scan

**Padrão:** iterar todas as entidades do mundo para achar algumas.

**Por que atrai:** uma linha resolve.

**Como falha:** custo proporcional à população inteira, repetido por quem
pergunta. Com vários mods fazendo o mesmo, o servidor gasta o tick procurando.

**No lugar:** busca por caixa/raio, índice próprio mantido por evento, ou memória
do Brain. Ver `performance-analysis.md`.

---

## Static State Abuse

**Padrão:** guardar estado do mundo em campo `static`.

**Por que atrai:** acesso global sem passar referência por dez camadas.

**Como falha:** o processo pode abrir outro save sem reiniciar. Estado do mundo
anterior vaza para o novo. Em servidor com múltiplos mundos, dá mistura silenciosa.
E nada disso é salvo, então some no restart.

**No lugar:** `PersistentState` para estado do mundo, dados da entidade para
estado da entidade. Se um ponto de acesso global for mesmo necessário, que seja
**explicitamente limpo** no start e no stop do servidor, e que isso esteja
documentado como decisão.

---

## Serialization Assumption

**Padrão:** assumir que um campo é salvo porque existe.

**Por que atrai:** em muitos frameworks, é.

**Como falha:** aqui não é. O campo vive e some no restart. O bug aparece como
"o mod esquece tudo quando eu fecho o mundo", muito depois de ter sido escrito.

**No lugar:** procure quem escreve e quem lê. Sem par escrita/leitura, não
persiste. Ver `persistence-analysis.md`.

---

## Client Authority

**Padrão:** o cliente decide, o servidor aceita.

**Por que atrai:** funciona perfeitamente no singleplayer.

**Como falha:** o singleplayer roda um servidor integrado no mesmo processo, e o
mesmo objeto acaba visível dos dois lados — o bug fica invisível até o
multiplayer. Aí vira dessincronia, ou um vetor de trapaça.

**No lugar:** cliente pede, servidor valida e executa, servidor sincroniza. Ver
`client-server-analysis.md`.

---

## Client Class Leakage

**Padrão:** referenciar classe de cliente em código comum.

**Por que atrai:** compila, e roda no ambiente de desenvolvimento.

**Como falha:** `NoClassDefFoundError` no servidor dedicado, no boot. O ambiente
de dev muitas vezes tem as duas metades no classpath, então o problema só aparece
para quem hospeda.

**No lugar:** entrypoint `client` separado. Teste em `runServer`, não só em
`runClient`.

---

## Registry Late Initialization

**Padrão:** registrar conteúdo fora da janela correta.

**Por que atrai:** "registro quando precisar" parece mais organizado.

**Como falha:** registro depois do congelamento lança exceção; registro
condicional produz ids diferentes entre cliente e servidor, e a conexão cai com
mismatch. Pior: ordem de registro instável entre execuções corrompe ids salvos.

**No lugar:** registro no entrypoint, incondicional, determinístico. Ver
`registry-analysis.md`.

---

## God Manager

**Padrão:** uma classe central que controla tudo.

**Por que atrai:** cresce um método de cada vez, e cada passo parece razoável.

**Como falha:** vira o ponto de acoplamento de tudo. Qualquer mudança toca a mesma
classe, e nada é testável isoladamente.

**No lugar:** responsabilidade por sistema, estado com dono claro.

---

## AI Every Tick

**Padrão:** decisão complexa de IA em todo tick.

**Por que atrai:** "responsivo".

**Como falha:** o custo é multiplicado pela população. Pior, produz oscilação — a
entidade troca de alvo entre ticks e nunca chega a lugar nenhum.

**No lugar:** memória com validade, sensor com intervalo, cooldown. O Brain do
Vanilla já é feito disso — use o mecanismo em vez de contorná-lo.

---

## Pathfinding Spam

**Padrão:** recalcular navegação continuamente.

**Por que atrai:** garante que o alvo está atualizado.

**Como falha:** pathfinding é dos cálculos mais caros do jogo. Recalcular por
tick, por entidade, é uma das formas mais rápidas de matar o TPS.

**No lugar:** só recalcule quando o destino mudar ou o caminho falhar. Em mobs de
`Brain`, escreva `WALK_TARGET` e deixe as tasks Vanilla de movimento fazerem o
trabalho.

---

## Copy Paste Mixin

**Padrão:** copiar um Mixin de outro mod sem entender.

**Por que atrai:** alguém já resolveu, e o código está ali.

**Como falha:** o Mixin foi escrito para outra versão, outro mapping, e depende
do estado que *aquele* mod garante. Fora daquele contexto, injeta no lugar certo
com premissa errada — que é o modo de falha mais difícil de diagnosticar.

**No lugar:** entenda o alvo, valide o injection point na sua versão, e escreva o
seu. Copiar a **ideia** é ótimo; copiar o código sem a análise, não.

---

## Infinite Retry

**Padrão:** comportamento que tenta indefinidamente uma ação impossível.

**Por que atrai:** "ele vai conseguir na próxima".

**Como falha:** o alvo pode ter sumido para sempre. A entidade fica presa,
consumindo CPU e sem nunca progredir, e o jogador vê um aldeão parado olhando
para o nada.

**No lugar:** todo comportamento precisa de condição de fracasso, timeout e saída.
Ver `entity-analysis.md`.

---

## Pesquisa infinita

**Padrão:** continuar lendo porque sempre há mais.

**Por que atrai:** parece diligência, e ler é confortável.

**Como falha:** consome o contexto inteiro, produz um resumo bonito e não muda
nenhuma decisão.

**No lugar:** a regra de parada do `SKILL.md`. Se a leitura não resolve dúvida,
não muda decisão, não valida hipótese e não reduz risco: `FUTURE RESEARCH`, e
pare.

---

## Certeza sem fonte

**Padrão:** escrever tudo com o mesmo tom de certeza.

**Por que atrai:** hedge o tempo todo fica ilegível, e o documento parece fraco.

**Como falha:** quem lê não sabe o que conferir. Uma inferência errada no meio de
dez fatos contamina todo o resto quando é descoberta.

**No lugar:** as etiquetas de `evidence-and-claims.md`. Elas dão precisão sem
transformar o texto em hedge — o leitor sabe exatamente onde pisar firme.
