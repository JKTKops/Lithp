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
        return new Failure(Symbol.value("invalid input to parser"), new Stream(""));
    }

    Parser parent(String nonterminal) {
        return new Parser(stream -> parse.apply(stream).map(list -> {
            list.add(0, Symbol.childMarker());
            list.add(0, Symbol.nonterminal(nonterminal));
            list.add(Symbol.parentMarker());
            return list;
        }));
    }
    Parser literal() {
        return new Parser(stream -> parse.apply(stream)).collapse()/*.map(list -> {
            if (list.size() != 1) { throw new IllegalStateException("Literals can only have one element."); }
            if (list.get(0).getType() != Symbol.SymbolType.VALUE) { throw new IllegalStateException("Literals can only be values."); }
            list.set(0, Symbol.value("'" + list.get(0).toString() + "'"));
            return list;
        })*/;
    }
    Parser collapse() {
        return new Parser(stream -> parse.apply(stream).map(list -> {
            List<Symbol> collapsed = new ArrayList<>();
            collapsed.add(Symbol.value(list.stream().map(symbol ->
                    symbol.getType() == Symbol.SymbolType.VALUE
                            ? symbol.getValue()
                            : ""
            ).reduce("", (a, b) -> a + b)));
            return collapsed;
        }));
    }
    Parser ignore() {
        return new Parser(stream -> parse.apply(stream).map(list -> new ArrayList<>()));
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
