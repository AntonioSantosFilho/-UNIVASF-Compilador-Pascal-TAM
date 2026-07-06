import java.util.ArrayList;
import java.util.List;

/**
 * Gerador de código para a máquina TAM (Etapa 5).
 *
 * Percorre a AST via padrão Visitor e produz uma lista de instruções TAM.
 * A tabela de símbolos (já verificada pelo Checker) é usada para obter
 * o deslocamento (offset) de cada variável em relação ao SB (Stack Base).
 *
 * Instruções TAM usadas:
 *   PUSH n           - reserva n palavras na pilha (alocação de variáveis)
 *   LOADL v          - empilha o literal inteiro v
 *   LOAD(1) d[SB]    - empilha o valor da variável no offset d
 *   STORE(1) d[SB]   - desempilha e armazena na variável no offset d
 *   CALL(SB) p[PB]   - chama primitiva p do banco de primitivas (PB)
 *   JUMP d[CB]       - desvio incondicional para instrução d
 *   JUMPIF(c) d[CB]  - desvio condicional: pula para d se topo == c
 *   HALT             - encerra a execução
 *
 * Primitivas TAM referenciadas (Watt & Brown, "Programming Language
 * Processors in Java", Apêndice C):
 *   PB+1:  not    PB+2:  and    PB+3:  or
 *   PB+6:  neg    PB+7:  add    PB+8:  sub
 *   PB+9:  mul    PB+10: div    PB+11: mod
 *   PB+12: lt     PB+13: le     PB+14: ge
 *   PB+15: gt     PB+16: eq     PB+17: ne
 */
public final class Coder implements Visitor<Void> {

    // Endereços das rotinas primitivas na área PB da máquina TAM.
    private static final int PRIM_NOT = 1;
    private static final int PRIM_AND = 2;
    private static final int PRIM_OR  = 3;
    private static final int PRIM_NEG = 6;
    private static final int PRIM_ADD = 7;
    private static final int PRIM_SUB = 8;
    private static final int PRIM_MUL = 9;
    private static final int PRIM_DIV = 10;
    private static final int PRIM_MOD = 11;
    private static final int PRIM_LT  = 12;
    private static final int PRIM_LE  = 13;
    private static final int PRIM_GE  = 14;
    private static final int PRIM_GT  = 15;
    private static final int PRIM_EQ  = 16;
    private static final int PRIM_NE  = 17;

    private final TabelaSimbolos tabela;
    private final List<String> instrucoes = new ArrayList<>();
    private final GeradorRotulos rotulos = new GeradorRotulos();

    public Coder(TabelaSimbolos tabela) {
        this.tabela = tabela;
    }

    /**
     * Gera o código TAM para o programa e retorna a lista de instruções.
     * Cada posição da lista corresponde a um endereço no code store (CB).
     */
    public List<String> gerar(No.Programa programa) {
        instrucoes.clear();
        programa.aceitar(this);
        return instrucoes;
    }

    // ─────────────────────────────────────────────
    // Utilitários de emissão e backpatching
    // ─────────────────────────────────────────────

    /** Emite uma instrução e retorna seu endereço (índice na lista). */
    private int emitir(String instrucao) {
        instrucoes.add(instrucao);
        return instrucoes.size() - 1;
    }

    private void emitirRotulo(String rotulo) {
        instrucoes.add(rotulo + ":");
    }

    private void emitirAlocacaoVariaveis() {
        List<String> nomes = new ArrayList<>();
        for (TabelaSimbolos.Entrada entrada : tabela.entradas()) {
            if (entrada.categoria == TabelaSimbolos.Categoria.VARIAVEL) {
                nomes.add(entrada.nome);
            }
        }
        if (!nomes.isEmpty()) {
            emitir("PUSH " + String.join(", ", nomes));
        }
    }

    /** Corrige uma instrução previamente emitida com endereço desconhecido. */
    private void corrigir(int posicao, String instrucao) {
        instrucoes.set(posicao, instrucao);
    }

    /** Retorna o endereço da próxima instrução a ser emitida. */
    private int enderecoAtual() {
        return instrucoes.size();
    }

    // ─────────────────────────────────────────────
    // Visitor: nó raiz e bloco
    // ─────────────────────────────────────────────

    @Override
    public Void visitarPrograma(No.Programa no) {
        String rotuloPrincipal = rotulos.novo("MAIN");
        if (!no.bloco.subprogramas.isEmpty()) {
            emitir("JUMP " + rotuloPrincipal);
        }
        for (No.DeclaracaoSubprograma sub : no.bloco.subprogramas) {
            sub.aceitar(this);
        }
        if (!no.bloco.subprogramas.isEmpty()) {
            emitirRotulo(rotuloPrincipal);
        }
        emitirAlocacaoVariaveis();
        no.bloco.corpo.aceitar(this);
        emitir("HALT");
        return null;
    }

    @Override
    public Void visitarBloco(No.Bloco no) {
        emitirAlocacaoVariaveis();
        for (No.DeclaracaoSubprograma sub : no.subprogramas) {
            sub.aceitar(this);
        }
        no.corpo.aceitar(this);
        return null;
    }

    @Override
    public Void visitarDeclaracaoVar(No.DeclaracaoVar no) {
        // Declarações já foram processadas pelo Checker; nada a emitir.
        return null;
    }

    @Override
    public Void visitarDeclaracaoProcedimento(No.DeclaracaoProcedimento no) {
        TabelaSimbolos.Entrada entrada = tabela.buscar(
            no.nome.lexema, no.nome.linha, no.nome.coluna);
        emitirRotulo(entrada.rotulo);
        for (No.DeclaracaoSubprograma sub : no.bloco.subprogramas) {
            sub.aceitar(this);
        }
        no.bloco.corpo.aceitar(this);
        emitir("RETURN");
        return null;
    }

    @Override
    public Void visitarDeclaracaoFuncao(No.DeclaracaoFuncao no) {
        TabelaSimbolos.Entrada entrada = tabela.buscar(
            no.nome.lexema, no.nome.linha, no.nome.coluna);
        emitirRotulo(entrada.rotulo);
        for (No.DeclaracaoSubprograma sub : no.bloco.subprogramas) {
            sub.aceitar(this);
        }
        no.bloco.corpo.aceitar(this);
        emitir("LOAD " + no.nome.lexema);
        emitir("RETURN");
        return null;
    }

    // ─────────────────────────────────────────────
    // Visitor: comandos
    // ─────────────────────────────────────────────

    @Override
    public Void visitarComposto(No.Composto no) {
        for (No.Comando cmd : no.comandos) {
            cmd.aceitar(this);
        }
        return null;
    }

    @Override
    public Void visitarAtribuicao(No.Atribuicao no) {
        // Empilha o valor da expressão.
        no.valor.aceitar(this);
        // Armazena no endereço da variável.
        TabelaSimbolos.Entrada entrada = tabela.buscar(
            no.variavel.lexema, no.variavel.linha, no.variavel.coluna);
        emitir("STORE " + entrada.nome);
        return null;
    }

    @Override
    public Void visitarChamadaProcedimento(No.ChamadaProcedimento no) {
        TabelaSimbolos.Entrada entrada = tabela.buscarProcedimento(
            no.nome.lexema, no.nome.linha, no.nome.coluna);
        emitir("CALL " + entrada.rotulo);
        return null;
    }

    @Override
    public Void visitarSe(No.Se no) {
        String rotuloElse = rotulos.novo("ELSE");
        String rotuloFim = rotulos.novo("FIM_IF");

        no.condicao.aceitar(this);
        emitir("JUMPIF(0) " + (no.senao == null ? rotuloFim : rotuloElse));
        no.entao.aceitar(this);
        if (no.senao != null) {
            emitir("JUMP " + rotuloFim);
            emitirRotulo(rotuloElse);
            no.senao.aceitar(this);
        }
        emitirRotulo(rotuloFim);
        return null;
    }

    @Override
    public Void visitarEnquanto(No.Enquanto no) {
        String rotuloInicio = rotulos.novo("WHILE");
        String rotuloFim = rotulos.novo("FIM_WHILE");

        emitirRotulo(rotuloInicio);
        no.condicao.aceitar(this);
        emitir("JUMPIF(0) " + rotuloFim);
        no.corpo.aceitar(this);
        emitir("JUMP " + rotuloInicio);
        emitirRotulo(rotuloFim);
        return null;
    }

    @Override
    public Void visitarVazio(No.Vazio no) {
        // Comando vazio não gera código.
        return null;
    }

    // ─────────────────────────────────────────────
    // Visitor: expressões
    // ─────────────────────────────────────────────

    @Override
    public Void visitarBinario(No.Binario no) {
        // Empilha operandos da esquerda para a direita, depois chama a primitiva.
        no.esquerda.aceitar(this);
        no.direita.aceitar(this);

        switch (no.operador.tipo) {
            case MAIS:        emitir("CALL(SB) " + PRIM_ADD + "[PB]"); break;
            case MENOS:       emitir("CALL(SB) " + PRIM_SUB + "[PB]"); break;
            case VEZES:       emitir("CALL(SB) " + PRIM_MUL + "[PB]"); break;
            case DIV:         emitir("CALL(SB) " + PRIM_DIV + "[PB]"); break;
            case MOD:         emitir("CALL(SB) " + PRIM_MOD + "[PB]"); break;
            case E:           emitir("CALL(SB) " + PRIM_AND + "[PB]"); break;
            case OU:          emitir("CALL(SB) " + PRIM_OR  + "[PB]"); break;
            case IGUAL:       emitir("CALL(SB) " + PRIM_EQ  + "[PB]"); break;
            case DIFERENTE:   emitir("CALL(SB) " + PRIM_NE  + "[PB]"); break;
            case MENOR:       emitir("CALL(SB) " + PRIM_LT  + "[PB]"); break;
            case MENOR_IGUAL: emitir("CALL(SB) " + PRIM_LE  + "[PB]"); break;
            case MAIOR_IGUAL: emitir("CALL(SB) " + PRIM_GE  + "[PB]"); break;
            case MAIOR:       emitir("CALL(SB) " + PRIM_GT  + "[PB]"); break;
            default:
                throw new RuntimeException(
                    "operador binario nao suportado no gerador: " + no.operador.lexema);
        }
        return null;
    }

    @Override
    public Void visitarUnario(No.Unario no) {
        no.operando.aceitar(this);
        switch (no.operador.tipo) {
            case MENOS: emitir("CALL(SB) " + PRIM_NEG + "[PB]"); break;
            case NAO:   emitir("CALL(SB) " + PRIM_NOT + "[PB]"); break;
            default:
                throw new RuntimeException(
                    "operador unario nao suportado no gerador: " + no.operador.lexema);
        }
        return null;
    }

    @Override
    public Void visitarIdentificador(No.Identificador no) {
        TabelaSimbolos.Entrada entrada = tabela.buscarVariavelOuFuncao(
            no.token.lexema, no.token.linha, no.token.coluna);
        if (entrada.categoria == TabelaSimbolos.Categoria.FUNCAO) {
            emitir("CALL " + entrada.rotulo);
        } else {
            emitir("LOAD " + entrada.nome);
        }
        return null;
    }

    @Override
    public Void visitarLiteralInteiro(No.LiteralInteiro no) {
        emitir("LOADL " + no.token.lexema);
        return null;
    }

    @Override
    public Void visitarLiteralBooleano(No.LiteralBooleano no) {
        // true = 1, false = 0 na máquina TAM.
        int valor = no.token.eh(Token.Tipo.VERDADEIRO) ? 1 : 0;
        emitir("LOADL " + valor);
        return null;
    }

    @Override
    public Void visitarLiteralTexto(No.LiteralTexto no) {
        throw new RuntimeException(
            "literal de texto nao suportado na geracao de codigo TAM");
    }
}
