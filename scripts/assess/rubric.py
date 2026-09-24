"""A regua da avaliacao tecnica: 14 criterios, nota de 0 a 4, limiares fixos.

Os limiares moram aqui e em docs/technical/avaliacao/METODOLOGIA.md, e as duas
fontes tem de concordar. Mudar um limiar muda o sentido de toda comparacao
futura: se for preciso, registre a mudanca na metodologia (secao "Versoes da
regua") e aumente RUBRIC_VERSION.
"""

from __future__ import annotations

RUBRIC_VERSION = 1


def ladder(value, steps, higher_is_better=True):
    """Nota pela primeira faixa atingida. steps = limiares das notas 4, 3, 2, 1."""
    if value is None:
        return None
    for score, limit in zip((4, 3, 2, 1), steps):
        if (value >= limit) if higher_is_better else (value <= limit):
            return score
    return 0


def _files_over(d):
    s = d['source']
    return round(100 * len(s['files_over_500']) / max(1, s['files']), 1)


def _cc(d):
    s = d['source']
    score = ladder(s['pct_cc_over_10'], (3, 6, 10, 15), higher_is_better=False)
    return min(score, 2) if s['cc_max'] > 50 else score


def _gametests(d):
    runs = [r for r in d['gametests'] if r.get('total')]
    if not runs:
        return None, 'sem rodada'
    total = sum(r['total'] for r in runs)
    failed = sum(r['failed'] for r in runs)
    rate = 100 * (total - failed) / total
    if failed == 0:
        return (4 if len(runs) >= 2 else 3), f'{len(runs)} rodada(s), 0 falha'
    return ladder(rate, (101, 101, 99.5, 98)), f'{failed} falha(s) em {len(runs)} rodada(s)'


def _static(d):
    sa, code = d['static_analysis'], d['source']['code_lines']
    if not sa:
        return None, 'sem log de compilação'
    per_k = round(1000 * sa['warnings'] / max(1, code), 1)
    return ladder(per_k, (2, 5, 10, 20), higher_is_better=False), f"{sa['warnings']} avisos ({per_k}/kLOC)"


def _perf(d):
    g = d['game_performance']
    if not g or not g.get('session_hours'):
        return None, 'sem sessão analisada'
    per_h = round(g['observations'].get('cycle_over_tick', 0) / g['session_hours'], 1)
    return ladder(per_h, (0, 5, 15, 40), higher_is_better=False), f'{per_h} ciclos > 1 tique por hora'


def _process(d):
    n = sum(d['process']['practices'].values())
    return ladder(n, (7, 6, 5, 4)), f'{n} de 7 práticas'


# (id, nome, dimensao, funcao -> (nota, texto da medida))
CRITERIA = [
    {'id': 'C01', 'name': 'Arquivos > 500 linhas (produção)', 'dim': 'Organização',
     'fn': lambda d: (ladder(_files_over(d), (0, 2, 5, 10), False), f"{_files_over(d)}% ({len(d['source']['files_over_500'])})")},
    {'id': 'C02', 'name': 'Tamanho de método (p90, linhas de código)', 'dim': 'Organização',
     'fn': lambda d: (ladder(d['source']['method_lines_p90'], (20, 35, 50, 80), False),
                      f"p90 {d['source']['method_lines_p90']:.0f}, máx {d['source']['method_lines_max']}")},
    {'id': 'C03', 'name': 'Complexidade ciclomática (% métodos > 10)', 'dim': 'Complexidade',
     'fn': lambda d: (_cc(d), f"{d['source']['pct_cc_over_10']}%, máx {d['source']['cc_max']}")},
    {'id': 'C04', 'name': 'Regra de camadas (core sem Minecraft)', 'dim': 'Arquitetura',
     'fn': lambda d: (ladder(len(d['source']['core_layer_violations']), (0, 0, 2, 5), False),
                      f"{len(d['source']['core_layer_violations'])} violações")},
    {'id': 'C05', 'name': 'Estado estático mutável (por kLOC)', 'dim': 'Arquitetura',
     'fn': lambda d: (ladder(d['source']['mutable_statics_per_kloc'], (1, 3, 6, 10), False),
                      f"{d['source']['mutable_statics']} campos ({d['source']['mutable_statics_per_kloc']}/kLOC)")},
    {'id': 'C06', 'name': 'Volume de teste (código de teste / produção)', 'dim': 'Testes',
     'fn': lambda d: (ladder(d['source']['test_to_main_ratio'], (1.0, 0.7, 0.5, 0.3)),
                      f"{d['source']['test_to_main_ratio']}x")},
    # O unitario e o instrumento do core e do data; o fabric e medido pela
    # bateria de jogo (C09). Ver METODOLOGIA.md, C07.
    {'id': 'C07', 'name': 'Cobertura de linha do core+data (JaCoCo)', 'dim': 'Testes',
     'fn': lambda d: (ladder(d['coverage'].get('line_core_data'), (80, 65, 50, 35)),
                      f"{d['coverage'].get('line_core_data')}% core+data "
                      f"(fabric por unitário: {d['coverage'].get('line_by_layer', {}).get('fabric')}%)")},
    {'id': 'C08', 'name': 'Mutação (PIT, core)', 'dim': 'Testes',
     'fn': lambda d: (ladder(d['mutation'].get('score'), (85, 75, 65, 50)),
                      f"{d['mutation'].get('score')}% mortas, força {d['mutation'].get('strength')}%")},
    {'id': 'C09', 'name': 'Estabilidade da bateria de jogo', 'dim': 'Testes', 'fn': _gametests},
    {'id': 'C10', 'name': 'Análise estática (Error Prone / kLOC)', 'dim': 'Qualidade', 'fn': _static},
    {'id': 'C11', 'name': 'Densidade de comentário (comentário / código)', 'dim': 'Legibilidade',
     'fn': lambda d: (ladder(d['source']['comment_ratio'], (0.6, 1.0, 1.5, 2.5), False) if d['source']['comment_ratio'] >= 0.15 else 1,
                      f"{d['source']['comment_ratio']}")},
    {'id': 'C12', 'name': 'Práticas de engenharia automatizadas', 'dim': 'Processo', 'fn': _process},
    {'id': 'C13', 'name': 'Desempenho em jogo (ciclo > 1 tique / h)', 'dim': 'Desempenho', 'fn': _perf},
    {'id': 'C14', 'name': 'Dívida crítica aberta (🔴 no TODO)', 'dim': 'Registros',
     'fn': lambda d: (ladder(d['records']['todo_open_critical'], (0, 2, 5, 10), False),
                      f"{d['records']['todo_open_critical']} itens 🔴 de {d['records']['todo_open']} abertos")},
]


def grade(mean: float) -> str:
    for g, limit in (('A', 3.5), ('B', 2.75), ('C', 2.0), ('D', 1.25)):
        if mean >= limit:
            return g
    return 'E'


def score_all(data: dict) -> dict:
    out = {}
    for c in CRITERIA:
        score, value = c['fn'](data)
        out[c['id']] = {'name': c['name'], 'dimension': c['dim'], 'score': score, 'value': value}
    valid = [v['score'] for v in out.values() if v['score'] is not None]
    mean = round(sum(valid) / max(1, len(valid)), 2)
    out['_overall'] = {'mean': mean, 'grade': grade(mean), 'criteria_scored': len(valid),
                       'rubric_version': RUBRIC_VERSION}
    return out
