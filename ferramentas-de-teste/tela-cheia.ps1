# Clica na janela do IT.FC e fotografa a TELA INTEIRA, para pegar tambem as
# listas suspensas, que sao janelas proprias e nao saem na foto da janela.
#
#   .\tela-cheia.ps1 -Clique "1727,76" -Foto lista
param(
  [string]$Clique = "",
  [string]$Foto = "tela",
  [int]$Esperar = 3
)

Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Drawing;
public class Cheia {
  [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
  [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint x, uint y, uint d, int e);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  public struct RECT { public int Left, Top, Right, Bottom; }
  public static void Clicar(IntPtr h, int x, int y) {
    RECT r; GetWindowRect(h, out r);
    SetCursorPos(r.Left + x, r.Top + y);
    mouse_event(0x0002,0,0,0,0); mouse_event(0x0004,0,0,0,0);
  }
}
"@ -ReferencedAssemblies System.Drawing

$p = Get-Process | Where-Object { $_.MainWindowTitle -eq 'IT.FC' } | Select-Object -First 1
if (-not $p) { Write-Host "a janela do IT.FC nao esta aberta"; exit 1 }
[Cheia]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 700

if ($Clique) {
  $xy = $Clique.Split(',')
  [Cheia]::Clicar($p.MainWindowHandle, [int]$xy[0], [int]$xy[1])
}
Start-Sleep -Seconds $Esperar

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$area = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
$bmp = New-Object System.Drawing.Bitmap $area.Width, $area.Height
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($area.Location, [System.Drawing.Point]::Empty, $area.Size)
# a foto fica dentro do proprio projeto, em provas/telas
$pasta = Join-Path (Split-Path -Parent $PSScriptRoot) "provas/telas"
if (-not (Test-Path -LiteralPath $pasta)) { New-Item -ItemType Directory -Force -Path $pasta | Out-Null }
$bmp.Save("$pasta\$Foto.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Host "foto: $pasta\$Foto.png"
