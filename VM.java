import java.util.*;

public class VM {
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
            return Builtin.call((ELambdaBuiltin) f, args, this, env);

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

    private ELambda curry(ELambda f, List<Expr> args, Environment env) {
        // not enough args; partial application (currying)
        // e.g. (f x) -> (lambda (y) (f x y))
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
