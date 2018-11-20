package ParserCombinator;

import java.util.function.BiFunction;
import java.util.function.Function;
import ParserCombinator.Result.Failure;

class Parser {
    static Result run(Parser p, Object i) {
        return p.run(i).fold(
                v -> System.out.println("success: " + v),
                e -> System.out.println("error: " + e)
        );
    }

    protected Function<Stream, Result> parse;

    Parser(Function<Stream, Result> setParse) {
        parse = setParse;
    }

    Result run(Object input) {
        if (input instanceof Stream) {
            return parse.apply((Stream) input);
        } else if (input instanceof String) {
            return parse.apply(new Stream((String) input));
        }
        return new Failure("invalid input to parser", new Stream(""));
    }

    Parser map(Function<String, String> fn) {
        return new Parser(stream -> parse.apply(stream).map(fn));
    }
    Parser bimap(Function<String, String> success, Function<String, String> failure) {
        return new Parser(stream -> parse.apply(stream).bimap(success, failure));
    }
    Parser chain(Function<String, Parser> f) {
        return new Parser(stream -> parse.apply(stream).chain((v, s) -> f.apply(v).run(s))); // v = value, s = stream of result
    }
    Parser fold(BiFunction<String, Stream, Result> success, BiFunction<String, Stream, Result> failure) {
        return new Parser(stream -> parse.apply(stream).fold(success, failure));
    }
}
