# Implementacao de Andrea: Parser, AST, Visitor e Printer

Este documento explica o que foi implementado por Andrea no compilador.

A ideia e ajudar quem ainda nao conhece bem Java, Pascal ou compiladores a entender:

- o que cada arquivo faz;
- como rodar a analise sintatica;
- como a AST funciona;
- como o padrao Visitor funciona;
- o que Antonio precisa saber para continuar.

## O que foi implementado

Foram criados estes arquivos:

```text
src/
  ErroSintatico.java
  No.java
  Visitor.java
  Parser.java
  Printer.java
```

Resumo simples:

- `ErroSintatico.java`: define como mostrar erros sintaticos.
- `No.java`: define todos os tipos de no da AST como classes internas.
- `Visitor.java`: interface do padrao Visitor para percorrer a AST.
- `Parser.java`: analisador sintatico; recebe tokens e monta a AST.
- `Printer.java`: imprime a AST no console com indentacao.

Alem disso, foram atualizados:

- `Compiler.java`: a opcao `--ast` agora chama o Parser e imprime a AST.
- `scripts/run-tests.ps1`: adicionados testes da etapa B.

## O que e analise sintatica?

A analise sintatica e a segunda etapa do compilador.

Ela recebe a lista de tokens criada pelo Scanner e verifica se a sequencia de tokens faz sentido segundo as regras da linguagem.

Exemplo:

```pascal
program Exemplo;
```

O Scanner ja entregou:

```text
PROGRAMA    "program"
IDENTIFICADOR "Exemplo"
PONTO_E_VIRGULA ";"
```

O Parser verifica: "esse programa comeca com program, depois um nome, depois ponto-e-virgula, certo."

Se algo estiver errado, como:

```pascal
program Exemplo
var
```

O Parser detecta que faltou `;` e mostra:

```text
Erro sintatico na linha 2, coluna 1: esperava ';', encontrado 'var'
```

## O que e a AST?

AST significa Arvore Sintatica Abstrata (em ingles: Abstract Syntax Tree).

E uma estrutura que representa a organizacao do programa de forma hierarquica.

Exemplo:

```pascal
x := 10 + y
```

Vira este trecho da arvore:

```text
:= x
  (+)
    int:10
    id:y
```

Ou seja:

- existe um comando de atribuicao a variavel `x`
- o valor e a expressao `10 + y`
- que e representada como um no binario com operador `+`

A AST facilita as proximas etapas porque cada parte do programa fica claramente separada.

## O que e No.java?

`No.java` e o arquivo que define todos os tipos de no da AST.

Cada tipo de no e uma classe interna dentro de `No`.

As classes existentes sao:

### Nos de programa e bloco

```java
No.Programa     // o programa inteiro
No.Bloco        // bloco de declaracoes + comandos
No.DeclaracaoVar // uma linha de declaracao: ex. "x, y: integer"
```

### Nos de comandos

```java
No.Composto     // begin ... end
No.Atribuicao   // variavel := expressao
No.Se           // if ... then ... [else ...]
No.Enquanto     // while ... do ...
No.Vazio        // comando vazio (ponto-e-virgula extra antes de end)
```

### Nos de expressoes

```java
No.Binario       // esquerda operador direita  ex.: x + 1, y >= 0
No.Unario        // operador operando          ex.: not b, -x
No.Identificador // nome de variavel           ex.: x, total
No.LiteralInteiro  // numero inteiro           ex.: 10, 42
No.LiteralBooleano // true ou false
No.LiteralTexto    // texto entre aspas        ex.: 'ola'
```

## O que e Visitor.java?

O Visitor e um padrao de projeto.

O problema que ele resolve e este: como percorrer a arvore sem precisar modificar os nos?

Sem o Visitor, para imprimir a arvore terias que colocar codigo de impressao dentro de cada no. Para gerar codigo, colocarias codigo de geracao em cada no. E assim por diante.

Com o Visitor, os nos ficam simples e apenas dizem "aceito um visitante". Quem quiser fazer algo com a arvore cria uma classe que implementa `Visitor`.

Exemplo da interface:

```java
public interface Visitor<T> {
    T visitarPrograma(No.Programa no);
    T visitarAtribuicao(No.Atribuicao no);
    T visitarSe(No.Se no);
    // ... um metodo para cada tipo de no
}
```

Para criar uma nova operacao sobre a AST, basta criar uma nova classe que implemente `Visitor` sem tocar nos nos existentes.

## O que e Parser.java?

`Parser.java` e o analisador sintatico.

Ele recebe a lista de tokens do Scanner e constrói a AST.

E chamado de "recursivo descendente" porque cada regra da gramatica vira um metodo Java que pode chamar outros metodos.

Exemplo: o metodo `parseBloco()` chama `parseSecaoVar()` e `parseComposto()`.

### Gramatica implementada

A gramatica descreve quais sequencias de tokens sao validas.

```text
Programa    ::= 'program' IDENT ';' Bloco '.'
Bloco       ::= SecaoVar? CompostoCmd
SecaoVar    ::= 'var' DeclVar+
DeclVar     ::= ListaIdent ':' Tipo ';'
ListaIdent  ::= IDENT (',' IDENT)*
Tipo        ::= 'integer' | 'boolean'
CompostoCmd ::= 'begin' (Cmd (';' Cmd)*)? 'end'
Cmd         ::= AtribCmd | SeCmd | EnquantoCmd | CompostoCmd | vazio
AtribCmd    ::= IDENT ':=' Expr
SeCmd       ::= 'if' Expr 'then' Cmd ('else' Cmd)?
EnquantoCmd ::= 'while' Expr 'do' Cmd
Expr        ::= ExpSimples (OpRel ExpSimples)?
OpRel       ::= '=' | '<>' | '<' | '<=' | '>' | '>='
ExpSimples  ::= Termo (('+' | '-' | 'or') Termo)*
Termo       ::= Fator (('*' | 'div' | 'mod' | 'and') Fator)*
Fator       ::= IDENT | INTEIRO | TEXTO | 'true' | 'false'
              | '(' Expr ')' | 'not' Fator | '-' Fator
```

Lendo esta gramatica:

- `::=` significa "e definido como".
- `|` significa "ou".
- `?` significa "opcional".
- `+` significa "um ou mais".
- `*` significa "zero ou mais".

### Precedencia de operadores

A gramatica define a precedencia das operacoes:

| Nivel | Operadores | Associacao |
|-------|-----------|------------|
| mais alto | `not`, `-` (unario) | direita |
| | `*`, `div`, `mod`, `and` | esquerda |
| | `+`, `-`, `or` | esquerda |
| mais baixo | `=`, `<>`, `<`, `<=`, `>`, `>=` | nenhuma |

Isso significa que `x + y * 2` e lido como `x + (y * 2)`, nao `(x + y) * 2`.

### Ponto-e-virgula em Pascal

Em Pascal o ponto-e-virgula e um separador entre comandos, nao um terminador.

Isso significa que o ultimo comando antes de `end` nao precisa de `;`:

```pascal
begin
  x := 1;
  y := 2     { sem ponto-e-virgula aqui }
end
```

O Parser aceita as duas formas:

- com ponto-e-virgula no final (antes de `end`)
- sem ponto-e-virgula no final

### Dangling else

Quando ha `if` dentro de `if`, o `else` se liga sempre ao `if` mais proximo:

```pascal
if a then if b then x := 1 else x := 2
```

E lido como:

```pascal
if a then (if b then x := 1 else x := 2)
```

Nao como:

```pascal
if a then (if b then x := 1) else x := 2
```

O Parser implementa isso de forma natural: quando encontra `else`, ele sempre consome.

## O que e Printer.java?

`Printer.java` imprime a AST no console com indentacao.

Ela implementa `Visitor<Void>`: visita cada no e imprime uma linha.

Exemplo de saida para o arquivo `exemplos/valido1.txt`:

```text
Programa: Exemplo
  Variaveis:
    var x, y: integer
    var ok: boolean
  Bloco
    := x
      int:10
    := y
      (+)
        (div)
          id:x
          int:2
        int:3
    := ok
      (>=)
        id:y
        (and)
          int:8
          bool:true
    Se
      condicao:
        id:ok
      entao:
        := y
          (+)
            id:y
            int:1
      senao:
        := y
          (-)
            id:y
            int:1
    Enquanto
      condicao:
        (>)
          id:y
          int:0
      corpo:
        := y
          (-)
            id:y
            int:1
```

Como ler:

- Cada dois espacos de recuo representam um nivel mais fundo na arvore.
- `:= x` significa atribuicao a variavel `x`.
- `(+)` significa operacao binaria de soma.
- `id:x` significa identificador `x`.
- `int:10` significa literal inteiro `10`.
- `bool:true` significa literal booleano `true`.

## O que e ErroSintatico.java?

`ErroSintatico.java` e a classe de excecao para erros da analise sintatica.

Funciona de forma parecida com `Erro.java` do lexico, mas mostra "Erro sintatico" no lugar de "Erro lexico".

Exemplo de mensagem:

```text
Erro sintatico na linha 2, coluna 1: esperava ';', encontrado 'var'
```

## Como rodar a analise sintatica

Para rodar o compilador com analise sintatica e impressao da AST:

```powershell
java -cp bin Compiler exemplos\valido1.txt --ast
```

Para compilar antes:

```powershell
javac -encoding UTF-8 -d bin src\*.java
```

Para rodar todos os testes, incluindo os da etapa B:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-tests.ps1
```

Saida esperada:

```text
[OK] valido1
[OK] erro lexico caractere
[OK] erro comentario aberto
Todos os testes base passaram.
[OK] ast valido1 imprime programa
[OK] ast valido1 imprime variaveis
[OK] ast valido1 imprime bloco
[OK] ast valido1 imprime Se
[OK] ast valido1 imprime Enquanto
[OK] ast erro sintatico detectado
Todos os testes da etapa B passaram.
```

## Combinados que devem ser mantidos para Antonio

O trabalho do Antônio depende da AST que esta etapa entrega.

Para que o trabalho dele nao quebre, tente manter iguais:

- a hierarquia de classes em `No.java` (nomes e campos publicos);
- a interface `Visitor<T>` e seus metodos;
- o metodo `parsePrograma()` como ponto de entrada do Parser;
- o comportamento de `ErroSintatico` (mensagem, linha, coluna);
- os nomes dos campos publicos: `no.bloco`, `no.declaracoes`, `no.corpo`, etc.

Se precisar mudar algo, avise o grupo primeiro.

## O que ficou de proposito para Antônio

Nao foi implementado nesta etapa:

- Checker (verificacao de contexto);
- tabela de simbolos;
- verificacao de tipos;
- Coder (geracao de codigo TAM).

Essas partes dependem da AST pronta. O fluxo fica assim:

```text
Scanner -> Parser -> AST -> Checker -> Coder
                     ^
              esta etapa entrega aqui
```

## Gramatica LL(1): FIRST e FOLLOW

Esta secao e mais tecnica e serve como referencia para quem precisar conferir a gramatica.

Uma gramatica e LL(1) quando, para cada regra, o proximo token determina com certeza qual alternativa usar.

Conjuntos FIRST relevantes:

```text
FIRST(Cmd)        = { IDENT, SE, ENQUANTO, INICIO, vazio }
FIRST(Fator)      = { IDENT, LITERAL_INTEIRO, LITERAL_TEXTO,
                      VERDADEIRO, FALSO, ABRE_PARENTESE, NAO, MENOS }
FIRST(Tipo)       = { INTEIRO, BOOLEANO }
FIRST(Expr)       = FIRST(Fator)
```

Conjuntos FOLLOW relevantes:

```text
FOLLOW(Cmd)       = { PONTO_E_VIRGULA, FIM, SENAO, FIM_ARQUIVO }
FOLLOW(Expr)      = { ENTAO, FACA, PONTO_E_VIRGULA, FIM,
                      SENAO, FECHA_PARENTESE, FIM_ARQUIVO }
```

Nao ha conflitos FIRST/FIRST nem FIRST/FOLLOW na gramatica implementada.

## Resumo final

A parte de Andrea deixa pronto:

- analise sintatica (Parser);
- verificacao da gramatica LL(1);
- AST com todos os nos necessarios;
- padrao Visitor para percorrer a AST;
- Printer para visualizar a arvore;
- tratamento de erros sintaticos com linha e coluna;
- testes automatizados da etapa B.

O ponto mais importante e este:

```text
Gabriel entrega tokens.
Andrea transforma tokens em AST.
Antônio usa a AST para checar e gerar codigo.
```
