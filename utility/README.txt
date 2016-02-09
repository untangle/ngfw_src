14-OCT-2014

The file extra_certs.tar.gz contains .der certificates exported from a
generic install of Firefox version 32 on Windows, and .cer certificates
exported from the "Trusted Root Certification Authorities" store and the
Intermediate Certificate Authorities" store from a generic Windows 7 PC.
See the comments in root_keystore_maker for more information.

------------------------------------------------------------------------------
09-FEB-2016

The file extra_certs.tar.gz has been updated to include the latest certs
exported from generic Firefox 44 on Windows along with certificates
exported from a clean Windows 10 installation.
					      
Firefox notes:

They changed some low level stuff which required a fix to the exporter
extension. The fixed version is included here, but is not signed so
installing requires changing xpinstall.signatures.required=False on
the about:config page. Once installed, go here to do the export:
Tools / Options / Advanced / Certificates / View Certificates / Export All

Windows notes:

I created and used the included Windows PowerShell script to export the
root certificates on a Windows 10 machine.  By default, Windows enforces
restrictions on PowerShell scripts, so you have to run powershell.exe as
Administrator, and issue this command:

Set-ExecutionPolicy -ExecutionPolicy unrestricted

Once you've done that, you can then use the included script like this:

powershell -File windows_exporter.ps1

This will export all of the certificates, and put them in C:\Temp which
you should either create, or you can modify the script.
