"""Resumo do relatório do GameTest: total, falhas e a mensagem de cada uma.

Uso: python scripts/gametest_summary.py [build/gametest-report.xml]
"""
import sys
import xml.etree.ElementTree as ET


def main() -> int:
    path = sys.argv[1] if len(sys.argv) > 1 else "build/gametest-report.xml"
    root = ET.parse(path).getroot()
    cases = list(root.iter("testcase"))
    failed = [case for case in cases if case.find("failure") is not None]

    print(f"{len(cases)} cases, {len(failed)} failed")

    for case in failed:
        message = case.find("failure").get("message") or ""
        print(f"  {case.get('name')}: {message[:200]}")

    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
