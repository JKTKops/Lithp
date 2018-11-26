package Lithp;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

class LithpEnv {
    private LithpEnv parent;
    private Map<String, LithpValue> vars;

    LithpEnv() {
        vars = new HashMap<>();
    }
    LithpEnv(LithpEnv e) {
        parent = e.parent;
        vars = new HashMap<>(e.vars);
    }

    void setParent(LithpEnv e) {
        parent = e;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LithpEnv)) return false;
        LithpEnv env = (LithpEnv) obj;
        return (parent.equals(env.parent)) && (vars.equals(env.vars));
    }

    LithpValue get(LithpValue key) {
        LithpValue ret = vars.get(key.getSym());
        if (ret != null) return ret;
        else if (parent != null) return parent.get(key);
        else return LithpValue.err("Unbound Symbol: " + key.getSym() + ".");
    }

    void put(LithpValue key, LithpValue value) {
        vars.put(key.getSym(), value);
    }

    void def(LithpValue key, LithpValue value) {
        LithpEnv current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        current.put(key, value);
    }

    boolean contains(LithpValue key) {
        return vars.containsKey(key.getSym());
    }

    private void addBuiltin(String name, BiFunction<LithpEnv, LithpValue, LithpValue> func) {
        vars.put(name, LithpValue.func(name, func));
    }
    private void addBuiltinMacro(String name, BiFunction<LithpEnv, LithpValue, LithpValue> macro) {
        vars.put(name, LithpValue.macro(name, macro));
    }

    void loadBuiltins(LithpEvaluator evaluator) {
        /* builtin values */
        vars.put("#<void>", LithpValue.voidValue());

        /* Builtin macros */
        addBuiltinMacro("def", evaluator::builtinDef);
        addBuiltinMacro("let", evaluator::builtinLet);
        addBuiltinMacro("def-values", evaluator::builtinDefValues);
        addBuiltinMacro("let-values", evaluator::builtinLetValues);
        addBuiltinMacro("lambda", evaluator::builtinLambda);
        addBuiltinMacro("quote", (env, arg) -> evaluator.builtinQuote(arg));
        // exit function
        addBuiltinMacro("exit", (env, arg) -> evaluator.builtinExit());

        /* List functions */
        addBuiltin("list", (env, args) -> evaluator.builtinList(args));
        addBuiltin("head", (env, arg) -> evaluator.builtinHead(arg));
        addBuiltin("tail", (env, arg) -> evaluator.builtinTail(arg));
        addBuiltin("len", (env, arg) -> evaluator.builtinLen(arg));
        addBuiltin("eval", evaluator::builtinEval);
        addBuiltin("join", (env, args) -> evaluator.builtinJoin(args));

        /* Math functions */
        addBuiltin("+", (env, args) -> evaluator.builtinAdd(args));
        addBuiltin("-", (env, args) -> evaluator.builtinSub(args));
        addBuiltin("*", (env, args) -> evaluator.builtinMult(args));
        addBuiltin("/", (env, args) -> evaluator.builtinDiv(args));
        addBuiltin("%", (env, args) -> evaluator.builtinMod(args));
        addBuiltin("^", (env, args) -> evaluator.builtinPow(args));
    }
}
