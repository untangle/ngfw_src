16-OCT-2018

I came up with new sources for creating our trusted-ca-list.jks file

We start with all of the .der certificates in /usr/share/ca-certificates
which are included in the Debian ca-certificates package.

Next we add the full list of Windows root CA's which can be downloaded from
the Microsoft update server using the CertUtil command. This worked on
Windows 7 and Winows 10 when I created this update:

CertUtil -generateSSTFromWU rootstore.sst

This file can then be passed to the sstexport.exe utility which will extract
each certificate and save it do a .der file in the current directory.

Finally we get the full list of Mozilla root CA's from the NSS suite.
The file can be downloaded directly from this URL:

wget https://hg.mozilla.org/mozilla-central/raw-file/tip/security/nss/lib/ckfw/builtins/certdata.txt

We pass this file to the convert_mozilla_certdata utility which will extract
each certificate and save it to a .pem file in the current directory.

./convert_mozilla_certdata -to-files certdata.txt

When I created the jks file for this update, the keytool reported
an "already exists" exception for all of the Mozilla certificates, which means
they were already added from one of the other two sources.

The file extra_certs.tar.gz contains all of the Windows and Mozilla
certificates that we used. I manually removed a couple dozen that were
extracted from Microsoft Update because they were expired. I also verified
than any cert with a public key size less than 1024 bits uses ecdsa so
we don't trust any certs than can be easily compromised as was found in
24-SEP-2018 Concordia TLS Security Interception review.

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

------------------------------------------------------------------------------
14-OCT-2014

The file extra_certs.tar.gz contains .der certificates exported from a
generic install of Firefox version 32 on Windows, and .cer certificates
exported from the "Trusted Root Certification Authorities" store and the
Intermediate Certificate Authorities" store from a generic Windows 7 PC.
See the comments in root_keystore_maker for more information.

