import Lithp.LithpEvaluator;
import ParserCombinator.*;
import java.util.Scanner;

/**
 * Main class of the REPL interpreter.
 * Maintains the Lithp grammar and handles the REPL.
 *
 * @author Max Kopinsky
 */
public class REPL {
    public static void main(String[] args) {
        LithpEvaluator evaluator = new LithpEvaluator();
        ParserCombinator lithp = new ParserCombinator(
                "<number> ::= /-?[0-9]+/\n" +
                        "<symbol> ::= /[a-zA-Z0-9_+\\-*\\/\\\\=<>?~!@#$%^&|]+/\n" +
                        "<ws>i ::= ' '*\n" +
                        "<expr> ::= <number> <ws> | <symbol> <ws> | <sexpr> <ws> | <qexpr> <ws>\n" +
                        "<sexpr> ::= '('i <ws> <expr>* ')'i\n" +
                        "<qexpr> ::= \"'(\"i <ws> <expr>* ')'i\n" +
                        "<lithp> ::= <expr>");
        String code;
        Scanner in = new Scanner(System.in);

        System.out.println("Lithp version 0.0.2.10\nUse (exit) to exit.");

        while(true) {
            System.out.print("Lithp>");

            code = in.nextLine();
            ParseTree parseTree = lithp.run(code);
            if (!parseTree.assertSuccess()) {
                System.out.println("Couldn't parse that input: " + parseTree.toString().substring(2));
                continue;
            }
            if (evaluator.eval(parseTree) < 0) break;
        }
    }
}
