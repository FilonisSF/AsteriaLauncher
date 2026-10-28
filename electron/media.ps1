# "Now playing" for AsteriaLauncher: the Windows media controls (Spotify, Yandex Music, browsers...).
# Reader mode (default): one UTF-8 JSON line every ~1.5 s: title, artist, playing, pos, dur, app, key;
#   "cover" (base64 PNG, 128x128) is added only when the track changes.
# Commands: the launcher writes 'toggle' | 'next' | 'prev' into the file in $env:ASTERIA_MEDIA_CMD; the reader runs it
#   at once and reports the new track right away (starting a new PowerShell per click took ~1 s).
$ErrorActionPreference = 'SilentlyContinue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing
$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
function Await($op, [Type]$t) { $task = $asTask.MakeGenericMethod($t).Invoke($null, @($op)); $task.Wait(-1) | Out-Null; $task.Result }
[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime] | Out-Null
[Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime] | Out-Null
[Windows.Storage.Streams.IInputStream, Windows.Storage.Streams, ContentType = WindowsRuntime] | Out-Null
# the thumbnail stream arrives as a bare COM object: PowerShell can't pick the AsStreamForRead overload itself, so it is called by reflection
$asStream = [System.IO.WindowsRuntimeStreamExtensions].GetMethod('AsStreamForRead', [Type[]]@([Windows.Storage.Streams.IInputStream]))
$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])

Add-Type -Namespace AsteriaKeys -Name K -MemberDefinition '[DllImport("user32.dll")] public static extern void keybd_event(byte b, byte s, uint f, UIntPtr e);'
function VolKey([byte]$vk, [int]$n) { for ($i = 0; $i -lt $n; $i++) { [AsteriaKeys.K]::keybd_event($vk, 0, 0, [UIntPtr]::Zero); [AsteriaKeys.K]::keybd_event($vk, 0, 2, [UIntPtr]::Zero) } }
function RunCmd($c) {
  if ($c -eq 'volup') { VolKey 0xAF 2; return } elseif ($c -eq 'voldown') { VolKey 0xAE 2; return } elseif ($c -eq 'mute') { VolKey 0xAD 1; return }
  $s = $mgr.GetCurrentSession(); if (-not $s) { return }
  if ($c -eq 'next') { Await ($s.TrySkipNextAsync()) ([bool]) | Out-Null }
  elseif ($c -eq 'prev') { Await ($s.TrySkipPreviousAsync()) ([bool]) | Out-Null }
  elseif ($c -eq 'toggle') { Await ($s.TryTogglePlayPauseAsync()) ([bool]) | Out-Null }
}
$cmdFile = $env:ASTERIA_MEDIA_CMD

function Cover($p) {
  try {
    if (-not $p.Thumbnail) { return $null }
    $ras = Await ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
    $st = $asStream.Invoke($null, @($ras)); $buf = New-Object System.IO.MemoryStream; $st.CopyTo($buf); $buf.Position = 0
    $img = [System.Drawing.Image]::FromStream($buf)
    $bmp = New-Object System.Drawing.Bitmap 128, 128
    $g = [System.Drawing.Graphics]::FromImage($bmp); $g.InterpolationMode = 'HighQualityBicubic'; $g.SmoothingMode = 'AntiAlias'; $g.Clear([System.Drawing.Color]::Transparent)
    $gp = New-Object System.Drawing.Drawing2D.GraphicsPath; $rr = 28 # rounded corners: the game shows the picture as it is
    $gp.AddArc(0, 0, $rr, $rr, 180, 90); $gp.AddArc(128 - $rr, 0, $rr, $rr, 270, 90); $gp.AddArc(128 - $rr, 128 - $rr, $rr, $rr, 0, 90); $gp.AddArc(0, 128 - $rr, $rr, $rr, 90, 90); $gp.CloseFigure(); $g.SetClip($gp)
    $side = [math]::Min($img.Width, $img.Height); $sx = [int](($img.Width - $side) / 2); $sy = [int](($img.Height - $side) / 2)
    $g.DrawImage($img, (New-Object System.Drawing.Rectangle 0, 0, 128, 128), $sx, $sy, $side, $side, [System.Drawing.GraphicsUnit]::Pixel)
    $ms = New-Object System.IO.MemoryStream; $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose(); $img.Dispose()
    return [Convert]::ToBase64String($ms.ToArray())
  } catch { return $null }
}

$lastKey = ''; $fast = 0; $coverTries = 0; $coverNext = [DateTime]::Now; $lastCover = ''
while ($true) {
  if ($cmdFile -and (Test-Path $cmdFile)) { $c = (Get-Content $cmdFile -Raw).Trim(); Remove-Item $cmdFile -Force; RunCmd $c; $fast = 25 } # then poll every 80 ms for ~2 s: the new track shows the moment the player switches
  $o = @{ playing = $false; title = ''; artist = '' }
  $s = $mgr.GetCurrentSession()
  if ($s) {
    $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
    $tl = $s.GetTimelineProperties(); $pb = $s.GetPlaybackInfo()
    $key = "$($s.SourceAppUserModelId)|$($p.Title)|$($p.Artist)"
    $playing = ($pb.PlaybackStatus -eq 'Playing'); $pos = $tl.Position.TotalSeconds; $dur = $tl.EndTime.TotalSeconds
    if ($playing -and $tl.LastUpdatedTime.Year -gt 2000) { $pos += ([DateTimeOffset]::Now - $tl.LastUpdatedTime).TotalSeconds } # players report the position rarely
    if ($dur -gt 0) { $pos = [math]::Max(0, [math]::Min($dur, $pos)) }
    $o = @{ title = $p.Title; artist = $p.Artist; playing = $playing; pos = [math]::Round($pos, 1); dur = [math]::Round($dur, 1); app = $s.SourceAppUserModelId; key = $key }
    # players change the title first and the picture a moment later: read the cover again a few times after a new track and send it when it changes
    if ($key -ne $lastKey) { $lastKey = $key; $coverTries = 4; $coverNext = [DateTime]::Now }
    if ($coverTries -gt 0 -and [DateTime]::Now -ge $coverNext) { $coverTries--; $coverNext = [DateTime]::Now.AddMilliseconds(700); $c = Cover $p; if (-not $c) { $c = '' }
      if ($c -ne $lastCover) { $lastCover = $c; $o.cover = $c; $o.key = "$key#" + $c.Length } }
  }
  [Console]::Out.WriteLine(($o | ConvertTo-Json -Compress)); [Console]::Out.Flush()
  if ($fast -gt 0) { $fast--; Start-Sleep -Milliseconds 80 }
  else { for ($i = 0; $i -lt 8; $i++) { Start-Sleep -Milliseconds 50; if ($cmdFile -and (Test-Path $cmdFile)) { break } } } # a command is picked up within 50 ms
}
