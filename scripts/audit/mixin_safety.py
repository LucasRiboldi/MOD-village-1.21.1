"""Regras de segurança dos mixins do mod (ADR-004 e guia de mixins do Fabric).

Para cada *.mixins.json de src/main/resources:
  - toda classe listada existe no pacote declarado;
  - toda classe com @Mixin no pacote está listada (mixin não listado não se aplica);
  - nenhuma usa @Overwrite (proibido salvo ADR), @Redirect ou @ModifyConstant
    (não encadeiam com outros mods; preferir MixinExtras @WrapOperation,
    @ModifyExpressionValue, @ModifyReturnValue);
  - todo método handler e todo campo novo têm prefixo `<modid>$` e os campos
    novos têm @Unique;
  - `injectors.defaultRequire` >= 1 (injeção que não acha o alvo falha alto).

Se houver JAR em build/libs ou downloads/, confere também que as strings de
alvo foram remapeadas para intermediary (method_/class_) ou que há refmap.

Uso: python -I -X utf8 scripts/audit/mixin_safety.py [--modid villagecolony] [--jar caminho]
Saída 1 se alguma regra falhar.
"""
from __future__ import annotations

import argparse
import glob
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FORBIDDEN = {'@Overwrite': 'proibido salvo ADR', '@Redirect': 'não encadeia: use @WrapOperation',
             '@ModifyConstant': 'não encadeia: use @ModifyExpressionValue'}
HANDLER = re.compile(r'@(Inject|ModifyVariable|ModifyArg|ModifyArgs|ModifyReturnValue|WrapOperation|'
                     r'ModifyExpressionValue|WrapWithCondition|Redirect|ModifyConstant)\b[^;{]*?\)\s*'
                     r'(?:private|public|protected)?\s*(?:static\s+)?[\w<>\[\], ?]+\s+([\w$]+)\s*\(', re.S)
FIELD = re.compile(r'^\s*((?:@\w+\s*)*)(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\], ?]+\s+([\w$]+)\s*[;=]', re.M)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--modid', default='villagecolony')
    ap.add_argument('--jar', default=None)
    args = ap.parse_args()
    fails: list[str] = []
    notes: list[str] = []
    configs = sorted(glob.glob(str(ROOT / 'src/main/resources/*.mixins.json')))
    if not configs:
        fails.append('nenhum *.mixins.json em src/main/resources')
    for cfg in configs:
        data = json.loads(Path(cfg).read_text(encoding='utf-8'))
        pkg = data.get('package', '')
        pkg_dir = ROOT / 'src/main/java' / pkg.replace('.', '/')
        listed = [c for k in ('mixins', 'client', 'server') for c in data.get(k) or []]
        if (data.get('injectors') or {}).get('defaultRequire', 0) < 1:
            fails.append(f'{Path(cfg).name}: injectors.defaultRequire < 1 (falha silenciosa)')
        on_disk = {p.stem: p for p in pkg_dir.glob('*.java') if '@Mixin' in p.read_text(encoding='utf-8')}
        for name in listed:
            if name not in on_disk:
                fails.append(f'{Path(cfg).name}: {name} listado e inexistente em {pkg}')
        for name in sorted(set(on_disk) - set(listed)):
            fails.append(f'{Path(cfg).name}: {name} tem @Mixin e não está listado (não se aplica)')
        for name, path in sorted(on_disk.items()):
            src = path.read_text(encoding='utf-8')
            rel = path.relative_to(ROOT).as_posix()
            for ann, why in FORBIDDEN.items():
                for mt in re.finditer(re.escape(ann) + r'\b', src):
                    fails.append(f'{rel}:{src[:mt.start()].count(chr(10)) + 1}: {ann} — {why}')
            # Descritores como "hasLabel(L...;)Z" têm ';' e ')' dentro da
            # string; sem apagar os literais o handler não é reconhecido.
            handlers = HANDLER.findall(re.sub(r'"(?:\\.|[^"\\])*"', '""', src))
            for kind, method in handlers:
                if not method.startswith(args.modid + '$'):
                    fails.append(f'{rel}: handler {method} (@{kind}) sem prefixo {args.modid}$')
            for annots, field in FIELD.findall(src):
                if '@Shadow' in annots:
                    continue
                if '@Unique' not in annots or not field.startswith(args.modid + '$'):
                    fails.append(f'{rel}: campo {field} sem @Unique e prefixo {args.modid}$')
            notes.append(f'{rel}: {len(handlers)} handler(s): '
                         + ', '.join(f'@{k} {m}' for k, m in handlers))
    jars = [args.jar] if args.jar else sorted(
        [p for p in glob.glob(str(ROOT / 'build/libs/*.jar')) + glob.glob(str(ROOT / 'downloads/*.jar'))
         if not p.endswith('-sources.jar') and 'Backup' not in p], key=lambda p: Path(p).stat().st_mtime)[-1:]
    for jar in jars:
        with zipfile.ZipFile(jar) as z:
            names = z.namelist()
            refmap = [n for n in names if n.endswith('refmap.json')]
            mixin_classes = [n for n in names if '/mixin/' in n and n.endswith('Mixin.class')]
            remapped = all(re.search(rb'method_\d+', z.read(n)) for n in mixin_classes)
            if refmap:
                notes.append(f'{Path(jar).name}: refmap presente ({", ".join(refmap)})')
            elif remapped and mixin_classes:
                notes.append(f'{Path(jar).name}: sem refmap; {len(mixin_classes)} mixins com alvo já em '
                             'intermediary (method_N) — remapeamento direto do Loom')
            else:
                fails.append(f'{Path(jar).name}: sem refmap e alvos não remapeados — mixin não acha o alvo em produção')
    for n in notes:
        print('  ', n)
    for f in fails:
        print('FALHA', f)
    print(f'{len(configs)} configs, {len(fails)} falha(s)')
    return 1 if fails else 0


if __name__ == '__main__':
    sys.exit(main())
