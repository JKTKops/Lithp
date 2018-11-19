package ParserCombinator;
import ParserCombinator.Result.*;

import java.util.function.Supplier;

abstract class Combinators {
    static Parser alternate(Parser... list) {
        return new Parser(stream -> {
            for (Parser parser : list) {
                Result result = parser.run(stream);
                if (result instanceof Success) {
                    return result;
                }
            }
            return new Failure("Alternation failed", stream);
        });
    }

    static Parser always(String value) {
        return new Parser(stream -> new Success(value, stream));
    }

    static Parser never(String value) {
        return new Parser(stream -> new Failure(value, stream));
    }

    static Parser concat(Parser p1, Parser p2) {
        return p1.chain(vs -> p2.map(v -> vs + v));
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
                (v, s) -> new Success(null, stream)
        ));
    }

    static Parser lookahead(Parser parser) {
        return new Parser(stream -> parser.run(stream).fold(
                v -> new Success(v, stream),
                v -> new Failure(v, stream)));
    }

    static Parser star(Parser parser) {
        return new Parser(stream ->
                parser.run(stream)
                .fold(
                    (value, s) -> star(parser).map(rest -> value + rest).run(s),
                    (value, s) -> new Success("", stream)));
    }

    static Parser plus(Parser parser) {
        return new Parser(stream ->
                parser.run(stream)
                .fold(
                    (value, s) -> star(parser).map(rest -> value + rest).run(s),
                    (value, s) -> new Failure("'Plus' parser failed: " + value, stream)));
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
                e -> "Failed to match \"" + str + "\": " + e
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
                e -> "Failed to match character set \"" + charSet + "\": " + e
        );
    }

    static Parser dot() {
        return new Parser(stream -> {
            if (stream.length() == 0) {
                return new Failure("unexpected EOF", stream);
            }
            return new Success(stream.head(), stream.move(1));
        });
    }

    static Parser not(Parser parser) {
        return new Parser(stream -> parser.run(stream).fold(
                (value, s) -> new Failure("'Not' parser failed", stream),
                (value, s) ->
                        stream.length() > 0
                                ? new Success(stream.head(), stream.move(1))
                                : new Failure("unexpected eof", stream)));
    }

    static Parser accept(char c) {
        return new Parser(stream -> {
            if (stream.length() == 0) {
                return new Failure("unexpected EOF", stream);
            }
            String value = stream.head();
            if (value.equals(((Character) c).toString())) {
                return new Success(value, stream.move(1));
            }
            return new Failure("\"" + value + "\" did not match \"" + c + "\"", stream);
        });
    }

    static Parser eof() {
        return new Parser(stream -> stream.length() == 0 ? new Success("", stream) : new Failure("'eof' failed.", stream));
    }

    static Parser delayed(Supplier<Parser> parserSupplier) {
        return new Parser(stream -> parserSupplier.get().run(stream));
    }
}
