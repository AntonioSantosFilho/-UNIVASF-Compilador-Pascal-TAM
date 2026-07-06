import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
            // Etapa 2-3: analise sintatica e montagem da AST.
            No.Programa ast;
            try {
                ast = new Parser(tokens).parsePrograma();
            } catch (ErroSintatico error) {
                System.err.println(error.getMessage());
                return 1;
            }

            // Etapa 4: analise de contexto (tabela de simbolos + verificacao de tipos).
            TabelaSimbolos tabela;
            try {
                tabela = new Checker().verificar(ast);
            } catch (ErroContexto error) {
                System.err.println(error.getMessage());
                return 1;
            }

            // Etapa 5: geracao de codigo TAM.
            List<String> codigo = new Coder(tabela).gerar(ast);

            // Formata e escreve em arquivo .txt com instrucoes TAM.
            String nomeSaida = options.inputFile.getFileName().toString()
                .replaceAll("\\.[^.]+$", "") + "-tam.txt";
            Path arquivoSaida = options.inputFile.resolveSibling(nomeSaida);

            List<String> linhas = new ArrayList<>();
            linhas.addAll(codigo);

            try {
                Files.write(arquivoSaida, linhas, StandardCharsets.UTF_8);
            } catch (IOException error) {
                System.err.println("Nao foi possivel escrever: " + arquivoSaida);
                System.err.println(error.getMessage());
                return 2;
            }

            System.out.println("Codigo TAM gerado em: " + arquivoSaida);
            System.out.println("Total de instrucoes: " + codigo.size());
        }

        if (!options.printTokens && !options.printAst && !options.generateCode) {
            System.out.println("Analise lexica concluida sem erros.");
            System.out.println("Use --ast para analise sintatica e visualizacao da AST.");
            System.out.println("Use --code para verificacao de contexto e geracao de codigo TAM.");
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
        System.out.println("  --ast       analisa sintaticamente e imprime a AST");
        System.out.println("  --code      verifica contexto e gera codigo TAM em arquivo .txt");
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
