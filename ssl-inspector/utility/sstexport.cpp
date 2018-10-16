#include <stdio.h>
#include <tchar.h>

#include "windows.h"
#include "wincrypt.h"
#include "atlbase.h"

#include <iostream>
#include <sstream>
#include <iomanip>
#include <algorithm>

#pragma comment(lib,"crypt32.lib")

std::string GetHexRepresentation(const unsigned char * Bytes, size_t Length)
{
std::ostringstream os;
os.fill('0');
os<<std::hex;

	for(const unsigned char * ptr=Bytes;ptr<Bytes+Length;ptr++)
	{
	os<<std::setw(2)<<(unsigned int)*ptr;
	}

std::string retval = os.str();
std::transform(retval.begin(), retval.end(),retval.begin(), ::toupper);
return retval;
}

BOOL WriteToFileWithHashAsFilename(PCCERT_CONTEXT pPrevCertContext)
{
#undef RETURN
#define RETURN(rv) \
{ \
if( hHash ) CryptDestroyHash(hHash); \
if( hProv ) CryptReleaseContext(hProv, 0); \
return rv; \
} 

HCRYPTPROV hProv = 0;
HCRYPTHASH hHash = 0;

BYTE byteFinalHash[20];
DWORD dwFinalHashSize = 20;

	if (!CryptAcquireContext(&hProv, NULL, NULL, PROV_RSA_FULL, CRYPT_VERIFYCONTEXT))
	{
	std::cout << "CryptAcquireContext failed: " << GetLastError() << std::endl;
	RETURN(FALSE);
	}

	if (!CryptCreateHash(hProv, CALG_SHA1, 0, 0, &hHash))
	{
	std::cout << "CryptCreateHash failed: " << GetLastError() << std::endl;
	RETURN(FALSE);
	}

	if (!CryptHashData(hHash, pPrevCertContext->pbCertEncoded, pPrevCertContext->cbCertEncoded, 0))
	{
	std::cout << "CryptHashData failed: " << GetLastError() << std::endl;
	RETURN(FALSE);
	}

	if (!CryptGetHashParam(hHash, HP_HASHVAL, byteFinalHash, &dwFinalHashSize, 0))
	{
	std::cout << "CryptGetHashParam failed: " << GetLastError() << std::endl;
	RETURN(FALSE);
	}

std::string strHash = GetHexRepresentation(byteFinalHash, dwFinalHashSize);
std::wostringstream filename;
filename << strHash.c_str() << ".der" <<std::ends;

FILE* f = _wfopen(filename.str().c_str(), L"wb+");

	if(!f)
	{
	std::wcout << "Failed to open file for writing: " << filename.str().c_str() << std::endl;
	RETURN(FALSE);
	}

int bytesWritten = fwrite(pPrevCertContext->pbCertEncoded, 1, pPrevCertContext->cbCertEncoded, f);
fclose(f);

	if(bytesWritten != pPrevCertContext->cbCertEncoded)
	{
	std::cout << "Failed to write file" << std::endl;
	RETURN(FALSE);
	}

RETURN(TRUE); 
}  

// usage: DumpCertsFromSst <output directory> <SST file 1> ... <SST file n>
int _tmain(int argc, _TCHAR* argv[])
{
SECURITY_ATTRIBUTES sa;   
memset(&sa, 0, sizeof(SECURITY_ATTRIBUTES));
sa.nLength = sizeof(SECURITY_ATTRIBUTES);
sa.bInheritHandle = FALSE;  

	if(argc < 2)
	{
	std::cout << "At least one argument must be provided: sstFile1 sstFile2 ... sstFileN etc" << std::endl;
	return 0;
	}

	for(int ii = 1; ii < argc; ++ii)
	{
	HANDLE       hFile = NULL;
	HCERTSTORE   hFileStore = NULL;
	LPCWSTR      pszFileName = argv[ii];

	//Open file
	hFile = CreateFile(pszFileName, GENERIC_READ, 0, &sa, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);                      

		if(INVALID_HANDLE_VALUE == hFile)
		{
		std::wcout << "Failed to open file: " << pszFileName  << std::endl;
		continue;
		}
		else
		{
		std::wcout << "Processing file: " << pszFileName  << std::endl;
		}

	//open certificate store
	hFileStore = CertOpenStore(CERT_STORE_PROV_FILE, 0, NULL, CERT_STORE_READONLY_FLAG, hFile);

		if(NULL == hFileStore)
		{
		CloseHandle(hFile);
		continue;
		}

	int count = 0;
	PCCERT_CONTEXT pPrevCertContext = NULL;
	pPrevCertContext = CertEnumCertificatesInStore(hFileStore, pPrevCertContext);

		while(NULL != pPrevCertContext)
		{
		if(WriteToFileWithHashAsFilename(pPrevCertContext)) ++count;
		pPrevCertContext = CertEnumCertificatesInStore(hFileStore, pPrevCertContext);
		}

	std::wcout << "Wrote " << count << " certificates" << std::endl;
	CloseHandle(hFile);
	CertCloseStore(hFileStore, 0);
	}

return 1;
}
