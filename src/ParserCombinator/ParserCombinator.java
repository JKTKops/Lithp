package ParserCombinator;

import java.util.HashMap;
import java.util.Map;

import static ParserCombinator.Combinators.*;

public class ParserCombinator {
    private static Map<String, Parser> parsers = new HashMap<>();

    public static void main(String[] args) {
        Parser number = plus(set("0123456789"));
        Parser op = set("+-*/");
        Parser term = alternate(sequence(accept('('), delayed(() -> parsers.get("expr")), accept(')')), number);
        Parser expr = sequence(term, accept(' '), op, accept(' '), term);
        parsers.put("expr", expr);

        Parser.run(expr, "(1 * 2) + (3 / 4)");
        Parser.run(expr, "((1 + 2) / 3) + (2 * 3)");
    }
}
