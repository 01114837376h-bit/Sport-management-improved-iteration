@echo off
REM ─────────────────────────────────────────────────────────────────
REM  Sports Management System — Build & Run Script (Windows)
REM
REM  Requirements: JDK 17 or higher installed, 'javac' on PATH.
REM  Check with:   java -version
REM  Download JDK: https://adoptium.net  (free, no account needed)
REM ─────────────────────────────────────────────────────────────────

set SRC=src\main\java
set OUT=out

echo [1/2] Compiling...
if not exist %OUT% mkdir %OUT%

javac -d %OUT% -sourcepath %SRC% ^
  %SRC%\sports\entities\Member.java ^
  %SRC%\sports\entities\Club.java ^
  %SRC%\sports\entities\Sport.java ^
  %SRC%\sports\trie\TrieNode.java ^
  %SRC%\sports\trie\Trie.java ^
  %SRC%\sports\sorting\SortingAlgorithms.java ^
  %SRC%\sports\search\SearchEngine.java ^
  %SRC%\sports\DataStore.java ^
  %SRC%\sports\gui\MainWindow.java ^
  %SRC%\sports\Main.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo *** COMPILATION FAILED. Check errors above. ***
    pause
    exit /b 1
)

echo [2/2] Running...
java -cp %OUT% sports.Main
