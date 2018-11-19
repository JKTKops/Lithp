package ParserCombinator;
import ParserCombinator.Result.*;

import java.util.Arrays;

public class ParserCombinator {
    private static Parser alternate(Parser... list) {
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

    private static Parser always(String value) {
        return new Parser(stream -> new Success(value, stream));
    }

    private static Parser never(String value) {
        return new Parser(stream -> new Failure(value, stream));
    }

    private static Parser concat(Parser p1, Parser p2) {
        return p1.chain(vs -> p2.map(v -> vs + ", " + v));
    }

    private static Parser sequence(Parser first, Parser... list) {
        Parser acc = first;
        for (Parser parser : list) {
            acc = concat(acc, parser);
        }
        return acc;
    }

    private static Parser maybe(Parser parser) {
        return new Parser(stream -> parser.run(stream).fold(
                (v, s) -> new Success(v, s),
                // If the parse fails, return a Success with empty value
                // that does NOT consume any of the input stream
                (v, s) -> new Success(null, stream)
        ));
    }

    private static Parser accept(char c) {
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

    public static void main(String[] args) {
        Parser AmaybeBC = sequence(accept('a'), maybe(accept('b')).map(v -> "maybe: b"), accept('c'))
        .map(v -> "[" + v + "]");

        Parser.run(AmaybeBC, "abc");
        Parser.run(AmaybeBC, "ac");
        Parser.run(AmaybeBC, "bc");
        Parser.run(AmaybeBC, "dog");
    }
}
