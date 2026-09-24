# Generate red/black/white theme icon (256x256 PNG) for the Electron client.
Add-Type -AssemblyName System.Drawing

$w = 256
$h = 256
$r = 56

$bmp = New-Object System.Drawing.Bitmap $w, $h
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = 'AntiAlias'
$g.Clear([System.Drawing.Color]::Transparent)

# Rounded-rect background: brand red
$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$path.AddArc(0, 0, 2 * $r, 2 * $r, 180, 90)
$path.AddArc($w - 2 * $r, 0, 2 * $r, 2 * $r, 270, 90)
$path.AddArc($w - 2 * $r, $h - 2 * $r, 2 * $r, 2 * $r, 0, 90)
$path.AddArc(0, $h - 2 * $r, 2 * $r, 2 * $r, 90, 90)
$path.CloseFigure()

$red = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 227, 6, 19))
$g.FillPath($red, $path)

# White bold "R"
$font = New-Object System.Drawing.Font('Arial Black', 150, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$white = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
$sf = New-Object System.Drawing.StringFormat
$sf.Alignment = 'Center'
$sf.LineAlignment = 'Center'
$rect = New-Object System.Drawing.RectangleF 0, 8, $w, ($h - 16)
$g.DrawString('R', $font, $white, $rect, $sf)

if (-not (Test-Path 'build')) { New-Item -ItemType Directory -Path 'build' | Out-Null }
$bmp.Save((Join-Path $PWD 'build\icon.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Output "icon saved: $PWD\build\icon.png"
