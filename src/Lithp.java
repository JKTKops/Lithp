import ParserCombinator.*;
import java.util.Scanner;

/**
 * Main class of the Lithp interpreter.
 * Handles the REPL.
 *
 * @author Max Kopinsky
 */
public class Lithp {
    public static void main(String[] args) {
        ParserCombinator test = new ParserCombinator(
                "<recur-1> ::= \"this\" / / \"should work\" /!/\n" +
                        "<recur-2> ::= <recur-1>\n" +
                        "<rule-3> ::= <recur-2>");
        System.out.println(test.run("this should work!"));

        ParserCombinator math = new ParserCombinator(
                "<first-digit> ::= /[1-9]/\n" +
                        "<other-digits> ::= /[0-9]/\n" +
                        "<number>l ::= <first-digit> <other-digits>*\n" +
                        "<op> ::= /[+\\-*\\/]/\n" +
                        "<term> ::= '('i <expr> ')'i | <number>\n" +
                        "<expr> ::= <term> ' 'i <op> ' 'i <term> | <term>\n" +
                        "<math> ::= <expr>");
        System.out.println(math.run("(1 + 2) / 304"));
        String input;
        Scanner in = new Scanner(System.in);

        System.out.println("Lithp version 0.0.0.2\nUse exit() to exit.");

        boolean done = false;
        while(!done) {
            System.out.print("Lithp>");

            input = in.nextLine();
            done = input.equals("exit()"); // todo: make this a builtin function
            System.out.println(String.format("%s", input));
        }
    }
}
