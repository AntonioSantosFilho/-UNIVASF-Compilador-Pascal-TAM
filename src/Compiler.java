import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Compiler {
    private Compiler() {
    }

    public static void main(String[] args) {
        int status = run(args);
        if (status != 0) {
            System.exit(status);
        }
    }

    static int run(String[] args) {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException error) {
            System.err.println(error.getMessage());
            printUsage();
            return 2;
        }

        if (options.help) {
            printUsage();
            return 0;
        }

        String source;
        try {
            source = Files.readString(options.inputFile, StandardCharsets.UTF_8);
        } catch (IOException error) {
            System.err.println("Nao foi possivel ler o arquivo: " + options.inputFile);
            System.err.println(error.getMessage());
            return 2;
        }

        List<Token> tokens;
        try {
            tokens = new Scanner(source).scanAll();
        } catch (Erro error) {
            System.err.println(error.getMessage());
            return 1;
        }

        if (options.printTokens) {
            printTokens(tokens);
        }

        if (options.printAst) {
            No.Programa ast;
            try {
                ast = new Parser(tokens).parsePrograma();
            } catch (ErroSintatico error) {
                System.err.println(error.getMessage());
                return 1;
            }
            new Printer().imprimir(ast);
        }

        if (options.generateCode) {
            System.out.println("Opcao --code reconhecida, mas a geracao de codigo pertence a etapa de Antonio.");
        }

        if (!options.printTokens && !options.printAst && !options.generateCode) {
            System.out.println("Analise lexica concluida sem erros. Use --ast para analise sintatica.");
        }

        return 0;
    }

    private static void printTokens(List<Token> tokens) {
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    private static void printUsage() {
        System.out.println("Uso: java -cp bin Compiler <arquivo-fonte> [opcoes]");
        System.out.println("Opcoes:");
        System.out.println("  --tokens    imprime a sequencia de tokens");
        System.out.println("  --ast       reserva a visualizacao da AST para a etapa sintatica");
        System.out.println("  --code      reserva a geracao de codigo para a etapa de codigo");
        System.out.println("  --help      mostra esta ajuda");
    }

    private static final class Options {
        private Path inputFile;
        private boolean printTokens;
        private boolean printAst;
        private boolean generateCode;
        private boolean help;

        static Options parse(String[] args) {
            Options options = new Options();

            if (args.length == 0) {
                throw new IllegalArgumentException("Arquivo-fonte nao informado.");
            }

            for (String arg : args) {
                switch (arg) {
                    case "--tokens":
                        options.printTokens = true;
                        break;
                    case "--ast":
                        options.printAst = true;
                        break;
                    case "--code":
                        options.generateCode = true;
                        break;
                    case "--help":
                    case "-h":
                        options.help = true;
                        break;
                    default:
                        if (arg.startsWith("--")) {
                            throw new IllegalArgumentException("Opcao desconhecida: " + arg);
                        }
                        if (options.inputFile != null) {
                            throw new IllegalArgumentException("Mais de um arquivo-fonte informado.");
                        }
                        options.inputFile = Path.of(arg);
                        break;
                }
            }

            if (!options.help && options.inputFile == null) {
                throw new IllegalArgumentException("Arquivo-fonte nao informado.");
            }

            return options;
        }
    }
}
