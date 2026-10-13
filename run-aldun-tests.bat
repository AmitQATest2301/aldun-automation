@echo off
rem ============================================================
rem  Aldun automation - runs the full positive flow:
rem  login -> check out (if needed) -> check in -> Log Interaction
rem  The summary report opens automatically when the browser closes.
rem  Double-click this file, or let the Windows scheduled task run it.
rem ============================================================
cd /d "%~dp0"

if not exist logs mkdir logs
echo [%date% %time%] Starting Aldun tests...
call mvn -B clean test > logs\last-run-console.log 2>&1
set RESULT=%ERRORLEVEL%

if %RESULT%==0 (
    echo [%date% %time%] All tests PASSED.
) else (
    echo [%date% %time%] Some tests FAILED - see the report and logs\last-run-console.log
)
exit /b %RESULT%
