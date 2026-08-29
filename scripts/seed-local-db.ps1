param(
    [string]$DatabaseUrl = $env:SPRING_DATASOURCE_URL,
    [string]$DatabaseUsername = $env:SPRING_DATASOURCE_USERNAME,
    [string]$DatabasePassword = $env:SPRING_DATASOURCE_PASSWORD
)

if ([string]::IsNullOrWhiteSpace($DatabaseUrl)) {
    $DatabaseUrl = "jdbc:postgresql://localhost:5432/pricingdb"
}

if ([string]::IsNullOrWhiteSpace($DatabaseUsername)) {
    $DatabaseUsername = "pricinguser"
}

if ($null -eq $DatabasePassword) {
    $DatabasePassword = "pricingpass"
}

$databaseMatch = [regex]::Match(
    $DatabaseUrl,
    "^jdbc:postgresql://(?<host>[^/:]+)(:(?<port>\d+))?/(?<database>[^?]+)"
)

if (-not $databaseMatch.Success) {
    throw "DatabaseUrl must be a PostgreSQL JDBC URL."
}

$databasePort = $databaseMatch.Groups["port"].Value
if ([string]::IsNullOrWhiteSpace($databasePort)) {
    $databasePort = "5432"
}

if ($null -eq (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw "psql is required to apply the seed files."
}

$seedDirectory = Join-Path $PSScriptRoot "seed"
$seedFiles = Get-ChildItem -Path $seedDirectory -Filter "*.sql" |
    Sort-Object Name

if ($seedFiles.Count -eq 0) {
    throw "No seed SQL files found in $seedDirectory."
}

$commands = @("BEGIN;")
$commands += $seedFiles | ForEach-Object {
    "\i '$($_.FullName.Replace("\", "/"))'"
}
$commands += "COMMIT;"

$previousPassword = $env:PGPASSWORD

try {
    $env:PGPASSWORD = $DatabasePassword
    $commands -join "`n" | & psql `
        --host $databaseMatch.Groups["host"].Value `
        --port $databasePort `
        --username $DatabaseUsername `
        --dbname $databaseMatch.Groups["database"].Value `
        --set ON_ERROR_STOP=1

    if ($LASTEXITCODE -ne 0) {
        throw "Database seeding failed."
    }
}
finally {
    if ($null -eq $previousPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
    else {
        $env:PGPASSWORD = $previousPassword
    }
}
