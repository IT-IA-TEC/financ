# Fotografa a JANELA do IT.FC pelo proprio Windows (PrintWindow), que funciona
# mesmo quando a captura da tela inteira nao esta disponivel.
param([string]$Foto = "janela")

Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Drawing;
public class Jan {
  [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr dc, uint f);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  public struct RECT { public int Left, Top, Right, Bottom; }
}
"@ -ReferencedAssemblies System.Drawing

$p = Get-Process | Where-Object { $_.MainWindowTitle -eq 'IT.FC' } | Select-Object -First 1
if (-not $p) { Write-Host "a janela do IT.FC nao esta aberta"; exit 1 }
[Jan]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 600

$r = New-Object Jan+RECT
[Jan]::GetWindowRect($p.MainWindowHandle, [ref]$r) | Out-Null
$largura = $r.Right - $r.Left
$altura = $r.Bottom - $r.Top

Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap $largura, $altura
$g = [System.Drawing.Graphics]::FromImage($bmp)
$dc = $g.GetHdc()
[Jan]::PrintWindow($p.MainWindowHandle, $dc, 2) | Out-Null
$g.ReleaseHdc($dc)
# a foto fica dentro do proprio projeto, em provas/telas
$pasta = Join-Path (Split-Path -Parent $PSScriptRoot) "provas/telas"
if (-not (Test-Path -LiteralPath $pasta)) { New-Item -ItemType Directory -Force -Path $pasta | Out-Null }
$bmp.Save("$pasta\$Foto.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Host "foto: $pasta\$Foto.png ($largura x $altura)"
