package ParserCombinator;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

abstract class Result {
    String value;
    Stream rest;

    Result(String v, Stream r) {
        value = v;
        rest = r;
    }

    abstract Result map(Function<String, String> fn);
    abstract Result bimap(Function<String, String> success, Function<String, String> failure);
    abstract Result chain(BiFunction<String, Stream, Result> fn);
    abstract Result fold(BiFunction<String, Stream, Result> success, BiFunction<String, Stream, Result> failure);
    abstract Result fold(Consumer<String> success, Consumer<String> failure);

    static class Success extends Result {
        Success(String value, Stream rest) {
            super(value, rest);
        }

        Result map(Function<String, String> fn) {
            return new Success(fn.apply(value), rest);
        }
        Result bimap(Function<String, String> success, Function<String, String> failure) {
            return new Success(success.apply(value), rest);
        }
        Result chain(BiFunction<String, Stream, Result> fn) {
            return fn.apply(value, rest);
        }
        Result fold(BiFunction<String, Stream, Result> success, BiFunction<String, Stream, Result> failure) {
            return success.apply(value, rest);
        }
        Result fold(Consumer<String> success, Consumer<String> failure) {
            success.accept(value);
            return this;
        }
    }

    static class Failure extends Result {
        Failure(String value, Stream rest) {
            super(value, rest);
        }


        Result map(Function<String, String> fn) {
            return this;
        }
        Result bimap(Function<String, String> success, Function<String, String> failure) {
            return new Failure(failure.apply(value), rest);
        }
        Result chain(BiFunction<String, Stream, Result> fn) {
            return this;
        }
        Result fold(BiFunction<String, Stream, Result> success, BiFunction<String, Stream, Result> failure) {
            return failure.apply(value, rest);
        }
        Result fold(Consumer<String> success, Consumer<String> failure) {
            failure.accept(value);
            return this;
        }
    }
}
