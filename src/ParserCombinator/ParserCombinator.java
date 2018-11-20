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
        Parser literal = alternate(sequence(accept('"'), text1, accept('"')),
                                    sequence(accept('\''), text2, accept('\''))).collapse();
        Parser rule_char = alternate(letter, digit, accept('-'));
        Parser rule_name = concat(letter, star(rule_char)).collapse();
        Parser term = alternate(literal, rule_name);

        Parser.run(term, "\"test\"");
        Parser.run(term, "this is a rule name");
    }

    public static void main(String[] args) {

    }
}
