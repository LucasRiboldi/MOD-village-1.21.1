# Plano: crescimento das profissões produtoras

## Objetivo

Substituir o teto fixo de profissões pela sequência autoral de sete
produtores, derivada do número de adultos da vila, preservando profissões
existentes e o trabalho de construção como atividade temporária.

## Lote 1

1. Registrar a política e sua compatibilidade em ADR.
2. Cobrir com testes os marcos de 7, 15, 16, 30, 31 e 32 adultos, além da
   retenção de profissionais após queda populacional.
3. Implementar a contagem de adultos no scanner, incluindo Nitwits na
   população mas mantendo-os inelegíveis para profissão.
4. Habilitar trabalhadores produtores a executar construção quando houver
   tarefa BUILD, sem substituir sua profissão permanente.
5. Rodar unitários, build e GameTests; parar para revisão do usuário.

## Lotes seguintes, após revisão

1. Criador: reunir tosquia e criação/colheita animal sem duplicar a
   profissão Vanilla.
2. Completar cadeias de materiais a partir do texto de requisitos e dos
   recursos/receitas já existentes.
3. Harmonizar regras, guias e documentação viva; acrescentar regra de
   pré-preenchimento da atividade prioritária para uso do Codex CLI.

## Restrições

- Sem alteração da profissão Vanilla do aldeão.
- Sem demissão por queda de população ou por atingir teto de função.
- Profissões antigas do save continuam legíveis; `SHEPHERD` e `BUILDER`
  não recebem novos candidatos.
- Núcleo puro continua sem dependências de Minecraft/Fabric.
