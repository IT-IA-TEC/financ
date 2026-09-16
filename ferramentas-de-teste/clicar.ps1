# Clica num ponto da janela do IT.FC (coordenadas de dentro da janela).
param([Parameter(Mandatory=$true)][string]$Onde, [int]$Esperar = 3)

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Cli {
  [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
  [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint x, uint y, uint d, int e);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  public struct RECT { public int Left, Top, Right, Bottom; }
}
"@

$p = Get-Process | Where-Object { $_.MainWindowTitle -eq 'IT.FC' } | Select-Object -First 1
if (-not $p) { Write-Host "a janela do IT.FC nao esta aberta"; exit 1 }
[Cli]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 600

$r = New-Object Cli+RECT
[Cli]::GetWindowRect($p.MainWindowHandle, [ref]$r) | Out-Null
$xy = $Onde.Split(',')
[Cli]::SetCursorPos($r.Left + [int]$xy[0], $r.Top + [int]$xy[1]) | Out-Null
[Cli]::mouse_event(0x0002,0,0,0,0)
[Cli]::mouse_event(0x0004,0,0,0,0)
Start-Sleep -Seconds $Esperar
Write-Host "cliquei em $Onde"
