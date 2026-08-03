@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0regen-openapi.ps1" %*
