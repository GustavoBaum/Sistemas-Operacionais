#!/bin/sh
# Compila e executa o Mini-SO no Linux/macOS (requer JDK 17+).
cd "$(dirname "$0")" || exit 1

if ! command -v javac >/dev/null 2>&1; then
    echo "[ERRO] javac não encontrado. Instale um JDK 17 ou superior (não basta o JRE)."
    exit 1
fi

mkdir -p bin
javac -encoding UTF-8 -sourcepath src -d bin src/minikernel/app/Main.java || {
    echo "[ERRO] Falha na compilação. Confira a versão com: javac -version"
    exit 1
}

exec java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp bin minikernel.app.Main
