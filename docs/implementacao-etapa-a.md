# Implementacao de Gabriel: Lexico, CLI e Testes Base

Este documento explica, com linguagem mais simples, o que foi implementado por Gabriel no compilador.

A ideia e ajudar quem ainda nao conhece bem Java, Pascal ou compiladores a entender:

- o que cada arquivo faz;
- como rodar o projeto;
- como o analisador lexico funciona;
- como Andréa e Antônio podem continuar o trabalho;
- quais combinados precisam ser mantidos para uma parte nao quebrar a outra.

## Antes de tudo: o que e um compilador?

Um compilador e um programa que le outro programa.

Neste projeto, o compilador vai ler uma linguagem parecida com Pascal e, aos poucos, passar por varias etapas:

1. Analise lexica: separa o texto em pedacos chamados tokens.
2. Analise sintatica: verifica se os tokens estao em uma ordem correta.
3. AST: monta uma arvore representando a estrutura do programa.
4. Analise de contexto: verifica escopo, variaveis declaradas e tipos.
5. Geracao de codigo: gera codigo para uma maquina chamada TAM.

Gabriel ficou responsavel principalmente pela primeira parte: a analise lexica.

## O que foi implementado

Foram criados estes arquivos:

```text
src/
  Compiler.java
  Main.java
  Scanner.java
  Token.java
  Erro.java

exemplos/
  valido1.txt
  erro-lexico.txt
  erro-lexico-comentario.txt
  erro-sintatico.txt
  erro-semantico.txt

scripts/
  run-tests.ps1
```

Resumo simples:

- `Compiler.java`: e o programa principal. Ele le o arquivo, chama o Scanner e controla as opcoes da linha de comando.
- `Main.java`: e uma entrada alternativa. Ele apenas chama o `Compiler`.
- `Scanner.java`: e o analisador lexico. Ele le caractere por caractere e cria tokens.
- `Token.java`: define quais tipos de tokens existem.
- `Erro.java`: define como mostrar erros lexicos.
- `exemplos/`: contem arquivos de teste.
- `scripts/run-tests.ps1`: compila e roda alguns testes automaticamente.

## O que significa "combinado" no projeto?

Em algumas areas da programacao, as pessoas usam a palavra "contrato" para falar de uma combinacao entre partes do codigo.

Mas, para deixar mais simples, vamos chamar isso de combinado.

Neste projeto, combinado quer dizer:

> "Esta parte do codigo promete entregar as informacoes neste formato, e as outras partes podem confiar nisso."

Exemplo:

- Gabriel combina que todo token tera `tipo`, `lexema`, `linha` e `coluna`.
- Andréa pode confiar nisso para fazer o Parser.
- Antônio pode confiar nisso indiretamente quando for usar a AST.

Ou seja, se uma pessoa muda esse combinado sem avisar, a parte das outras pessoas pode parar de funcionar.

Um exemplo bem simples:

```java
public final String lexema;
```

Esse campo guarda o texto do token. Se alguem mudar o nome de `lexema` para `texto`, qualquer codigo que usava `token.lexema` vai quebrar.

Por isso, quando o documento falar em "combinado", leia como:

> "Isso aqui deve continuar igual, a menos que o grupo combine uma mudanca."

## Como rodar o projeto

Abra o terminal na pasta do projeto:

```text
D:\-UNIVASF-Compilador-Pascal-TAM
```

Compile os arquivos Java:

```powershell
javac -encoding UTF-8 -d bin src\*.java
```

Esse comando faz o seguinte:

- `javac`: chama o compilador do Java;
- `-encoding UTF-8`: diz que os arquivos usam UTF-8;
- `-d bin`: coloca os arquivos compilados dentro da pasta `bin`;
- `src\*.java`: compila todos os arquivos `.java` da pasta `src`.

Depois rode o compilador criado:

```powershell
java -cp bin Compiler exemplos\valido1.txt --tokens
```

Esse comando faz o seguinte:

- `java`: executa um programa Java;
- `-cp bin`: diz que os arquivos compilados estao na pasta `bin`;
- `Compiler`: nome da classe principal;
- `exemplos\valido1.txt`: arquivo-fonte que sera analisado;
- `--tokens`: pede para imprimir os tokens encontrados.

Tambem da para rodar com as opcoes planejadas para o projeto completo:

```powershell
java -cp bin Compiler exemplos\valido1.txt --tokens --ast --code
```

Atencao:

- `--tokens` ja funciona.
- `--ast` ainda nao monta AST; isso e da Andréa.
- `--code` ainda nao gera codigo; isso e do Antônio.

## Como rodar os testes

Existe um script de teste:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-tests.ps1
```

Ele faz automaticamente:

1. compila os arquivos Java;
2. roda um exemplo valido;
3. roda exemplos com erro lexico;
4. verifica se as mensagens esperadas aparecem.

Se estiver tudo certo, aparece:

```text
[OK] valido1
[OK] erro lexico caractere
[OK] erro comentario aberto
Todos os testes base passaram.
```

## O que e analise lexica?

Analise lexica e a primeira etapa de um compilador.

Ela pega um texto grande, como este:

```pascal
x := 10;
```

E transforma em pedacos menores:

```text
IDENTIFICADOR    "x"
ATRIBUICAO       ":="
LITERAL_INTEIRO  "10"
PONTO_E_VIRGULA  ";"
```

Esses pedacos sao chamados de tokens.

Pense assim:

- para uma pessoa, `x := 10;` parece facil de ler;
- para o compilador, e melhor separar em categorias;
- cada categoria ajuda a proxima etapa a entender a estrutura do programa.

## O que e um token?

Um token e um pedaco reconhecido do codigo-fonte.

Exemplos:

```pascal
program Exemplo;
```

Vira:

```text
PROGRAMA         "program"
IDENTIFICADOR    "Exemplo"
PONTO_E_VIRGULA  ";"
```

Cada token tem quatro informacoes:

```java
public final Tipo tipo;
public final String lexema;
public final int linha;
public final int coluna;
```

Explicando:

- `tipo`: o tipo do token. Exemplo: `PROGRAMA`, `IDENTIFICADOR`, `LITERAL_INTEIRO`.
- `lexema`: o texto exato encontrado no arquivo. Exemplo: `"program"`, `"Exemplo"`, `"10"`.
- `linha`: a linha onde o token apareceu.
- `coluna`: a coluna onde o token apareceu.

Exemplo de saida:

```text
IDENTIFICADOR    "x"             linha 6, coluna 3
ATRIBUICAO       ":="            linha 6, coluna 5
LITERAL_INTEIRO  "10"            linha 6, coluna 8
```

Isso significa:

- encontrou um identificador chamado `x`;
- ele estava na linha 6, coluna 3;
- depois encontrou `:=`;
- depois encontrou o numero `10`.

## O que e Scanner.java?

`Scanner.java` e o arquivo que faz a analise lexica.

Ele recebe o conteudo inteiro do arquivo como texto e vai lendo caractere por caractere.

Por exemplo, se o arquivo contem:

```pascal
x := 10;
```

O Scanner olha:

1. `x`: e uma letra, entao pode ser identificador.
2. espaco: ignora.
3. `:` seguido de `=`: reconhece `:=`.
4. espaco: ignora.
5. `1` e `0`: reconhece numero inteiro.
6. `;`: reconhece ponto e virgula.

No fim, ele tambem cria um token especial chamado `FIM_ARQUIVO`.

`FIM_ARQUIVO` significa que o Scanner chegou ao fim do arquivo.

Ele e util porque o Parser da Andréa pode saber quando chegou ao final da entrada.

## O que e Token.java?

`Token.java` define a lista de tipos de tokens que o Scanner pode gerar.

Por exemplo:

```java
PROGRAMA
VAR
INICIO
FIM
IDENTIFICADOR
LITERAL_INTEIRO
ATRIBUICAO
PONTO_E_VIRGULA
FIM_ARQUIVO
```

Essa lista e muito importante porque Andréa vai usar esses nomes no Parser.

Exemplo:

Se o Parser quiser reconhecer o inicio de um programa, ele provavelmente vai esperar:

```text
PROGRAMA IDENTIFICADOR PONTO_E_VIRGULA
```

Ou seja:

```pascal
program NomeDoPrograma;
```

## O que e Compiler.java?

`Compiler.java` e quem organiza a execucao.

Ele faz mais ou menos isso:

1. le os argumentos da linha de comando;
2. abre o arquivo informado;
3. manda o conteudo para o `Scanner`;
4. recebe a lista de tokens;
5. se o usuario passou `--tokens`, imprime os tokens;
6. se acontecer erro lexico, mostra a mensagem e para.

Hoje o fluxo e:

```text
arquivo .txt -> Compiler -> Scanner -> lista de tokens -> impressao opcional
```

No futuro, com as partes de Andréa e Antônio, o fluxo devera ser:

```text
arquivo .txt -> Scanner -> Parser -> AST -> Checker -> Coder
```

## O que sao palavras reservadas?

Palavras reservadas sao palavras que tem significado especial na linguagem.

Exemplos:

```text
program
var
begin
end
if
then
else
while
do
integer
boolean
true
false
```

Elas nao devem ser tratadas como nomes comuns de variaveis.

Por exemplo:

```pascal
program Exemplo;
```

`program` vira token `PROGRAMA`.

`Exemplo` vira token `IDENTIFICADOR`, porque e um nome escolhido pelo programador.

## Lista de tokens implementados

### Fim de arquivo

```text
FIM_ARQUIVO
```

### Nomes e valores

```text
IDENTIFICADOR
LITERAL_INTEIRO
LITERAL_TEXTO
```

Exemplos:

```pascal
x
total
123
'texto'
```

### Palavras reservadas

```text
PROGRAMA
VAR
PROCEDIMENTO
FUNCAO
INICIO
FIM
SE
ENTAO
SENAO
ENQUANTO
FACA
INTEIRO
BOOLEANO
VERDADEIRO
FALSO
E
OU
NAO
DIV
MOD
```

### Operadores

```text
MAIS           +
MENOS          -
VEZES          *
BARRA          /
ATRIBUICAO     :=
IGUAL          =
DIFERENTE      <>
MENOR          <
MENOR_IGUAL    <=
MAIOR          >
MAIOR_IGUAL    >=
```

### Separadores

```text
PONTO_E_VIRGULA  ;
DOIS_PONTOS      :
VIRGULA           ,
PONTO             .
ABRE_PARENTESE    (
FECHA_PARENTESE   )
ABRE_COLCHETE     [
FECHA_COLCHETE    ]
```

## O que o Scanner ignora?

O Scanner ignora espacos, quebras de linha, tabulacoes e comentarios.

Exemplo:

```pascal
x:=10;
```

E:

```pascal
x   :=   10;
```

Geram praticamente os mesmos tokens.

A diferenca fica apenas na linha e coluna registradas.

## Comentarios aceitos

Foram aceitos tres tipos de comentario.

Comentario com chaves:

```pascal
{ isto e um comentario }
```

Comentario com parenteses e asterisco:

```pascal
(* isto tambem e um comentario *)
```

Comentario de uma linha:

```pascal
// comentario ate o fim da linha
```

Comentarios nao aparecem na lista de tokens.

Se um comentario nao for fechado, o Scanner mostra erro.

Exemplo:

```pascal
{ comentario sem fim
```

Mensagem:

```text
Erro lexico na linha 3, coluna 3: comentario iniciado com '{' nao foi fechado
```

## Identificadores

Identificador e nome criado pelo programador.

Pode ser nome de:

- programa;
- variavel;
- funcao;
- procedimento.

Exemplos validos:

```text
x
idade
total_geral
_temporario
valor1
```

Regra implementada:

```text
deve comecar com letra ou _
depois pode ter letra, numero ou _
```

Exemplos invalidos:

```text
1valor
10abc
```

## Numeros inteiros

Numeros inteiros sao sequencias de digitos.

Exemplos:

```text
0
1
10
2026
```

O Scanner nao aceita numero colado com letra.

Exemplo invalido:

```text
123abc
```

Isso gera erro porque nao fica claro se deveria ser numero ou identificador.

## Strings

Strings usam aspas simples:

```pascal
'ola'
```

Isso gera:

```text
LITERAL_TEXTO "ola"
```

Importante:

- o Scanner reconhece strings;
- mas a linguagem final do grupo pode decidir usar ou nao strings;
- se o grupo nao quiser strings, Andréa simplesmente nao coloca `LITERAL_TEXTO` na gramatica sintatica.

## Erros lexicos

Erro lexico acontece quando o Scanner encontra algo que nao pertence a linguagem.

Exemplo:

```pascal
x := 10 @ 2
```

O caractere `@` nao foi definido como token.

Entao aparece:

```text
Erro lexico na linha 3, coluna 11: caractere inesperado '@'
```

Erros tratados atualmente:

- caractere desconhecido;
- comentario `{` nao fechado;
- comentario `(*` nao fechado;
- string nao fechada;
- string quebrada antes do fim;
- numero seguido diretamente por letra ou `_`.

## Arquivos de exemplo

### `exemplos/valido1.txt`

Exemplo lexicamente valido.

Ele contem:

- declaracao de programa;
- declaracao de variaveis;
- tipo inteiro;
- tipo booleano;
- atribuicoes;
- `if then else`;
- `while do`;
- operadores aritmeticos e relacionais.

### `exemplos/erro-lexico.txt`

Tem um caractere invalido:

```pascal
@
```

Serve para testar erro lexico.

### `exemplos/erro-lexico-comentario.txt`

Tem um comentario aberto e nao fechado.

Serve para testar erro de comentario.

### `exemplos/erro-sintatico.txt`

Esse arquivo nao deve dar erro no Scanner, mas deve dar erro no Parser.

Ele existe para ajudar Andréa.

Exemplo de problema:

```pascal
program ErroSintatico
```

Falta `;` depois do nome do programa.

### `exemplos/erro-semantico.txt`

Esse arquivo deve passar pelo Scanner e provavelmente pelo Parser, mas deve falhar no Checker.

Ele existe para ajudar Antônio.

Exemplo de problema:

```pascal
x := true
```

Se `x` foi declarado como `integer`, nao deveria receber `true`, que e booleano.

## Como Andréa deve continuar

Andréa vai fazer principalmente:

- Parser;
- AST;
- Visitor;
- Printer.

O Parser deve receber os tokens que ja foram criados pelo Scanner.

Exemplo de ideia em Java:

```java
List<Token> tokens = new Scanner(source).scanAll();
Parser parser = new Parser(tokens);
Node ast = parser.parseProgram();
```

Explicando em portugues:

1. O Scanner transforma o texto em tokens.
2. O Parser recebe esses tokens.
3. O Parser verifica se eles estao na ordem correta.
4. Se estiverem, monta uma AST.

Andréa nao precisa ler o arquivo de novo.

Andréa tambem nao precisa separar caracteres de novo.

Isso ja e trabalho do Scanner.

## Como Antônio deve continuar

Antônio vai fazer principalmente:

- Checker;
- tabela de simbolos;
- verificacao de tipos;
- Coder;
- geracao de codigo TAM.

Antônio deve trabalhar depois da AST criada pela Andréa.

Fluxo ideal:

```text
Scanner -> Parser -> AST -> Checker -> Coder
```

Ou seja:

1. Scanner cria tokens.
2. Parser cria AST.
3. Checker verifica se o programa faz sentido.
4. Coder gera codigo objeto.

Regra importante:

```text
Se tiver erro lexico, sintatico ou semantico, nao deve gerar codigo.
```

## Coisas que devem continuar iguais por enquanto

Para evitar quebrar o trabalho das outras pessoas, tentem manter iguais:

- os nomes dos tokens em `Token.Tipo`;
- os campos `tipo`, `lexema`, `linha` e `coluna`;
- o fato de `scanAll()` sempre terminar com token `FIM_ARQUIVO`;
- a opcao `--tokens`;
- a ideia de passar o arquivo pela linha de comando;
- o uso de UTF-8.

Se precisar mudar algo, o ideal e avisar o grupo primeiro.

## Coisas que ficaram de proposito para Andréa e Antônio

Nao foram implementados agora:

- Parser;
- verificacao da gramatica LL(1);
- calculo de FIRST e FOLLOW;
- AST;
- Visitor;
- Printer;
- Checker;
- tabela de simbolos;
- verificacao de tipos;
- geracao de codigo TAM.

Isso foi proposital para nao invadir a divisao do grupo.

## Pequena gramatica lexica

Esta parte e mais formal, mas ainda serve como base para a documentacao final.

```text
letra       ::= A..Z | a..z
digito      ::= 0..9
ident       ::= (letra | "_") (letra | digito | "_")*
inteiro     ::= digito+
string      ::= "'" qualquer_texto_sem_quebra_de_linha "'"
```

Comentarios:

```text
comentario ::= "{" texto "}"
            |  "(*" texto "*)"
            |  "//" texto_ate_fim_da_linha
```

Operadores compostos:

```text
:=
<>
<=
>=
```

Operadores e separadores simples:

```text
+ - * / = < > ; : , . ( ) [ ]
```

## Expressao regular aproximada

Uma expressao regular aproximada para os tokens e:

```text
([A-Za-z_][A-Za-z0-9_]*)
|([0-9]+)
|('[^\r\n']*')
|(:=|<>|<=|>=)
|[+\-*/=<>;:,.()[\]]
```

Nao se preocupe se isso parecer estranho no comeco.

Ela apenas resume, em formato compacto, as regras que o Scanner implementa manualmente.

## Resumo final

A parte de Gabriel deixa pronto:

- leitura de arquivo;
- linha de comando;
- analise lexica;
- impressao de tokens;
- erros lexicos com linha e coluna;
- exemplos de teste;
- script de teste.

Andréa deve pegar a lista de tokens e montar o Parser/AST.

Antônio deve pegar a AST pronta, verificar contexto e gerar codigo.

O ponto mais importante e este:

```text
Gabriel entrega tokens.
Andréa transforma tokens em AST.
Antônio usa a AST para checar e gerar codigo.
```
