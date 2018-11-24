package Lithp;

import ParserCombinator.ParseTree;

import java.util.List;

public class LithpEvaluator {
    public int eval(ParseTree AST) {
        LithpValue result = eval(LithpValue.read(AST.getRoot().getChild()));
        if (result.getType() == LithpValue.Type.ERR && result.getErr().equals("exit")) return -1;
        System.out.println(result);
        return 0;
    }

    private LithpValue evalSexpr(LithpValue value) {
        List<LithpValue> cells = value.getCells();
        if (cells.size() == 0) return value;
        LithpValue quoteCheck = eval(cells.get(0));
        if (quoteCheck.getType() == LithpValue.Type.SYM && quoteCheck.toString().equals("quote")) {
            value.pop();
            return builtinQuote(value);
        }

        cells.set(0, quoteCheck);
        for (int i = 1; i < value.getCount(); i++) {
            cells.set(i, eval(cells.get(i)));
        } // evaluate all children
        for (LithpValue cell : value) {
            if (cell.getType() == LithpValue.Type.ERR) return cell;
        }

        if (cells.get(0).getType() != LithpValue.Type.SYM) {
            return LithpValue.err("S-expression does not start with symbol: " + value);
        }

        return builtin(value, value.pop().toString());
    }

    private LithpValue eval(LithpValue value) {
        if (value.getType() == LithpValue.Type.S_EXPR) {
            return evalSexpr(value);
        }
        return value;
    }

    private LithpValue builtin(LithpValue args, String function) {
        switch (function) {
            case "list": return builtinList(args);
            case "head": return builtinHead(args);
            case "tail": return builtinTail(args);
            case "join": return builtinJoin(args);
            case "eval": return builtinEval(args);
            case "len": return builtinLen(args);
            case "exit": return LithpValue.exit();
        }
        if ("+-/*%^".contains(function)) return builtinOp(args, function);
        return LithpValue.err("Unknown function: " + function);
    }

    private LithpValue builtinLen(LithpValue arg) {
        //<editor-fold desc="Error checking">
        if (arg.getCount() != 1) {
            return LithpValue.err("Function 'len' not passed exactly one argument.");
        }
        if (arg.getCells().get(0).getType() != (LithpValue.Type.Q_EXPR)) {
            return LithpValue.err("Function 'len' passed incorrect types.");
        }
        //</editor-fold>
        return LithpValue.num(arg.getCells().get(0).getCount());
    }
    private LithpValue builtinQuote(LithpValue arg) {
        //<editor-fold desc="Error checking">
        if (arg.getCount() != 1) {
            return LithpValue.err("Function 'quote' not passed exactly one argument.");
        }
        if (arg.getCells().get(0).getType() != (LithpValue.Type.S_EXPR)) {
            return LithpValue.err("Function 'quote' passed incorrect types.");
        }
        //</editor-fold>
        arg.getCells().get(0).setType(LithpValue.Type.Q_EXPR);
        return arg.getCells().get(0);
    }
    private LithpValue builtinHead(LithpValue arg) {
        //<editor-fold desc="Error checking">
        if (arg.getCount() != 1) return LithpValue.err("Function 'head' not passed exactly one argument.");
        if (arg.getCells().get(0).getType() != LithpValue.Type.Q_EXPR) {
            return LithpValue.err("Function 'head' passed incorrect types.");
        }
        if (arg.getCells().get(0).getCount() == 0) {
            return LithpValue.err("Function 'head' passed '().");
        }
        //</editor-fold>
        LithpValue ret = arg.pop();
        List<LithpValue> qexpr = ret.getCells();
        while (qexpr.size() > 1) {
            qexpr.remove(1);
        }
        return ret;
    }
    private LithpValue builtinTail(LithpValue arg) {
        //<editor-fold desc="Error checking">
        if (arg.getCount() != 1){
            return LithpValue.err("Function 'tail' not passed exactly one argument.");
        }
        if (arg.getCells().get(0).getType() != LithpValue.Type.Q_EXPR) {
            return LithpValue.err("Function 'tail' passed incorrect types.");
        }
        if (arg.getCells().get(0).getCount() == 0) {
            return LithpValue.err("Function 'tail' passed '().");
        }
        //</editor-fold>
        LithpValue ret = arg.pop();
        ret.getCells().remove(0);
        return ret;
    }
    private LithpValue builtinJoin(LithpValue args) {
        //<editor-fold desc="Error checking">
        if (args.getCount() == 0) {
            return LithpValue.err("Function 'join' passed 0 arguments.");
        }
        for (LithpValue v : args) {
            if (v.getType() != LithpValue.Type.Q_EXPR) {
                return LithpValue.err("Function 'join' passed incorrect type.");
            }
        }
        //</editor-fold>
        LithpValue x = args.pop();
        while (args.getCount() > 0) {
            x.join(args.pop());
        }
        return x;
    }
    private LithpValue builtinList(LithpValue arg) {
        arg.setType(LithpValue.Type.Q_EXPR);
        return arg;
    }
    private LithpValue builtinEval(LithpValue arg) {
        //<editor-fold desc="Error checking">
        if (arg.getCount() != 1) {
            return LithpValue.err("Function 'eval' not passed exactly one argument.");
        }
        if (arg.getCells().get(0).getType() != LithpValue.Type.Q_EXPR) {
            return LithpValue.err("Function 'eval' passed incorrect type." + arg);
        }
        //</editor-fold>
        LithpValue x = arg.pop();
        x.setType(LithpValue.Type.S_EXPR);
        return eval(x);
    }
    private LithpValue builtinOp(LithpValue args, String op) {
        for (LithpValue arg : args) {
            if (arg.getType() != LithpValue.Type.NUM) return LithpValue.err(args.toString() + " contains a non-number.");
        }
        LithpValue x = args.pop();
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
}
