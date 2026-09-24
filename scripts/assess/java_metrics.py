"""Metricas estaticas de codigo Java, sem dependencia externa.

Parte da metodologia de avaliacao tecnica (docs/technical/avaliacao/
METODOLOGIA.md). Mede o que da para medir lendo o fonte: tamanho de arquivo
e de metodo, complexidade ciclomatica aproximada, densidade de comentario,
estado estatico mutavel e dependencias entre camadas.

A complexidade e a de McCabe contada por pontos de decisao (if, for, while,
case, catch, &&, ||, ?:) dentro do corpo de cada metodo. E aproximada — nao
ha AST —, mas e a mesma conta em toda avaliacao, e o que se compara e a
evolucao.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path

DECISION = re.compile(r'\b(if|for|while|case|catch)\b|&&|\|\||\?(?!\s*[>,)])')
# Comeco de declaracao de metodo: modificadores, tipo e nome seguido de "(".
# A assinatura pode continuar nas linhas seguintes; o corpo comeca no "{".
METHOD_START = re.compile(
    r'^\s{4}(?:(?:public|protected|private|static|final|abstract|synchronized|default)\s+)*'
    r'(?:<[^>]+>\s+)?[\w.<>\[\]?]+(?:<[^()]*>)?(?:\[\])*\s+(\w+)\s*\(')
NOT_METHODS = {'if', 'for', 'while', 'switch', 'catch', 'synchronized', 'return', 'new', 'else'}


def strip_code(text: str) -> str:
    """Troca comentarios, strings e chars por espacos, preservando as linhas."""
    out, i, n = [], 0, len(text)
    while i < n:
        if text.startswith('//', i):
            j = text.find('\n', i)
            j = n if j < 0 else j
            out.append(' ' * (j - i)); i = j
        elif text.startswith('/*', i):
            j = text.find('*/', i + 2)
            j = n if j < 0 else j + 2
            out.append(''.join(c if c == '\n' else ' ' for c in text[i:j])); i = j
        elif text.startswith('"""', i):
            j = text.find('"""', i + 3)
            j = n if j < 0 else j + 3
            out.append(''.join(c if c == '\n' else ' ' for c in text[i:j])); i = j
        elif text[i] in '"\'':
            q, j = text[i], i + 1
            while j < n and text[j] != q and text[j] != '\n':
                j += 2 if text[j] == '\\' else 1
            j = min(j + 1, n)
            out.append(' ' * (j - i)); i = j
        else:
            out.append(text[i]); i += 1
    return ''.join(out)


@dataclass
class Method:
    name: str
    start: int
    lines: int
    complexity: int


@dataclass
class FileMetrics:
    path: str
    layer: str
    lines: int
    code_lines: int
    comment_lines: int
    methods: list[Method] = field(default_factory=list)
    mutable_statics: int = 0
    imports: list[str] = field(default_factory=list)


def layer_of(path: str) -> str:
    p = path.replace('\\', '/')
    for name in ('core', 'fabric', 'data'):
        if f'/villagecolony/{name}/' in p:
            return name
    return 'root'


def analyze_file(path: Path, root: Path) -> FileMetrics:
    text = path.read_text(encoding='utf-8', errors='replace')
    code = strip_code(text)
    raw_lines = text.split('\n')
    code_lines = code.split('\n')
    n_code = sum(1 for l in code_lines if l.strip())
    n_comment = sum(1 for raw, c in zip(raw_lines, code_lines) if raw.strip() and not c.strip())
    rel = str(path.relative_to(root)).replace('\\', '/')
    fm = FileMetrics(rel, layer_of(rel), len(raw_lines), n_code, n_comment)
    fm.imports = re.findall(r'^import\s+(?:static\s+)?([\w.]+)', text, re.M)

    # Estado estatico mutavel: campo static nao-final, ou static final de
    # colecao mutavel (Map/Set/List/Deque criados com new).
    for l in code_lines:
        s = l.strip()
        if re.match(r'(?:(?:private|public|protected)\s+)?static\s+(?!final)[\w<>, .?]+\s+\w+\s*[=;]', s):
            fm.mutable_statics += 1
        elif re.match(r'(?:(?:private|public|protected)\s+)?static\s+final\s+[\w<>, .?]+\s+\w+\s*=\s*new\s+'
                      r'(HashMap|LinkedHashMap|TreeMap|HashSet|LinkedHashSet|ArrayList|ArrayDeque|EnumMap)', s):
            fm.mutable_statics += 1

    i = 0
    while i < len(code_lines):
        m = METHOD_START.match(code_lines[i])
        if not m or m.group(1) in NOT_METHODS:
            i += 1
            continue
        # Acha o "{" do corpo; um ";" antes dele e declaracao sem corpo.
        k = i
        while k < len(code_lines) and '{' not in code_lines[k] and ';' not in code_lines[k]:
            k += 1
        if k >= len(code_lines) or ('{' not in code_lines[k]) or \
                (';' in code_lines[k] and code_lines[k].index(';') < code_lines[k].index('{')):
            i += 1
            continue
        depth, j, body = 0, k, []
        while j < len(code_lines):
            body.append(code_lines[j])
            depth += code_lines[j].count('{') - code_lines[j].count('}')
            if depth <= 0:
                break
            j += 1
        if m.group(1) == path.stem:
            i = j + 1  # construtor
            continue
        complexity = 1 + sum(len(DECISION.findall(b)) for b in body)
        # Tamanho em linhas de CODIGO: o projeto comenta muito, e contar o
        # comentario puniria justamente o metodo mais documentado.
        size = sum(1 for l in code_lines[i:j + 1] if l.strip())
        fm.methods.append(Method(m.group(1), i + 1, size, complexity))
        i = j + 1
    return fm


def scan(root: Path, source_dir: str) -> list[FileMetrics]:
    base = root / source_dir
    return [analyze_file(p, root) for p in sorted(base.rglob('*.java'))]


def percentile(values: list[int], pct: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    k = (len(ordered) - 1) * pct
    lo, hi = int(k), min(int(k) + 1, len(ordered) - 1)
    return ordered[lo] + (ordered[hi] - ordered[lo]) * (k - lo)
