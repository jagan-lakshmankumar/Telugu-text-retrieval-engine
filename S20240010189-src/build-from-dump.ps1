param(
    [string]$DumpPath = "C:\Users\miste\Downloads\tewiki-20261101-pages-articles-multistream.xml.bz2",
    [int]$MaxDocuments = 1000
)
$ErrorActionPreference = 'Stop'
chcp 65001 | Out-Null
javac -encoding UTF-8 -d build src\*.java
java -cp build WikipediaDumpIndexer $DumpPath generated_index $MaxDocuments
java -cp build RareTermReport generated_index output.txt
