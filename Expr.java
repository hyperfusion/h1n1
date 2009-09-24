import java.util.*;

public abstract class Expr {
    public abstract boolean equals(Object o);
    public abstract String toString();
}

class EList extends Expr {
    public Expr car;
    public EList cdr;
    public EList(Expr car, EList cdr) { this.car = car; this.cdr = cdr; }

    public boolean equals(Object o) { return (EList) o == this; }

    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        Iterator<Expr> i = iterator();
        while (i.hasNext()) {
            sb.append(i.next());
            if (i.hasNext())
                sb.append(' ');
        }
        sb.append(')');
        return sb.toString();
    }

    static class EListIterator implements Iterator<Expr> {
        private EList pos;
        public EListIterator(EList start) { pos = start; }
        public boolean hasNext() { return pos != NULL; }
        public Expr next() {
            Expr car = pos.car;
            pos = pos.cdr;
            return car;
        }
        public void remove() { throw new UnsupportedOperationException(); }
    }
    public Iterator<Expr> iterator() { return new EListIterator(this); }

    public List<Expr> toList() {
        ArrayList<Expr> l = new ArrayList<Expr>();
        for (Iterator<Expr> i = iterator(); i.hasNext(); )
            l.add(i.next());
        return l;
    }

    public static EList cast(Expr e) {
        if (!(e instanceof EList))
            throw new RuntimeException(e.toString() + " is not a list");
        return (EList) e;
    }

    // this is used for list termination
    public static final EList NULL;
    static {
        NULL = new EList(null, null);
    }
}

class EAtom extends Expr {
    public String val;
    public EAtom(String val) { this.val = val; }
    public boolean equals(Object o) { return ((EAtom) o).val.equals(val); }
    public int hashCode() { return val.hashCode(); }
    public String toString() { return val; }

    public static EAtom cast(Expr e) {
        if (!(e instanceof EAtom))
            throw new RuntimeException(e.toString() + " is not an atom");
        return (EAtom) e;
    }

    // this NIL is used for everything except list termination
    /*public static final EAtom NIL;
    static {
        NIL = new EAtom("NIL");
    }*/
}

class ENum extends Expr {
    public double val;
    public ENum(double val) { this.val = val; }
    public boolean equals(Object o) { return ((ENum) o).val == val; }
    public String toString() { return Double.toString(val); }

    public static ENum cast(Expr e) {
        if (!(e instanceof ENum))
            throw new RuntimeException(e.toString() + " is not a number");
        return (ENum) e;
    }
}

class EString extends Expr {
    public String val;
    public EString(String val) { this.val = val; }
    public boolean equals(Object o) { return ((EString) o).val == val; }
    public String toString() { return "\"" + val + "\""; }

    public static EString cast(Expr e) {
        if (!(e instanceof EString))
            throw new RuntimeException(e.toString() + " is not a string");
        return (EString) e;
    }
}

class EBool extends Expr {
    public boolean val;
    public EBool(boolean val) { this.val = val; }
    public boolean equals(Object o) { return ((EBool) o).val == val; }
    public String toString() { return val ? "#t" : "#f"; }

    public static EBool cast(Expr e) {
        if (!(e instanceof EBool))
            throw new RuntimeException(e.toString() + " is not a boolean");
        return (EBool) e;
    }

    public static final EBool TRUE, FALSE;
    static {
        TRUE  = new EBool(true);
        FALSE = new EBool(false);
    }
}

class ELambda extends Expr {
    List<EAtom> args;
    List<Expr> body;
    VM.Environment env;
    public ELambda(List<EAtom> args, List<Expr> body, VM.Environment env) { this.args = args; this.body = body; this.env = env; }
    public boolean equals(Object o) { return ((ELambda) o) == this; }
    public String toString() {
        return "(lambda (" + args.size() +  (args.size() == 1 ? " arg" : " args") + ") (...))";
    }

    public static ELambda cast(Expr e) {
        if (!(e instanceof ELambda))
            throw new RuntimeException(e.toString() + " is not a function");
        return (ELambda) e;
    }

    public int compareArgSize(int nargs) {
        return args.size() - nargs;
    }
}

class ELambdaBuiltin extends ELambda {
    EAtom name;

    public ELambdaBuiltin(String name, int nargs) {
        super(null, null, null);
        args = new ArrayList<EAtom>(nargs);
        while (nargs-- > 0) args.add(null);
        this.name = new EAtom(name);
    }

    public String toString() { return "builtin:" + name.toString(); }
}
