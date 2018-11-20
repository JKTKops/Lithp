package ParserCombinator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

abstract class Result {
    List<Symbol> value;
    Stream rest;

    Result(List<Symbol> v, Stream r) {
        value = v;
        rest = r;
    }
    Result(Symbol v, Stream r) {
        List<Symbol> newList = new ArrayList<>();
        newList.add(v);
        value = newList;
        rest = r;
    }

    abstract Result map(Function<List<Symbol>, List<Symbol>> fn);
    abstract Result bimap(Function<List<Symbol>, List<Symbol>> success, Function<List<Symbol>, List<Symbol>> failure);
    abstract Result chain(BiFunction<List<Symbol>, Stream, Result> fn);
    abstract Result fold(BiFunction<List<Symbol>, Stream, Result> success, BiFunction<List<Symbol>, Stream, Result> failure);
    abstract Result fold(Consumer<List<Symbol>> success, Consumer<List<Symbol>> failure);

    static class Success extends Result {
        Success(List<Symbol> value, Stream rest) {
            super(value, rest);
        }
        Success(Symbol v, Stream rest) {
            super(v, rest);
        }

        Result map(Function<List<Symbol>, List<Symbol>> fn) {
            return new Success(fn.apply(value), rest);
        }
        Result bimap(Function<List<Symbol>, List<Symbol>> success, Function<List<Symbol>, List<Symbol>> failure) {
            return new Success(success.apply(value), rest);
        }
        Result chain(BiFunction<List<Symbol>, Stream, Result> fn) {
            return fn.apply(value, rest);
        }
        Result fold(BiFunction<List<Symbol>, Stream, Result> success, BiFunction<List<Symbol>, Stream, Result> failure) {
            return success.apply(value, rest);
        }
        Result fold(Consumer<List<Symbol>> success, Consumer<List<Symbol>> failure) {
            success.accept(value);
            return this;
        }
    }

    static class Failure extends Result {
        Failure(List<Symbol> value, Stream rest) {
            super(value, rest);
        }
        Failure(Symbol value, Stream rest) {
            super(value, rest);
        }

        Result map(Function<List<Symbol>, List<Symbol>> fn) {
            return this;
        }
        Result bimap(Function<List<Symbol>, List<Symbol>> success, Function<List<Symbol>, List<Symbol>> failure) {
            return new Failure(failure.apply(value), rest);
        }
        Result chain(BiFunction<List<Symbol>, Stream, Result> fn) {
            return this;
        }
        Result fold(BiFunction<List<Symbol>, Stream, Result> success, BiFunction<List<Symbol>, Stream, Result> failure) {
            return failure.apply(value, rest);
        }
        Result fold(Consumer<List<Symbol>> success, Consumer<List<Symbol>> failure) {
            failure.accept(value);
            return this;
        }
    }
}
