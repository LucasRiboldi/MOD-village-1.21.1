#!/usr/bin/env python3
"""Janela local do Sentinela Local, sem dependências externas."""

from __future__ import annotations

import argparse
import os
import queue
import subprocess
import sys
import threading
import tkinter as tk
from pathlib import Path
from tkinter import messagebox, ttk

ROOT = Path(__file__).resolve().parent.parent
RUNNER = ROOT / "scripts" / "test_lab.py"
REPORTS = ROOT / "reports" / "test-runs"
PROFILES = ("quick", "fast", "full")


class SentinelaGui(tk.Tk):
    """Configura e executa o orquestrador oficial em processo separado."""

    def __init__(self) -> None:
        super().__init__()
        self.title("Sentinela Local")
        self.minsize(720, 500)
        self.profile = tk.StringVar(value="quick")
        self.repeat = tk.StringVar(value="1")
        self.dry_run = tk.BooleanVar(value=False)
        self.resume = tk.StringVar()
        self.status = tk.StringVar(value="Pronto. Nenhum teste foi iniciado.")
        self.events: queue.Queue[tuple[str, str | int]] = queue.Queue()
        self.process: subprocess.Popen[str] | None = None
        self._build()
        self.after(100, self._drain_events)

    def _build(self) -> None:
        frame = ttk.Frame(self, padding=16)
        frame.grid(sticky="nsew")
        self.columnconfigure(0, weight=1)
        self.rowconfigure(0, weight=1)
        frame.columnconfigure(1, weight=1)
        frame.rowconfigure(5, weight=1)

        ttk.Label(frame, text="Sentinela Local", font=("Segoe UI", 16, "bold")).grid(
            column=0, row=0, columnspan=3, sticky="w")
        ttk.Label(frame, text="Executa testes locais do projeto. Não usa agente de IA.").grid(
            column=0, row=1, columnspan=3, sticky="w", pady=(0, 12))

        profiles = ttk.LabelFrame(frame, text="Perfil", padding=8)
        profiles.grid(column=0, row=2, columnspan=3, sticky="ew")
        self.profile_buttons: list[ttk.Radiobutton] = []
        for index, profile in enumerate(PROFILES):
            button = ttk.Radiobutton(profiles, text=profile, value=profile, variable=self.profile)
            button.grid(column=index, row=0, padx=(0, 18), sticky="w")
            self.profile_buttons.append(button)
        ttk.Label(profiles, text="quick: unitários e propriedades; fast: inclui GameTests; full: inclui auditoria e PIT.").grid(
            column=0, row=1, columnspan=3, sticky="w", pady=(6, 0))

        ttk.Label(frame, text="Repetições:").grid(column=0, row=3, sticky="w", pady=(12, 0))
        self.repeat_entry = ttk.Entry(frame, textvariable=self.repeat, width=8)
        self.repeat_entry.grid(column=1, row=3, sticky="w", pady=(12, 0))
        self.dry_button = ttk.Checkbutton(
            frame, text="Simular comandos (não inicia Gradle)", variable=self.dry_run)
        self.dry_button.grid(column=2, row=3, sticky="w", pady=(12, 0))

        ttk.Label(frame, text="Retomar execução (UTC-id):").grid(column=0, row=4, sticky="w", pady=(10, 0))
        self.resume_entry = ttk.Entry(frame, textvariable=self.resume)
        self.resume_entry.grid(column=1, row=4, columnspan=2, sticky="ew", pady=(10, 0))
        ttk.Label(frame, text="Ao preencher, o perfil e as repetições da execução salva são usados.").grid(
            column=0, row=5, columnspan=3, sticky="nw", pady=(4, 0))

        self.output = tk.Text(frame, height=14, wrap="word", state="disabled")
        self.output.grid(column=0, row=6, columnspan=3, sticky="nsew", pady=(10, 0))
        scrollbar = ttk.Scrollbar(frame, orient="vertical", command=self.output.yview)
        scrollbar.grid(column=3, row=6, sticky="ns", pady=(10, 0))
        self.output["yscrollcommand"] = scrollbar.set

        ttk.Label(frame, textvariable=self.status).grid(column=0, row=7, columnspan=3, sticky="w", pady=(10, 0))
        actions = ttk.Frame(frame)
        actions.grid(column=0, row=8, columnspan=3, sticky="e", pady=(10, 0))
        self.open_button = ttk.Button(actions, text="Abrir relatórios", command=self._open_reports)
        self.open_button.grid(column=0, row=0, padx=(0, 8))
        self.cancel_button = ttk.Button(actions, text="Cancelar", command=self._cancel, state="disabled")
        self.cancel_button.grid(column=1, row=0, padx=(0, 8))
        self.run_button = ttk.Button(actions, text="Executar", command=self._run)
        self.run_button.grid(column=2, row=0)

    def _arguments(self) -> list[str] | None:
        resume = self.resume.get().strip()
        if resume:
            if Path(resume).name != resume:
                messagebox.showerror("Sentinela Local", "O identificador de retomada não pode conter pastas.")
                return None
            arguments = ["--resume", resume]
            if self.dry_run.get():
                arguments.append("--dry-run")
            return arguments
        try:
            repeat = int(self.repeat.get())
        except ValueError:
            repeat = 0
        if repeat < 1:
            messagebox.showerror("Sentinela Local", "Repetições deve ser um número inteiro maior que zero.")
            return None
        arguments = [self.profile.get(), "--repeat", str(repeat)]
        if self.dry_run.get():
            arguments.append("--dry-run")
        return arguments

    def _run(self) -> None:
        arguments = self._arguments()
        if arguments is None:
            return
        if not RUNNER.is_file():
            messagebox.showerror("Sentinela Local", f"Orquestrador não encontrado: {RUNNER}")
            return
        self._append("$ " + " ".join([sys.executable, str(RUNNER), *arguments]) + "\n")
        self._set_running(True)
        threading.Thread(target=self._execute, args=(arguments,), daemon=True).start()

    def _execute(self, arguments: list[str]) -> None:
        try:
            self.process = subprocess.Popen(
                [sys.executable, str(RUNNER), *arguments], cwd=ROOT,
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                encoding="utf-8", errors="replace")
            if self.process.stdout:
                for line in self.process.stdout:
                    self.events.put(("output", line))
            self.events.put(("finished", self.process.wait()))
        except OSError as error:
            self.events.put(("output", f"Erro ao iniciar: {error}\n"))
            self.events.put(("finished", 1))

    def _drain_events(self) -> None:
        try:
            while True:
                kind, value = self.events.get_nowait()
                if kind == "output":
                    self._append(str(value))
                else:
                    self.process = None
                    self._set_running(False)
                    result = int(value)
                    self.status.set("Concluído com sucesso." if result == 0 else f"Concluído com código {result}.")
        except queue.Empty:
            pass
        self.after(100, self._drain_events)

    def _set_running(self, running: bool) -> None:
        state = "disabled" if running else "normal"
        self.run_button.configure(state=state)
        self.open_button.configure(state=state)
        self.cancel_button.configure(state="normal" if running else "disabled")
        self.repeat_entry.configure(state=state)
        self.resume_entry.configure(state=state)
        self.dry_button.configure(state=state)
        for button in self.profile_buttons:
            button.configure(state=state)
        if running:
            self.status.set("Executando localmente. Os logs serão preservados em reports/test-runs/.")

    def _cancel(self) -> None:
        if self.process and self.process.poll() is None:
            self.process.terminate()
            self.status.set("Cancelamento solicitado. Aguardando o processo terminar.")

    def _open_reports(self) -> None:
        REPORTS.mkdir(parents=True, exist_ok=True)
        if os.name == "nt":
            os.startfile(REPORTS)  # type: ignore[attr-defined]
        else:
            messagebox.showinfo("Sentinela Local", f"Relatórios: {REPORTS}")

    def _append(self, text: str) -> None:
        self.output.configure(state="normal")
        self.output.insert("end", text)
        self.output.see("end")
        self.output.configure(state="disabled")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="confirma que a interface pode ser importada")
    args = parser.parse_args()
    if args.check:
        print("Sentinela GUI disponível.")
        return 0
    SentinelaGui().mainloop()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
