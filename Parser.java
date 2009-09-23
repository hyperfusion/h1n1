import java.util.*;
import java.util.regex.*;

class Token {
    Rule type;
    String str;
    public Token(Rule type, String str) {
        this.type = type;
        this.str = str;
    }
}

enum Rule {
    QUOTE      ("'"),
    BOOL       ("#t|#f"),
    NUM        ("[-+]?\\d*\\.?\\d+"),
    ATOM       ("[a-zA-Z%\\^\\&\\*\\-\\+=\\|<>/\\?#0-9]+"),
    STRING     ("\"[^\"]+\""),
    LIST_OPEN  ("\\("),
    LIST_CLOSE ("\\)"),
    WHITESPACE ("\\s+"),
    COMMENT    (";");

    final Pattern pat;
    Rule(String re) { pat = Pattern.compile(re); }
}

// a regular expressions-based tokenizer
class Tokenizer implements Iterator<Token> {
    private String str;
    private int len;
    private int pos;
    private Matcher m;
 
    public Tokenizer(String str) {
        this.str = str;
        len = str.length();
        pos = 0;
        m = Pattern.compile("").matcher(str);
        m.useTransparentBounds(true).useAnchoringBounds(false);
    }

    // returns the next token in the string.
    // returns null when the whole string is consumed.
    // if consume == false, then this serves as a peek function
    public Token next(boolean consume) {
        int oldpos = pos;
        while (pos < len) {
            m.region(pos, len);
            // try to match a rule
            for (Rule r : Rule.values()) {
                if (m.usePattern(r.pat).lookingAt()) {
                    pos = m.end();

                    // just absorb whitespace
                    if (r == Rule.WHITESPACE) {
                        --pos; // to compensate for ++pos
                        break;
                    }

                    // and comments too
                    if (r == Rule.COMMENT) {
                        // go up to a newline
                        pos = str.indexOf('\n', pos);
                        if (pos == -1) pos = len - 1;
                        break;
                    }

                    if (!consume) pos = oldpos;
                    return new Token(r, str.substring(m.start(), m.end()));
                }
            }

            // if nothing is matched, move ahead
            ++pos;
        }
        if (!consume) pos = oldpos;
        return null;
    }

    public Token peek() { return next(false); }
    public Token next() { return next(true); }
    public void remove() { throw new UnsupportedOperationException(); }
    public boolean hasNext() { return pos < len; }
    public String rest() { return str.substring(pos); }
}

public class Parser {
    private Tokenizer tokenizer;
    public Parser(Tokenizer tokenizer) { this.tokenizer = tokenizer; }

    public Expr parse() {
        Token tok = tokenizer.next();
        if (tok == null) return null;

        switch (tok.type) {
        case QUOTE:
            return new EList(new EAtom("quote"), new EList(parse(), EList.NULL));
        case ATOM:
            return new EAtom(tok.str);
        case NUM:
            return new ENum(Double.parseDouble(tok.str));
        case STRING:
            return new EString(tok.str.substring(1, tok.str.length() - 1));
        case BOOL:
            return tok.str.equals("#t") ? EBool.TRUE : EBool.FALSE;
        case LIST_OPEN:
            // keep consuming tokens till LIST_CLOSE is reached,
            // placing them into the list
            EList list = null, pos = null;

            // get the first item
            Token next = tokenizer.peek();
            if (next != null && next.type != Rule.LIST_CLOSE) {
                list = new EList(parse(), EList.NULL);
                pos = list;
            }

            // get the rest
            while ((next = tokenizer.peek()) != null && next.type != Rule.LIST_CLOSE) {
                pos.cdr = new EList(parse(), EList.NULL);
                pos = pos.cdr;
            }

            // if we reached the end without getting a LIST_CLOSE,
            // then there was a syntax error
            if (next == null)
                throw new RuntimeException("expected closing )");

            // here, nextTok.type == Rule.LIST_CLOSE
            // consume the end paren
            tokenizer.next();

            // empty list case
            if (list == null)
                return EList.NULL;

            return list;
        case LIST_CLOSE:
            // we should never reach this (this should be consumed in LIST_OPEN)
            throw new RuntimeException("expected opening (");
        }

        return null;
    }
}
