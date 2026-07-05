import java.util.List;

/**
 * Hierarquia de nós da AST (Abstract Syntax Tree).
 *
 * Cada classe interna representa um tipo de nó.
 * O padrão Visitor é usado para percorrer a árvore sem modificar os nós.
 *
 * Gramática suportada (resumo):
 *
 *   Programa    ::= 'program' IDENT ';' Bloco '.'
 *   Bloco       ::= SecaoVar? CompostoCmd
 *   SecaoVar    ::= 'var' DeclVar+
 *   DeclVar     ::= ListaIdent ':' Tipo ';'
 *   CompostoCmd ::= 'begin' (Cmd (';' Cmd)*)? 'end'
 *   Cmd         ::= AtribCmd | SeCmd | EnquantoCmd | CompostoCmd | vazio
 *   AtribCmd    ::= IDENT ':=' Expr
 *   SeCmd       ::= 'if' Expr 'then' Cmd ('else' Cmd)?
 *   EnquantoCmd ::= 'while' Expr 'do' Cmd
 *   Expr        ::= ExpSimples (OpRel ExpSimples)?
 *   ExpSimples  ::= Termo (('+' | '-' | 'or') Termo)*
 *   Termo       ::= Fator (('*' | 'div' | 'mod' | 'and') Fator)*
 *   Fator       ::= IDENT | INTEIRO | TEXTO | 'true' | 'false'
 *                 | '(' Expr ')' | 'not' Fator | '-' Fator
 */
public abstract class No {

    public abstract <T> T aceitar(Visitor<T> v);

    // ─────────────────────────────────────────────
    // Nó raiz
    // ─────────────────────────────────────────────

    public static final class Programa extends No {
        public final Token nome;
        public final Bloco bloco;

        public Programa(Token nome, Bloco bloco) {
            this.nome = nome;
            this.bloco = bloco;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarPrograma(this);
        }
    }

    // ─────────────────────────────────────────────
    // Bloco e declarações
    // ─────────────────────────────────────────────

    public static final class Bloco extends No {
        /** Lista de grupos de declaração de variáveis (pode ser vazia). */
        public final List<DeclaracaoVar> declaracoes;
        public final Composto corpo;

        public Bloco(List<DeclaracaoVar> declaracoes, Composto corpo) {
            this.declaracoes = declaracoes;
            this.corpo = corpo;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarBloco(this);
        }
    }

    /** Uma linha de declaração: ex. "x, y : integer". */
    public static final class DeclaracaoVar extends No {
        public final List<Token> nomes;
        public final Token tipo;

        public DeclaracaoVar(List<Token> nomes, Token tipo) {
            this.nomes = nomes;
            this.tipo = tipo;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarDeclaracaoVar(this);
        }
    }

    // ─────────────────────────────────────────────
    // Comandos
    // ─────────────────────────────────────────────

    public abstract static class Comando extends No {}

    /** begin ... end */
    public static final class Composto extends Comando {
        public final List<Comando> comandos;

        public Composto(List<Comando> comandos) {
            this.comandos = comandos;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarComposto(this);
        }
    }

    /** variavel := expressao */
    public static final class Atribuicao extends Comando {
        public final Token variavel;
        public final Expressao valor;

        public Atribuicao(Token variavel, Expressao valor) {
            this.variavel = variavel;
            this.valor = valor;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarAtribuicao(this);
        }
    }

    /** if condicao then entao [else senao] */
    public static final class Se extends Comando {
        /** Token 'if' — usado para mensagens de erro com linha/coluna. */
        public final Token token;
        public final Expressao condicao;
        public final Comando entao;
        /** Pode ser null quando não há cláusula else. */
        public final Comando senao;

        public Se(Token token, Expressao condicao, Comando entao, Comando senao) {
            this.token = token;
            this.condicao = condicao;
            this.entao = entao;
            this.senao = senao;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarSe(this);
        }
    }

    /** while condicao do corpo */
    public static final class Enquanto extends Comando {
        /** Token 'while' — usado para mensagens de erro com linha/coluna. */
        public final Token token;
        public final Expressao condicao;
        public final Comando corpo;

        public Enquanto(Token token, Expressao condicao, Comando corpo) {
            this.token = token;
            this.condicao = condicao;
            this.corpo = corpo;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarEnquanto(this);
        }
    }

    /** Comando vazio — produzido por ponto-e-vírgula extra antes de 'end'. */
    public static final class Vazio extends Comando {
        public static final Vazio INSTANCIA = new Vazio();

        private Vazio() {}

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarVazio(this);
        }
    }

    // ─────────────────────────────────────────────
    // Expressões
    // ─────────────────────────────────────────────

    public abstract static class Expressao extends No {}

    /** esquerda operador direita  (ex.: x + 1, y >= 0, a and b) */
    public static final class Binario extends Expressao {
        public final Expressao esquerda;
        public final Token operador;
        public final Expressao direita;

        public Binario(Expressao esquerda, Token operador, Expressao direita) {
            this.esquerda = esquerda;
            this.operador = operador;
            this.direita = direita;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarBinario(this);
        }
    }

    /** operador operando  (ex.: not b, -x) */
    public static final class Unario extends Expressao {
        public final Token operador;
        public final Expressao operando;

        public Unario(Token operador, Expressao operando) {
            this.operador = operador;
            this.operando = operando;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarUnario(this);
        }
    }

    /** Nome de variável usado como expressão. */
    public static final class Identificador extends Expressao {
        public final Token token;

        public Identificador(Token token) {
            this.token = token;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarIdentificador(this);
        }
    }

    /** Literal numérico inteiro. Ex.: 10, 42. */
    public static final class LiteralInteiro extends Expressao {
        public final Token token;

        public LiteralInteiro(Token token) {
            this.token = token;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarLiteralInteiro(this);
        }
    }

    /** Literal booleano: true ou false. */
    public static final class LiteralBooleano extends Expressao {
        public final Token token;

        public LiteralBooleano(Token token) {
            this.token = token;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarLiteralBooleano(this);
        }
    }

    /** Literal de texto entre aspas simples. Ex.: 'ola'. */
    public static final class LiteralTexto extends Expressao {
        public final Token token;

        public LiteralTexto(Token token) {
            this.token = token;
        }

        @Override
        public <T> T aceitar(Visitor<T> v) {
            return v.visitarLiteralTexto(this);
        }
    }
}
