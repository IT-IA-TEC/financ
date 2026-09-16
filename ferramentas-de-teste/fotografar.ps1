# Abre o IT.FC direto numa tela, fotografa e fecha.
#   .\fotografar.ps1 -Tela pagar -Foto v-pagar

param([string]$Tela = "painel", [string]$Foto = "tela", [int]$Esperar = 30)

$projeto = Split-Path -Parent $PSScriptRoot
# a foto fica dentro do proprio projeto, em provas/telas
$pasta = Join-Path (Split-Path -Parent $PSScriptRoot) "provas/telas"
if (-not (Test-Path -LiteralPath $pasta)) { New-Item -ItemType Directory -Force -Path $pasta | Out-Null }

Get-Process | Where-Object { $_.MainWindowTitle -eq 'IT.FC' } | ForEach-Object {
  Stop-Process -Id $_.Id -Force
}
Start-Sleep -Seconds 2

Start-Process powershell.exe -ArgumentList '-ExecutionPolicy','Bypass','-File','conferir-tela.ps1',$Tela `
  -WorkingDirectory $projeto -WindowStyle Hidden
Start-Sleep -Seconds $Esperar

Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Drawing;
public class Foto {
  [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  public struct RECT { public int Left, Top, Right, Bottom; }
  public static void Tirar(IntPtr h, string caminho) {
    RECT r; GetWindowRect(h, out r);
    using (Bitmap bmp = new Bitmap(r.Right - r.Left, r.Bottom - r.Top))
    using (Graphics g = Graphics.FromImage(bmp)) {
      IntPtr hdc = g.GetHdc(); PrintWindow(h, hdc, 2); g.ReleaseHdc(hdc);
      bmp.Save(caminho, System.Drawing.Imaging.ImageFormat.Png);
    }
  }
}
"@ -ReferencedAssemblies System.Drawing

$p = Get-Process | Where-Object { $_.MainWindowTitle -eq 'IT.FC' } | Select-Object -First 1
if (-not $p) { Write-Host "a janela nao abriu"; exit 1 }
[Foto]::Tirar($p.MainWindowHandle, "$pasta\$Foto.png")
Write-Host "foto: $pasta\$Foto.png"
