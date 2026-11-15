@echo off
chcp 65001 >nul
title Datura Origins - Compilar
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
	echo [ERRO] Java nao encontrado. Instale o JDK 21 e tente de novo.
	pause
	exit /b 1
)

for /f "tokens=2 delims==" %%v in ('findstr /b "mod_version=" gradle.properties') do set VERSAO=%%v

echo ==========================================
echo  Datura Origins v%VERSAO% - Fabric 1.21.1
echo ==========================================
echo.
echo Compilando, isso pode levar alguns minutos...
echo.

call gradlew.bat :mc1211:build --console=plain
if errorlevel 1 goto falhou

set JAR=mc1211\build\libs\datura-origins-fabric-1.21.1-%VERSAO%.jar
if not exist "%JAR%" goto falhou

if not exist release mkdir release
copy /y "%JAR%" "release\Datura-Origins-%VERSAO%-fabric-1.21.1.jar" >nul

echo.
echo [OK] Compilacao concluida com sucesso!
echo Arquivo gerado: release\Datura-Origins-%VERSAO%-fabric-1.21.1.jar
echo.
pause
exit /b 0

:falhou
echo.
echo [ERRO] A compilacao falhou. Veja as mensagens acima para saber o motivo.
echo.
pause
exit /b 1
