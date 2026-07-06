import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tabela de simbolos do programa.
 *
 * Registra variaveis, procedimentos e funcoes. A geracao de codigo da Etapa 5
 * usa nomes simbolicos, mas o deslocamento continua disponivel para documentar
 * a ordem de alocacao na pilha.
 */
public final class TabelaSimbolos {

    public enum Tipo {
        INTEIRO, BOOLEANO;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum Categoria {
        VARIAVEL, PROCEDIMENTO, FUNCAO
    }

    /** Informacoes de um simbolo declarado. */
    public static final class Entrada {
        public final String nome;
        public final Categoria categoria;
        public final Tipo tipo;
        public final int deslocamento;
        public final String rotulo;

        public Entrada(String nome, Categoria categoria, Tipo tipo,
                       int deslocamento, String rotulo) {
            this.nome = nome;
            this.categoria = categoria;
            this.tipo = tipo;
            this.deslocamento = deslocamento;
            this.rotulo = rotulo;
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
                    "identificador '" + nome + "' ja foi declarado", linha, coluna);
        }
        tabela.put(nome, new Entrada(nome, Categoria.VARIAVEL, tipo, proximo++, null));
    }

    public void declararProcedimento(String nome, String rotulo, int linha, int coluna) {
        declararSubprograma(nome, Categoria.PROCEDIMENTO, null, rotulo, linha, coluna);
    }

    public void declararFuncao(String nome, Tipo tipo, String rotulo, int linha, int coluna) {
        declararSubprograma(nome, Categoria.FUNCAO, tipo, rotulo, linha, coluna);
    }

    private void declararSubprograma(String nome, Categoria categoria, Tipo tipo,
                                     String rotulo, int linha, int coluna) {
        if (tabela.containsKey(nome)) {
            throw new ErroContexto(
                    "identificador '" + nome + "' ja foi declarado", linha, coluna);
        }
        tabela.put(nome, new Entrada(nome, categoria, tipo, -1, rotulo));
    }

    /**
     * Busca um simbolo pelo nome.
     *
     * @throws ErroContexto se o nome nao tiver sido declarado.
     */
    public Entrada buscar(String nome, int linha, int coluna) {
        Entrada e = tabela.get(nome);
        if (e == null) {
            throw new ErroContexto(
                    "identificador '" + nome + "' nao foi declarado", linha, coluna);
        }
        return e;
    }

    public Entrada buscarVariavelOuFuncao(String nome, int linha, int coluna) {
        Entrada e = buscar(nome, linha, coluna);
        if (e.categoria != Categoria.VARIAVEL && e.categoria != Categoria.FUNCAO) {
            throw new ErroContexto("'" + nome + "' nao pode ser usado como expressao", linha, coluna);
        }
        return e;
    }

    public Entrada buscarProcedimento(String nome, int linha, int coluna) {
        Entrada e = buscar(nome, linha, coluna);
        if (e.categoria != Categoria.PROCEDIMENTO) {
            throw new ErroContexto("'" + nome + "' nao e procedimento", linha, coluna);
        }
        return e;
    }

    public Iterable<Entrada> entradas() {
        return tabela.values();
    }

    /** Total de variaveis declaradas (= numero de palavras a reservar na pilha). */
    public int totalVariaveis() {
        return proximo;
    }
}
