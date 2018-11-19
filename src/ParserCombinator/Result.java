package ParserCombinator;

abstract class Result {
    String value;
    Stream rest;

    class Success extends Result {
        // lambdas
    }

    class Failure extends Result {
        // more lambdas
    }
}
