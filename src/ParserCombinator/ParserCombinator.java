package ParserCombinator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ParserCombinator.ParseTree.Node;

import static ParserCombinator.Combinators.*;

/**
 * Main class of the ParserCombinator package.
 * Represents a complex Parser constructed dynamically from an input grammar in BNF form.
 *
 * @author Max Kopinsky
 */
public class ParserCombinator {
    private Parser parseGrammar;
    private Map<String, Parser> parsers = new HashMap<>();

    public ParserCombinator(String BNFGrammar) throws IllegalArgumentException {
        Parser syntax = getBNFParser();
        System.out.println(new ParseTree(syntax.run("<first-rule> ::= <first-production>  \n <second-rule> ::= <second-production> \n  " +
                "<third-rule> ::= <prod-1> \"some literal\" | <prod-2>")));

        ParseTree grammar =  new ParseTree(syntax.run(BNFGrammar));
        if (!grammar.assertSuccess()) {
            throw new IllegalArgumentException("Failed to parse the input grammar. " + grammar);
        }
        System.out.println(grammar); // todo: delete
        int startCount = 0;
        String startSymbol = null;

        // todo: throw an error if a rule is left-recursive
        //<editor-fold desc="Identify the start symbol. Throws errors if one can't be found or if there are multiple.">
        List<String> referencedRules = new ArrayList<>();
        List<String> definedRules = new ArrayList<>();
        for (Node rule : grammar.getRoot()) {
            if (definedRules.contains(rule.getChild().getValue())) {
                throw new IllegalArgumentException("A rule was defined twice. Alternation should be declared with '|' characters.");
            }
            definedRules.add(rule.getChild().getValue());
            for (Node list : rule.getChild(1)) {
                for (Node potentialReference : list) {
                    if (potentialReference.getValue().equals("rule-name")) {
                        String ruleName = potentialReference.getChild().getValue();
                        if (!referencedRules.contains(ruleName) && !ruleName.equals(rule.getChild().getValue())) {
                            referencedRules.add(ruleName);
                        }
                    }
                }
            }
        }
        for (String rule : definedRules) {
            if (!referencedRules.contains(rule)) {
                startCount++;
                if (startCount > 1) { throw new IllegalArgumentException("More than one potential start symbol (unreferenced rule)"); }
                startSymbol = rule;
            }
        }
        if (startCount == 0) {
            throw new IllegalStateException("No potential start symbol (all nonterminals are reference in other productions)");
        }
        //</editor-fold>
        System.out.println((startSymbol == null ? "null" : startSymbol)); // todo: delete

        // iterate through rules, store the rule name
        // Create a list of lists of temporary Parsers.
        // for each list in the expression, create a list of temporary Parsers and add it to the list above
        // for each term in the list, add a parser that matches it to the list above
            // whenever a rule is encountered, check if it is in the Map. If it is, reference it directly with Map.get("rule name")
            // if it is not, reference it through a delayed combinator: delayed(() -> Map.get("rule name"))

        // Create a temporary Parser.
        // for each List<Parser> in the List<List<Parser>>
        // if the List<Parser> has size 1, map it to a Parser that is equivalent
        // otherwise map it to the sequence of the Parsers in the List (in order)
        // Results in a List<Parser>, one per list of this rule.
        // Set the temporary Parser equal to the alternation of each Parser in the above list.
        // Store the temp Parser in the Map under the key of its name.
        // If its name is the start symbol, then also set parseGrammar to the temp Parser.
    }

    /**
     * Parser supplier that gets a Parser which recognizes the grammar of BNF grammars.
     * @return A Parser that recognizes BNF grammars.
     */
    private static Parser getBNFParser() {
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

        // I highly recommend collapsing the lambda code block in the following line for readability.
        Parser regex = sequence(accept('/').ignore(), new Parser(stream -> {
            Stream s = stream;
            StringBuilder ret = new StringBuilder();
            for (String head = s.head(); !head.equals("/"); s = s.move(1), head = s.head()) {
                if (head.equals("\\")) {
                    s = s.move(1);
                    head = s.head();
                }
                ret.append(head);
                if (line_end.run(s.move(1)) instanceof Result.Success) {
                    return new Result.Failure(Symbol.value("Unclosed regex in grammar!"), s);
                }
            }
            return new Result.Success(Symbol.value(ret.toString()), s);
        }), accept('/').ignore())
                .bimap(
                        v -> v,
                        e -> { e.clear(); e.add(Symbol.value("Couldn't match 'regex' pattern")); return e; }).parent("regex");
        Parser literal = alternate(sequence(accept('"').ignore(), text1, accept('"').ignore()),
                sequence(accept('\'').ignore(), text2, accept('\'').ignore())).literal().bimap(
                        v -> v,
                        e -> { e.clear(); e.add(Symbol.value("Couldn't match 'literal' pattern")); return e; }).parent("literal");
        Parser rule_name = sequence(accept('<').ignore(), concat(letter, star(rule_char)).literal().parent("rule-name"), accept('>').ignore())
                .bimap(
                        v -> v,
                        e -> { e.clear(); e.add(Symbol.value("Couldn't match 'rule name' pattern")); return e; });

        Parser term = alternate(literal, rule_name, regex)/*.parent("term")*/; // will become significant when option flags are added
        Parser list = concat(term, star(concat(opt_whitespace, term))).parent("list");
        Parser expr = concat(list, star(sequence(
                opt_whitespace, string("|").ignore(),
                opt_whitespace, list))).parent("expression");
        Parser rule = sequence(
                opt_whitespace, rule_name.literal(),
                opt_whitespace, string("::=").ignore(),
                opt_whitespace, expr,
                line_end).parent("rule");
        return concat(plus(rule).parent("syntax"), eof()).bimap(v -> v, e -> {e.add( Symbol.value("Input did not end with a valid rule.")); return e; });
    }

    public static void main(String[] args) {
        ParserCombinator test = new ParserCombinator(
                "<recur-1> ::= /./ <recur-1> \n" +
                        "<recur-2> ::= <recur-1>");
        Map<String, Parser> parsers = new HashMap<>();

        /*Parser letter = set("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        Parser word = plus(letter).parent("word");
        Parser sentence = concat(word, plus(concat(string(" "), word))).parent("sentence");
        System.out.println(sentence.run("These are some words"));
        System.out.println(new ParseTree(sentence.run("These are some words")));
        System.out.println(new ParseTree(sentence.literal().run("These are some words")));*/

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
