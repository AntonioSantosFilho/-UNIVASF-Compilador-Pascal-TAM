import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tabela de símbolos para variáveis globais do programa.
 *
 * Armazena, para cada variável declarada:
 *   - o tipo (INTEIRO ou BOOLEANO);
 *   - o deslocamento em relação ao SB (Stack Base) da máquina TAM.
 *
 * Como a linguagem suportada só possui escopo global (sem procedimentos
 * ou funções nesta versão), uma única tabela cobre todo o programa.
 */
public final class TabelaSimbolos {

    public enum Tipo {
        INTEIRO, BOOLEANO;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /** Informações de uma variável declarada. */
    public static final class Entrada {
        /** Tipo da variável. */
        public final Tipo tipo;
        /** Offset em palavras a partir de SB (Stack Base). */
        public final int deslocamento;

        public Entrada(Tipo tipo, int deslocamento) {
            this.tipo = tipo;
            this.deslocamento = deslocamento;
        }
    }

    private final Map<String, Entrada> tabela = new LinkedHashMap<>();
    private int proximo = 0;

    /**
     * Declara uma nova variável.
     *
     * @throws ErroContexto se o nome já tiver sido declarado.
     */
    public void declarar(String nome, Tipo tipo, int linha, int coluna) {
        if (tabela.containsKey(nome)) {
            throw new ErroContexto(
                "variavel '" + nome + "' ja foi declarada", linha, coluna);
        }
        tabela.put(nome, new Entrada(tipo, proximo++));
    }

    /**
     * Busca uma variável pelo nome.
     *
     * @throws ErroContexto se o nome não tiver sido declarado.
     */
    public Entrada buscar(String nome, int linha, int coluna) {
        Entrada e = tabela.get(nome);
        if (e == null) {
            throw new ErroContexto(
                "variavel '" + nome + "' nao foi declarada", linha, coluna);
        }
        return e;
    }

    /** Total de variáveis declaradas (= número de palavras a reservar na pilha). */
    public int totalVariaveis() {
        return proximo;
    }
}
