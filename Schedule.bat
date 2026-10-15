@echo off
rem ============================================================
rem  Creates a Windows scheduled task that runs run-aldun-tests.bat
rem  every day at RUN_TIME. Double-click once to set it up.
rem  The task runs as you, while you are logged in, so Edge is visible.
rem  Change RUN_TIME below (24-hour HH:MM) and double-click again to update.
rem ============================================================
set RUN_TIME=09:05
set TASK_NAME=Aldun Automation Tests

schtasks /Create /TN "%TASK_NAME%" /TR "\"%~dp0run-aldun-tests.bat\"" /SC DAILY /ST %RUN_TIME% /F
if %ERRORLEVEL%==0 (
    echo.
    echo Scheduled "%TASK_NAME%" every day at %RUN_TIME%.
    echo To run it right now:  schtasks /Run /TN "%TASK_NAME%"
    echo To remove it:         double-click remove-schedule.bat
) else (
    echo.
    echo Could not create the scheduled task.
)
pause
