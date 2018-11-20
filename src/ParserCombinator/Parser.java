package ParserCombinator;

import java.util.ArrayList;
import java.util.List;
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

    private Function<Stream, Result> parse;

    Parser(Function<Stream, Result> setParse) {
        parse = setParse;
    }

    Result run(Object input) {
        if (input instanceof Stream) {
            return parse.apply((Stream) input);
        } else if (input instanceof String) {
            return parse.apply(new Stream((String) input));
        }
        List<Symbol> error = new ArrayList<>();
        error.add(Symbol.value("invalid input to parser"));
        return new Failure(error, new Stream(""));
    }

    Parser collapse() {
        return new Parser(stream -> parse.apply(stream).map(list -> {
            List<Symbol> collapsed = new ArrayList<>();
            collapsed.add(Symbol.value(list.stream().map(symbol -> symbol.toString()).reduce("", (a, b) -> a + b)));
            return collapsed;
        }));
    }
    Parser map(Function<List<Symbol>, List<Symbol>> fn) {
        return new Parser(stream -> parse.apply(stream).map(fn));
    }
    Parser bimap(Function<List<Symbol>, List<Symbol>> success, Function<List<Symbol>, List<Symbol>> failure) {
        return new Parser(stream -> parse.apply(stream).bimap(success, failure));
    }
    Parser chain(Function<List<Symbol>, Parser> f) {
        return new Parser(stream -> parse.apply(stream).chain((v, s) -> f.apply(v).run(s))); // v = value, s = stream of result
    }
    Parser fold(BiFunction<List<Symbol>, Stream, Result> success, BiFunction<List<Symbol>, Stream, Result> failure) {
        return new Parser(stream -> parse.apply(stream).fold(success, failure));
    }
}
