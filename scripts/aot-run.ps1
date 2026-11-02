param(
    [Parameter(Mandatory = $true)]
    [string]$Java,
    [string]$CachePath = "game4j.aot",
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ApplicationArguments
)

$ErrorActionPreference = "Stop"
if (Test-Path -LiteralPath $CachePath) {
    & $Java "-XX:AOTMode=auto" "-XX:AOTCache=$CachePath" "-Xlog:aot=info" @ApplicationArguments
} else {
    Write-Warning "AOT cache '$CachePath' was not found; starting without it."
    & $Java @ApplicationArguments
}
exit $LASTEXITCODE
