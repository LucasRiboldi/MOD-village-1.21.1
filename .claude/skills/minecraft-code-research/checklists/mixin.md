# Checklist — Mixin

> Use ao avaliar, escrever ou revisar um Mixin. Complementa
> `checklists/before-modifying-vanilla.md`, que vem antes.

## Justificativa

- [ ] Passei pela escada de extensão e **registrei o motivo** de cada alternativa
      não servir
- [ ] O motivo está escrito num documento (`templates/mixin-analysis.md`), não só
      na conversa
- [ ] Escolhi o **tipo menos invasivo** que resolve

## Alvo verificado

- [ ] Classe existe nesta versão: `unzip -l "$MC_JAR" | grep "<Classe>.class"`
- [ ] Método existe, com assinatura conferida: `javap -s -cp "$MC_JAR" <fqn>`
- [ ] **Se há sobrecarga, o `method =` inclui o descriptor**
- [ ] O alvo não é lambda, classe anônima nem método sintético
- [ ] Se o alvo é `default` de interface, estou mirando a interface

## Injeção

- [ ] Ponto escolhido conscientemente (`HEAD` / `TAIL` / `RETURN` / `INVOKE` / `FIELD`)
- [ ] **Se `RETURN`:** o efeito é idempotente (roda por return, não uma vez)
- [ ] Não assumo índice de lista — outro mod pode ter inserido antes
- [ ] Prioridade definida, se importa

## Configuração

- [ ] Registrado no `*.mixins.json`
- [ ] `package` correto
- [ ] `compatibilityLevel` compatível com o Java do projeto
- [ ] Mixin de cliente na lista `client`, não na comum
- [ ] `injectors.defaultRequire: 1` — falha alto se não aplicar

> `require = 0` só se a ausência do alvo for esperada **e tratada**. Mixin que
> silenciosamente não aplica vira bug fantasma.

## Conteúdo

- [ ] Nome do método com prefixo `modid$` (evita colisão)
- [ ] **A lógica NÃO mora no Mixin** — ele delega para uma classe normal
- [ ] Não removo nem cancelo comportamento Vanilla (ou, se removo, está justificado)
- [ ] **Nenhuma exceção escapa** — capturo e logo

## Degradação

- [ ] Sei o que acontece se o injector não aplicar
- [ ] Sei o que acontece se o meu código lançar
- [ ] **Sem o Mixin, o resultado é comportamento Vanilla** — não estado quebrado

## Compatibilidade

- [ ] Classifiquei: LOW / MEDIUM / HIGH
- [ ] Se `@Redirect` ou `@Overwrite`: sei que são **exclusivos** e justifiquei
- [ ] Verifiquei se `@WrapOperation` (encadeável) resolveria no lugar
- [ ] Sei quais mods conhecidos miram esta mesma classe
- [ ] Conflito conhecido está **documentado**, não escondido

## Teste

- [ ] Compila
- [ ] O injector aplica — sem aviso no log de boot
- [ ] O comportamento novo acontece
- [ ] **O comportamento Vanilla preservado continua acontecendo**
- [ ] Gametest cobrindo o caso, quando aplicável
- [ ] Testado em `runServer`, não só em `runClient`

## Sinais de que algo está errado

- [ ] `@Overwrite` em método grande → reimplementa Vanilla e congela a versão
- [ ] Regra de negócio dentro do corpo do Mixin
- [ ] Mixin em classe de outro mod sem dependência declarada
- [ ] Muitos Mixins na mesma classe central
- [ ] Copiado de outro mod sem análise do alvo nesta versão
