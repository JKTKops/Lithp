package ParserCombinator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ParserCombinator.Combinators.*;

public class ParserCombinator {
    private Map<String, Parser> parsers = new HashMap<>();

    public ParserCombinator(String grammar) {
        ParseTree parsedGrammar = new ParseTree("syntax");

        // Note that this grammar is not all that similar to one that this combinator
        // would generate. It is optimized to build a direct AST rather than a
        // parse tree through use of .ignore() to remove syntax sugar of the BNF
        // as well as skipping all redundant single-child chains.
        // Based on the BNF grammar on the BNF wikipedia page.
        final Map<String, Parser> initParsers = new HashMap<>();
        Parser digit = set("0123456789");
        Parser letter = set("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        Parser symbol = set("| !#$%&()*+,-./:;>=<?@[\\]^_`{}~");
        Parser character = alternate(letter, digit, symbol);
        Parser character1 = alternate(character, accept('\''));
        Parser character2 = alternate(character, accept('"'));
        Parser text1 = alternate(concat(character1, delayed(() -> initParsers.get("text1"))), always(""));
        Parser text2 = alternate(concat(character2, delayed(() -> initParsers.get("text2"))), always(""));
        initParsers.put("text1", text1);
        initParsers.put("text2", text2);
        Parser rule_char = alternate(letter, digit, accept('-'));
        Parser opt_whitespace = alternate(concat(accept(' '), delayed(() -> initParsers.get("opt-space"))), always("")).ignore();
        initParsers.put("opt-space", opt_whitespace);
        Parser line_end = concat(opt_whitespace, alternate(string("\n"), string(System.lineSeparator()), eof())).ignore();

        Parser literal = alternate(sequence(accept('"').ignore(), text1, accept('"').ignore()),
                                    sequence(accept('\'').ignore(), text2, accept('\'').ignore())).literal().parent("literal");
        Parser rule_name = sequence(accept('<').ignore(), concat(letter, star(rule_char)).literal().parent("rule-name"), accept('>').ignore());

        Parser term = alternate(literal, rule_name);
        Parser list = concat(term, star(concat(opt_whitespace, term.sibling()))).parent("list");
        Parser expr = concat(list, star(sequence(
                opt_whitespace, accept('|').literal().sibling(),
                opt_whitespace, list.sibling()))).parent("expression");
        Parser rule = sequence(
                opt_whitespace, rule_name,
                opt_whitespace, string("::=").literal().sibling(),
                opt_whitespace, expr.sibling(),
                line_end).parent("rule");
        Parser syntax = plus(rule).parent("syntax");

        Parser.run(term, "\"test\"");
        Parser.run(expr, "<this-is-a-rule-name> <also-a-rule> | <alternative-rule-1> '?' <alternative-rule-2>");
        Parser.run(rule, "<rule> ::= \"goes to\" <production-1> | <production-2>");
        Parser.run(syntax, "<first-rule> ::= <first-production>  \n  <second-rule> ::= <second-production>");
    }

    public static void main(String[] args) {
        ParserCombinator test = new ParserCombinator("");
        /*Map<String, Parser> parsers = new HashMap<>();
        Parser number = concat(set("123456789"), star(set("0123456789"))).literal().parent("number");
        Parser op = set("+*-/").literal().parent("op");
        Parser term = alternate(sequence(accept('(').literal(), delayed(() -> parsers.get("expr")).sibling(), accept(')').literal().sibling()),
                number).parent("term");
        Parser expr = alternate(
                sequence(term, accept(' ').literal().sibling(), op.sibling(), accept(' ').literal().sibling(), term.sibling()), term
            ).parent("expr");

        parsers.put("expr", expr);
        Parser math = concat(expr.parent("math"), eof());

        Parser.run(math, "((1 + 2) / 3)");*/
    }
}
