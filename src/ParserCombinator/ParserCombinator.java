package ParserCombinator;

import static ParserCombinator.Combinators.*;

public class ParserCombinator {
    public static void main(String[] args) {
        Parser html = sequence(string("<tag>"), star(not(string("</tag>"))), string("</tag>"));

        Parser.run(html, "<tag> some garbage text </tag>");
    }
}
