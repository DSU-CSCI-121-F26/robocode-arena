@echo off
REM Runs Robocode battles between the bots in this repo. See README.
cd /d "%~dp0"
java tools\Tourney.java %*
