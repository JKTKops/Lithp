package ParserCombinator;

class ParseTree {
    private Node root;

    ParseTree(String startSymbol) {
        root = new Node(startSymbol);
    }

    Node getRoot() {
        return root;
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

        ret.append(toString(current.child, indent, false));
        ret.append(toString(current.sibling, indent, true));
        return ret.toString();
    }

    class Node {
        private String value;
        private Node child;
        private Node sibling;

        Node(String v) {
            value = v;
        }

        String getValue() {
            return value;
        }

        Node addChild(String v) {
            if (child == null) {
                child = new Node(v);
                return child;
            }
            Node current = child;
            while (current.sibling != null) {
                current = current.sibling;
            }
            current.sibling = new Node(v);
            return current.sibling;
        }

        Node addSibling(String v) {
            Node current = this;
            while (current.sibling != null) {
                current = current.sibling;
            }
            current.sibling = new Node(v);
            return current.sibling;
        }
    }
}
