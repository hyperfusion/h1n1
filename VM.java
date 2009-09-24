import java.util.*;

public class VM {
    // builtin functions
    static class Builtin {
        // too bad enums can't extend anything else, so we'll fake it here
        public static final ELambdaBuiltin PLUS, MINUS, MULTIPLY, DIVIDE, MOD, LESS, GREATER, LEQ, GEQ, EQ, QUOTE, IF, DEFINE, LAMBDA, LIST, CONS, CAR, CDR, EQV, EQUAL;
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
        public static ELambdaBuiltin[] all() {
            return new ELambdaBuiltin[] { PLUS, MINUS, MULTIPLY, DIVIDE, MOD, LESS, GREATER, LEQ, GEQ, EQ, QUOTE, IF, DEFINE, LAMBDA, LIST, CONS, CAR, CDR, EQV, EQUAL };
        }
    }

    public static class Environment {
        private Map<EAtom, Expr> map;
        private Environment parent;
        public Environment(Environment parent) {
            map = new HashMap<EAtom, Expr>();
            this.parent = parent;
        }
        public void put(EAtom k, Expr v) { map.put(k, v); }
        public Expr get(EAtom k) {
            if (map.containsKey(k))
                return map.get(k);
            if (parent != null)
                return parent.get(k);
            return null;
        }
        public String toString() { return map.toString(); }
    }
    private Environment global;

    public VM() {
        global = new Environment(null);

        // fill in builtins at the global level
        for (ELambdaBuiltin e : Builtin.all())
            global.put(e.name, e);
    }

    // returns null if the expression evaluates to nothing
    public Expr eval(Expr expr, Environment env) {
        if (expr == null || expr == EList.NULL)
            return expr;

        if (expr instanceof EAtom) {
            EAtom atom = (EAtom) expr;
            Expr e = env.get(atom);
            if (e == null)
                throw new RuntimeException("not defined: " + atom);
            return e;
        }

        if (expr instanceof EList) {
            // get the function name atom
            EList list = (EList) expr;
            Expr e = eval(list.car, env);
            if (!(e instanceof ELambda))
                throw new RuntimeException("not a function: " + e);

            // get the arguments
            ELambda f = (ELambda) e;
            List<Expr> args = list.cdr.toList();

            return call((ELambda) f, args, env);
        }
        return expr;
    }
    public Expr eval(Expr expr) { return eval(expr, global); }

    private Expr call(ELambda f, List<Expr> args, Environment env) {
         // apply partially if there aren't enough arguments
        if (args.size() < f.args.size())
            return curry(f, args, env);

        if (f instanceof ELambdaBuiltin)
            return callBuiltin((ELambdaBuiltin) f, args, env);

        if (f.args.size() != args.size())
            throw new RuntimeException("expected " + f.args.size() + " args; got " + args.size());

        // create the local environment by stacking on top of the previous one
        Environment local = new Environment(f.env);
        for (int i = 0; i < args.size(); ++i)
            local.put(f.args.get(i), eval(args.get(i), env));

        // run it, returning the last value
        Expr ret = null;
        for (Expr l : f.body)
            ret = eval(l, local);
        return ret;
    }

    private Expr callBuiltin(ELambdaBuiltin f, List<Expr> args, Environment env) {
        if (f == Builtin.QUOTE)
            return args.get(0);

        if (f == Builtin.PLUS) {
            double d = 0.0;
            Iterator<Expr> i = args.iterator();
            while (i.hasNext())
                d += ENum.cast(eval(i.next(), env)).val;
            return new ENum(d);
        }

        if (f == Builtin.MINUS) {
            if (args.size() == 1)
                return new ENum(-ENum.cast(eval(args.get(0), env)).val);
            if (args.size() == 2)
                return new ENum(ENum.cast(eval(args.get(0), env)).val - ENum.cast(eval(args.get(1), env)).val);
            throw new RuntimeException("expected 1 or 2 args to -");
        }

        if (f == Builtin.MULTIPLY) {
            double d = 1.0;
            Iterator<Expr> i = args.iterator();
            while (i.hasNext())
                d *= ENum.cast(eval(i.next(), env)).val;
            return new ENum(d);
        }

        if (f == Builtin.DIVIDE) {
            if (args.size() == 1)
                return new ENum(1.0 / ENum.cast(eval(args.get(0), env)).val);
            if (args.size() == 2)
                return new ENum(ENum.cast(eval(args.get(0), env)).val / ENum.cast(eval(args.get(1), env)).val);
            throw new RuntimeException("expected 1 or 2 args to /");
        }

        if (f == Builtin.MOD) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to %");
            return new ENum(ENum.cast(eval(args.get(0), env)).val % ENum.cast(eval(args.get(1), env)).val);
        }

        if (f == Builtin.LESS) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to <");
            return ENum.cast(eval(args.get(0), env)).val < ENum.cast(eval(args.get(1), env)).val ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.GREATER) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to >");
            return ENum.cast(eval(args.get(0), env)).val > ENum.cast(eval(args.get(1), env)).val ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.LEQ) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to <=");
            return ENum.cast(eval(args.get(0), env)).val <= ENum.cast(eval(args.get(1), env)).val ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.GEQ) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to >=");
            return ENum.cast(eval(args.get(0), env)).val >= ENum.cast(eval(args.get(1), env)).val ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.EQ) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to =");
            return ENum.cast(eval(args.get(0), env)).val == ENum.cast(eval(args.get(1), env)).val ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.IF) {
            if (args.size() != 3)
                throw new RuntimeException("expected 3 args to if");
            return EBool.cast(eval(args.get(0), env)).val ? eval(args.get(1), env) : eval(args.get(2), env);
        }

        if (f == Builtin.DEFINE) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to define");
            env.put(EAtom.cast(args.get(0)), eval(args.get(1), env));
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
            EList list = new EList(eval(args.get(0), env), EList.NULL), pos = list;
            args.remove(0);
            for (Expr arg : args) {
                pos.cdr = new EList(eval(arg, env), EList.NULL);
                pos = pos.cdr;
            }
            return list;
        }

        if (f == Builtin.CONS) {
            if (args.size() != 2)
                throw new RuntimeException("expected = 2 args to cons");
            return new EList(eval(args.get(0), env), EList.cast(eval(args.get(1), env)));
        }

        if (f == Builtin.CAR) {
            if (args.size() != 1)
                throw new RuntimeException("expected 1 arg to car");
            return EList.cast(eval(args.get(0), env)).car;
        }

        if (f == Builtin.CDR) {
            if (args.size() != 1)
                throw new RuntimeException("expected 1 arg to cdr");
            return EList.cast(eval(args.get(0), env)).cdr;
        }

        if (f == Builtin.EQV) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to eqv?");
            Expr a = eval(args.get(0), env), b = eval(args.get(1), env);
            if (!a.getClass().equals(b.getClass()))
                return EBool.FALSE;
            return a.equals(b) ? EBool.TRUE : EBool.FALSE;
        }

        if (f == Builtin.EQUAL) {
            if (args.size() != 2)
                throw new RuntimeException("expected 2 args to equal?");
            return eval(args.get(0), env).toString().equals(eval(args.get(1), env).toString()) ? EBool.TRUE : EBool.FALSE;
        }

        return null;
    }

    private ELambda curry(ELambda f, List<Expr> args, Environment env) {
        // not enough args; partial application (currying)
        // (f x) -> (lambda (y) (f x y))
        final int nrest = f.args.size() - args.size();
        List<EAtom> argspec = new ArrayList<EAtom>(nrest);
        for (int i = 0; i < nrest; ++i)
            argspec.add(new EAtom("a_" + f.hashCode() + "_" + i));

        EList wrapper = new EList(f, EList.NULL);
        EList pos = wrapper;
        int i = 0;
        for ( ; i < args.size(); ++i) {
            pos.cdr = new EList(args.get(i), EList.NULL);
            pos = pos.cdr;
        }
        for ( ; i < f.args.size(); ++i) {
            pos.cdr = new EList(argspec.get(i - args.size()), EList.NULL);
            pos = pos.cdr;
        }
        List<Expr> tmp = new ArrayList<Expr>(1);
        tmp.add(wrapper);
        return new ELambda(argspec, tmp, env);
    }
}
