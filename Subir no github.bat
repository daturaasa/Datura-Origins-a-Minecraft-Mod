@echo off
chcp 65001 >nul
title Datura Origins - Subir no GitHub
cd /d "%~dp0"

set REPO=https://github.com/daturaasa/Datura-Origins-a-Minecraft-Mod.git
set SLUG=daturaasa/Datura-Origins-a-Minecraft-Mod
set AUTO=%~1

where git >nul 2>nul
if errorlevel 1 (
	echo [ERRO] Git nao encontrado. Instale o Git e tente de novo.
	pause
	exit /b 1
)

for /f "tokens=2 delims==" %%v in ('findstr /b "mod_version=" gradle.properties') do set VERSAO=%%v
set JAR=release\Datura-Origins-%VERSAO%-fabric-1.21.1.jar

echo ==========================================
echo  Enviar Datura Origins v%VERSAO% ao GitHub
echo ==========================================
echo.
echo Repositorio: %REPO%
echo O conteudo do branch main sera substituido por este projeto.
echo O historico e os releases antigos NAO serao apagados.
echo.
if /i "%AUTO%"=="S" (set OK=S) else set /p OK=Confirmar envio? (S/N): 
if /i not "%OK%"=="S" goto cancelado

if not exist .git (
	git init >nul
	git remote add origin %REPO%
)
git remote set-url origin %REPO%

echo.
echo Buscando o repositorio...
git fetch origin
if errorlevel 1 goto erro

git rev-parse --verify HEAD >nul 2>nul
if errorlevel 1 (
	git rev-parse --verify origin/main >nul 2>nul
	if not errorlevel 1 git reset origin/main >nul
)

git add -A
git diff --cached --quiet
if errorlevel 1 (
	git commit -m "Datura Origins v%VERSAO%"
	if errorlevel 1 goto erro
) else (
	echo Nada novo para enviar.
)

git branch -M main
git push -u origin main
if errorlevel 1 goto erro

echo.
echo [OK] Projeto enviado para o GitHub.
echo.

if not exist "%JAR%" (
	echo O .jar ainda nao existe. Rode o Compilar.bat antes para criar o release.
	goto fim
)

if /i "%AUTO%"=="S" (set REL=S) else set /p REL=Criar o release v%VERSAO% com o .jar? (S/N): 
if /i not "%REL%"=="S" goto fim

where gh >nul 2>nul
if errorlevel 1 (
	echo [AVISO] GitHub CLI nao encontrado. Crie o release pelo site anexando %JAR%
	echo e usando o texto de release\NOTAS-v%VERSAO%.md
	goto fim
)

gh release create v%VERSAO% "%JAR%" --repo %SLUG% --title "Datura Origins v%VERSAO%" --notes-file "release\NOTAS-v%VERSAO%.md" --latest
if errorlevel 1 goto erro
echo.
echo [OK] Release v%VERSAO% criado.
goto fim

:cancelado
echo Envio cancelado.
goto fim

:erro
echo.
echo [ERRO] Algo deu errado. Veja as mensagens acima.

:fim
echo.
pause
