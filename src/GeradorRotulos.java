public final class GeradorRotulos {
    private int contador;

    public String novo(String prefixo) {
        return prefixo + "_" + contador++;
    }
}
