package Lithp;

import ParserCombinator.ParseTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

public class LithpValue implements Iterable<LithpValue> {
    enum Type { ERR, VOID, NUM, SYM, S_EXPR, Q_EXPR /* stops evaluation */, FUNC, MACRO }

    /** The type of this L-val */
    private Type type;
    /** The number stored by a num L-Val */
    private long num;
    /** ERR and SYM types store a string */
    private String err;
    private String sym;
    /** S_EXPR types store a list of L-Vals */
    private List<LithpValue> lvals;
    /** FUNC types store a (LithpEnv, LithpValue) -> LithpValue function */
    private BiFunction<LithpEnv, LithpValue, LithpValue> function;

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
            case "void":
                return LithpValue.voidValue();
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
            case VOID: return "#<void>";
            case SYM: return sym;
            case S_EXPR: return exprString("(", ")");
            case Q_EXPR: return exprString("'(", ")");
            case FUNC: return "<function>: " + sym;
            case MACRO: return "<function>: " + sym; // not sure if this one can happen without evaluating the macro
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
    String getSym() {
        return sym;
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
    BiFunction<LithpEnv, LithpValue, LithpValue> getFunction() {
        return function;
    }

    private LithpValue() {}
    private LithpValue(LithpValue toCopy) {
        type = toCopy.type;
        switch(type) {
            case FUNC:
            case MACRO:
                sym = toCopy.sym;
                function = toCopy.function;
                break;
            case VOID: break;
            case NUM: num = toCopy.num; break;
            case SYM: sym = toCopy.sym; break;
            case ERR: err = toCopy.err; break;
            case S_EXPR:
            case Q_EXPR:
                lvals = new ArrayList<>();
                for (LithpValue copy : toCopy) {
                    lvals.add(new LithpValue(copy));
                }
                break;
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
    static LithpValue voidValue() {
        LithpValue v = new LithpValue();
        v.type = Type.VOID;
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
    static LithpValue func(String symbol, BiFunction<LithpEnv, LithpValue, LithpValue> func) {
        LithpValue v = new LithpValue();
        v.type = Type.FUNC;
        v.sym = symbol;
        v.function = func;
        return v;
    }
    static LithpValue macro(String symbol, BiFunction<LithpEnv, LithpValue, LithpValue> func) {
        LithpValue v = new LithpValue();
        v.type = Type.MACRO;
        v.sym = symbol;
        v.function = func;
        return v;
    }
    static LithpValue exit() {
        LithpValue v = new LithpValue();
        v.type = Type.ERR;
        v.err = "exit";
        return v;
    }
}
