param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

python "$PSScriptRoot/test_lab.py" @Arguments
exit $LASTEXITCODE
