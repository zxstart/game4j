param(
    [Parameter(Mandatory = $true)]
    [string]$Java,
    [string]$CachePath = "game4j.aot",
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ApplicationArguments
)

$ErrorActionPreference = "Stop"
& $Java "-XX:AOTCacheOutput=$CachePath" @ApplicationArguments
exit $LASTEXITCODE
