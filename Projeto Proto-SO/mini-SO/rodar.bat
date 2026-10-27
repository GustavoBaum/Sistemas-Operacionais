@echo off
REM Compila e executa o Mini-SO no Windows (requer JDK 17+ no PATH).
REM Uso: dê duplo clique ou, no terminal da pasta do projeto, digite: rodar
chcp 65001 >nul
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERRO] javac nao encontrado. Instale um JDK 17 ou superior ^(nao basta o JRE^).
    echo        Sugestao: https://adoptium.net  ^(Temurin 21^)
    pause
    exit /b 1
)

if not exist bin mkdir bin
javac -encoding UTF-8 -sourcepath src -d bin src\minikernel\app\Main.java
if errorlevel 1 (
    echo.
    echo [ERRO] Falha na compilacao. Confira a versao com: javac -version
    pause
    exit /b 1
)

java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp bin minikernel.app.Main
pause
