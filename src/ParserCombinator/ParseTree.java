package ParserCombinator;

import java.util.ArrayList;
import java.util.List;

/**
 * A tree structure that is the output of running a ParserCombinator's Parser.
 * The tree structure is a Left Child, Right Sibling binary tree.
 * Also used internally as the result of parsing the input grammar.
 * The root of the tree can be accessed with the getRoot() method.
 *
 * @author Max Kopinsky
 */
public class ParseTree {
    /** The root node of the tree. */
    private Node root;
    /** Whether or not this tree represents a successful parse. */
    private boolean successful;

    /**
     * Constructor. Takes a Result and builds the tree from it.
     *
     * @param input The Result to build the tree from.
     */
    ParseTree(Result input) {
        buildTree(input);
    }

    /**
     * Builds a tree out of the input Result.
     * If the Result is a Failure, the output tree will have only one Node which holds the error message.
     *
     * @param input The Result to build the tree from.
     */
    private void buildTree(Result input) {
        if (input instanceof Result.Failure) {
            successful = false;
            root = new Terminal("The parser failed with error: " + input.value.stream().map(
                    symbol -> symbol.toString()).reduce(
                            "", (a, b) -> a + (a.equals("") || a.endsWith(": ") ? "" : ", ") + b), null);
        }
        successful = true;
        List<Symbol> symbols = new ArrayList<>(input.value); // Don't destroy the input list
        if (symbols.size() == 1) {
            root = new Terminal(symbols.remove(0).toString(), null);
            return;
        }
        root = new Nonterminal(symbols.remove(0).toString(), null);

        Node current = root;
        while (symbols.size() > 0) {
            Symbol next = symbols.remove(0);
            switch (next.getType()) {
                case CHILD_MARKER:
                    next = symbols.remove(0);
                    next.assertValue("Child marker not followed by value.");
                    current = current.addChild(next);
                    break;
                case NONTERMINAL: // loose Symbols are siblings
                case VALUE:
                    next.assertValue("Sibling marker not followed by value.");
                    current = current.addSibling(next);
                    break;
                case PARENT_MARKER:
                    current = current.parent;
                    break;
            }
        }
    }

    /**
     * For asserting that the tree represents a successful parse.
     * @return Whether or not this tree represents a successful parse.
     */
    public boolean assertSuccess() {
        return successful;
    }

    /**
     * Getter for the root of the tree. Necessary for classes outside the package to read the tree.
     * @return The root of the tree.
     */
    public Node getRoot() {
        return root;
    }

    /**
     * This toString() method adapted from this StackOverflow answer:
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

    /**
     * A class representing the nodes of the tree. Each Node stores
     * 1) a value
     * 2) a reference to its parent node, which is not visible outside the ParseTree class
     * 3) a reference to its leftmost child
     * 4) a reference to its first sibling on its right.
     *
     * Nodes can be Terminal or Nonterminal. A Node should be a leaf node if and only if it is Terminal.
     */
    abstract class Node {
        private String value;
        private Node parent;
        private Node child;
        private Node sibling;

        /**
         * Constructor. Sets the Node's value and its parent.
         * @param v Value.
         * @param p Parent.
         */
        Node(String v, Node p) {
            value = v;
            parent = p;
        }

        /**
         * Getter for the value of a Node.
         *
         * @return The Node's value.
         */
        public String getValue() {
            return value;
        }

        /**
         * Getter for the children of a Node. Returns a list containing all children from left to right.
         *
         * @return A List of all the Node's children.
         */
        public List<Node> getChildren() {
            List<Node> ret = new ArrayList<>();
            for (Node current = child; current != null; current = current.sibling) {
                ret.add(current);
            }
            return ret;
        }

        /**
         * Getter for the first sibling on the right of a Node. Probably useful for building ASTs out of ParseTrees.
         *
         * @return The first sibling to the right of the Node.
         */
        public Node getSibling() {
            return sibling;
        }

        /**
         * Adds a child to a Node from a given Symbol.
         *
         * @param symbol The Symbol to make a Node from.
         * @return The added child.
         */
        Node addChild(Symbol symbol) {
            if (this instanceof Terminal) {
                throw new IllegalStateException("Can't add child to a terminal.");
            }
            Node toAdd;
            String v = symbol.getValue();
            if (symbol.getType() == Symbol.SymbolType.VALUE) {
                toAdd = new Terminal(v, this);
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

        /**
         * Adds a sibling to a Node from a given Symbol.
         *
         * @param symbol The Symbol to make a Node from.
         * @return The added sibling.
         */
        Node addSibling(Symbol symbol) {
            Node current = this;
            while (current.sibling != null) {
                current = current.sibling;
            }
            String v = symbol.getValue();
            if (symbol.getType() == Symbol.SymbolType.VALUE) {
                current.sibling = new Terminal(v, this.parent);
            } else {
                current.sibling = new Nonterminal(v, this.parent);
            }
            return current.sibling;
        }
    }

    /** Class representing Terminal Nodes. */
    class Terminal extends Node {
        Terminal(String literal, Node parent) {
            super(literal, parent);
        }
    }

    /** Class representing Nonterminal Nodes. */
    class Nonterminal extends Node {
        Nonterminal(String nonterminal, Node parent) {
            super(nonterminal, parent);
        }
    }
}
