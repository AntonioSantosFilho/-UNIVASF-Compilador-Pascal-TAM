Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$bin = Join-Path $root "bin"

New-Item -ItemType Directory -Force -Path $bin | Out-Null
javac -encoding UTF-8 -d $bin (Join-Path $root "src\*.java")

function Invoke-CompilerCase {
    param(
        [string] $Name,
        [string] $SourceFile,
        [int] $ExpectedExitCode,
        [string] $ExpectedText
    )

    $outputFile = New-TemporaryFile
    $errorFile = New-TemporaryFile
    $process = Start-Process -FilePath "java" `
        -ArgumentList @("-cp", $bin, "Compiler", $SourceFile, "--tokens") `
        -NoNewWindow `
        -Wait `
        -PassThru `
        -RedirectStandardOutput $outputFile `
        -RedirectStandardError $errorFile

    $combinedOutput = (Get-Content $outputFile -Raw) + (Get-Content $errorFile -Raw)
    Remove-Item $outputFile, $errorFile

    if ($process.ExitCode -ne $ExpectedExitCode) {
        throw "[$Name] codigo de saida esperado: $ExpectedExitCode; obtido: $($process.ExitCode)"
    }

    if ($combinedOutput -notlike "*$ExpectedText*") {
        throw "[$Name] saida nao contem: $ExpectedText"
    }

    Write-Host "[OK] $Name"
}

Invoke-CompilerCase "valido1" (Join-Path $root "exemplos\Teste_Fonte.txt") 0 "FIM_ARQUIVO"
Invoke-CompilerCase "erro lexico caractere" (Join-Path $root "exemplos\erro-lexico.txt") 1 "caractere inesperado '@'"
Invoke-CompilerCase "erro comentario aberto" (Join-Path $root "exemplos\erro-lexico-comentario.txt") 1 "comentario iniciado com '{' nao foi fechado"

Write-Host "Todos os testes base passaram."

# ── Testes da etapa B: analise sintatica (Parser + AST) ──────────────────────

function Invoke-AstCase {
    param(
        [string] $Name,
        [string] $SourceFile,
        [int]    $ExpectedExitCode,
        [string] $ExpectedText
    )

    $outputFile = New-TemporaryFile
    $errorFile  = New-TemporaryFile
    $process = Start-Process -FilePath "java" `
        -ArgumentList @("-cp", $bin, "Compiler", $SourceFile, "--ast") `
        -NoNewWindow `
        -Wait `
        -PassThru `
        -RedirectStandardOutput $outputFile `
        -RedirectStandardError  $errorFile

    $combinedOutput = (Get-Content $outputFile -Raw) + (Get-Content $errorFile -Raw)
    Remove-Item $outputFile, $errorFile

    if ($process.ExitCode -ne $ExpectedExitCode) {
        throw "[$Name] codigo de saida esperado: $ExpectedExitCode; obtido: $($process.ExitCode)"
    }

    if ($combinedOutput -notlike "*$ExpectedText*") {
        throw "[$Name] saida nao contem: $ExpectedText"
    }

    Write-Host "[OK] $Name"
}

Invoke-AstCase "ast valido1 imprime programa"   (Join-Path $root "exemplos\valido1.txt")      0 "Programa: Exemplo"
Invoke-AstCase "ast valido1 imprime variaveis"  (Join-Path $root "exemplos\valido1.txt")      0 "Variaveis:"
Invoke-AstCase "ast valido1 imprime bloco"      (Join-Path $root "exemplos\valido1.txt")      0 "Bloco"
Invoke-AstCase "ast valido1 imprime Se"         (Join-Path $root "exemplos\valido1.txt")      0 "Se"
Invoke-AstCase "ast valido1 imprime Enquanto"   (Join-Path $root "exemplos\valido1.txt")      0 "Enquanto"
Invoke-AstCase "ast erro sintatico detectado"   (Join-Path $root "exemplos\erro-sintatico.txt") 1 "Erro sintatico"

Write-Host "Todos os testes da etapa B passaram."

# ── Testes das etapas C e D: analise de contexto + geracao de codigo TAM ─────

function Invoke-CodeCase {
    param(
        [string] $Name,
        [string] $SourceFile,
        [int]    $ExpectedExitCode,
        [string] $ExpectedText
    )

    $outputFile = New-TemporaryFile
    $errorFile  = New-TemporaryFile
    $process = Start-Process -FilePath "java" `
        -ArgumentList @("-cp", $bin, "Compiler", $SourceFile, "--code") `
        -NoNewWindow `
        -Wait `
        -PassThru `
        -RedirectStandardOutput $outputFile `
        -RedirectStandardError  $errorFile

    $combinedOutput = (Get-Content $outputFile -Raw) + (Get-Content $errorFile -Raw)
    Remove-Item $outputFile, $errorFile

    if ($process.ExitCode -ne $ExpectedExitCode) {
        throw "[$Name] codigo de saida esperado: $ExpectedExitCode; obtido: $($process.ExitCode)"
    }

    if ($combinedOutput -notlike "*$ExpectedText*") {
        throw "[$Name] saida nao contem: $ExpectedText"
    }

    Write-Host "[OK] $Name"
}

# valido2 deve gerar codigo com sucesso
Invoke-CodeCase "code valido2 gera arquivo tam"  (Join-Path $root "exemplos\valido2.txt") 0 "Codigo TAM gerado"
Invoke-CodeCase "code procedimento funcao"  (Join-Path $root "exemplos\procedimento-funcao.txt") 0 "Codigo TAM gerado"

# verifica que o arquivo .txt de TAM foi criado
$tamFile = Join-Path $root "exemplos\valido2-tam.txt"
if (-not (Test-Path $tamFile)) {
    throw "[code valido2 arquivo existe] arquivo TAM .txt nao foi criado"
}
Write-Host "[OK] code valido2 arquivo existe"

# verifica conteudo do arquivo .tam
$tamContent = Get-Content $tamFile -Raw
if ($tamContent -notlike "*PUSH*") {
    throw "[code valido2 contem PUSH] arquivo .tam nao contem PUSH"
}
Write-Host "[OK] code valido2 contem PUSH"

if ($tamContent -notlike "*HALT*") {
    throw "[code valido2 contem HALT] arquivo .tam nao contem HALT"
}
Write-Host "[OK] code valido2 contem HALT"

if ($tamContent -notlike "*WHILE_*") {
    throw "[code valido2 contem rotulo] arquivo TAM nao contem rotulo de desvio"
}
Write-Host "[OK] code valido2 contem rotulo"

if ($tamContent -notlike "*STORE x*") {
    throw "[code valido2 usa nomes] arquivo TAM nao referencia variaveis por nome"
}
Write-Host "[OK] code valido2 usa nomes"

$subTamFile = Join-Path $root "exemplos\procedimento-funcao-tam.txt"
$subTamContent = Get-Content $subTamFile -Raw
if ($subTamContent -notlike "*PROC_incrementar*") {
    throw "[code procedimento contem rotulo] arquivo TAM nao contem rotulo de procedimento"
}
Write-Host "[OK] code procedimento contem rotulo"

if ($subTamContent -notlike "*FUNC_positivo*") {
    throw "[code funcao contem rotulo] arquivo TAM nao contem rotulo de funcao"
}
Write-Host "[OK] code funcao contem rotulo"

if ($subTamContent -notlike "*CALL PROC_incrementar*") {
    throw "[code chama procedimento] arquivo TAM nao chama procedimento"
}
Write-Host "[OK] code chama procedimento"

# erro semantico deve falhar com mensagem de contexto
Invoke-CodeCase "code erro semantico detectado" (Join-Path $root "exemplos\erro-semantico.txt") 1 "Erro de contexto"

# valido1 falha no checker por 'and' entre inteiro e booleano
Invoke-CodeCase "code valido1 falha no checker" (Join-Path $root "exemplos\valido1.txt") 1 "Erro de contexto"

Write-Host "Todos os testes das etapas C e D passaram."
