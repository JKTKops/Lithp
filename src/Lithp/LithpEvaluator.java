package Lithp;

import ParserCombinator.ParseTree;

import java.util.List;
import java.util.stream.Collectors;

public class LithpEvaluator {
    public int eval(ParseTree AST) {
        LithpValue result = eval(LithpValue.read(AST.getRoot().getChild()));
        if (result.getType() == LithpValue.Type.ERR && result.getErr().equals("exit")) return -1;
        System.out.println(result);
        return 0;
    }

    private LithpValue evalSexpr(LithpValue value) {
        value.setCells(value.getCells().stream().map(this::eval).collect(Collectors.toList()));
        for (LithpValue cell : value) {
            if (cell.getType() == LithpValue.Type.ERR) return cell;
        }
        List<LithpValue> cells = value.getCells();
        if (cells.size() == 0) return value;
        if (cells.size() == 1) {
            if (cells.get(0).getType() == LithpValue.Type.SYM && cells.get(0).toString().equals("exit")) return LithpValue.exit(); // todo: fix this later
            return cells.get(0);
        }
        if (cells.get(0).getType() != LithpValue.Type.SYM) {
            return LithpValue.err("S-expression does not start with symbol: " + value);
        }

        return builtinOp(value, value.pop().toString());
    }

    private LithpValue eval(LithpValue value) {
        if (value.getType() == LithpValue.Type.S_EXPR) {
            return evalSexpr(value);
        }
        return value;
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
