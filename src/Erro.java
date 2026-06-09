public class Erro extends RuntimeException {
    private final int linha;
    private final int coluna;

    public Erro(String mensagem, int linha, int coluna) {
        super(String.format("Erro lexico na linha %d, coluna %d: %s", linha, coluna, mensagem));
        this.linha = linha;
        this.coluna = coluna;
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }
}
