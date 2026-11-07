# Telugu-text-retrieval-engine

## Positional Index for Telugu Wikipedia
### IR Assignment 1 — M2026
### Name : P JNSV Lakshmankumar
### Roll number: S20240010189

1. Dataset
The index was built from the supplied Telugu Wikipedia multistream pages-articles dump:

    tewiki-20261101-pages-articles-multistream.xml.bz2

Official source: https://dumps.wikimedia.org/tewiki/20261101/tewiki-20261101-pages-articles-multistream.xml.bz2

The dump is not included in this submission, as instructed. `WikipediaDumpIndexer.java` streams and decompresses it without expanding the entire XML file or loading it into memory. The verified submission run indexed the first 100 nonempty article pages, producing 562 vocabulary terms.

2. Requirements
* JDK 17 or newer.
* Python 3 (`py -3`) for the standard-library `bz2` streaming decompressor on Windows. No Java third-party library is required.

3. Build from the supplied dump
Open PowerShell in this folder and run:

    .\build-from-dump.ps1 -DumpPath "C:\Users\miste\Downloads\tewiki-20261101-pages-articles-multistream.xml.bz2" -MaxDocuments 100

The script compiles Java sources, streams the dump, writes `generated_index\vocabulary.tsv` and `generated_index\postings.dat`, and generates `output.txt`. Increase `MaxDocuments` to index a larger sample. The program does not include the dump or generated index in the submission archive.

4. Search
After building:

    chcp 65001
    java -cp build SearchIndex generated_index term అమలయింది
    java -cp build SearchIndex generated_index phrase "కృత్రిమ మేధస్సు"

Term queries print DF, CF, and document-position postings. Phrase queries check adjacency using positional lists.

5. Modules
* TeluguTokenizer.java — Unicode Telugu/English normalization and tokenization.
* WikipediaDumpIndexer.java — streaming `.xml.bz2` decompression, StAX XML page extraction, basic wiki-markup cleanup, and positional indexing.
* BuildIndex.java — alternative builder for a folder of UTF-8 text documents.
* DiskIndex.java — keeps vocabulary/DF/CF in RAM and persists seekable postings lists on disk.
* SearchIndex.java — single-term and exact phrase retrieval.
* RareTermReport.java — produces ten rare Telugu-term positional-index examples from the disk index.
* IndexTests.java — five repeatable checks using the included smoke corpus.
* build-from-dump.ps1 — build and report command for this Telugu Wikipedia dump.

6. Index design
`vocabulary.tsv` contains each term's document frequency (DF), collection frequency (CF), byte offset, and byte length. `postings.dat` is the secondary-storage postings file, with a record such as:

    అమలయింది\twiki183:2

All vocabulary metadata is read into main memory. For a lookup, only the needed byte range is sought and read from `postings.dat`; the complete postings file is never loaded into memory. Positions are zero-based normalized-token positions within each page.

7. Included verification corpus
`src\sample_corpus` contains ten small original Telugu documents only for deterministic tests. It is not the assignment dataset. Run the five tests:

    .\build-and-test.ps1

8. Submission contents
* S20240010189-src
* output.txt

The source archive contains Java source, PowerShell scripts, and the sample smoke corpus—never the Wikipedia dump or generated index.
