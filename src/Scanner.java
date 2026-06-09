import java.util.ArrayList;
import java.util.List;

public final class Scanner {
    private final String source;
    private int index;
    private int linha;
    private int coluna;

    public Scanner(String source) {
        this.source = source == null ? "" : source;
        this.index = 0;
        this.linha = 1;
        this.coluna = 1;
    }

    public List<Token> scanAll() {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = nextToken();
            tokens.add(token);
        } while (!token.eh(Token.Tipo.FIM_ARQUIVO));
        return tokens;
    }

    public Token nextToken() {
        skipSeparatorsAndComments();

        int linhaInicial = linha;
        int colunaInicial = coluna;

        if (isAtEnd()) {
            return new Token(Token.Tipo.FIM_ARQUIVO, "", linhaInicial, colunaInicial);
        }

        char current = peek();

        if (isIdentifierStart(current)) {
            String lexema = readIdentifier();
            return new Token(Token.palavraReservadaOuIdentificador(lexema), lexema, linhaInicial, colunaInicial);
        }

        if (Character.isDigit(current)) {
            return new Token(Token.Tipo.LITERAL_INTEIRO, readNumber(), linhaInicial, colunaInicial);
        }

        if (current == '\'') {
            return new Token(Token.Tipo.LITERAL_TEXTO, readString(linhaInicial, colunaInicial), linhaInicial, colunaInicial);
        }

        return readSymbol(linhaInicial, colunaInicial);
    }

    private void skipSeparatorsAndComments() {
        boolean consumed;
        do {
            consumed = false;

            while (!isAtEnd() && Character.isWhitespace(peek())) {
                advance();
                consumed = true;
            }

            if (matchComment()) {
                consumed = true;
            }
        } while (consumed);
    }

    private boolean matchComment() {
        if (isAtEnd()) {
            return false;
        }

        if (peek() == '{') {
            int linhaInicial = linha;
            int colunaInicial = coluna;
            advance();
            while (!isAtEnd() && peek() != '}') {
                advance();
            }
            if (isAtEnd()) {
                throw new Erro("comentario iniciado com '{' nao foi fechado", linhaInicial, colunaInicial);
            }
            advance();
            return true;
        }

        if (peek() == '(' && peekNext() == '*') {
            int linhaInicial = linha;
            int colunaInicial = coluna;
            advance();
            advance();
            while (!isAtEnd() && !(peek() == '*' && peekNext() == ')')) {
                advance();
            }
            if (isAtEnd()) {
                throw new Erro("comentario iniciado com '(*' nao foi fechado", linhaInicial, colunaInicial);
            }
            advance();
            advance();
            return true;
        }

        if (peek() == '/' && peekNext() == '/') {
            while (!isAtEnd() && peek() != '\n') {
                advance();
            }
            return true;
        }

        return false;
    }

    private String readIdentifier() {
        StringBuilder builder = new StringBuilder();
        while (!isAtEnd() && isIdentifierPart(peek())) {
            builder.append(advance());
        }
        return builder.toString();
    }

    private String readNumber() {
        StringBuilder builder = new StringBuilder();
        while (!isAtEnd() && Character.isDigit(peek())) {
            builder.append(advance());
        }

        if (!isAtEnd() && isIdentifierStart(peek())) {
            throw new Erro("numero inteiro nao pode ser seguido diretamente por letra ou '_'", linha, coluna);
        }

        return builder.toString();
    }

    private String readString(int linhaInicial, int colunaInicial) {
        StringBuilder builder = new StringBuilder();
        advance();

        while (!isAtEnd() && peek() != '\'') {
            char current = advance();
            if (current == '\n' || current == '\r') {
                throw new Erro("literal de string nao foi fechado antes do fim da linha", linhaInicial, colunaInicial);
            }
            builder.append(current);
        }

        if (isAtEnd()) {
            throw new Erro("literal de string nao foi fechado", linhaInicial, colunaInicial);
        }

        advance();
        return builder.toString();
    }

    private Token readSymbol(int linhaInicial, int colunaInicial) {
        char current = advance();

        switch (current) {
            case '+':
                return token(Token.Tipo.MAIS, "+", linhaInicial, colunaInicial);
            case '-':
                return token(Token.Tipo.MENOS, "-", linhaInicial, colunaInicial);
            case '*':
                return token(Token.Tipo.VEZES, "*", linhaInicial, colunaInicial);
            case '/':
                return token(Token.Tipo.BARRA, "/", linhaInicial, colunaInicial);
            case ';':
                return token(Token.Tipo.PONTO_E_VIRGULA, ";", linhaInicial, colunaInicial);
            case ':':
                if (match('=')) {
                    return token(Token.Tipo.ATRIBUICAO, ":=", linhaInicial, colunaInicial);
                }
                return token(Token.Tipo.DOIS_PONTOS, ":", linhaInicial, colunaInicial);
            case ',':
                return token(Token.Tipo.VIRGULA, ",", linhaInicial, colunaInicial);
            case '.':
                return token(Token.Tipo.PONTO, ".", linhaInicial, colunaInicial);
            case '(':
                return token(Token.Tipo.ABRE_PARENTESE, "(", linhaInicial, colunaInicial);
            case ')':
                return token(Token.Tipo.FECHA_PARENTESE, ")", linhaInicial, colunaInicial);
            case '[':
                return token(Token.Tipo.ABRE_COLCHETE, "[", linhaInicial, colunaInicial);
            case ']':
                return token(Token.Tipo.FECHA_COLCHETE, "]", linhaInicial, colunaInicial);
            case '=':
                return token(Token.Tipo.IGUAL, "=", linhaInicial, colunaInicial);
            case '<':
                if (match('=')) {
                    return token(Token.Tipo.MENOR_IGUAL, "<=", linhaInicial, colunaInicial);
                }
                if (match('>')) {
                    return token(Token.Tipo.DIFERENTE, "<>", linhaInicial, colunaInicial);
                }
                return token(Token.Tipo.MENOR, "<", linhaInicial, colunaInicial);
            case '>':
                if (match('=')) {
                    return token(Token.Tipo.MAIOR_IGUAL, ">=", linhaInicial, colunaInicial);
                }
                return token(Token.Tipo.MAIOR, ">", linhaInicial, colunaInicial);
            default:
                throw new Erro("caractere inesperado '" + printable(current) + "'", linhaInicial, colunaInicial);
        }
    }

    private Token token(Token.Tipo tipo, String lexema, int linhaInicial, int colunaInicial) {
        return new Token(tipo, lexema, linhaInicial, colunaInicial);
    }

    private boolean match(char expected) {
        if (isAtEnd() || peek() != expected) {
            return false;
        }
        advance();
        return true;
    }

    private char advance() {
        char current = source.charAt(index++);
        if (current == '\n') {
            linha++;
            coluna = 1;
        } else {
            coluna++;
        }
        return current;
    }

    private char peek() {
        return source.charAt(index);
    }

    private char peekNext() {
        if (index + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(index + 1);
    }

    private boolean isAtEnd() {
        return index >= source.length();
    }

    private boolean isIdentifierStart(char value) {
        return Character.isLetter(value) || value == '_';
    }

    private boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    private String printable(char value) {
        switch (value) {
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";
            default:
                return Character.toString(value);
        }
    }
}
