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

Invoke-CompilerCase "valido1" (Join-Path $root "exemplos\valido1.txt") 0 "FIM_ARQUIVO"
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
