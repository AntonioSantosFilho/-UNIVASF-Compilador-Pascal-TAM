# Compilador Pascal para TAM

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)
![Analisador sintático](https://img.shields.io/badge/parser-LL(1)-4C8BF5)
![Projeto acadêmico](https://img.shields.io/badge/UNIVASF-projeto%20acadêmico-2E7D32)

Compilador acadêmico de um subconjunto da linguagem **Pascal** para código da **TAM (Triangle Abstract Machine)**. O projeto cobre todas as etapas essenciais de compilação: análise léxica, análise sintática descendente recursiva, construção da árvore de sintaxe abstrata (AST), análise de contexto e geração de código.

Desenvolvido para a disciplina de **Compiladores** do curso de Engenharia da Computação da Universidade Federal do Vale do São Francisco (UNIVASF), campus Juazeiro.

## Sumário

- [Funcionalidades](#funcionalidades)
- [Arquitetura](#arquitetura)
- [Linguagem aceita](#linguagem-aceita)
- [Pré-requisitos](#pré-requisitos)
- [Compilação](#compilação)
- [Como usar](#como-usar)
- [Exemplo](#exemplo)
- [Testes](#testes)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Documentação](#documentação)
- [Autores](#autores)

## Funcionalidades

- Analisador léxico com posição precisa de cada token (linha e coluna).
- Suporte a comentários `{ ... }`, `(* ... *)` e `// ...`.
- Parser preditivo **LL(1)** implementado por descida recursiva.
- Construção e impressão indentada da AST.
- Análise de identificadores, escopo e compatibilidade de tipos.
- Tabela de símbolos para variáveis, procedimentos e funções.
- Geração de instruções TAM e rótulos simbólicos.
- Diagnósticos de erros léxicos, sintáticos, semânticos e de uso.
- Exemplos válidos e inválidos para todas as fases.
- Script automatizado de testes para PowerShell.

## Arquitetura

```mermaid
flowchart LR
    A["Programa Pascal"] --> B["Scanner"]
    B --> C["Tokens"]
    C --> D["Parser LL(1)"]
    D --> E["AST"]
    E --> F["Checker"]
    F --> G["Tabela de símbolos"]
    E --> H["Coder"]
    G --> H
    H --> I["Código TAM"]
```

O projeto usa o padrão **Visitor** para separar as operações realizadas sobre a AST. As responsabilidades principais são:

| Componente | Responsabilidade |
| --- | --- |
| `Scanner` | Reconhecer lexemas, produzir tokens e detectar erros léxicos |
| `Parser` | Validar a gramática e construir a AST |
| `No` | Definir os nós que compõem a AST |
| `Printer` | Percorrer e exibir a AST |
| `Checker` | Verificar declarações, usos e tipos |
| `TabelaSimbolos` | Registrar variáveis, procedimentos e funções |
| `Coder` | Traduzir a AST para instruções TAM |
| `GeradorRotulos` | Criar rótulos únicos para desvios e subprogramas |
| `Compiler` | Disponibilizar a interface de linha de comando e coordenar as fases |

## Linguagem aceita

A linguagem-fonte é um subconjunto imperativo e estaticamente tipado de Pascal. Um programa possui cabeçalho, declarações opcionais, subprogramas opcionais e um bloco principal.

### Recursos disponíveis

- Tipos primitivos: `integer` e `boolean`.
- Declaração de variáveis com `var`.
- Atribuição com `:=`.
- Blocos `begin ... end`.
- Condicionais `if ... then ... else`.
- Repetições `while ... do`.
- Procedimentos e funções sem parâmetros.
- Operadores aritméticos: `+`, `-`, `*`, `div` e `mod`.
- Operadores relacionais: `=`, `<>`, `<`, `<=`, `>` e `>=`.
- Operadores lógicos: `and`, `or` e `not`.
- Literais inteiros e booleanos `true` e `false`.

### Decisões e limitações

- O escopo implementado é único e global.
- Procedimentos e funções não recebem parâmetros.
- Uma função retorna por atribuição ao próprio nome dentro de seu corpo.
- Não há comandos de entrada e saída, como `read` ou `write`.
- Literais de texto são reconhecidos pelo léxico, mas rejeitados na análise de contexto, pois não existe o tipo `string`.
- `and` e `or` avaliam os dois operandos, sem curto-circuito.
- Variáveis devem receber um valor antes do uso; não há inicialização automática.

## Pré-requisitos

- **JDK 17 ou superior**.
- Um terminal: PowerShell, Prompt de Comando ou shell Linux.
- Não são necessárias bibliotecas externas ou ferramentas de build.

Confirme a instalação do Java:

```bash
java -version
javac -version
```

## Compilação

Clone o repositório e entre na pasta do projeto:

```bash
git clone https://github.com/AntonioSantosFilho/-UNIVASF-Compilador-Pascal-TAM.git
cd -UNIVASF-Compilador-Pascal-TAM
```

### Windows (PowerShell)

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
javac -encoding UTF-8 -d bin src\*.java
```

### Linux

```bash
mkdir -p bin
javac -encoding UTF-8 -d bin src/*.java
```

Os arquivos `.class` serão armazenados em `bin/`.

## Como usar

Execute os comandos a partir da raiz do projeto:

```bash
java -cp bin Compiler <arquivo-fonte> [opções]
```

| Opção | Fases executadas | Resultado |
| --- | --- | --- |
| sem opção | Léxica | Confirma que não foram encontrados erros léxicos |
| `--tokens` | Léxica | Imprime tipo, lexema, linha e coluna de cada token |
| `--ast` | Léxica e sintática | Constrói e imprime a AST |
| `--code` | Todas as fases | Verifica o contexto e gera código TAM |
| `--help` ou `-h` | — | Exibe a ajuda da linha de comando |

As opções de processamento podem ser combinadas, por exemplo:

```bash
java -cp bin Compiler exemplos/valido2.txt --tokens --ast --code
```

Quando `--code` é utilizado, o resultado é salvo ao lado do arquivo-fonte com o sufixo `-tam.txt`. Assim, `exemplos/valido2.txt` gera `exemplos/valido2-tam.txt`.

### Códigos de saída

| Código | Significado |
| ---: | --- |
| `0` | Processamento concluído com sucesso |
| `1` | Erro léxico, sintático ou de contexto |
| `2` | Erro de argumentos, leitura ou escrita de arquivo |

## Exemplo

Programa Pascal (`exemplos/procedimento-funcao.txt`):

```pascal
program Subprogramas;
var
  x: integer;
  ok: boolean;

procedure incrementar;
begin
  x := x + 1
end;

function positivo: boolean;
begin
  positivo := x > 0
end;

begin
  x := 1;
  incrementar;
  ok := positivo
end.
```

Para analisar e gerar o código TAM:

```bash
java -cp bin Compiler exemplos/procedimento-funcao.txt --tokens --ast --code
```

Trecho da saída gerada:

```text
JUMP MAIN_0
PROC_incrementar_0:
LOAD x
LOADL 1
CALL(SB) 7[PB]
STORE x
RETURN
```

Erros são apresentados com a fase e a localização do problema:

```text
Erro sintatico na linha 3, coluna 1: ...
Erro de contexto na linha 8, coluna 5: ...
```

## Testes

No Windows, execute a suíte completa pelo PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-tests.ps1
```

O script recompila o projeto e verifica:

- aceitação e rejeição de cadeias pelo analisador léxico;
- construção da AST e detecção de erros sintáticos;
- detecção de erros de contexto;
- geração do arquivo TAM;
- instruções, rótulos, procedimentos e funções no código gerado.

Os casos de teste estão na pasta [`exemplos/`](exemplos/), incluindo programas válidos e arquivos com erros intencionais.

## Estrutura do repositório

```text
.
├── src/                         # Código-fonte Java do compilador
├── exemplos/                    # Programas Pascal e códigos TAM de exemplo
├── scripts/
│   └── run-tests.ps1            # Suíte automatizada de testes
├── DOCUMENTAÇÃO COMPILADORES.pdf
└── README.md
```

## Documentação

A especificação detalhada, a gramática, os conjuntos FIRST/FOLLOW, as decisões de projeto, os algoritmos, os manuais e os exemplos de execução estão disponíveis em:

> [Documentação completa do compilador (PDF)](DOCUMENTAÇÃO%20COMPILADORES.pdf)

## Autores

- Andréa Carvalho Pires
- Antonio dos Santos Filho
- Gabriel Menezes Carvalho

Projeto desenvolvido em 2026 para a disciplina de Compiladores da **Universidade Federal do Vale do São Francisco — UNIVASF**.
