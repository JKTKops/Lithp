package Lithp;

import ParserCombinator.ParseTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LithpValue implements Iterable<LithpValue> {
    enum Type { ERR, NUM, SYM, S_EXPR, Q_EXPR /* stops evaluation */ }

    /** The type of this L-val */
    private Type type;
    /** The number stored by a num L-Val */
    private long num;
    /** ERR and SYM types store a string */
    private String err;
    private String sym;
    /** S_EXPR types store a list of L-Vals */
    private List<LithpValue> lvals;

    void add(LithpValue toAdd) {
        if (!(type == Type.S_EXPR || type == Type.Q_EXPR)) { return; }
        lvals.add(toAdd);
    }
    void join(LithpValue y) {
        while (y.getCount() > 0) {
            add(y.pop());
        }
    }

    static LithpValue read(ParseTree.Node node) {
        switch (node.getValue()) {
            case "number":
                return readNum(node.getChild());
            case "symbol":
                return LithpValue.sym(node.getDeepValue(1));
            case "expr":
                return read(node.getChild());
        }
        LithpValue ret;
        switch (node.getValue()) {
            case "sexpr":
                ret = LithpValue.sexpr();
                break;
            case "qexpr":
                ret = LithpValue.qexpr();
                break;
            default: ret = LithpValue.qexpr(); break; // shouldn't happen.
        }
        for (ParseTree.Node child : node) {
            ret.add(read(child));
        }
        return ret;
    }
    private static LithpValue readNum(ParseTree.Node node) {
        try {
            return LithpValue.num(Long.valueOf(node.getValue()));
        } catch (NumberFormatException e) {
            return LithpValue.err("Error: Invalid Number");
        }
    }

    @Override
    public Iterator<LithpValue> iterator() {
        if (type != Type.S_EXPR) {
            return new ArrayList<LithpValue>().iterator();
        }
        return lvals.iterator();
    }

    @Override
    public String toString() {
        switch(type) {
            case NUM: return String.valueOf(num);
            case SYM: return sym;
            case S_EXPR: return exprString("(", ")");
            case Q_EXPR: return exprString("'(", ")");
            case ERR: return "Error: " + err;
        }
        return "Untyped Lithp Value";
    }
    private String exprString(String open, String close) {
        StringBuilder ret = new StringBuilder(open);
        for (LithpValue v : lvals) {
            ret.append(v.toString()).append(' ');
        }
        return ret.toString().trim() + close;
    }
    Type getType() {
        return type;
    }
    void setType(Type setType) {
        type = setType;
    }
    long getNum() {
        return num;
    }
    void setNum(long setNum) {
        num = setNum;
    }
    String getErr() {
        return err;
    }
    LithpValue pop() {
        return lvals.remove(0);
    }
    LithpValue pop(int i) {
        return lvals.remove(i);
    }
    int getCount() {
        return lvals.size();
    }
    List<LithpValue> getCells() {
        return lvals;
    }
    void setCells(List<LithpValue> cells) {
        lvals = cells;
    }

    private LithpValue() {}
    LithpValue(LithpValue toCopy) {
        type = toCopy.type;
        switch (toCopy.type) {
            case NUM: num = toCopy.num; break;
            case ERR: err = toCopy.err; break;
            case SYM: sym = toCopy.sym; break;
            case Q_EXPR:
            case S_EXPR: lvals = new ArrayList<>(toCopy.lvals); break;

        }
    }

    static LithpValue num(long n) {
        LithpValue v = new LithpValue();
        v.type = Type.NUM;
        v.num = n;
        return v;
    }
    static LithpValue err(String e) {
        LithpValue v = new LithpValue();
        v.type = Type.ERR;
        v.err = e;
        return v;
    }
    static LithpValue sym(String s) {
        LithpValue v = new LithpValue();
        v.type = Type.SYM;
        v.sym = s;
        return v;
    }
    static LithpValue sexpr() {
        LithpValue v = new LithpValue();
        v.type = Type.S_EXPR;
        v.lvals = new ArrayList<>();
        return v;
    }
    static LithpValue qexpr() {
        LithpValue v = new LithpValue();
        v.type = Type.Q_EXPR;
        v.lvals = new ArrayList<>();
        return v;
    }
    static LithpValue exit() {
        LithpValue v = new LithpValue();
        v.type = Type.ERR;
        v.err = "exit";
        return v;
    }
}
