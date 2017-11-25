function Show-Menu
{
    param (
        [string]$Title = 'Redatasense - Help menu'
    )
    Clear-Host
    Write-Host "================ $Title ================"
    
    Write-Host "1: Press 'F' for File Discovery."
    Write-Host "2: Press 'C' for Column Discovery"
    Write-Host "3: Press 'D' for Data Discovery"
    Write-Host "Q: Press 'Q' to quit."
	Write-Host ""
}
$filediscovery = 'java -jar RedataSense.jar file-discovery'
$columndiscovery = 'java -jar RedataSense.jar database-discovery -c'
$datadiscovery = 'java -jar RedataSense.jar database-discovery -d'
do
 {
     Show-Menu
     $selection = Read-Host "Please make a selection"
     switch ($selection)
     {
         'F' {
             iex $filediscovery
			 Write-Host " All done!"
			 Write-Host " Result is available: FileDiscoveryResult.json"
         } 'C' {
             iex $columndiscovery
			 Write-Host " All done!"
			 Write-Host " Result is available: ColumnDiscoveryResult.json"
         } 'D' {
             iex $datadiscovery
			 Write-Host " All done!"
			 Write-Host " Result is available: DataDiscoveryResult.json"
         }
     }
     pause
 }
 until ($selection -eq 'q')
show-menu