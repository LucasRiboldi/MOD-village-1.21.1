---
name: minecraft-test-install
description: Caminho da instalação Minecraft (TLauncher) que o usuário usa para testar o mod manualmente
metadata: 
  node_type: memory
  type: project
  modified: 2026-08-07T02:09:13.742Z
---

O usuário testa o mod manualmente numa instalação Minecraft via TLauncher
2.9371 em `C:\Users\lucas\AppData\Roaming\.minecraft`. Mods vão em
`.minecraft\mods`.

Isso é separado do ambiente de desenvolvimento: `./gradlew runClient` e
`runServer` usam o diretório `run/` do Loom dentro do projeto, que tem seu
próprio mundo e seu próprio `eula.txt`. Aceitar o EULA em um não vale para
o outro.

**Por quê:** para entregar um build testável de verdade, o jar de
`build/libs/` precisa ir para essa pasta — não basta o `runClient`.

Estado da instalação em 2026-08-06:

- perfil `Fabric 1.21.1` com **Fabric Loader 0.17.2** (mais antigo que o
  0.19.3 do ambiente de dev)
- `fabric-api-0.116.15+1.21.1.jar` instalado
- `tl_skin_cape` (mod do próprio TLauncher)

**Como aplicar:** quando o usuário pedir para testar no jogo "de verdade"
(e não no ambiente de dev), copiar `build/libs/village-colony-<versão>.jar`
para `.minecraft\mods`. Atenção ao Loader 0.17.2: o `fabric.mod.json`
declara `fabricloader >=0.16.0` justamente para não excluir esta
instalação — não elevar esse mínimo para casar com a versão de build, ou o
mod para de carregar aqui. Ver [[village-colony-build-env]].

**Onde ficam os logs das sessões dele** — é lá que vive a evidência
quando ele diz "entrei no jogo e vi coisas":

- `C:\Users\lucas\AppData\Roaming\.minecraft\logs\latest.log` é a sessão
  **em andamento**; ela é rotacionada para `logs\AAAA-MM-DD-N.log.gz` na
  abertura seguinte, então uma sessão que ele acabou de descrever já pode
  estar num `.gz`. Achar a certa: `zcat` cada um procurando uma frase do
  relato.
- Nesse log as linhas do mod saem **sem** o prefixo `(villagecolony)` que
  o `runGametest` imprime — grepar por `villagecolony` não acha quase
  nada. Grepar pelo texto da linha (`miners:`, `could not reach`,
  `TEST BARRIER`, `opens a mine`).

## ANTES de diagnosticar qualquer relato de jogo: confira o md5

**Custou a sessão de 2026-09-05 inteira.** O autor relatou quatro defeitos
depois de "entrar e testar", e o jar em `.minecraft\mods` era do commit
`fee3c17`, de **09-03 01:38** — dois dias e onze commits atrás. Três dos
quatro relatos eram de código que já não existia; um deles eu tinha
"diagnosticado" no dia anterior contra o build errado, e cheguei a afirmar
ao autor que a picareta de diamante estava corrigida no jar que ele rodou.
Não estava.

```bash
md5sum downloads/village-colony-0.3.0.jar \
       "$APPDATA/.minecraft/mods/village-colony-0.3.0.jar"
```

Diferentes: o relato é sobre outro código. **Diga isso antes de qualquer
análise** — e não gaste leitura de log explicando comportamento que o
build atual já não tem.

Duas confirmações baratas dentro do próprio log, quando o md5 não estiver
à mão:

- `grep -c "lending a hand"` — maior que zero quer dizer jar anterior a
  2026-09-05.
- a data do arquivo em `mods` (`stat -c '%y'`), que é quando a cópia à mão
  funcionou pela última vez.

**Por que ele envelhece calado:** copiar com o Minecraft aberto falha sem
erro (ver o CLAUDE.md do projeto). O autor acha que instalou, joga o jar
velho, e relata defeitos já corrigidos.
