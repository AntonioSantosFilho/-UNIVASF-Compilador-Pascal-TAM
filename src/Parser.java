import java.util.ArrayList;
import java.util.List;

/**
 * Parser recursivo descendente LL(1) para o subconjunto de Pascal do projeto.
 *
 * Recebe a lista de tokens produzida pelo Scanner e monta a AST.
 * Lança ErroSintatico quando encontra um token inesperado.
 *
 * Gramática implementada:
 *
 *   Programa    ::= 'program' IDENT ';' Bloco '.'
 *   Bloco       ::= SecaoVar? CompostoCmd
 *   SecaoVar    ::= 'var' DeclVar+
 *   DeclVar     ::= ListaIdent ':' Tipo ';'
 *   ListaIdent  ::= IDENT (',' IDENT)*
 *   Tipo        ::= 'integer' | 'boolean'
 *   CompostoCmd ::= 'begin' (Cmd (';' Cmd)*)? 'end'
 *   Cmd         ::= AtribCmd | SeCmd | EnquantoCmd | CompostoCmd | vazio
 *   AtribCmd    ::= IDENT ':=' Expr
 *   SeCmd       ::= 'if' Expr 'then' Cmd ('else' Cmd)?
 *   EnquantoCmd ::= 'while' Expr 'do' Cmd
 *   Expr        ::= ExpSimples (OpRel ExpSimples)?
 *   OpRel       ::= '=' | '<>' | '<' | '<=' | '>' | '>='
 *   ExpSimples  ::= Termo (('+' | '-' | 'or') Termo)*
 *   Termo       ::= Fator (('*' | 'div' | 'mod' | 'and') Fator)*
 *   Fator       ::= IDENT | INTEIRO | TEXTO | 'true' | 'false'
 *                 | '(' Expr ')' | 'not' Fator | '-' Fator
 */
public final class Parser {

    private final List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    // ─────────────────────────────────────────────
    // Ponto de entrada público
    // ─────────────────────────────────────────────

    public No.Programa parsePrograma() {
        consumir(Token.Tipo.PROGRAMA);
        Token nome = consumir(Token.Tipo.IDENTIFICADOR);
        consumir(Token.Tipo.PONTO_E_VIRGULA);
        No.Bloco bloco = parseBloco();
        consumir(Token.Tipo.PONTO);
        consumir(Token.Tipo.FIM_ARQUIVO);
        return new No.Programa(nome, bloco);
    }

    // ─────────────────────────────────────────────
    // Bloco e declarações
    // ─────────────────────────────────────────────

    private No.Bloco parseBloco() {
        List<No.DeclaracaoVar> declaracoes = new ArrayList<>();
        if (atual().eh(Token.Tipo.VAR)) {
            declaracoes = parseSecaoVar();
        }
        No.Composto corpo = parseComposto();
        return new No.Bloco(declaracoes, corpo);
    }

    private List<No.DeclaracaoVar> parseSecaoVar() {
        consumir(Token.Tipo.VAR);
        List<No.DeclaracaoVar> lista = new ArrayList<>();
        // Enquanto a próxima linha começa com um identificador, há mais declarações.
        while (atual().eh(Token.Tipo.IDENTIFICADOR)) {
            lista.add(parseDeclVar());
        }
        if (lista.isEmpty()) {
            Token t = atual();
            throw new ErroSintatico(
                "esperava pelo menos uma declaracao de variavel apos 'var'",
                t.linha, t.coluna);
        }
        return lista;
    }

    private No.DeclaracaoVar parseDeclVar() {
        List<Token> nomes = new ArrayList<>();
        nomes.add(consumir(Token.Tipo.IDENTIFICADOR));
        while (atual().eh(Token.Tipo.VIRGULA)) {
            consumir(Token.Tipo.VIRGULA);
            nomes.add(consumir(Token.Tipo.IDENTIFICADOR));
        }
        consumir(Token.Tipo.DOIS_PONTOS);
        Token tipo = parseTipo();
        consumir(Token.Tipo.PONTO_E_VIRGULA);
        return new No.DeclaracaoVar(nomes, tipo);
    }

    private Token parseTipo() {
        Token t = atual();
        if (t.eh(Token.Tipo.INTEIRO) || t.eh(Token.Tipo.BOOLEANO)) {
            return avancar();
        }
        throw new ErroSintatico(
            "tipo esperado ('integer' ou 'boolean'), encontrado '" + t.lexema + "'",
            t.linha, t.coluna);
    }

    // ─────────────────────────────────────────────
    // Comandos
    // ─────────────────────────────────────────────

    private No.Composto parseComposto() {
        consumir(Token.Tipo.INICIO);
        List<No.Comando> comandos = new ArrayList<>();

        if (!atual().eh(Token.Tipo.FIM)) {
            comandos.add(parseComando());
            while (atual().eh(Token.Tipo.PONTO_E_VIRGULA)) {
                consumir(Token.Tipo.PONTO_E_VIRGULA);
                // Ponto-e-vírgula antes de 'end' é permitido; encerra a lista.
                if (atual().eh(Token.Tipo.FIM)) {
                    break;
                }
                comandos.add(parseComando());
            }
        }

        consumir(Token.Tipo.FIM);
        return new No.Composto(comandos);
    }

    private No.Comando parseComando() {
        Token t = atual();

        if (t.eh(Token.Tipo.IDENTIFICADOR)) {
            return parseAtribuicao();
        }
        if (t.eh(Token.Tipo.SE)) {
            return parseSe();
        }
        if (t.eh(Token.Tipo.ENQUANTO)) {
            return parseEnquanto();
        }
        if (t.eh(Token.Tipo.INICIO)) {
            return parseComposto();
        }
        // Qualquer outro token encerra a lista silenciosamente (comando vazio).
        return No.Vazio.INSTANCIA;
    }

    private No.Atribuicao parseAtribuicao() {
        Token variavel = consumir(Token.Tipo.IDENTIFICADOR);
        consumir(Token.Tipo.ATRIBUICAO);
        No.Expressao valor = parseExpressao();
        return new No.Atribuicao(variavel, valor);
    }

    private No.Se parseSe() {
        Token tokenSe = consumir(Token.Tipo.SE);
        No.Expressao condicao = parseExpressao();
        consumir(Token.Tipo.ENTAO);
        No.Comando entao = parseComando();
        // O 'else' sempre se liga ao 'if' mais próximo (dangling-else padrão).
        No.Comando senao = null;
        if (atual().eh(Token.Tipo.SENAO)) {
            consumir(Token.Tipo.SENAO);
            senao = parseComando();
        }
        return new No.Se(tokenSe, condicao, entao, senao);
    }

    private No.Enquanto parseEnquanto() {
        Token tokenEnquanto = consumir(Token.Tipo.ENQUANTO);
        No.Expressao condicao = parseExpressao();
        consumir(Token.Tipo.FACA);
        No.Comando corpo = parseComando();
        return new No.Enquanto(tokenEnquanto, condicao, corpo);
    }

    // ─────────────────────────────────────────────
    // Expressões (precedência de operadores)
    // ─────────────────────────────────────────────

    private No.Expressao parseExpressao() {
        No.Expressao esq = parseExpSimples();
        Token t = atual();
        if (t.eh(Token.Tipo.IGUAL)       || t.eh(Token.Tipo.DIFERENTE) ||
            t.eh(Token.Tipo.MENOR)        || t.eh(Token.Tipo.MENOR_IGUAL) ||
            t.eh(Token.Tipo.MAIOR)        || t.eh(Token.Tipo.MAIOR_IGUAL)) {
            Token op = avancar();
            No.Expressao dir = parseExpSimples();
            return new No.Binario(esq, op, dir);
        }
        return esq;
    }

    private No.Expressao parseExpSimples() {
        No.Expressao esq = parseTermo();
        Token t = atual();
        while (t.eh(Token.Tipo.MAIS) || t.eh(Token.Tipo.MENOS) || t.eh(Token.Tipo.OU)) {
            Token op = avancar();
            No.Expressao dir = parseTermo();
            esq = new No.Binario(esq, op, dir);
            t = atual();
        }
        return esq;
    }

    private No.Expressao parseTermo() {
        No.Expressao esq = parseFator();
        Token t = atual();
        while (t.eh(Token.Tipo.VEZES) || t.eh(Token.Tipo.DIV) ||
               t.eh(Token.Tipo.MOD)   || t.eh(Token.Tipo.E)) {
            Token op = avancar();
            No.Expressao dir = parseFator();
            esq = new No.Binario(esq, op, dir);
            t = atual();
        }
        return esq;
    }

    private No.Expressao parseFator() {
        Token t = atual();

        if (t.eh(Token.Tipo.IDENTIFICADOR)) {
            return new No.Identificador(avancar());
        }
        if (t.eh(Token.Tipo.LITERAL_INTEIRO)) {
            return new No.LiteralInteiro(avancar());
        }
        if (t.eh(Token.Tipo.LITERAL_TEXTO)) {
            return new No.LiteralTexto(avancar());
        }
        if (t.eh(Token.Tipo.VERDADEIRO) || t.eh(Token.Tipo.FALSO)) {
            return new No.LiteralBooleano(avancar());
        }
        if (t.eh(Token.Tipo.ABRE_PARENTESE)) {
            consumir(Token.Tipo.ABRE_PARENTESE);
            No.Expressao expr = parseExpressao();
            consumir(Token.Tipo.FECHA_PARENTESE);
            return expr;
        }
        if (t.eh(Token.Tipo.NAO)) {
            Token op = avancar();
            return new No.Unario(op, parseFator());
        }
        if (t.eh(Token.Tipo.MENOS)) {
            Token op = avancar();
            return new No.Unario(op, parseFator());
        }

        throw new ErroSintatico(
            "expressao esperada, encontrado '" + t.lexema + "'",
            t.linha, t.coluna);
    }

    // ─────────────────────────────────────────────
    // Utilidades
    // ─────────────────────────────────────────────

    private Token consumir(Token.Tipo esperado) {
        Token t = atual();
        if (!t.eh(esperado)) {
            throw new ErroSintatico(
                "esperava '" + nomeAmigavel(esperado) + "', encontrado '" + t.lexema + "'",
                t.linha, t.coluna);
        }
        return avancar();
    }

    private Token avancar() {
        Token t = tokens.get(pos);
        if (pos < tokens.size() - 1) {
            pos++;
        }
        return t;
    }

    private Token atual() {
        return tokens.get(pos);
    }

    private static String nomeAmigavel(Token.Tipo tipo) {
        switch (tipo) {
            case PROGRAMA:        return "program";
            case VAR:             return "var";
            case INICIO:          return "begin";
            case FIM:             return "end";
            case SE:              return "if";
            case ENTAO:           return "then";
            case SENAO:           return "else";
            case ENQUANTO:        return "while";
            case FACA:            return "do";
            case INTEIRO:         return "integer";
            case BOOLEANO:        return "boolean";
            case PONTO_E_VIRGULA: return ";";
            case DOIS_PONTOS:     return ":";
            case VIRGULA:         return ",";
            case PONTO:           return ".";
            case ATRIBUICAO:      return ":=";
            case ABRE_PARENTESE:  return "(";
            case FECHA_PARENTESE: return ")";
            case IDENTIFICADOR:   return "identificador";
            case FIM_ARQUIVO:     return "fim de arquivo";
            default:              return tipo.name();
        }
    }
}
