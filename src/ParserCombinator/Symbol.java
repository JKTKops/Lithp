package ParserCombinator;

class Symbol {
    enum SymbolType {CHILD_MARKER, SIBLING_MARKER, PARENT_MARKER, VALUE};

    private SymbolType type;
    private String value;

    private Symbol() {}

    SymbolType getType() {
        return type;
    }

    @Override
    public String toString() {
        switch(type) {
            case CHILD_MARKER: return "(";
            case SIBLING_MARKER: return "-";
            case PARENT_MARKER: return ")";
            case VALUE: return value;
        }
        return "UntypedSymbol";
    }

    static Symbol childMarker() {
        Symbol ret = new Symbol();
        ret.type = SymbolType.CHILD_MARKER;
        return ret;
    }

    static Symbol siblingMarker() {
        Symbol ret = new Symbol();
        ret.type = SymbolType.SIBLING_MARKER;
        return ret;
    }

    static Symbol parentMarker() {
        Symbol ret = new Symbol();
        ret.type = SymbolType.PARENT_MARKER;
        return ret;
    }

    static Symbol value(String v) {
        Symbol ret = new Symbol();
        ret.type = SymbolType.VALUE;
        ret.value = v;
        return ret;
    }
}
