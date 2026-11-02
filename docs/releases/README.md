# Registros de release

`REGISTRO-RELEASE` usa um arquivo `v<versão>.md` desta pasta como fonte
humana da entrega. Ele não reescreve `README.md`, `STATE.md` nem `CHANGELOG.md`.

Copie este modelo antes de publicar uma versão:

```markdown
# Release vX.Y.Z

## Mudanças

- Resumo objetivo das mudanças publicadas.

## Evidências

- Sentinela Local: `full`, data, resultado e link/caminho do relatório.
- CI do PR: URL e resultado.

## Playtest

- Mundo novo ou mundo salvo, comandos usados e resultado.

## Pendências

- Nenhuma.
```

Enquanto houver `PENDENTE`, `REGISTRO-RELEASE` recusa a publicação. Ao fechar:

```powershell
python scripts/release_workflow.py registro-release --jar build/libs/village-colony-X.Y.Z.jar --output release-evidence.md
```
