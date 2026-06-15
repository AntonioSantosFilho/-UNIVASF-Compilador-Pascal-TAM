/**
 * Interface do padrão Visitor para percorrer a AST.
 *
 * Cada método recebe um tipo específico de nó e retorna um valor do tipo T.
 * Use T = Void quando o visitor não precisa retornar nada (ex.: Printer).
 *
 * Para adicionar uma nova operação sobre a AST basta criar uma nova classe
 * que implemente esta interface, sem modificar os nós existentes.
 */
public interface Visitor<T> {
    T visitarPrograma(No.Programa no);
    T visitarBloco(No.Bloco no);
    T visitarDeclaracaoVar(No.DeclaracaoVar no);
    T visitarComposto(No.Composto no);
    T visitarAtribuicao(No.Atribuicao no);
    T visitarSe(No.Se no);
    T visitarEnquanto(No.Enquanto no);
    T visitarVazio(No.Vazio no);
    T visitarBinario(No.Binario no);
    T visitarUnario(No.Unario no);
    T visitarIdentificador(No.Identificador no);
    T visitarLiteralInteiro(No.LiteralInteiro no);
    T visitarLiteralBooleano(No.LiteralBooleano no);
    T visitarLiteralTexto(No.LiteralTexto no);
}
