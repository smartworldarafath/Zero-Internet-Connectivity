@echo off
title Zero Network Connectivity - Desktop Web Server
echo ========================================================
echo   Starting Zero Network Connectivity Desktop Web...
echo ========================================================
cd /d "%~dp0desktop-web"
start http://localhost:5000
node server.js
pause
