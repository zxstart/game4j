param(
    [Parameter(Mandatory = $true)]
    [string]$Java,
    [string]$RecordingPath = (Join-Path $env:TEMP "game4j-startup.jfr"),
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ApplicationArguments
)

$ErrorActionPreference = "Stop"
$resolvedRecordingPath = [System.IO.Path]::GetFullPath($RecordingPath)
& $Java "-XX:StartFlightRecording=filename=$resolvedRecordingPath,settings=profile,dumponexit=true" @ApplicationArguments
exit $LASTEXITCODE
