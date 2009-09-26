import java.util.*;

public class Builtin {
    public static final ELambdaBuiltin PLUS, MINUS, MULTIPLY, DIVIDE, MOD, LESS, GREATER, LEQ, GEQ, EQ, QUOTE, IF, DEFINE, LAMBDA, LIST, CONS, CAR, CDR, EQV, EQUAL;
    public static ELambdaBuiltin[] all() {
        return new ELambdaBuiltin[] { PLUS, MINUS, MULTIPLY, DIVIDE, MOD, LESS, GREATER, LEQ, GEQ, EQ, QUOTE, IF, DEFINE, LAMBDA, LIST, CONS, CAR, CDR, EQV, EQUAL };
    }
    static {
        PLUS     = new ELambdaBuiltin("+", 2);
        MINUS    = new ELambdaBuiltin("-", 1);
        MULTIPLY = new ELambdaBuiltin("*", 2);
        DIVIDE   = new ELambdaBuiltin("/", 1);
        MOD      = new ELambdaBuiltin("%", 2);
        LESS     = new ELambdaBuiltin("<", 2);
        GREATER  = new ELambdaBuiltin(">", 2);
        LEQ      = new ELambdaBuiltin("<=", 2);
        GEQ      = new ELambdaBuiltin(">=", 2);
        EQ       = new ELambdaBuiltin("=", 2);
        QUOTE    = new ELambdaBuiltin("quote", 1);
        IF       = new ELambdaBuiltin("if", 3);
        DEFINE   = new ELambdaBuiltin("define", 2);
        LAMBDA   = new ELambdaBuiltin("lambda", 2);
        LIST     = new ELambdaBuiltin("list", 1);
        CONS     = new ELambdaBuiltin("cons", 2);
        CAR      = new ELambdaBuiltin("car", 1);
        CDR      = new ELambdaBuiltin("cdr", 1);
        EQV      = new ELambdaBuiltin("eqv?", 2);
        EQUAL    = new ELambdaBuiltin("equal?", 2);
    }

    public static Expr call(ELambdaBuiltin f, List<Expr> args, VM vm, VM.Environment env) {
        if (f == Builtin.QUOTE) {
            if (args.size() != 1)
                throw new RuntimeException("expected 1 arg to quote");
            return args.get(0);
        }

        if (f == Builtin.PLUS) {
            double d = 0.0;
            Iterator<Expr> i = args.iterator();
            while (i.hasNext())
                d += ENum.cast(vm.eval(i.next(), env)).val;
            return new ENum(d);
        }

        if (f == Builtin.MINUS) {
            if (args.size() == 1)
                return new ENum(-ENum.cast(vm.eval(args.get(0), env)).val);
            if (args.size() == 2)
                return new ENum(ENum.cast(vm.eval(args.get(0), env)).val - ENum.cast(vm.eval(args.get(1), env)).val);
            throw new RuntimeException("expected 1 or 2 args to -");
        }

        if (f == Builtin.MULTIPLY) {
            double d = 1.0;
            Iterator<Expr> i = args.iterator();
            while (i.hasNext())
                d *= ENum.cast(vm.eval(i.next(), env)).val;
            return new ENum(d);
        }

        if (f == Builtin.DIVIDE) {
            if (args.size() == 1)
                return new ENum(1.0 / ENum.cast(vm.eval(args.get(0), env)).val);
            if (args.size() == 2)
                return new ENum(ENum.cast(vm.eval(args.get(0), env)).val / ENum.cast(vm.eval(args.get(1), env)).val);
            throw new RuntimeException("expected 1 or 2 args to /");
        }

        if (f == Builtin.MOD) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to %");
            return new ENum(ENum.cast(vm.eval(args.get(0), env)).val % ENum.cast(vm.eval(args.get(1), env)).val);
        }

        if (f == Builtin.LESS) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to <");
            return ENum.cast(vm.eval(args.get(0), env)).val < ENum.cast(vm.eval(args.get(1), env)).val ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.GREATER) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to >");
            return ENum.cast(vm.eval(args.get(0), env)).val > ENum.cast(vm.eval(args.get(1), env)).val ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.LEQ) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to <=");
            return ENum.cast(vm.eval(args.get(0), env)).val <= ENum.cast(vm.eval(args.get(1), env)).val ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.GEQ) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to >=");
            return ENum.cast(vm.eval(args.get(0), env)).val >= ENum.cast(vm.eval(args.get(1), env)).val ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.EQ) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to =");
            return ENum.cast(vm.eval(args.get(0), env)).val == ENum.cast(vm.eval(args.get(1), env)).val ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.IF) {
            if (args.size() != 3)
                throw new RuntimeException("expected 3 args to if");
            return EBool.cast(vm.eval(args.get(0), env)).val ? vm.eval(args.get(1), env) : vm.eval(args.get(2), env);
        }

        if (f == Builtin.DEFINE) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to define");
            env.put(EAtom.cast(args.get(0)), vm.eval(args.get(1), env));
            return null;
        }

        if (f == Builtin.LAMBDA) {
            if (args.size() < 2)
                throw new RuntimeException("expected >= 2 args to lambda");

            // get the lambda's args as a list of atoms
            List<EAtom> largs = new ArrayList<EAtom>();
            for (Iterator<Expr> i = EList.cast(args.get(0)).iterator(); i.hasNext(); )
                largs.add(EAtom.cast(i.next()));

            // the body is everything after the first list
            args.remove(0);

            return new ELambda(largs, args, env);
        }

        if (f == Builtin.LIST) {
            EList list = new EList(vm.eval(args.get(0), env), EList.NULL), pos = list;
            args.remove(0);
            for (Expr arg : args) {
                pos.cdr = new EList(vm.eval(arg, env), EList.NULL);
                pos = pos.cdr;
            }
            return list;
        }

        if (f == Builtin.CONS) {
            if (args.size() != 2)
                throw new RuntimeException("expected = 2 args to cons");
            return new EList(vm.eval(args.get(0), env), EList.cast(vm.eval(args.get(1), env)));
        }

        if (f == Builtin.CAR) {
            if (args.size() != 1)
                throw new RuntimeException("expected 1 arg to car");
            return EList.cast(vm.eval(args.get(0), env)).car;
        }

        if (f == Builtin.CDR) {
            if (args.size() != 1)
                throw new RuntimeException("expected 1 arg to cdr");
            return EList.cast(vm.eval(args.get(0), env)).cdr;
        }

        if (f == Builtin.EQV) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to eqv?");
            Expr a = vm.eval(args.get(0), env), b = vm.eval(args.get(1), env);
            if (!a.getClass().equals(b.getClass()))
                return EBool.FALSE;
            return a.equals(b) ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.EQUAL) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to equal?");
            return vm.eval(args.get(0), env).toString().equals(vm.eval(args.get(1), env).toString()) ? EBool.TRUE : EBool.FALSE;
        }

        return null;
    }
}
