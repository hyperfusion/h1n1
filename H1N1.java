import java.io.*;

public class H1N1 {
    public static void main(String[] args) {
        VM vm = new VM();

        if (args.length == 0) {
            // REPL
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String line = null;
            while (true) {
                System.out.print("> ");
                try {
                    line = in.readLine();
                } catch (IOException e) {
                    break;
                }
                if (line == null) break;

                Tokenizer t = new Tokenizer(line);
                Parser p = new Parser(t);
                Expr expr = null;
                try {
                    while (t.hasNext())
                        expr = vm.eval(p.parse());
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (RuntimeException e) {
                    System.err.println("error: " + e.getMessage());
                }
                if (expr != null)
                    System.out.println(expr);
            }

            System.out.println();
        } else {
            // interpret a source file
            char[] buf = null;
            try {
                File f = new File(args[0]);
                FileReader in = new FileReader(f);
                buf = new char[(int) f.length()];
                in.read(buf, 0, buf.length);
                in.close();
            } catch (FileNotFoundException e) {
                System.out.println("file not found: " + args[0]);
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Tokenizer t = new Tokenizer(new String(buf));
            Parser p = new Parser(t);
            try {
                while (t.hasNext())
                    vm.eval(p.parse());
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                System.err.println("error: " + e.getMessage());
            }
        }
    }
}
