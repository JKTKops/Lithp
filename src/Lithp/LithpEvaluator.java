package Lithp;

import ParserCombinator.ParseTree;

import java.util.List;

public class LithpEvaluator {
    private LithpEnv globalEnv;

    public LithpEvaluator() {
        globalEnv = new LithpEnv();
        globalEnv.loadBuiltins(this);
    }

    public int eval(ParseTree AST) {
        LithpValue result = eval(globalEnv, LithpValue.read(AST.getRoot().getChild()));
        if (result.getType() == LithpValue.Type.ERR && result.getErr().equals("exit")) return -1;
        System.out.println(result);
        return 0;
    }

    private LithpValue evalSexpr(LithpEnv env, LithpValue value) {
        List<LithpValue> cells = value.getCells();
        if (cells.size() == 0) return value;
        LithpValue macroCheck = eval(env, cells.get(0));
        if (macroCheck.getType() == LithpValue.Type.MACRO) {
            value.pop(); // pull the macro out of the expression and evaluate args
            return macroCheck.getBuiltinFunction().apply(env, value);
        }
        if (cells.size() == 1) {
            return macroCheck;
        }
        cells.set(0, macroCheck);
        for (int i = 1; i < value.getCount(); i++) {
            cells.set(i, eval(env, cells.get(i)));
        } // evaluate all children
        for (LithpValue cell : value) {
            if (cell.getType() == LithpValue.Type.ERR) return cell;
        }

        LithpValue function = value.pop();
        if (function.getType() != LithpValue.Type.FUNC) {
            return LithpValue.err("S-expression does not start with function: " + value);
        }

        return builtinCall(env, function, value);
        //return function.getBuiltinFunction().apply(env, value);
    }

    private LithpValue eval(LithpEnv env, LithpValue value) {
        if (value.getType() == LithpValue.Type.SYM) {
            return env.get(value);
        }
        if (value.getType() == LithpValue.Type.S_EXPR) {
            return evalSexpr(env, value);
        }
        return value;
    }

    // builtin macros
    LithpValue builtinLambda(LithpEnv creator, LithpValue args) {
        if (args.getCount() != 2) {
            return LithpValue.err("Function 'lambda' actual and formal argument lists differ in length.\n" +
                    "Formal: 2, Actual: " + args.getCount());
        }
        LithpValue formals = args.pop();
        LithpValue body = args.pop();
        if (formals.getType() != LithpValue.Type.S_EXPR && formals.getType() != LithpValue.Type.Q_EXPR) {
            return LithpValue.err("Function 'lambda' passed incorrect type for first argument.\n" +
                    "Expected S-Expression or List, found " + typeName(formals) + ": " + formals + ".");
        }
        if (body.getType() != LithpValue.Type.S_EXPR && body.getType() != LithpValue.Type.Q_EXPR) {
            return LithpValue.err("Function 'lambda' passed incorrect type for second argument.\n" +
                    "Expected S-Expression or List, found " + typeName(body) + ": " + body + ".");
        }
        for (LithpValue sym : formals) {
            if (sym.getType() != LithpValue.Type.SYM) {
                return LithpValue.err("Function 'lambda' first argument contains non-symbols.\n" +
                        "Expected Symbol, found " + typeName(sym) + ": " + sym + ".");
            }
        }
        return LithpValue.lambda(creator, formals, body);
    }
    LithpValue builtinDef(LithpEnv env, LithpValue args) {
        return builtinVar(env, args, "def");
    }
    LithpValue builtinLet(LithpEnv env, LithpValue args) {
        return builtinVar(env, args, "let");
    }
    LithpValue builtinDefValues(LithpEnv env, LithpValue args) {
        return builtinVarValues(env, args, "def-values");
    }
    LithpValue builtinLetValues(LithpEnv env, LithpValue args) {
        return builtinVarValues(env, args, "let-values");
    }
    private LithpValue builtinVarValues(LithpEnv env, LithpValue args, String func) {
        if (args.getCount() == 0) {
            return LithpValue.VOID;
        }
        if (args.getCount() != 2) {
            return LithpValue.err("Function 'def-values' actual and formal argument lists differ in length.\n" +
                    "Formal: 2, Actual: " + args.getCount() + ".");
        }
        LithpValue sym = args.pop();
        if (sym.getType() == LithpValue.Type.SYM) {
            if (env.contains(sym)) {
                sym = env.get(sym);
            } else {
                return builtinVar(env, sym, func);
            }
        }
        LithpValue.Type listType = sym.getType();
        if (listType != LithpValue.Type.Q_EXPR && listType != LithpValue.Type.S_EXPR) {
            return LithpValue.err("Function 'def-values' passed incorrect type for first argument.\n" +
                    "Expected Symbol or List, found " + typeName(listType) + ": " + sym + ".");
        }
        LithpValue valueList = eval(env, args.pop());
        if (valueList.getType() != LithpValue.Type.Q_EXPR) {
            return LithpValue.err("Function 'def-values' passed incorrect type for second argument.\n" +
                    "Expected List, found " + typeName(valueList.getType()) + ": " + valueList + ".");
        }
        if (sym.getCount() != valueList.getCount()) {
            return LithpValue.err("Function 'def-values' passed lists of different length.\n" +
                    "Keys: " + sym +"\n" +
                    "Values: " + valueList);
        }
        List<LithpValue> keys = sym.getCells();
        List<LithpValue> values = valueList.getCells();
        if (func.equals("def-values")) {
            for (int i = 0; i < sym.getCount(); i++) {
                env.def(keys.get(i), eval(env, values.get(i)));
            }
        }
        if (func.equals("let-values")) {
            for (int i = 0; i < sym.getCount(); i++) {
                env.put(keys.get(i), eval(env, values.get(i)));
            }
        }
        return LithpValue.VOID;
    }
    private LithpValue builtinVar(LithpEnv env, LithpValue args, String func) {
        if (args.getCount() == 0) {
            return LithpValue.VOID;
        }
        LithpValue sym = args.pop();
        //<editor-fold desc="Error checking">
        if (args.getCount() != 1) { // symbol arg popped already
            return LithpValue.err("Function '"+func+"' actual and formal arguments lists differ in length.\n" +
                    "Formal: 2, Actual: " + (args.getCount() + 1) + ".");
        }
        LithpValue.Type listType = sym.getType();
        if (listType != LithpValue.Type.SYM) {
            return LithpValue.err("Function '"+func+"' passed incorrect type for first argument.\n" +
                    "Expected Symbol, found " + typeName(listType) + ": " + sym + ".");
        }
        //</editor-fold>;
        if (func.equals("def")) env.def(sym, eval(env, args.pop()));
        if (func.equals("let")) env.put(sym, eval(env, args.pop()));
        return LithpValue.VOID;
    }
    LithpValue builtinQuote(LithpValue arg) {
        //<editor-fold desc="Error checking">
        if (arg.getCount() != 1) {
            return LithpValue.err("Function 'quote' actual and formal argument lists differ in length.\n" +
                    "Formal: 1, Actual: " + arg.getCount() + ".");
        }
        LithpValue sexp = arg.pop();
        if (sexp.getType() != (LithpValue.Type.S_EXPR)) {
            return LithpValue.err("Function 'quote' passed incorrect type.\n" +
                    "Expected S-Expression, found " + typeName(sexp) + ": " + sexp + ".");
        }
        //</editor-fold>
        sexp.setType(LithpValue.Type.Q_EXPR);
        return sexp;
    }
    LithpValue builtinExit() {
        return LithpValue.exit();
    }
    // builtin functions
    private LithpValue builtinCall(LithpEnv env, LithpValue func, LithpValue args) {if (func.isBuiltin()) {
            return func.getBuiltinFunction().apply(env, args);
        }
        int formal = func.getFormals().getCount();
        int actual = args.getCount();
        if (formal == 0 && actual == 1 && args.get(0).equals(LithpValue.VOID)) {
            func.getEnv().setParent(env);
            return eval(func.getEnv(), new LithpValue(func.getBody()));
        }
        func = new LithpValue(func); // copy now so we don't consume formals
        while (args.getCount() > 0) {
            if (func.getFormals().getCount() <= 0) {
                return LithpValue.err("A function's formal and actual argument lists differ in length.\n" +
                        "Formal: " + formal + ", Actual: " + actual + ".");
            }
            func.getEnv().put(func.getFormals().pop(), args.pop());
        }
        if (func.getFormals().getCount() == 0) {
            func.getEnv().setParent(env);
            return eval(func.getEnv(), new LithpValue(func.getBody()));
        } else {
            return func;
        }
    }
    LithpValue builtinLen(LithpValue arg) {
        //<editor-fold desc="Error checking">
        if (arg.getCount() != 1) {
            return LithpValue.err("Function 'len' actual and formal argument lists differ in length.\n" +
                    "Formal: 1, Actual: " + arg.getCount() + ".");
        }
        LithpValue list = arg.pop();
        if (list.getType() != (LithpValue.Type.Q_EXPR)) {
            return LithpValue.err("Function 'len' passed incorrect type.\n" +
                    "Expected List, found " + typeName(list) + ": " + list + ".");
        }
        //</editor-fold>
        return LithpValue.num(list.getCount());
    }
    LithpValue builtinHead(LithpValue arg) {
        //<editor-fold desc="Error checking">
        if (arg.getCount() != 1) {
            return LithpValue.err("Function 'head' actual and formal argument lists differ in length.\n" +
                    "Formal: 1, Actual: " + arg.getCount() + ".");
        }
        LithpValue ret = arg.pop();
        if (ret.getType() != LithpValue.Type.Q_EXPR) {
            return LithpValue.err("Function 'head' passed incorrect type.\n" +
                    "Expected List, found " + typeName(ret) + ": " + ret + ".");
        }
        if (ret.getCount() == 0) {
            return LithpValue.err("Function 'head' passed '().");
        }
        //</editor-fold>
        List<LithpValue> qexpr = ret.getCells(); // ret is defined in the editor fold
        while (qexpr.size() > 1) {
            qexpr.remove(1);
        }
        return ret;
    }
    LithpValue builtinTail(LithpValue arg) {
        //<editor-fold desc="Error checking">
        if (arg.getCount() != 1){
            return LithpValue.err("Function 'tail' actual and formal argument lists differ in length.\n" +
                    "Formal: 1, Actual: " + arg.getCount() + ".");
        }
        LithpValue ret = arg.pop();
        if (ret.getType() != LithpValue.Type.Q_EXPR) {
            return LithpValue.err("Function 'tail' passed incorrect type.\n" +
                    "Expected List, found " + typeName(ret) + ": " + ret + ".");
        }
        if (ret.getCount() == 0) {
            return LithpValue.err("Function 'tail' passed '().");
        }
        //</editor-fold>
        ret.getCells().remove(0); // ret is defined in the editor fold, ret = arg.pop()
        return ret;
    }
    LithpValue builtinJoin(LithpValue args) {
        //<editor-fold desc="Error checking">
        if (args.getCount() == 0) {
            return LithpValue.err("Function 'join' passed 0 arguments.");
        }
        for (LithpValue v : args) {
            if (v.getType() != LithpValue.Type.Q_EXPR) {
                return LithpValue.err("Function 'join' passed incorrect type.\n" +
                        "Expected List, found " + typeName(v) + ": " + v + ".");
            }
        }
        //</editor-fold>
        LithpValue x = args.pop();
        while (args.getCount() > 0) {
            x.join(args.pop());
        }
        return x;
    }
    LithpValue builtinList(LithpValue arg) {
        arg.setType(LithpValue.Type.Q_EXPR);
        return arg;
    }
    LithpValue builtinEval(LithpEnv env, LithpValue arg) {
        //<editor-fold desc="Error checking">
        if (arg.getCount() != 1) {
            return LithpValue.err("Function 'eval' actual and formal argument lists differ in length.\n" +
                    "Formal: 1, Actual: " + arg.getCount() + ".");
        }
        LithpValue x = arg.pop();
        if (x.getType() != LithpValue.Type.Q_EXPR) {
            return LithpValue.err("Function 'eval' passed incorrect type.\n" +
                    "Expected List, found " + typeName(x) + ": " + x + ".");
        }
        //</editor-fold>
        x.setType(LithpValue.Type.S_EXPR); // x = arg.pop() in the editor fold
        return eval(env, x);
    }
    private LithpValue builtinOp(LithpValue args, String op) {
        for (LithpValue arg : args) {
            if (arg.getType() != LithpValue.Type.NUM) return LithpValue.err(args.toString() + " contains a non-number.");
        }
        LithpValue x = new LithpValue(args.pop());
        if (op.equals("-") && args.getCount() == 0) {
            x.setNum(-x.getNum());
        }
        loop:
        while (args.getCount() > 0) {
            LithpValue y = args.pop();
            switch (op) {
                case "+":
                    x.setNum(x.getNum() + y.getNum());
                    break;
                case "-":
                    x.setNum(x.getNum() - y.getNum());
                    break;
                case "*":
                    x.setNum(x.getNum() * y.getNum());
                    break;
                case "/":
                    if (y.getNum() == 0) {
                        x = LithpValue.err("Division by Zero");
                        break loop;
                    }
                    x.setNum(x.getNum() / y.getNum());
                    break;
                case "%":
                    x.setNum(x.getNum() % y.getNum());
                    break;
                case "^":
                    x.setNum((long) Math.pow(x.getNum(), y.getNum()));
                    break;
            }
        }
        return x;
    }
    LithpValue builtinAdd(LithpValue args) {
        return builtinOp(args, "+");
    }
    LithpValue builtinSub(LithpValue args) {
        return builtinOp(args, "-");
    }
    LithpValue builtinMult(LithpValue args) {
        return builtinOp(args, "*");
    }
    LithpValue builtinDiv(LithpValue args) {
        return builtinOp(args, "/");
    }
    LithpValue builtinMod(LithpValue args) {
        return builtinOp(args, "%");
    }
    LithpValue builtinPow(LithpValue args) {
        return builtinOp(args, "^");
    }

    // for error reporting
    private String typeName(LithpValue.Type type) {
        switch (type) {
            case Q_EXPR:
                return "List";
            case SYM:
                return "Symbol";
            case VOID:
                return "Void";
            case MACRO:
            case FUNC:
                return "Function";
            case S_EXPR:
                return "S-Expression";
            case NUM:
                return "Number";
            case ERR:
                return "Error";
            default:
                return "Unknown";
        }
    }
    private String typeName(LithpValue value) {
        return typeName(value.getType());
    }
}
