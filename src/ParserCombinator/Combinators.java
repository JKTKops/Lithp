package ParserCombinator;
import ParserCombinator.Result.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

abstract class Combinators {
    static Parser alternate(Parser... list) {
        return new Parser(stream -> {
            List<Symbol> error = new ArrayList<>();
            for (Parser parser : list) {
                Result result = parser.run(stream);
                if (result instanceof Success) {
                    return result;
                } else if (result instanceof Failure) {
                    String e = "";
                    for (Symbol symbol : result.value) {
                        e += (e.endsWith(":") ? " " : "; ") + symbol.toString();
                    }
                    error.add(Symbol.value(e.substring(2)));
                }
            }
            return new Failure(error, stream);
        });
    }

    static Parser always(String value) {
        return new Parser(stream -> new Success(Symbol.value(value), stream));
    }

    static Parser never(String value) {
        return new Parser(stream -> new Failure(Symbol.value(value), stream));
    }

    static Parser concat(Parser p1, Parser p2) {
        return p1.chain(vs -> p2.map(v -> { vs.addAll(v); return vs; }));
    }

    static Parser sequence(Parser first, Parser... list) {
        Parser acc = first;
        for (Parser parser : list) {
            acc = concat(acc, parser);
        }
        return acc;
    }

    static Parser maybe(Parser parser) {
        return new Parser(stream -> parser.run(stream).fold(
                (v, s) -> new Success(v, s),
                // If the parse fails, return a Success with empty value
                // that does NOT consume any of the input stream
                (v, s) -> new Success(new ArrayList<>(), stream)
        ));
    }

    static Parser lookahead(Parser parser) {
        return new Parser(stream -> parser.run(stream).fold(
                (v, s) -> new Success(new ArrayList<>(), stream),
                (v, s) -> new Failure(v, stream)));
    }

    static Parser star(Parser parser) {
        return new Parser(stream ->
                parser.run(stream)
                .fold(
                    (value, s) -> star(parser).map(rest -> {value.addAll(rest); return value; }).run(s),
                    (value, s) -> new Success(new ArrayList<>(), stream)));
    }

    static Parser plus(Parser parser) {
        return new Parser(stream ->
                parser.run(stream)
                .fold(
                    (value, s) -> star(parser).map(rest -> { value.addAll(rest); return value; }).run(s),
                    (error, s) -> {
                        error.add(0, Symbol.value("'Plus' parser failed:"));
                        return new Failure(error, stream);
                    }));
    }

    static Parser string(final String str) {
        if (str.length() == 0) {
            return always("");
        }
        Parser first = accept(str.charAt(0));
        String restStr = str.substring(1);
        Parser[] list = new Parser[restStr.length()];
        for (int i = 0; i < restStr.length(); i++) {
            list[i] = accept(restStr.charAt(i));
        }

        return sequence(first, list).bimap(
                v -> v,
                e -> {
                    e.add(0, Symbol.value("Failed to match \"" + str + "\":"));
                    return e;
                }
        );
    }

    static Parser set(final String charSet) {
        if (charSet.length() == 0) {
            return never("'set' failed: empty set");
        }
        Parser[] list = new Parser[charSet.length()];
        for (int i = 0; i < charSet.length(); i++) {
            list[i] = accept(charSet.charAt(i));
        }
        return alternate(list).bimap(
                v -> v,
                e -> {
                    e.add(0, Symbol.value("Failed to match character set \"" + charSet + "\":"));
                    return e;
                }
        );
    }

    static Parser dot() {
        return new Parser(stream -> stream.length() == 0
                                    ? new Failure(Symbol.value("unexpected EOL"), stream)
                                    : new Success(Symbol.value(stream.head()), stream.move(1)));
    }

    static Parser not(Parser parser) {
        return new Parser(stream -> parser.run(stream).fold(
                (value, s) -> { value.add(0, Symbol.value("'Not' parser failed; matched:"));
                                return new Failure(value, stream); },
                (value, s) -> stream.length() > 0
                                ? new Success(Symbol.value(stream.head()), stream.move(1))
                                : new Success(new ArrayList<>(), stream)));
    }

    static Parser accept(char c) {
        return new Parser(stream -> {
            if (stream.length() == 0) {
                return new Failure(Symbol.value("unexpected EOL"), stream);
            }
            String value = stream.head();
            if (value.equals(((Character) c).toString())) {
                return new Success(Symbol.value(value), stream.move(1));
            }
            return new Failure(Symbol.value("\"" + value + "\" did not match \"" + c + "\""), stream);
        });
    }

    static Parser eof() {
        return new Parser(stream -> stream.length() == 0
                ? new Success(new ArrayList<>(), stream)
                : new Failure(Symbol.value("'eof' failed."), stream));
    }

    static Parser delayed(Supplier<Parser> parserSupplier) {
        return new Parser(stream -> parserSupplier.get().run(stream));
    }
}
