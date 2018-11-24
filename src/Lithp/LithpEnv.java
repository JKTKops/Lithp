package Lithp;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

class LithpEnv {
    private Map<String, LithpValue> vars;

    LithpEnv() {
        vars = new HashMap<>();
    }

    LithpValue get(LithpValue key) {
        LithpValue ret = vars.get(key.getSym());
        return ret != null ? ret : LithpValue.err("Unbound Symbol.");
    }

    void put(LithpValue key, LithpValue value) {
        vars.put(key.getSym(), value);
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

        /* Exit function */
        addBuiltinMacro("exit", (env, arg) -> evaluator.builtinExit());

        /* List functions */
        addBuiltin("list", (env, args) -> evaluator.builtinList(args));
        addBuiltin("head", (env, arg) -> evaluator.builtinHead(arg));
        addBuiltin("tail", (env, arg) -> evaluator.builtinTail(arg));
        addBuiltin("len", (env, arg) -> evaluator.builtinLen(arg));
        addBuiltin("eval", evaluator::builtinEval);
        addBuiltin("join", (env, args) -> evaluator.builtinJoin(args));
        addBuiltinMacro("quote", (env, arg) -> evaluator.builtinQuote(arg));

        /* Math functions */
        addBuiltin("+", (env, args) -> evaluator.builtinAdd(args));
        addBuiltin("-", (env, args) -> evaluator.builtinSub(args));
        addBuiltin("*", (env, args) -> evaluator.builtinMult(args));
        addBuiltin("/", (env, args) -> evaluator.builtinDiv(args));
        addBuiltin("%", (env, args) -> evaluator.builtinMod(args));
        addBuiltin("^", (env, args) -> evaluator.builtinPow(args));
    }
}
