# Checklist — persistência de aldeão

> **Nada do seu mod é salvo automaticamente.** O Vanilla salva o que é dele.

## O que o Vanilla já salva

```text
VillagerData (tipo, profissão, nível, XP) · inventário · gossips · offers
idade · saúde · posição · memórias COM CODEC
```

```text
[ ] sei o que o Vanilla salva, e não estou duplicando
```

## O que é meu

```text
[ ] listei todo estado que o meu mod acrescenta
[ ] cada um tem mecanismo definido
```

| Estado | Onde | Persiste |
|---|---|---|
| por aldeão, pequeno | memória com codec, ou NBT | |
| por colônia/vila | `PersistentState` | |
| índice global | `PersistentState` | |
| **nunca** | `static Map` | |

## A decisão que define a robustez

```text
Existe no MUNDO?            → pergunte ao mundo, NÃO salve
Só existe na cabeça do mod? → salve
```

```text
[ ] profissão/papel atribuído pelo mod        → salvo
[ ] id da colônia                              → salvo
[ ] posição do baú dele                        → NÃO salvo, redescoberto
[ ] alvo atual de trabalho                     → NÃO salvo
[ ] progresso legível do mundo                 → NÃO salvo
[ ] a decisão de NÃO persistir está ESCRITA, com o motivo
```

> Persistir o que o mundo já sabe cria uma segunda verdade que envelhece. E sem a
> decisão registrada, alguém "conserta" isso depois — e introduz o bug.

## Memórias

```text
[ ] com codec onde precisa persistir
[ ] sem codec onde é intenção do momento
[ ] verifiquei qual escolhi — não o que esperava
```

## Nada de static

```text
[ ] nenhum static Map<UUID, ...> com estado de IA
[ ] se há registro global, é limpo no SERVER_STARTED (antes de carregar)
[ ] e no SERVER_STOPPING (depois de salvar)
[ ] a decisão de ter acesso global está documentada
```

> Nos dois lados, não em um só. O processo abre outro save sem reiniciar, e
> aldeões do mundo anterior vazam. Invisível em dev, certo em produção.

## Arquivo

```text
[ ] dados relacionados por id vão no MESMO PersistentState
[ ] markDirty() após TODA mutação
[ ] o KEY não mudou entre versões
```

> Trabalhador aponta para colônia por id? Os dois no mesmo arquivo. **Não há
> transação entre arquivos** — um restart no meio deixa órfão.

## Identidade

```text
[ ] cura de zumbi → UUID NOVO → o estado antigo não o encontra (e isso é correto)
[ ] isso está documentado
[ ] consultar por UUID e não achar NÃO significa que ele morreu
```

## Migração

```text
[ ] há int de versão no NBT
[ ] campos novos têm default seguro
[ ] campos removidos são ignorados sem erro
[ ] save da versão anterior do mod abre
```

## Ciclo de vida

```text
[ ] AFTER_DEATH remove do registro
[ ] MOB_CONVERSION remove do registro     ← o caso mais comum
[ ] ausência na varredura NÃO remove
[ ] a ordem de limpeza está certa (liberar o que depende da posição ANTES de esquecer)
```

## Teste — o decisivo

```text
[ ] criar mundo → atribuir papéis → usar → FECHAR → REABRIR → o estado voltou
[ ] abrir outro save na mesma sessão não traz estado do anterior
[ ] mundo da versão anterior do mod abre
[ ] matar um aldeão → a vaga reabre → reabrir o mundo → continua reaberta
[ ] zumbificar um aldeão → a vaga reabre
```

> O primeiro pega `markDirty` esquecido, par escrita/leitura incompleto e KEY
> errado — de uma vez. E é quase nunca feito.
