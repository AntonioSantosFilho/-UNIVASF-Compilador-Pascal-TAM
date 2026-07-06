/**
 * Imprime a AST no console com indentação para mostrar a hierarquia.
 *
 * Implementa Visitor<Void> — não precisa retornar valores, apenas imprime.
 *
 * Exemplo de saída para o programa:
 *   program Exemplo;
 *   var x: integer;
 *   begin x := 10 end.
 *
 *   Programa: Exemplo
 *     Variaveis:
 *       var x: integer
 *     Bloco
 *       := x
 *         int:10
 */
public final class Printer implements Visitor<Void> {

    private int nivel;

    public void imprimir(No no) {
        nivel = 0;
        no.aceitar(this);
    }

    // ─────────────────────────────────────────────
    // Nó raiz
    // ─────────────────────────────────────────────

    @Override
    public Void visitarPrograma(No.Programa no) {
        linha("Programa: " + no.nome.lexema);
        nivel++;
        no.bloco.aceitar(this);
        nivel--;
        return null;
    }

    // ─────────────────────────────────────────────
    // Bloco e declarações
    // ─────────────────────────────────────────────

    @Override
    public Void visitarBloco(No.Bloco no) {
        if (!no.declaracoes.isEmpty()) {
            linha("Variaveis:");
            nivel++;
            for (No.DeclaracaoVar d : no.declaracoes) {
                d.aceitar(this);
            }
            nivel--;
        }
        if (!no.subprogramas.isEmpty()) {
            linha("Subprogramas:");
            nivel++;
            for (No.DeclaracaoSubprograma sub : no.subprogramas) {
                sub.aceitar(this);
            }
            nivel--;
        }
        no.corpo.aceitar(this);
        return null;
    }

    @Override
    public Void visitarDeclaracaoVar(No.DeclaracaoVar no) {
        StringBuilder sb = new StringBuilder("var");
        for (int i = 0; i < no.nomes.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(" ").append(no.nomes.get(i).lexema);
        }
        sb.append(": ").append(no.tipo.lexema);
        linha(sb.toString());
        return null;
    }

    @Override
    public Void visitarDeclaracaoProcedimento(No.DeclaracaoProcedimento no) {
        linha("procedure " + no.nome.lexema);
        nivel++;
        no.bloco.aceitar(this);
        nivel--;
        return null;
    }

    @Override
    public Void visitarDeclaracaoFuncao(No.DeclaracaoFuncao no) {
        linha("function " + no.nome.lexema + ": " + no.tipoRetorno.lexema);
        nivel++;
        no.bloco.aceitar(this);
        nivel--;
        return null;
    }

    // ─────────────────────────────────────────────
    // Comandos
    // ─────────────────────────────────────────────

    @Override
    public Void visitarComposto(No.Composto no) {
        linha("Bloco");
        nivel++;
        for (No.Comando cmd : no.comandos) {
            cmd.aceitar(this);
        }
        nivel--;
        return null;
    }

    @Override
    public Void visitarAtribuicao(No.Atribuicao no) {
        linha(":= " + no.variavel.lexema);
        nivel++;
        no.valor.aceitar(this);
        nivel--;
        return null;
    }

    @Override
    public Void visitarChamadaProcedimento(No.ChamadaProcedimento no) {
        linha("call " + no.nome.lexema);
        return null;
    }

    @Override
    public Void visitarSe(No.Se no) {
        linha("Se");
        nivel++;
        linha("condicao:");
        nivel++;
        no.condicao.aceitar(this);
        nivel--;
        linha("entao:");
        nivel++;
        no.entao.aceitar(this);
        nivel--;
        if (no.senao != null) {
            linha("senao:");
            nivel++;
            no.senao.aceitar(this);
            nivel--;
        }
        nivel--;
        return null;
    }

    @Override
    public Void visitarEnquanto(No.Enquanto no) {
        linha("Enquanto");
        nivel++;
        linha("condicao:");
        nivel++;
        no.condicao.aceitar(this);
        nivel--;
        linha("corpo:");
        nivel++;
        no.corpo.aceitar(this);
        nivel--;
        nivel--;
        return null;
    }

    @Override
    public Void visitarVazio(No.Vazio no) {
        // Comando vazio não produz saída.
        return null;
    }

    // ─────────────────────────────────────────────
    // Expressões
    // ─────────────────────────────────────────────

    @Override
    public Void visitarBinario(No.Binario no) {
        linha("(" + no.operador.lexema + ")");
        nivel++;
        no.esquerda.aceitar(this);
        no.direita.aceitar(this);
        nivel--;
        return null;
    }

    @Override
    public Void visitarUnario(No.Unario no) {
        linha("[" + no.operador.lexema + "]");
        nivel++;
        no.operando.aceitar(this);
        nivel--;
        return null;
    }

    @Override
    public Void visitarIdentificador(No.Identificador no) {
        linha("id:" + no.token.lexema);
        return null;
    }

    @Override
    public Void visitarLiteralInteiro(No.LiteralInteiro no) {
        linha("int:" + no.token.lexema);
        return null;
    }

    @Override
    public Void visitarLiteralBooleano(No.LiteralBooleano no) {
        linha("bool:" + no.token.lexema);
        return null;
    }

    @Override
    public Void visitarLiteralTexto(No.LiteralTexto no) {
        linha("texto:'" + no.token.lexema + "'");
        return null;
    }

    // ─────────────────────────────────────────────
    // Utilitário
    // ─────────────────────────────────────────────

    private void linha(String texto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nivel; i++) {
            sb.append("  ");
        }
        sb.append(texto);
        System.out.println(sb.toString());
    }
}
