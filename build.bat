@echo off
REM Compiles the POS system, packages a runnable jar, then compiles and runs
REM the test suite. Requires only a JDK 11+ on the PATH (no Maven, no network).
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
set "OUT=%ROOT%out"

if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%\classes"
mkdir "%OUT%\test-classes"

echo ^>^> Compiling main sources...
dir /s /b "%ROOT%src\main\java\*.java" > "%OUT%\sources.txt"
javac -encoding UTF-8 -d "%OUT%\classes" @"%OUT%\sources.txt"
if errorlevel 1 exit /b 1

echo ^>^> Packaging runnable jar...
jar cfe "%OUT%\pos-system.jar" com.rudra.pos.Main -C "%OUT%\classes" .
if errorlevel 1 exit /b 1

echo ^>^> Compiling tests...
dir /s /b "%ROOT%src\test\java\*.java" > "%OUT%\test-sources.txt"
javac -encoding UTF-8 -cp "%OUT%\classes" -d "%OUT%\test-classes" @"%OUT%\test-sources.txt"
if errorlevel 1 exit /b 1

echo ^>^> Running test suite...
java -cp "%OUT%\classes;%OUT%\test-classes" com.rudra.pos.TestMain
if errorlevel 1 exit /b 1

echo.
echo Build OK.  Run the app with:  java -jar out\pos-system.jar
echo Or the scripted demo with:    java -jar out\pos-system.jar --demo
endlocal
