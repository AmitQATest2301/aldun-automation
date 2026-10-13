@echo off
rem Removes the daily "Aldun Automation Tests" scheduled task created by schedule-daily.bat
schtasks /Delete /TN "Aldun Automation Tests" /F
pause
