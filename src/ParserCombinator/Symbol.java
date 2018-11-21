package ParserCombinator;

class Symbol {
    enum SymbolType {CHILD_MARKER, PARENT_MARKER, NONTERMINAL, VALUE}

    private SymbolType type;
    private String value;

    private Symbol() {}

    SymbolType getType() {
        return type;
    }

    String getValue() {
        return value;
    }

    void assertValue(String e) {
        if (!(type == SymbolType.VALUE || type == SymbolType.NONTERMINAL)) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        switch(type) {
            case CHILD_MARKER: return "(";
            case PARENT_MARKER: return ")";
            case NONTERMINAL:
            case VALUE: return value;
        }
        return "UntypedSymbol";
    }

    static Symbol childMarker() {
        Symbol ret = new Symbol();
        ret.type = SymbolType.CHILD_MARKER;
        return ret;
    }

    static Symbol parentMarker() {
        Symbol ret = new Symbol();
        ret.type = SymbolType.PARENT_MARKER;
        return ret;
    }

    static Symbol nonterminal(String v) {
        Symbol ret = new Symbol();
        ret.type = SymbolType.NONTERMINAL;
        ret.value = v;
        return ret;
    }

    static Symbol value(String v) {
        Symbol ret = new Symbol();
        ret.type = SymbolType.VALUE;
        ret.value = v;
        return ret;
    }
}
