import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Token {
    public enum Tipo {
        FIM_ARQUIVO,
        IDENTIFICADOR,
        LITERAL_INTEIRO,
        LITERAL_TEXTO,

        PROGRAMA,
        VAR,
        PROCEDIMENTO,
        FUNCAO,
        INICIO,
        FIM,
        SE,
        ENTAO,
        SENAO,
        ENQUANTO,
        FACA,
        INTEIRO,
        BOOLEANO,
        VERDADEIRO,
        FALSO,
        E,
        OU,
        NAO,
        DIV,
        MOD,

        MAIS,
        MENOS,
        VEZES,
        BARRA,
        ATRIBUICAO,
        IGUAL,
        DIFERENTE,
        MENOR,
        MENOR_IGUAL,
        MAIOR,
        MAIOR_IGUAL,

        PONTO_E_VIRGULA,
        DOIS_PONTOS,
        VIRGULA,
        PONTO,
        ABRE_PARENTESE,
        FECHA_PARENTESE,
        ABRE_COLCHETE,
        FECHA_COLCHETE
    }

    private static final Map<String, Tipo> PALAVRAS_RESERVADAS;

    static {
        Map<String, Tipo> palavras = new HashMap<>();
        palavras.put("program", Tipo.PROGRAMA);
        palavras.put("var", Tipo.VAR);
        palavras.put("procedure", Tipo.PROCEDIMENTO);
        palavras.put("function", Tipo.FUNCAO);
        palavras.put("begin", Tipo.INICIO);
        palavras.put("end", Tipo.FIM);
        palavras.put("if", Tipo.SE);
        palavras.put("then", Tipo.ENTAO);
        palavras.put("else", Tipo.SENAO);
        palavras.put("while", Tipo.ENQUANTO);
        palavras.put("do", Tipo.FACA);
        palavras.put("integer", Tipo.INTEIRO);
        palavras.put("boolean", Tipo.BOOLEANO);
        palavras.put("true", Tipo.VERDADEIRO);
        palavras.put("false", Tipo.FALSO);
        palavras.put("and", Tipo.E);
        palavras.put("or", Tipo.OU);
        palavras.put("not", Tipo.NAO);
        palavras.put("div", Tipo.DIV);
        palavras.put("mod", Tipo.MOD);
        PALAVRAS_RESERVADAS = Collections.unmodifiableMap(palavras);
    }

    public final Tipo tipo;
    public final String lexema;
    public final int linha;
    public final int coluna;

    public Token(Tipo tipo, String lexema, int linha, int coluna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linha = linha;
        this.coluna = coluna;
    }

    public static Tipo palavraReservadaOuIdentificador(String lexema) {
        Tipo palavra = PALAVRAS_RESERVADAS.get(lexema.toLowerCase());
        return palavra == null ? Tipo.IDENTIFICADOR : palavra;
    }

    public boolean eh(Tipo esperado) {
        return tipo == esperado;
    }

    @Override
    public String toString() {
        return String.format("%-16s %-15s linha %d, coluna %d", tipo, entreAspas(lexema), linha, coluna);
    }

    private static String entreAspas(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "\"\"";
        }
        return "\"" + valor.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}
