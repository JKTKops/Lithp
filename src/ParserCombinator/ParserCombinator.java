package ParserCombinator;
import ParserCombinator.Result.*;

public class ParserCombinator {
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

    public static void main(String[] args) {
        accept('a')
                .run("b")
                .fold(v -> System.out.println("success: " + v),
                        e -> System.out.println("error: " + e));
    }
}
