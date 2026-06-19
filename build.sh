#!/usr/bin/env bash
#
# Compiles the POS system, packages a runnable jar, then compiles and runs the
# test suite. Requires only a JDK 11+ on the PATH — no Maven, no network.
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"

rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/test-classes"

echo ">> Compiling main sources..."
find "$ROOT/src/main/java" -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 -d "$OUT/classes" @"$OUT/sources.txt"

if [ -d "$ROOT/src/main/resources" ]; then
  echo ">> Bundling resources (web UI)..."
  cp -r "$ROOT/src/main/resources/." "$OUT/classes/"
fi

echo ">> Packaging runnable jar..."
jar cfe "$OUT/pos-system.jar" com.rudra.pos.Main -C "$OUT/classes" .

echo ">> Compiling tests..."
find "$ROOT/src/test/java" -name '*.java' > "$OUT/test-sources.txt"
javac -encoding UTF-8 -cp "$OUT/classes" -d "$OUT/test-classes" @"$OUT/test-sources.txt"

echo ">> Running test suite..."
java -cp "$OUT/classes:$OUT/test-classes" com.rudra.pos.TestMain

echo ""
echo "Build OK.  Run the app with:  java -jar out/pos-system.jar"
echo "Or the scripted demo with:    java -jar out/pos-system.jar --demo"
