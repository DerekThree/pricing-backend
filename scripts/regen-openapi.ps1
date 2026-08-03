param(
	[switch]$RunTests
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$generatedDir = Join-Path $repoRoot "target/generated-sources/openapi"
$classesDir = Join-Path $repoRoot "target/classes"
$testClassesDir = Join-Path $repoRoot "target/test-classes"

function Remove-IfExists {
	param([string]$PathToRemove)

	if (Test-Path -LiteralPath $PathToRemove) {
		Remove-Item -LiteralPath $PathToRemove -Recurse -Force
	}
}

Write-Host "Removing generated output..."
Remove-IfExists $generatedDir
Remove-IfExists $classesDir
Remove-IfExists $testClassesDir

Push-Location $repoRoot

try {
	Write-Host "Generating OpenAPI sources..."
	& .\mvnw.cmd -q generate-sources
	if ($LASTEXITCODE -ne 0) {
		throw "OpenAPI generation failed."
	}

	Write-Host "Compiling project..."
	& .\mvnw.cmd -q -DskipTests compile
	if ($LASTEXITCODE -ne 0) {
		throw "Compile failed."
	}

	if ($RunTests) {
		Write-Host "Running test suite..."
		& .\mvnw.cmd -q test
		if ($LASTEXITCODE -ne 0) {
			throw "Tests failed."
		}
	}
}
finally {
	Pop-Location
}
