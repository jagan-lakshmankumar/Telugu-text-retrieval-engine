$ErrorActionPreference = 'Stop'
chcp 65001 | Out-Null
javac -encoding UTF-8 -d build src\*.java
Push-Location src
java -cp ..\build IndexTests
Pop-Location
