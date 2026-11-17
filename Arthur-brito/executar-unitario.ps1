$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
$unit = Join-Path $raiz "testes\unitario"
Set-Location $raiz

if (-not (Test-Path (Join-Path $unit "sut\BuzzEmbeddedURL.php"))) {
    node (Join-Path $unit "baixar-sut.mjs")
}

$phar = Join-Path $unit "phpunit.phar"
if (-not (Test-Path $phar)) {
    Write-Host "Baixando PHPUnit 9.6"
    curl.exe -fsSL "https://phar.phpunit.de/phpunit-9.6.22.phar" -o $phar
}

$php = Get-Command php -ErrorAction SilentlyContinue
if ($php) {
    & php $phar -c (Join-Path $unit "phpunit.xml") (Join-Path $unit "RegrasDeTextoTest.php") (Join-Path $unit "VideoUrlTest.php")
    exit $LASTEXITCODE
}

Write-Host "PHP local ausente. Usando php:8.3-cli no Docker."
docker run --rm `
    -v "${raiz}:/work" `
    -w /work/testes/unitario `
    php:8.3-cli `
    php phpunit.phar -c phpunit.xml
exit $LASTEXITCODE
