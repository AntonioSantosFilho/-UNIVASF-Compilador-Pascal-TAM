# Implementacao das Etapas 4 e 5: Analise de Contexto e Geracao de Codigo TAM

Este documento explica o que foi implementado nas etapas 4 e 5 do compilador.

A ideia e ajudar quem ainda nao conhece bem o assunto a entender:

- o que cada arquivo faz;
- como rodar a analise de contexto e a geracao de codigo;
- como a tabela de simbolos funciona;
- como a maquina TAM funciona;
- quais combinados devem ser mantidos.

## O que foi implementado

Foram criados estes arquivos:

```text
src/
  ErroContexto.java
  TabelaSimbolos.java
  Checker.java
  Coder.java

exemplos/
  valido2.txt
```

Resumo simples:

- `ErroContexto.java`: define como mostrar erros de contexto (escopo e tipos).
- `TabelaSimbolos.java`: guarda o tipo e o endereco de cada variavel declarada.
- `Checker.java`: percorre a AST e verifica as regras de tipos e escopo.
- `Coder.java`: percorre a AST e gera instrucoes da maquina TAM.
- `exemplos/valido2.txt`: programa Pascal valido em todas as etapas.

Alem disso, foram atualizados:

- `No.java`: nos `Se` e `Enquanto` agora guardam o token da palavra-chave (para erros).
- `Parser.java`: passa o token `if`/`while` para os nos `Se`/`Enquanto`.
- `Compiler.java`: a opcao `--code` agora executa o pipeline completo.
- `scripts/run-tests.ps1`: adicionados testes das etapas C e D.

## O que e analise de contexto?

A analise de contexto e a quarta etapa do compilador.

Ela recebe a AST montada pelo Parser e verifica coisas que a gramatica nao consegue verificar sozinha.

Exemplos do que a gramatica nao consegue verificar:

- Se uma variavel foi declarada antes de ser usada.
- Se os tipos dos operandos de uma operacao sao compativeis.
- Se o valor atribuido a uma variavel tem o mesmo tipo dela.
- Se a condicao de um `if` ou `while` e booleana.

O verificador e chamado de Checker.

Exemplo de erro de contexto:

```pascal
var x: integer;
begin
  x := true
end.
```

O tipo de `x` e inteiro, mas o valor `true` e booleano. O Checker detecta isso:

```text
Erro de contexto na linha 3, coluna 3: tipo incompativel na atribuicao de 'x': variavel e inteiro, expressao e booleano
```

## O que e a tabela de simbolos?

A tabela de simbolos e uma estrutura que guarda informacoes sobre cada variavel declarada.

Para cada variavel, ela guarda:

- o tipo (`inteiro` ou `booleano`);
- o deslocamento em relacao ao registrador SB da maquina TAM.

Exemplo: para o programa

```pascal
var
  x, y: integer;
  positivo: boolean;
```

A tabela fica assim:

| Nome | Tipo | Deslocamento |
|------|------|-------------|
| x | inteiro | 0 |
| y | inteiro | 1 |
| positivo | booleano | 2 |

Isso significa: `x` esta na posicao `0[SB]`, `y` em `1[SB]`, `positivo` em `2[SB]`.

Essa tabela e construida pelo Checker e repassada ao Coder para que ele saiba onde armazenar cada variavel.

## Regras de tipos implementadas

| Operador | Tipo dos operandos | Tipo do resultado |
|----------|--------------------|-------------------|
| `+`, `-`, `*`, `div`, `mod` | inteiro × inteiro | inteiro |
| `and`, `or` | booleano × booleano | booleano |
| `not` | booleano | booleano |
| `-` (unario) | inteiro | inteiro |
| `<`, `<=`, `>`, `>=` | inteiro × inteiro | booleano |
| `=`, `<>` | mesmo tipo × mesmo tipo | booleano |

Regras adicionais:

- Condicao do `if` deve ser booleana.
- Condicao do `while` deve ser booleana.
- O tipo do valor atribuido deve ser igual ao tipo da variavel.

## O que e a maquina TAM?

TAM significa Triangle Abstract Machine.

E uma maquina imaginaria baseada em pilha, definida no livro "Programming Language Processors in Java" de Watt e Brown.

### Registradores principais

| Registrador | Significado |
|-------------|-------------|
| CB | Code Base (inicio da area de codigo) |
| CT | Code Top (fim da area de codigo) |
| PB | Primitive Base (inicio das primitivas) |
| SB | Stack Base (inicio da pilha) |
| ST | Stack Top (topo atual da pilha) |

### Instrucoes usadas neste projeto

```text
PUSH n           reserva n palavras na pilha
LOADL v          empilha o valor literal v
LOAD(1) d[SB]    empilha o valor da variavel no offset d a partir de SB
STORE(1) d[SB]   desempilha e armazena na variavel no offset d
CALL(SB) p[PB]   chama a rotina primitiva no offset p a partir de PB
JUMP d[CB]       desvio incondicional para a instrucao d
JUMPIF(c) d[CB]  desvio condicional: pula para d se o topo da pilha for c
HALT             encerra a execucao
```

### Primitivas TAM

As primitivas sao rotinas pre-definidas que executam operacoes basicas.

| Offset | Primitiva | Operacao |
|--------|-----------|----------|
| PB+1 | not | negacao booleana |
| PB+2 | and | e logico |
| PB+3 | or | ou logico |
| PB+6 | neg | negacao aritmetica |
| PB+7 | add | adicao |
| PB+8 | sub | subtracao |
| PB+9 | mul | multiplicacao |
| PB+10 | div | divisao inteira |
| PB+11 | mod | resto da divisao |
| PB+12 | lt | menor que |
| PB+13 | le | menor ou igual |
| PB+14 | ge | maior ou igual |
| PB+15 | gt | maior que |
| PB+16 | eq | igual |
| PB+17 | ne | diferente |

Valores booleanos na TAM:

- `true` = 1
- `false` = 0

### Como a pilha funciona

No inicio do programa, `PUSH n` reserva espaco para as `n` variaveis globais.

Depois disso, a pilha cresce conforme expressoes sao avaliadas e diminui conforme resultados sao armazenados.

Exemplo para `x := 10`:

```text
LOADL 10       ; empilha o valor 10
STORE(1) 0[SB] ; desempilha e armazena em x (offset 0)
```

## Como a geracao de codigo funciona

### Atribuicao

Para `x := expressao`:

1. Avalia a expressao (o resultado fica no topo da pilha).
2. Emite `STORE(1) d[SB]` onde `d` e o offset de `x`.

### Expressao binaria

Para `a + b`:

1. Avalia `a` (empilha resultado).
2. Avalia `b` (empilha resultado).
3. Emite `CALL(SB) 7[PB]` (chama primitiva de adicao).

A primitiva consome os dois valores do topo e empilha o resultado.

### Desvio condicional (if)

Para `if C then S1 else S2`:

```text
; avalia C (0 ou 1 no topo)
JUMPIF(0) enderecoElse[CB]   ; pula se C e false
; codigo de S1
JUMP enderecoFim[CB]
; enderecoElse: codigo de S2
; enderecoFim:
```

Para `if C then S1` (sem else):

```text
; avalia C
JUMPIF(0) enderecoFim[CB]
; codigo de S1
; enderecoFim:
```

O endereco de destino e preenchido depois (backpatching): primeiro emite o JUMP com `?`, depois corrige o endereco quando se sabe onde e o destino.

### Laco while

Para `while C do S`:

```text
; enderecoLoop:
; avalia C
JUMPIF(0) enderecoFim[CB]   ; sai do laco se C e false
; codigo de S
JUMP enderecoLoop[CB]        ; volta para testar C
; enderecoFim:
```

## Exemplo completo de saida

Para o programa `exemplos/valido2.txt`:

```pascal
program Valido;
var
  x, y: integer;
  positivo: boolean;
begin
  x := 10;
  y := x div 2 + 3;
  positivo := y >= 0;
  if positivo then
    y := y + 1
  else
    y := y - 1;
  while y > 0 do
    y := y - 1
end.
```

O codigo TAM gerado e:

```text
  0: PUSH 3
  1: LOADL 10
  2: STORE(1) 0[SB]
  3: LOAD(1) 0[SB]
  4: LOADL 2
  5: CALL(SB) 10[PB]
  6: LOADL 3
  7: CALL(SB) 7[PB]
  8: STORE(1) 1[SB]
  9: LOAD(1) 1[SB]
 10: LOADL 0
 11: CALL(SB) 14[PB]
 12: STORE(1) 2[SB]
 13: LOAD(1) 2[SB]
 14: JUMPIF(0) 20[CB]
 15: LOAD(1) 1[SB]
 16: LOADL 1
 17: CALL(SB) 7[PB]
 18: STORE(1) 1[SB]
 19: JUMP 24[CB]
 20: LOAD(1) 1[SB]
 21: LOADL 1
 22: CALL(SB) 8[PB]
 23: STORE(1) 1[SB]
 24: LOAD(1) 1[SB]
 25: LOADL 0
 26: CALL(SB) 15[PB]
 27: JUMPIF(0) 33[CB]
 28: LOAD(1) 1[SB]
 29: LOADL 1
 30: CALL(SB) 8[PB]
 31: STORE(1) 1[SB]
 32: JUMP 24[CB]
 33: HALT
```

Como ler:

- `0: PUSH 3` — reserva 3 palavras: x (offset 0), y (offset 1), positivo (offset 2).
- `1-2` — x := 10.
- `3-8` — y := x div 2 + 3.
- `9-12` — positivo := y >= 0.
- `13-23` — if positivo then y := y + 1 else y := y - 1.
  - `14: JUMPIF(0) 20` — se falso, pula para o else (instrucao 20).
  - `19: JUMP 24` — ao final do then, pula para depois do if.
- `24-32` — while y > 0 do y := y - 1.
  - `27: JUMPIF(0) 33` — se falso, sai do loop.
  - `32: JUMP 24` — volta para testar a condicao.
- `33: HALT` — fim do programa.

## Como rodar

Compilar:

```powershell
javac -encoding UTF-8 -d bin src\*.java
```

Gerar codigo TAM:

```powershell
java -cp bin Compiler exemplos\valido2.txt --code
```

O arquivo `exemplos\valido2.tam` e criado automaticamente.

Rodar todos os testes:

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
[OK] code valido2 gera arquivo tam
[OK] code valido2 arquivo existe
[OK] code valido2 contem PUSH
[OK] code valido2 contem HALT
[OK] code erro semantico detectado
[OK] code valido1 falha no checker
Todos os testes das etapas C e D passaram.
```

## Observacao sobre valido1.txt

O arquivo `exemplos/valido1.txt` e valido lexica e sintaticamente, mas falha na analise de contexto.

Isso acontece porque ele contem:

```pascal
ok := y >= 8 and true;
```

Que e analisado como `ok := (y >= (8 and true))`.

A expressao `8 and true` aplica o operador `and` sobre um inteiro (`8`) e um booleano (`true`), o que viola as regras de tipos.

Para um programa valido em todas as etapas, use `valido2.txt`.

## Fluxo completo do compilador

```text
arquivo .pas
    |
    v
Scanner (Etapa 1)
    |-- Erro lexico --> mensagem e encerra
    v
lista de tokens
    |
    v
Parser (Etapas 2 e 3)
    |-- Erro sintatico --> mensagem e encerra
    v
AST (Arvore Sintatica Abstrata)
    |
    v
Checker (Etapa 4)
    |-- Erro de contexto --> mensagem e encerra
    v
TabelaSimbolos preenchida
    |
    v
Coder (Etapa 5)
    v
arquivo .tam (codigo TAM)
```

## Resumo final

```text
Gabriel entrega tokens.
Andrea transforma tokens em AST.
Etapas C e D: verificam contexto e geram codigo TAM.
```

A tabela de simbolos e construida pelo Checker e reutilizada pelo Coder.

O codigo TAM gerado pode ser executado em um simulador da maquina TAM conforme descrito no livro "Programming Language Processors in Java" (Watt e Brown, 2000).
