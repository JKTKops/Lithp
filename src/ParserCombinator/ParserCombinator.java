package ParserCombinator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ParserCombinator.Combinators.*;

/**
 * Main class of the ParserCombinator package.
 * Represents a complex Parser constructed dynamically from an input grammar in BNF form.
 *
 * @author Max Kopinsky
 */
public class ParserCombinator {
    private Map<String, Parser> parsers = new HashMap<>();

    public ParserCombinator(String grammar) {
        // Note that this grammar is not all that similar to one that this combinator
        // would generate. It is optimized to build a direct AST rather than a
        // parse tree through use of .ignore() to remove syntax sugar of the BNF
        // as well as skipping redundant single-child chains.
        // Based on the BNF grammar on the BNF wikipedia page.
        Parser digit = set("0123456789");
        Parser letter = set("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        Parser symbol = set("| !#$%&()*+,-./:;>=<?@[\\]^_`{}~");
        Parser character = alternate(letter, digit, symbol);
        Parser character1 = alternate(character, accept('\''));
        Parser character2 = alternate(character, accept('"'));
        Parser text1 = star(character1);
        Parser text2 = star(character2);
        Parser rule_char = alternate(letter, digit, accept('-'));
        Parser opt_whitespace = star(accept(' ')).ignore();
        Parser line_end = concat(opt_whitespace, alternate(string("\n"), string(System.lineSeparator()), eof())).ignore();

        Parser literal = alternate(sequence(accept('"').ignore(), text1, accept('"').ignore()),
                                    sequence(accept('\'').ignore(), text2, accept('\'').ignore())).literal().parent("literal");
        Parser rule_name = sequence(accept('<').ignore(), concat(letter, star(rule_char)).literal().parent("rule-name"), accept('>').ignore());

        Parser term = alternate(literal, rule_name);
        Parser list = concat(term, star(concat(opt_whitespace, term))).parent("list");
        Parser expr = concat(list, star(sequence(
                opt_whitespace, string("|").ignore(),
                opt_whitespace, list))).parent("expression");
        Parser rule = sequence(
                opt_whitespace, rule_name.literal(),
                opt_whitespace, string("::=").ignore(),
                opt_whitespace, expr,
                line_end).parent("rule");
        Parser syntax = concat(concat(rule, star(rule)).parent("syntax"), eof());

        Parser.run(syntax, "<first-rule> ::= <first-production>  \n <second-rule> ::= <second-production> \n  " +
                        "<third-rule> ::= <prod-1> \"some literal\" | <prod-2>");

        System.out.println(new ParseTree(syntax.run("<first-rule> ::= <first-production>  \n <second-rule> ::= <second-production> \n  " +
                "<third-rule> ::= <prod-1> \"some literal\" | <prod-2>")));
        System.out.println(new ParseTree(syntax.run("first-rule ::= \"literal\"")));
    }



    public static void main(String[] args) {
        ParserCombinator test = new ParserCombinator("");
        Map<String, Parser> parsers = new HashMap<>();

        Parser letter = set("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        Parser word = plus(letter).parent("word");
        Parser sentence = concat(word, plus(concat(string(" "), word))).parent("sentence");
        System.out.println(sentence.run("These are some words"));
        System.out.println(new ParseTree(sentence.run("These are some words")));
        System.out.println(new ParseTree(sentence.literal().run("These are some words")));

        /*Parser number = concat(set("123456789"), star(set("0123456789"))).literal().parent("number");
        Parser op = set("+*-/").literal().parent("op");
        Parser term = alternate(sequence(string("("), delayed(() -> parsers.get("expr")), string(")")),
                number).parent("term");
        Parser expr = alternate(
                sequence(term, string(" ").ignore(), op, string(" ").ignore(), term), term
            ).parent("expr");

        parsers.put("expr", expr);
        Parser math = concat(expr.parent("math"), eof());

        System.out.println(new ParseTree().buildTree(math.run("((1 + 2) / 3)")));*/
    }
}
