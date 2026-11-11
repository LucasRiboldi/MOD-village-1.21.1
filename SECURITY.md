# Segurança

O Village Colony é um mod Fabric para Minecraft 1.21.1: um `.jar` que o Minecraft carrega. Ele não é um programa
instalável. Não tem instalador, não pede administrador e não roda fora do jogo.

## O que o mod faz no seu computador

| Faz | Não faz |
|---|---|
| Lê e altera o mundo do jogo, como qualquer mod de conteúdo | Não abre conexão de rede própria. Não há cliente HTTP nem socket no código |
| Troca pacotes do próprio Minecraft entre servidor e cliente (o painel das bancadas e o sinalizador dos caminhos, este só para operador) | Não executa outros programas (`Runtime.exec`, `ProcessBuilder`) |
| Grava estado da vila dentro do save, como o jogo faz | Não mexe no registro do Windows, não cria tarefa nem inicia com o sistema |
| Grava relatórios em texto, listados abaixo | Não coleta nem envia dados pessoais |

Arquivos que o mod escreve, todos em texto e só no seu computador:

| Arquivo | Onde | Para quê |
|---|---|---|
| `villagecolony-actions-<data>.jsonl` | pasta `logs` do jogo | Diário de ações dos aldeões, lido por `scripts/action_report.py` |
| `<data>.jsonl` | `<save>/villagecolony/analytics/` | Métricas da vila; cada arquivo para em 32 MB |
| `supply-gaps.jsonl`, `unreachable-pieces.jsonl` | pasta `villagecolony` do jogo | Faltas de material e peças que a obra não alcança |
| Linhas `VC_*` | `latest.log` do jogo | Relatórios de diagnóstico |

Esses arquivos guardam coordenadas, identificadores de aldeões e números da vila. Não guardam nome de jogador, conta ou
endereço. O `latest.log` é do próprio Minecraft e tem o nome do jogador, como em qualquer instalação.

## Conferir o JAR

Cada versão publicada traz o SHA-256 do `.jar` na página de versões do GitHub e no registro do `STATE.md`. Para conferir
o arquivo baixado:

```powershell
Get-FileHash .\village-colony-0.4.52.jar -Algorithm SHA256
```

O valor tem de ser igual ao publicado. Se for diferente, não use o arquivo.

## Build verificado

- **Gradle Wrapper:** `gradle/wrapper/gradle-wrapper.properties` fixa o SHA-256 da distribuição do Gradle
  (`distributionSha256Sum`). O wrapper recusa um zip adulterado.
- **Dependências:** `gradle/verification-metadata.xml` guarda o SHA-256 de cada biblioteca e plugin que o build baixa,
  inclusive as nativas de Windows, Linux e macOS. O Gradle recusa o artefato cujo hash não bate.
- As versões das dependências são fixas: nunca `latest`, `+` ou SNAPSHOT.

Ao atualizar o Gradle, o Loom, o Fabric ou outra dependência, regenere a verificação e revise o diff antes do commit:

```bash
./gradlew --write-verification-metadata sha256 build runGametest pitest remapJar
```

Um hash que mudou sem mudança de versão é sinal de artefato adulterado: investigue antes de aceitar.

Exceção, escolhida pelo autor em 2026-11-11: os jars do grupo `net.fabricmc.fabric-api` não têm o hash conferido. Naquele
dia o Maven do Fabric servia esses jars com o mesmo conteúdo e outro zip conforme o ponto do CDN: o cache local, um cache
vazio e a CI receberam três hashes diferentes do mesmo arquivo. O `.pom` deles continua verificado, e todas as outras dependências
baixadas também.

O que o Loom gera no próprio computador também fica sem conferência, porque não é download e o hash muda a cada
geração: os mods remapeados (grupos `remapped.…`) e o Minecraft montado com os mapeamentos (`minecraft-…`). Quando o Maven do Fabric voltar a servir um arquivo estável, a regra pode sair do
`verification-metadata.xml`.

## Antivírus

O `.jar` é um arquivo Java comum, sem ofuscação, sem compactador e sem código nativo próprio. Se um antivírus o
sinalizar, confira o SHA-256 como acima e informe o falso positivo ao fabricante do antivírus, com o arquivo e o link
da versão.

## Reportar uma falha

Use a aba **Security** do repositório no GitHub ("Report a vulnerability"), que abre um aviso privado. Não publique em
issue aberta os detalhes de como explorar a falha. Diga a versão do mod, o que aconteceu e como reproduzir.

Só a versão mais recente recebe correção.
