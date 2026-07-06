/**
 * Analisador de contexto (Etapa 4).
 *
 * Percorre a AST via padrão Visitor e verifica:
 *   - todas as variáveis usadas foram declaradas;
 *   - tipos dos operandos são compatíveis com cada operador;
 *   - tipos de expressões são compatíveis com as variáveis atribuídas;
 *   - condições de 'if' e 'while' são do tipo booleano.
 *
 * Retorna a TabelaSimbolos preenchida para ser usada pelo Coder.
 *
 * Regras de tipos implementadas:
 *
 *   Operadores aritméticos (+, -, *, div, mod):
 *     operandos: inteiro × inteiro → inteiro
 *
 *   Operadores lógicos (and, or):
 *     operandos: booleano × booleano → booleano
 *
 *   Operador lógico unário (not):
 *     operando: booleano → booleano
 *
 *   Negação aritmética unária (-):
 *     operando: inteiro → inteiro
 *
 *   Operadores relacionais (<, <=, >, >=):
 *     operandos: inteiro × inteiro → booleano
 *
 *   Operadores de igualdade (=, <>):
 *     operandos: mesmo tipo × mesmo tipo → booleano
 */
public final class Checker implements Visitor<TabelaSimbolos.Tipo> {

    private TabelaSimbolos tabela;
    private GeradorRotulos rotulos;
    private String funcaoAtual;

    /**
     * Ponto de entrada: verifica o programa e retorna a tabela preenchida.
     *
     * @throws ErroContexto em caso de violação de regra de contexto.
     */
    public TabelaSimbolos verificar(No.Programa programa) {
        tabela = new TabelaSimbolos();
        rotulos = new GeradorRotulos();
        funcaoAtual = null;
        programa.aceitar(this);
        return tabela;
    }

    // ─────────────────────────────────────────────
    // Nó raiz e bloco
    // ─────────────────────────────────────────────

    @Override
    public TabelaSimbolos.Tipo visitarPrograma(No.Programa no) {
        no.bloco.aceitar(this);
        return null;
    }

    @Override
    public TabelaSimbolos.Tipo visitarBloco(No.Bloco no) {
        for (No.DeclaracaoVar d : no.declaracoes) {
            d.aceitar(this);
        }
        for (No.DeclaracaoSubprograma sub : no.subprogramas) {
            if (sub instanceof No.DeclaracaoProcedimento) {
                tabela.declararProcedimento(
                    sub.nome.lexema, rotulos.novo("PROC_" + sub.nome.lexema),
                    sub.nome.linha, sub.nome.coluna);
            } else {
                No.DeclaracaoFuncao f = (No.DeclaracaoFuncao) sub;
                tabela.declararFuncao(
                    f.nome.lexema, tokenParaTipo(f.tipoRetorno),
                    rotulos.novo("FUNC_" + f.nome.lexema),
                    f.nome.linha, f.nome.coluna);
            }
        }
        for (No.DeclaracaoSubprograma sub : no.subprogramas) {
            sub.aceitar(this);
        }
        no.corpo.aceitar(this);
        return null;
    }

    @Override
    public TabelaSimbolos.Tipo visitarDeclaracaoVar(No.DeclaracaoVar no) {
        TabelaSimbolos.Tipo tipo = tokenParaTipo(no.tipo);
        for (Token nome : no.nomes) {
            tabela.declarar(nome.lexema, tipo, nome.linha, nome.coluna);
        }
        return null;
    }

    @Override
    public TabelaSimbolos.Tipo visitarDeclaracaoProcedimento(No.DeclaracaoProcedimento no) {
        no.bloco.aceitar(this);
        return null;
    }

    @Override
    public TabelaSimbolos.Tipo visitarDeclaracaoFuncao(No.DeclaracaoFuncao no) {
        String anterior = funcaoAtual;
        funcaoAtual = no.nome.lexema;
        no.bloco.aceitar(this);
        funcaoAtual = anterior;
        return null;
    }

    // ─────────────────────────────────────────────
    // Comandos
    // ─────────────────────────────────────────────

    @Override
    public TabelaSimbolos.Tipo visitarComposto(No.Composto no) {
        for (No.Comando cmd : no.comandos) {
            cmd.aceitar(this);
        }
        return null;
    }

    @Override
    public TabelaSimbolos.Tipo visitarAtribuicao(No.Atribuicao no) {
        TabelaSimbolos.Entrada entrada = tabela.buscar(
            no.variavel.lexema, no.variavel.linha, no.variavel.coluna);
        if (entrada.categoria == TabelaSimbolos.Categoria.PROCEDIMENTO) {
            throw new ErroContexto(
                "'" + no.variavel.lexema + "' e procedimento e nao recebe atribuicao",
                no.variavel.linha, no.variavel.coluna);
        }
        if (entrada.categoria == TabelaSimbolos.Categoria.FUNCAO
                && !entrada.nome.equals(funcaoAtual)) {
            throw new ErroContexto(
                "'" + no.variavel.lexema + "' e funcao e so pode receber atribuicao dentro do proprio corpo",
                no.variavel.linha, no.variavel.coluna);
        }
        TabelaSimbolos.Tipo tipoValor = no.valor.aceitar(this);
        if (entrada.tipo != tipoValor) {
            throw new ErroContexto(
                "tipo incompativel na atribuicao de '" + no.variavel.lexema + "': " +
                "variavel e " + entrada.tipo + ", expressao e " + tipoValor,
                no.variavel.linha, no.variavel.coluna);
        }
        return null;
    }

    @Override
    public TabelaSimbolos.Tipo visitarChamadaProcedimento(No.ChamadaProcedimento no) {
        tabela.buscarProcedimento(no.nome.lexema, no.nome.linha, no.nome.coluna);
        return null;
    }

    @Override
    public TabelaSimbolos.Tipo visitarSe(No.Se no) {
        TabelaSimbolos.Tipo tipoCondicao = no.condicao.aceitar(this);
        if (tipoCondicao != TabelaSimbolos.Tipo.BOOLEANO) {
            throw new ErroContexto(
                "condicao do 'if' deve ser booleana, mas e " + tipoCondicao,
                no.token.linha, no.token.coluna);
        }
        no.entao.aceitar(this);
        if (no.senao != null) {
            no.senao.aceitar(this);
        }
        return null;
    }

    @Override
    public TabelaSimbolos.Tipo visitarEnquanto(No.Enquanto no) {
        TabelaSimbolos.Tipo tipoCondicao = no.condicao.aceitar(this);
        if (tipoCondicao != TabelaSimbolos.Tipo.BOOLEANO) {
            throw new ErroContexto(
                "condicao do 'while' deve ser booleana, mas e " + tipoCondicao,
                no.token.linha, no.token.coluna);
        }
        no.corpo.aceitar(this);
        return null;
    }

    @Override
    public TabelaSimbolos.Tipo visitarVazio(No.Vazio no) {
        return null;
    }

    // ─────────────────────────────────────────────
    // Expressões
    // ─────────────────────────────────────────────

    @Override
    public TabelaSimbolos.Tipo visitarBinario(No.Binario no) {
        TabelaSimbolos.Tipo esq = no.esquerda.aceitar(this);
        TabelaSimbolos.Tipo dir = no.direita.aceitar(this);
        Token op = no.operador;

        switch (op.tipo) {
            case MAIS: case MENOS: case VEZES: case DIV: case MOD:
                exigir(esq, TabelaSimbolos.Tipo.INTEIRO, op, "esquerda");
                exigir(dir, TabelaSimbolos.Tipo.INTEIRO, op, "direita");
                return TabelaSimbolos.Tipo.INTEIRO;

            case E: case OU:
                exigir(esq, TabelaSimbolos.Tipo.BOOLEANO, op, "esquerda");
                exigir(dir, TabelaSimbolos.Tipo.BOOLEANO, op, "direita");
                return TabelaSimbolos.Tipo.BOOLEANO;

            case MENOR: case MENOR_IGUAL: case MAIOR: case MAIOR_IGUAL:
                exigir(esq, TabelaSimbolos.Tipo.INTEIRO, op, "esquerda");
                exigir(dir, TabelaSimbolos.Tipo.INTEIRO, op, "direita");
                return TabelaSimbolos.Tipo.BOOLEANO;

            case IGUAL: case DIFERENTE:
                if (esq != dir) {
                    throw new ErroContexto(
                        "operandos de '" + op.lexema + "' devem ter o mesmo tipo: " +
                        "esquerda e " + esq + ", direita e " + dir,
                        op.linha, op.coluna);
                }
                return TabelaSimbolos.Tipo.BOOLEANO;

            default:
                throw new ErroContexto(
                    "operador binario nao suportado: '" + op.lexema + "'",
                    op.linha, op.coluna);
        }
    }

    @Override
    public TabelaSimbolos.Tipo visitarUnario(No.Unario no) {
        TabelaSimbolos.Tipo tipo = no.operando.aceitar(this);
        Token op = no.operador;
        switch (op.tipo) {
            case MENOS:
                exigir(tipo, TabelaSimbolos.Tipo.INTEIRO, op, "operando");
                return TabelaSimbolos.Tipo.INTEIRO;
            case NAO:
                exigir(tipo, TabelaSimbolos.Tipo.BOOLEANO, op, "operando");
                return TabelaSimbolos.Tipo.BOOLEANO;
            default:
                throw new ErroContexto(
                    "operador unario nao suportado: '" + op.lexema + "'",
                    op.linha, op.coluna);
        }
    }

    @Override
    public TabelaSimbolos.Tipo visitarIdentificador(No.Identificador no) {
        return tabela.buscarVariavelOuFuncao(
            no.token.lexema, no.token.linha, no.token.coluna).tipo;
    }

    @Override
    public TabelaSimbolos.Tipo visitarLiteralInteiro(No.LiteralInteiro no) {
        return TabelaSimbolos.Tipo.INTEIRO;
    }

    @Override
    public TabelaSimbolos.Tipo visitarLiteralBooleano(No.LiteralBooleano no) {
        return TabelaSimbolos.Tipo.BOOLEANO;
    }

    @Override
    public TabelaSimbolos.Tipo visitarLiteralTexto(No.LiteralTexto no) {
        throw new ErroContexto(
            "literal de texto nao e suportado: a linguagem aceita apenas 'integer' e 'boolean'",
            no.token.linha, no.token.coluna);
    }

    // ─────────────────────────────────────────────
    // Utilitários
    // ─────────────────────────────────────────────

    private void exigir(TabelaSimbolos.Tipo obtido, TabelaSimbolos.Tipo esperado,
                        Token operador, String lado) {
        if (obtido != esperado) {
            throw new ErroContexto(
                "operando " + lado + " de '" + operador.lexema +
                "' deve ser " + esperado + ", mas e " + obtido,
                operador.linha, operador.coluna);
        }
    }

    private static TabelaSimbolos.Tipo tokenParaTipo(Token t) {
        if (t.eh(Token.Tipo.INTEIRO))  return TabelaSimbolos.Tipo.INTEIRO;
        if (t.eh(Token.Tipo.BOOLEANO)) return TabelaSimbolos.Tipo.BOOLEANO;
        throw new ErroContexto("tipo desconhecido: '" + t.lexema + "'", t.linha, t.coluna);
    }
}
