# Numerals

Numerals is a mate search chess program.

## Usage

Java 22 or later is required.

```
java -jar Numerals.jar
```

Numerals uses the [Universal Chess Interface](https://chessprogramming.org/UCI) protocol with a
minimal subset of commands: `uci`, `isready`, `position fen <fenstring>`, `go mate <x>`,
`go perft <x>`, `quit`.

## Example

> Sam Loyd, Bradford Courier 1878

### Input

```
position fen 8/8/8/1R6/8/kBp1p3/1qQ4K/1nBn4 w - - 0 1
go mate 2
```

### Output

```
info score mate 2 pv c2g2 b1d2 g2a8
bestmove c2g2
```

## Author

Ivan Denkovski is the author of Numerals.
