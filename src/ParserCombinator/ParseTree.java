package ParserCombinator;

import java.util.ArrayList;
import java.util.List;

class ParseTree {
    private Node root;

    ParseTree buildTree(Result input) {
        if (input instanceof Result.Failure) {
            root = new Terminal("The parser failed with error: " + input.value.stream().map(symbol -> symbol.toString()).reduce("", (a, b) -> a + b), null);
            return this;
        }
        List<Symbol> symbols = new ArrayList<>(input.value); // Don't destroy the input list
        if (symbols.size() == 1) {
            root = new Terminal(symbols.remove(0).toString(), null);
            return this;
        }
        root = new Nonterminal(symbols.remove(0).toString(), null);

        Node current = root;
        while (symbols.size() > 0) {
            Symbol next = symbols.remove(0);
            switch (next.getType()) {
                case CHILD_MARKER:
                    next = symbols.remove(0);
                    next.assertValue("Child marker not followed by value.");
                    current = current.addChild(next.toString());
                    break;
                case VALUE: // if loose value, we'll assume it was supposed to be a sibling.
                case SIBLING_MARKER:
                    next = symbols.remove(0);
                    next.assertValue("Sibling marker not followed by value.");
                    current = current.addSibling(next.toString());
                    break;
                case PARENT_MARKER:
                    current = current.parent;
                    break;
            }
        }
        return this;
    }

    /**
     * This toString() method  adapted from this StackOverflow answer:
     * https://stackoverflow.com/a/1649223
     * @return A string representation of this ParseTree.
     */
    @Override
    public String toString() {
        return toString(root, "", true);
    }
    private String toString(Node current, String indent, boolean last) {
        if (current == null) {
            return "";
        }
        StringBuilder ret = new StringBuilder();
        ret.append(indent);
        if (last) {
            ret.append("\\-");
            indent += "  ";
        } else {
            ret.append("|-");
            indent += "| ";
        }
        ret.append(current.value).append("\n");

        List<Node> children = current.getChildren();
        for (int i = 0; i < children.size(); i++) {
            ret.append(toString(children.get(i), indent, i == children.size() - 1));
        }
        return ret.toString();
    }

    abstract class Node {
        private String value;
        private Node parent;
        private Node child;
        private Node sibling;

        Node(String v, Node p) {
            value = v;
            parent = p;
        }

        String getValue() {
            return value;
        }

        List<Node> getChildren() {
            List<Node> ret = new ArrayList<>();
            for (Node current = child; current != null; current = current.sibling) {
                ret.add(current);
            }
            return ret;
        }

        Node addChild(String v) {
            if (this instanceof Terminal) {
                throw new IllegalStateException("Can't add child to a terminal.");
            }
            Node toAdd;
            if (v.matches("'.*'")) {
                toAdd = new Terminal(v.substring(1, v.length() - 1), this);
            } else {
                toAdd = new Nonterminal(v, this);
            }
            if (child == null) {
                child = toAdd;
                return child;
            }
            Node current = child;
            while (current.sibling != null) {
                current = current.sibling;
            }
            current.sibling = toAdd;
            return current.sibling;
        }

        Node addSibling(String v) {
            Node current = this;
            while (current.sibling != null) {
                current = current.sibling;
            }

            if (v.matches("'.*'")) {
                current.sibling = new Terminal(v.substring(1, v.length() - 1), this.parent);
            } else {
                current.sibling = new Nonterminal(v, this.parent);
            }
            return current.sibling;
        }
    }

    class Terminal extends Node {
        Terminal(String literal, Node parent) {
            super(literal, parent);
        }
    }

    class Nonterminal extends Node {
        Nonterminal(String nonterminal, Node parent) {
            super(nonterminal, parent);
        }
    }
}
