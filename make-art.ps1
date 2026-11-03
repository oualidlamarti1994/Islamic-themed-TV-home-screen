<#
  Draws Noor's app icon (192x192), TV banner (320x180) and the matching CleanMyTV Home wallpaper (1920x1080):
  emerald night, eight-pointed star tessellation, gold mihrab arch and crescent. Uses only System.Drawing.
#>
param([string]$Out = (Join-Path $PSScriptRoot 'app\res'), [string]$Wall = (Join-Path $PSScriptRoot 'docs\wallpaper.jpg'))
Add-Type -AssemblyName System.Drawing
$gold = [Drawing.Color]::FromArgb(255, 217, 180, 90)
$emerald = [Drawing.Color]::FromArgb(255, 6, 42, 35); $night = [Drawing.Color]::FromArgb(255, 3, 19, 26)

function Star($g, $pen, $cx, $cy, $r) {
  foreach ($sq in 0, 1) {
    $pts = 0..3 | ForEach-Object { $a = $sq * [Math]::PI / 4 + $_ * [Math]::PI / 2; New-Object Drawing.PointF ($cx + $r * [Math]::Cos($a)), ($cy + $r * [Math]::Sin($a)) }
    $g.DrawPolygon($pen, [Drawing.PointF[]]$pts)
  }
}
function Arch($cx, $half, $top, $bottom) {
  $p = New-Object Drawing.Drawing2D.GraphicsPath
  $spring = $top + ($bottom - $top) * 0.30
  $p.AddLine([single]($cx - $half), [single]$bottom, [single]($cx - $half), [single]$spring)
  $p.AddBezier([single]($cx - $half), [single]$spring, [single]($cx - $half), [single]($spring - ($spring - $top) * 0.55), [single]($cx - $half * 0.25), [single]($top + ($spring - $top) * 0.25), [single]$cx, [single]$top)
  $p.AddBezier([single]$cx, [single]$top, [single]($cx + $half * 0.25), [single]($top + ($spring - $top) * 0.25), [single]($cx + $half), [single]($spring - ($spring - $top) * 0.55), [single]($cx + $half), [single]$spring)
  $p.AddLine([single]($cx + $half), [single]$spring, [single]($cx + $half), [single]$bottom)
  $p
}
function Crescent($g, $cx, $cy, $r) {
  $a = New-Object Drawing.Region (New-Object Drawing.Drawing2D.GraphicsPath)
  $p1 = New-Object Drawing.Drawing2D.GraphicsPath; $p1.AddEllipse([single]($cx - $r), [single]($cy - $r), [single](2 * $r), [single](2 * $r))
  $p2 = New-Object Drawing.Drawing2D.GraphicsPath; $p2.AddEllipse([single]($cx - $r * 0.4), [single]($cy - $r * 1.05), [single](1.7 * $r), [single](1.7 * $r))
  $reg = New-Object Drawing.Region $p1; $reg.Exclude($p2)
  $g.FillRegion((New-Object Drawing.SolidBrush $gold), $reg)
}
function Canvas($w, $h, [bool]$arch, [double]$tile, [bool]$calmLeft) {
  $bmp = New-Object Drawing.Bitmap $w, $h
  $g = [Drawing.Graphics]::FromImage($bmp); $g.SmoothingMode = 'AntiAlias'
  $bg = New-Object Drawing.Drawing2D.LinearGradientBrush((New-Object Drawing.Point 0, 0), (New-Object Drawing.Point 0, $h), $emerald, $night)
  $g.FillRectangle($bg, 0, 0, $w, $h)
  $pen = New-Object Drawing.Pen ([Drawing.Color]::FromArgb(48, $gold)), ([Math]::Max(1, $h / 800))
  for ($y = -$tile; $y -lt $h + $tile; $y += $tile) { for ($x = -$tile; $x -lt $w + $tile; $x += $tile) { Star $g $pen ($x + $tile / 2) ($y + $tile / 2) ($tile * 0.42); Star $g $pen $x $y ($tile * 0.16) } }
  $vp = New-Object Drawing.Drawing2D.GraphicsPath; $vp.AddEllipse([single](-$w * 0.25), [single](-$h * 0.25), [single]($w * 1.5), [single]($h * 1.5))
  $vb = New-Object Drawing.Drawing2D.PathGradientBrush $vp
  $vb.CenterColor = [Drawing.Color]::FromArgb(200, 4, 28, 24); $vb.SurroundColors = @([Drawing.Color]::FromArgb(0, 4, 28, 24))
  $g.FillRectangle($vb, 0, 0, $w, $h)
  if ($calmLeft) {
    $sh = New-Object Drawing.Drawing2D.LinearGradientBrush((New-Object Drawing.Point 0, 0), (New-Object Drawing.Point ([int]($w * 0.55)), 0), [Drawing.Color]::FromArgb(215, 3, 22, 20), [Drawing.Color]::FromArgb(0, 3, 22, 20))
    $g.FillRectangle($sh, 0, 0, [int]($w * 0.55), $h)
  }
  if ($arch) {
    $cx = if ($calmLeft) { $w * 0.74 } else { $w / 2 }
    $half = if ($calmLeft) { $w * 0.17 } else { $w * 0.30 }
    $top = $h * 0.16; $bottom = $h * 1.02
    $g.DrawPath((New-Object Drawing.Pen $gold, ([Math]::Max(2, $h / 220))), (Arch $cx $half $top $bottom))
    $g.DrawPath((New-Object Drawing.Pen ([Drawing.Color]::FromArgb(140, $gold)), ([Math]::Max(1, $h / 700))), (Arch $cx ($half - $h * 0.025) ($top + $h * 0.035) $bottom))
    Crescent $g $cx ($top - $h * 0.05) ($h * 0.03)
  }
  @{ bmp = $bmp; g = $g }
}
function SavePng($c, $path) { New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null; $c.bmp.Save($path, [Drawing.Imaging.ImageFormat]::Png); $c.g.Dispose(); $c.bmp.Dispose(); "saved $path" }

# icon: crescent + star on emerald disc
$c = Canvas 192 192 $false 64 $false
$c.g.DrawEllipse((New-Object Drawing.Pen $gold, 6), 8, 8, 176, 176)
Crescent $c.g 92 96 52
Star $c.g (New-Object Drawing.Pen $gold, 4) 132 70 16
SavePng $c (Join-Path $Out 'mipmap\icon.png')

# banner: arch + name
$c = Canvas 320 180 $true 40 $false
$f = New-Object Drawing.Font 'Segoe UI Semibold', 30
$sf = New-Object Drawing.StringFormat; $sf.Alignment = 'Center'; $sf.LineAlignment = 'Center'
$c.g.DrawString('Noor', $f, (New-Object Drawing.SolidBrush ([Drawing.Color]::FromArgb(255, 246, 235, 210))), (New-Object Drawing.RectangleF 0, 70, 320, 70), $sf)
SavePng $c (Join-Path $Out 'drawable\banner.png')

# wallpaper for CleanMyTV Home (split layout: clock left, apps right)
$c = Canvas 1920 1080 $true 135 $true
$codec = [Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object MimeType -eq 'image/jpeg'
$ep = New-Object Drawing.Imaging.EncoderParameters 1; $ep.Param[0] = New-Object Drawing.Imaging.EncoderParameter ([Drawing.Imaging.Encoder]::Quality), 92L
$c.bmp.Save($Wall, $codec, $ep); $c.g.Dispose(); $c.bmp.Dispose(); "saved $Wall"
