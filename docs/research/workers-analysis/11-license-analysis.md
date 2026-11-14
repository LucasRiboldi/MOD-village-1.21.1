# 11 — Licença e propriedade intelectual

---

## 1. A licença efetivamente presente nos arquivos locais

`workers-maingit/LICENSE.txt`, na íntegra:

```text
Copyright 2023 Talha Kantar

All rights reserved. This work and any accompanying documentation is the
intellectual property of Talha Kantar. Any unauthorized reproduction,
modification, distribution, transmission, or other exploitation is
strictly prohibited and may be a violation of applicable laws.

This work is provided "as is" without warranty of any kind, either
expressed or implied, including but not limited to the implied warranties
of merchantability and/or fitness for a particular purpose.
```

`workers-maingit/README.md`, 6 linhas:

```text
# workers
Mod for Minecraft that adds Worker Villagers.
https://www.curseforge.com/minecraft/mc-mods/workers
All Rights Reserved unless otherwise explicitly stated.
```

`workers-maingit/CREDITS.txt` — **não é do Workers.** É o arquivo de
créditos do Minecraft Forge (LexManos, cpw, MCP), que veio junto com o
template de mod. Não outorga nada sobre o código do Workers.

`gradle.properties` — ainda o do template (`mod_id=examplemod`,
`mod_license=All Rights Reserved`). Confirma a mesma leitura.

Nenhum arquivo `.java` do Workers tem cabeçalho de copyright próprio;
todos herdam o `LICENSE.txt` da raiz.

---

## 2. Leitura da licença

**O que ela permite:**

* ler o código;
* estudá-lo;
* aprender com ele.

Nada disso é concedido pela licença — decorre de você possuir uma cópia
legítima do fonte. A licença não os proíbe.

**O que ela proíbe, explicitamente:**

```text
reproduction     copiar, no todo ou em parte
modification     adaptar, traduzir, portar
distribution     redistribuir, mesmo modificado
transmission     repassar
other exploitation   qualquer outro uso
```

Todos sem autorização. Não há autorização em nenhum arquivo do
repositório.

**A ausência é o ponto.** "All rights reserved" não é uma licença
permissiva com restrições; é a **negação** de licença. O padrão do direito
autoral aplica-se por inteiro: nada é concedido além do que a lei já
permite ao possuidor de uma cópia.

O briefing §13 antecipou a divergência entre a página de distribuição
(All Rights Reserved) e os arquivos do repositório. **Não há divergência.**
Os arquivos locais dizem a mesma coisa, e dizem de forma mais dura.

---

## 3. A linha entre ideia e código

Esta é a distinção que torna esta análise legítima, e ela não é uma
sutileza retórica — é a base do direito autoral de software.

| Protegido | Não protegido |
|---|---|
| o texto do código-fonte | a ideia que ele implementa |
| a estrutura e organização específicas | o método ou algoritmo em si |
| nomes e comentários originais | o fato de que um problema tem uma solução |
| a expressão da solução | o princípio de funcionamento |

Direito autoral protege **expressão**, não **ideia**. É por isso que:

* saber que "o trabalhador deve declarar o item que lhe falta em vez de
  falhar" é conhecimento livre;
* copiar `NeededItem.java` é reprodução proibida;
* escrever a sua própria classe, com o seu nome, os seus tipos
  (`ResourceId` em vez de `Predicate<ItemStack>`), os seus comentários e a
  sua estrutura, a partir do conceito, é trabalho seu.

**Não são pesos e medidas convenientes.** A diferença prática é
verificável: se alguém puser os dois arquivos lado a lado, a
reimplementação não se parece com o original — porque os tipos, a
linguagem dos comentários, a decomposição em métodos e a arquitetura ao
redor são outros.

O caso do Village Colony é ainda mais claro que a média: **nenhuma classe
do Workers compila no seu projeto**. Loader diferente, mappings
diferentes, versão do jogo diferente, base de entidade que vem de um mod
que você não tem. Copiar não é apenas ilegal aqui; é impossível.

---

## 4. Regras operacionais para este projeto

Vinculantes a partir de agora.

```text
1. Nenhum arquivo de workers-maingit/ é copiado, traduzido ou portado.

2. Nenhum trecho de código do Workers entra no Village Colony,
   nem sob refatoração.

3. Nenhum comentário, javadoc, nome de constante ou texto de mensagem
   do Workers é reproduzido.

4. Nomes de conceito genéricos do domínio Minecraft continuam livres
   ("work area", "needed item", "storage"), porque são vocabulário
   corrente e não expressão original.

5. Um conceito aprendido aqui é implementado a partir da DESCRIÇÃO
   nesta pasta, sem o arquivo original aberto ao lado.

6. Onde uma solução for inspirada, o comentário no seu código registra
   a INSPIRAÇÃO, não a origem do código. Exemplo aceitável:

       // O envelhecimento por ciclo impede que a tarefa mais antiga
       // nunca seja atendida. É aging clássico de escalonador.

   Exemplo inaceitável:

       // Copiado do Workers (LumberjackWorkGoal:543)

   O primeiro é honesto e correto; o segundo confessa reprodução
   e não é verdade se o código foi reescrito.

7. workers-maingit/ NÃO é commitado neste repositório.
   Hoje ela está como untracked. Deve entrar no .gitignore.
```

O item 7 é o mais urgente e o mais fácil de errar. Commitar o
`workers-maingit/` seria **redistribuição** de obra com todos os direitos
reservados, num repositório público
(`github.com/LucasRiboldi/MOD-village-1.21.1`). É exatamente o que a
licença proíbe, e o `git status` do início desta sessão mostra a pasta
como não rastreada — ou seja, um `git add .` a incluiria.

---

## 5. O que fazer com a pasta

```text
imediato    acrescentar workers-maingit/ ao .gitignore

durante     manter local, para consulta

depois      quando a análise estiver incorporada, remover a pasta.
            Esta pasta docs/workers-analysis/ passa a ser a única
            memória do estudo — e ela é obra sua, com texto seu,
            e pode ser commitada e publicada sem qualquer problema.
```

---

## 6. Conclusão jurídica

> Não copie o código.

O briefing §13 disse "se houver qualquer dúvida jurídica, não copie". Não
há dúvida: a licença é inequívoca e proíbe reprodução, modificação e
distribuição.

O que **pode** ser feito, e é o que esta análise fez: estudar a
arquitetura, entender os problemas que o Workers resolveu, e reimplementar
de forma independente aquilo que fizer sentido para o Village Colony.

Isso é engenharia legítima. É o que todo mod aprende de todo mod.
