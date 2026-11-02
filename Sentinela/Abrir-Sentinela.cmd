@echo off
setlocal

where py >nul 2>nul
if not errorlevel 1 (
    py -3 "%~dp0sentinela_gui.py" %*
    set "CODE=%ERRORLEVEL%"
) else (
    python "%~dp0sentinela_gui.py" %*
    set "CODE=%ERRORLEVEL%"
)

if not "%CODE%"=="0" pause
exit /b %CODE%
