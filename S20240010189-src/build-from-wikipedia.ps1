param([int]$ArticleCount = 20)
$ErrorActionPreference = 'Stop'
chcp 65001 | Out-Null
javac -encoding UTF-8 -d build src\*.java
java -cp build FetchTeluguWikipedia data\telugu_wikipedia $ArticleCount
java -cp build BuildIndex data\telugu_wikipedia generated_index
