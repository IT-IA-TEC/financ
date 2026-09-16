# Ajuda a conferir a janela do IT.FC: clica, digita e tira foto.
#
#   .\conferir.ps1 -Clique "820,60" -Foto "menu-titulos"
#   .\conferir.ps1 -Digitar "William{ENTER}" -Foto "entrou"

param(
  [string]$Clique = "",
  [string]$Digitar = "",
  [string]$Foto = "tela",
  [int]$Esperar = 4
)

Add-Type @"
using System;
using System.Runtime.InteropServices;
using System.Drawing;
public class Janela {
  [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
  [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint x, uint y, uint d, int e);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr h, IntPtr hdc, uint flags);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  public struct RECT { public int Left, Top, Right, Bottom; }
  public static void Clicar(IntPtr h, int x, int y) {
    RECT r; GetWindowRect(h, out r);
    SetCursorPos(r.Left + x, r.Top + y);
    mouse_event(0x0002,0,0,0,0); mouse_event(0x0004,0,0,0,0);
  }
  public static void Fotografar(IntPtr h, string caminho) {
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
if (-not $p) { Write-Host "a janela do IT.FC nao esta aberta"; exit 1 }
[Janela]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 700

if ($Clique) {
  $xy = $Clique.Split(',')
  [Janela]::Clicar($p.MainWindowHandle, [int]$xy[0], [int]$xy[1])
  Start-Sleep -Milliseconds 700
}
if ($Digitar) {
  Add-Type -AssemblyName System.Windows.Forms
  [System.Windows.Forms.SendKeys]::SendWait($Digitar)
}
Start-Sleep -Seconds $Esperar

# a foto fica dentro do proprio projeto, em provas/telas
$pasta = Join-Path (Split-Path -Parent $PSScriptRoot) "provas/telas"
if (-not (Test-Path -LiteralPath $pasta)) { New-Item -ItemType Directory -Force -Path $pasta | Out-Null }
[Janela]::Fotografar($p.MainWindowHandle, "$pasta\$Foto.png")
Write-Host "foto: $pasta\$Foto.png"
