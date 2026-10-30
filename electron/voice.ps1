# The announcer's phrases with the Windows voices (Irina, Pavel ...), WinRT speech synthesis -> WAV files.
# env: AV_MODE = list | make ; AV_VOICE = voice id ; AV_RATE, AV_PITCH ; AV_IN = json [{id, text}] ; AV_OUT = folder
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
function Await($op, $t) { $m = $asTask.MakeGenericMethod($t); $task = $m.Invoke($null, @($op)); $task.Wait(-1) | Out-Null; $task.Result }
[void][Windows.Media.SpeechSynthesis.SpeechSynthesizer, Windows.Media.SpeechSynthesis, ContentType = WindowsRuntime]
[void][Windows.Storage.Streams.DataReader, Windows.Storage.Streams, ContentType = WindowsRuntime]
if ($env:AV_MODE -eq 'list') {
  $l = @([Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices | ForEach-Object { [pscustomobject]@{ id = $_.Id; name = $_.DisplayName; lang = $_.Language; gender = "$($_.Gender)" } })
  [Console]::OutputEncoding = [Text.Encoding]::UTF8; ConvertTo-Json -InputObject $l -Compress; exit
}
$syn = New-Object Windows.Media.SpeechSynthesis.SpeechSynthesizer
$v = [Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices | Where-Object { $_.Id -eq $env:AV_VOICE } | Select-Object -First 1; if ($v) { $syn.Voice = $v }
try { $syn.Options.SpeakingRate = [double]::Parse($env:AV_RATE, [Globalization.CultureInfo]::InvariantCulture); $syn.Options.AudioPitch = [double]::Parse($env:AV_PITCH, [Globalization.CultureInfo]::InvariantCulture) } catch {}
$items = Get-Content -LiteralPath $env:AV_IN -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($it in $items) {
  $st = Await ($syn.SynthesizeTextToStreamAsync([string]$it.text)) ([Windows.Media.SpeechSynthesis.SpeechSynthesisStream])
  $size = [uint32]$st.Size; $r = New-Object Windows.Storage.Streams.DataReader($st.GetInputStreamAt(0))
  Await ($r.LoadAsync($size)) ([uint32]) | Out-Null; $bytes = New-Object byte[] $size; $r.ReadBytes($bytes)
  [IO.File]::WriteAllBytes((Join-Path $env:AV_OUT ([string]$it.id + '.wav')), $bytes)
}
'done'
