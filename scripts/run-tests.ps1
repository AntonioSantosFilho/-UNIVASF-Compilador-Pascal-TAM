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
