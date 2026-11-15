param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$backend = Join-Path $repo 'aurea-backend'
$supplier = Join-Path (Split-Path $repo -Parent) 'ProveedorSOAP'
$compose = Join-Path $backend 'docker-compose.integration.yml'
$project = "aurea-verify-$PID"
$previousJavaHome = $env:JAVA_HOME
$providerProcess = $null
$startedCompose = $false
function Assert-Exit([string]$operation) {
    if ($LASTEXITCODE -ne 0) { throw "$operation falló (exit $LASTEXITCODE)." }
}
try {
    if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome 'bin/java.exe'))) {
        throw 'Configurar JAVA_HOME con Java 25 o pasar -JavaHome.'
    }
    $env:JAVA_HOME = $JavaHome
    foreach ($port in @(18091, 5673, 15673, 55433)) {
        if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
            throw "Puerto $port ocupado. Detener el entorno de pruebas anterior."
        }
    }
    Push-Location $supplier
    try { & mvn.cmd -B package; Assert-Exit 'Build del proveedor' } finally { Pop-Location }
    $startedCompose = $true
    & docker compose -p $project -f $compose up -d --wait
    Assert-Exit 'Inicio de PostgreSQL y RabbitMQ'
    $providerProcess = Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -WindowStyle Hidden -PassThru `
        -WorkingDirectory $supplier -ArgumentList @('-Duser.timezone=UTC', '-jar', 'target/proveedor-soap-1.0.0.jar',
        '--proveedor.endpoint=http://127.0.0.1:18091/soap/proveedor', '--spring.datasource.url=jdbc:h2:mem:proveedor-integration') `
        -RedirectStandardOutput (Join-Path $supplier 'target/integration-stdout.log') `
        -RedirectStandardError (Join-Path $supplier 'target/integration-stderr.log')
    $ready = $false
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        if ($providerProcess.HasExited) { throw 'El proveedor terminó antes de iniciar. Revisar target/integration-stderr.log.' }
        try {
            $response = Invoke-WebRequest 'http://127.0.0.1:18091/soap/proveedor?wsdl' -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { Start-Sleep -Seconds 1 }
    }
    if (-not $ready) { throw 'El proveedor no respondió dentro del tiempo esperado.' }
    Push-Location $backend
    try {
        & mvn.cmd -B test '-Dspring.config.import=' '-Daurea.integration=true' '-Duser.timezone=UTC'
        Assert-Exit 'Pruebas de Áurea'
    } finally { Pop-Location }
} finally {
    if ($providerProcess -and -not $providerProcess.HasExited) { Stop-Process -Id $providerProcess.Id }
    if ($startedCompose) { & docker compose -p $project -f $compose down --volumes }
    $env:JAVA_HOME = $previousJavaHome
}
