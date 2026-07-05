public final class ErroContexto extends RuntimeException {
    private final int linha;
    private final int coluna;

    public ErroContexto(String mensagem, int linha, int coluna) {
        super(String.format("Erro de contexto na linha %d, coluna %d: %s", linha, coluna, mensagem));
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
