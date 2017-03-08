$type = [System.Security.Cryptography.X509Certificates.X509ContentType]::Cert

$certs = get-childitem -path cert:\LocalMachine\AuthRoot
foreach($item in $certs)
{
	$hash = $item.GetCertHashString()
	$path = "c:\temp\" + $hash + ".der"
	[System.IO.File]::WriteAllBytes($path, $item.export($type))
}

$certs = get-childitem -path cert:\LocalMachine\CA
foreach($item in $certs)
{
	$hash = $item.GetCertHashString()
	$path = "c:\temp\" + $hash + ".der"
	[System.IO.File]::WriteAllBytes($path, $item.export($type))
}

$certs = get-childitem -path cert:\LocalMachine\Root
foreach($item in $certs)
{
	$hash = $item.GetCertHashString()
	$path = "c:\temp\" + $hash + ".der"
	[System.IO.File]::WriteAllBytes($path, $item.export($type))
}

$certs = get-childitem -path cert:\CurrentUser\AuthRoot
foreach($item in $certs)
{
	$hash = $item.GetCertHashString()
	$path = "c:\temp\" + $hash + ".der"
	[System.IO.File]::WriteAllBytes($path, $item.export($type))
}

$certs = get-childitem -path cert:\CurrentUser\CA
foreach($item in $certs)
{
	$hash = $item.GetCertHashString()
	$path = "c:\temp\" + $hash + ".der"
	[System.IO.File]::WriteAllBytes($path, $item.export($type))
}

$certs = get-childitem -path cert:\CurrentUser\Root
foreach($item in $certs)
{
	$hash = $item.GetCertHashString()
	$path = "c:\temp\" + $hash + ".der"
	[System.IO.File]::WriteAllBytes($path, $item.export($type))
}

