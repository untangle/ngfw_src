import pytest
import unittest
import json
import os
from glob import glob
from os.path import join, getctime
from tests.common import NGFWTestCase
import tests.global_functions as global_functions
import runtests.test_registry as test_registry
import runtests.remote_control as remote_control
import runtests.overrides as overrides
import requests
import re

certData='''-----BEGIN CERTIFICATE-----
MIIFuTCCA6GgAwIBAgICEAEwDQYJKoZIhvcNAQELBQAwZzELMAkGA1UEBhMCVVMx
EzARBgNVBAgMCkNhbGlmb3JuaWExDzANBgNVBAoMBkFyaXN0YTERMA8GA1UECwwI
U2VjdXJpdHkxHzAdBgNVBAMMFkFyaXN0YS1DQS1JbnRlcm1lZGlhdGUwHhcNMjQw
MzE5MTMzMDMwWhcNNDQwMzE0MTMzMDMwWjBgMQswCQYDVQQGEwJVUzETMBEGA1UE
CAwKQ2FsaWZvcm5pYTEPMA0GA1UECgwGQXJpc3RhMREwDwYDVQQLDAhTZWN1cml0
eTEYMBYGA1UEAwwPZWRnZS5hcmlzdGEuY29tMIICIjANBgkqhkiG9w0BAQEFAAOC
Ag8AMIICCgKCAgEA5MbtvkBGGxTbUwDiFYgQEhVCIK/dXErCHYlD8pknK/bjdcBd
ByK2JEJKY5sLqEiHwqILlIZgeCEwTA/Jw4U/DMbT5yBlDwM2BzboHte4O0kbiRU8
NIj9ywAw7RtstRixK1MZao1/hxp9u+sz5MvemZp6paU7ltv08pLfJnVvVUX4bXX2
ZDJ0jMzbMRB5XJc5DkyD7cwNRCtQaSkCGQdlmJicRLbYQRKX4Q9kgEet6X/fh5eu
VDHJS5IAXZikTsoNLLTZBlzk5cBR1hrS+chaI6wwl1HzKjE1PiJs4m4mbiN+OsfS
yUVvBDWxVl/ppG9Vu7uQ1lNsfCdPkHj/OL8B0zBJ1iSCNndUFy0Ib75b3uVAeT2W
7KG4sEpWK2mhjkd1quEroSoK0mmU9xA5DUe0bmOmuOo1xkicK0q4PnG94G2M57Vj
fKq6yQcUBYI1p5IQJfsiyYRT0YKKvyd7tgvkhCswalxRZl2O26vXQKsJ6+eeJ0nA
hR4euf22sq2pzYYbE1DjQlqNy19DGvERFPgKqNQQOc9Ol3xM1NULBjR29qGe93TK
oIH/VosPgKAgcd0A8I2rLOWA0pIjjRiWzL7rga+ABiPg3F42xUShIO4vxT6gFo2A
nrPMG91lSKmdr+EgOYunfpiYpSsjueRj0NxfQdkG5YtEOm+1C9iYhy2vilcCAwEA
AaN2MHQwCQYDVR0TBAIwADALBgNVHQ8EBAMCBeAwGgYDVR0RBBMwEYIPZWRnZS5h
cmlzdGEuY29tMB0GA1UdDgQWBBR+Wjy4y0JmS7xC7KqrmdqOOY6T/DAfBgNVHSME
GDAWgBSSTkQTlOPSgmpopZHv6P/wVyAAcTANBgkqhkiG9w0BAQsFAAOCAgEALOsz
Cj14BafGM22HTM6ZfDXXCsRUX+QtkwktfFsi8Aizg2HGncj0bzVzDlc08fyHbIMm
SUSNotLqnFUSO9s1aCT4UPcFNOdSk5T4ZroOFFbSD/BvvB6LyE1cYUcVQyRUfBBz
z6XEzKfsNDPnI6XgGnCrW1sZObddOvFNNTnz48TCN69an/GQbAqw17J1w0wpxqG6
rReEZiz34jg2PjnTg0QteORRe9Cjt2bhzFLA8GDDmllK0ZpINwNUJILCQkxL8dcy
Z6QivTQpeEq3UAxySXvGWkKgWyaGcNQ1zdVjEj5UGSkPX1qD8wPCVP1/3ARla7oO
Z/iCvu5wXCkFk19vwm0wo5uawy6zwtmCn69L5IDSHzM7gX+7+5xPBOk0Leqm25WB
omLr/p2UZZ9jV/m0kN2Hl9sgBg8y7jzOvZbqTDHiqAi7hYZblc/FWLQTf1Ui5paX
1gPY+gX19Z1wQ6tK+TfeWBQHJHAoEV0edpjjeSnyiVEx76oNamJzqq1Z5cTy/HEU
wGshQchIgHvEO0/GU5wBXMLOn1Auu/eHfdybsTMy8r6+qsJz2BEtlnoqdTFR+ycY
JDG3L1eibll2LGLFg8L33Q1u+RS4E5j57SI5oUyskbShDOYwfxMmd/Sdv8SuUOos
RojmGPA8X2/pWj2Zz15xLJIA2Fs8vd51HjM5iwo=
-----END CERTIFICATE-----'''
keyData='''-----BEGIN PRIVATE KEY-----
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
kT+xgttJGeyjCRr+4twLV5Ejex8ikQ==
-----END PRIVATE KEY-----'''
extraData='''-----BEGIN CERTIFICATE-----
MIIF9jCCA96gAwIBAgICEAAwDQYJKoZIhvcNAQELBQAwgZwxCzAJBgNVBAYTAlVT
MRMwEQYDVQQIDApDYWxpZm9ybmlhMRQwEgYDVQQHDAtTYW50YSBDbGFyYTEPMA0G
A1UECgwGQXJpc3RhMREwDwYDVQQLDAhTZWN1cml0eTEXMBUGA1UEAwwOQXJpc3Rh
LUNBLVJvb3QxJTAjBgkqhkiG9w0BCQEWFnJvaGl0LnNpbmdoQGFyaXN0YS5jb20w
HhcNMjQwMzE5MTMzMDI5WhcNMjUwMzE5MTMzMDI5WjBnMQswCQYDVQQGEwJVUzET
MBEGA1UECAwKQ2FsaWZvcm5pYTEPMA0GA1UECgwGQXJpc3RhMREwDwYDVQQLDAhT
ZWN1cml0eTEfMB0GA1UEAwwWQXJpc3RhLUNBLUludGVybWVkaWF0ZTCCAiIwDQYJ
KoZIhvcNAQEBBQADggIPADCCAgoCggIBAKejSNWygEOGBpPb7d3SpXciKJOVvHEt
DwSnzoqBim+NIY3HZuXkqluLGtdYlCPOipbzMWr/wrMPqhGXReEDASTvU+aDhPCY
wBxT//eyvuaFQ0M5mZDUyX3GlNTV6hqtXGmpgVCHnjBLNA2oNcHoEmwufkPYPFo5
vTC6TBXmA6s9NAFavMhTO1erlDZ4ULi10+wuarM6mGeB4MB5AYZyGCHdVJCR13Kc
8snBBM7dSTVF8AleP7y5ytskY+wgj+8xzsQ6nzP9aX78AgPvAsdCoo3kQh4LD+Pr
EA5dFDRyAwMLq3mWe9gjirjGu/myXHVZuhlNU4SPvr6wcw311sxaW7TeuODr5yTe
4Ag2CmyWPMBMsFect++6D86OhEIZ/68tp9hb23g/JV+UDkBghPqX8nf1fvYV0M9a
ebGsJIASMGBGmgavZTT7z8NXPSjOj02t68/IdzxyyGs7mnOi5yNa16TDxPWqC1OT
tGvmZ7YfAcpbq+9p0rZDoH9aMfYb9M4x2FiT8GSQlkjNYDxlixFFJ0Btii8qwq4s
9lhz0dTmQfKtY1A7UFbOX62CtpGFkA9i4KqXd7DB0oSkg59d1AzNqARI/rqGbjnN
y0EI2wBTfLxJ9Avc2U9463oP7y7ooaZZjSmc2coEgvo2jX7tz+3z6qgEdie6FL4O
q+QFcJ1l6jrPAgMBAAGjdjB0MB0GA1UdDgQWBBSSTkQTlOPSgmpopZHv6P/wVyAA
cTAfBgNVHSMEGDAWgBR18JvCP02y4ssiqL6Yb92WyHsOmTAPBgNVHRMBAf8EBTAD
AQH/MCEGA1UdEQQaMBiBFnJvaGl0LnNpbmdoQGFyaXN0YS5jb20wDQYJKoZIhvcN
AQELBQADggIBAJ8IkLlgEnpQ1Kfi92/skHYUjG5L2z5ODDF8vgEKJuou7Ci55cin
waB88HoSLpJ+gZvYSlufthP61paEDmnptAWAyI17zTCxB652Fu+24H9WxeHnDFFY
EI1IWhwe39h/iis0Z6qGyABkSDGTpwGB6soZlYhKYUt8FA2YgCvwg3PS7QTP50Sp
BLX5nJG1flEdSrrFcdU4ygW40q7A97p/2bLRJJShOADgMs6LY7L2N2Fln6WunJm/
5JESQYCi8GlEb/PQKGFdEuvA+PGLOU7PQw1l/AfmYTXVhi1xUbXi+moGVyVLWhWi
2vG1i2VP8Uzh4Eu1Bb7CO7ikbF+dkMcYe6FJU46ELtErrzWwFccNcv4PmbH8h7CH
/gIhNlIhodqGTVfEPKBmGlxSm7Tn57pW6cgN8trU8b3rsfmYU41cye1z56N+X/Bc
2e2mid1Cg4Hk+OVhXSLQesxWVc1SiuCcwqdo00Fm2eL5bcirq9WYHx9lIubbBiEb
/ISnEY61OPtLVYGpc0f9Ayt1DjYvFUx1PY8hneGIyHe3pM5bH6cwVCfciGPBQjiF
/IDeS/0Ts0oV84yxYEjhJngSfMaG/vP8PA3EjFjXKSQ7IupyE1rVjavYRWoWQZ++
hDRcKyZMp5smt+U+H5WlBB0AyiJiJiQoExuttEaTmOrBRm0aEnYyaZPT
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
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
-----END CERTIFICATE-----'''
chained_certificate_payload = {"certData": certData, "keyData": keyData, "extraData": extraData}
invalid_certificate_payload={"certData": "-----BEGIN CERTIFICATE----\n-----END CERTIFICATE-----\n", "keyData": "-----BEGIN PRIVATE KEY-----\n-----END PRIVATE KEY-----\n"}
username = overrides.get("Login_username", default="admin")
password = overrides.get("Login_password", default="passwd")
@pytest.mark.administration_tests
class AdministrationTests(NGFWTestCase):
    not_an_app = True

    @staticmethod
    def module_name():
        return "administration-tests"

    @staticmethod
    def appName():
        return "administration-tests"

    def test_010_client_is_online(self):
        result = remote_control.is_online()
        assert (result == 0)

    # Tests Certificate information in Config > Administration > Certificates
    def test_020_certificate_authority_info(self):
        cert_manager = global_functions.uvmContext.certificateManager()
        root_cert_info = cert_manager.getRootCertificateInformation()

        # Checks for CN=[Arista Site], O=Arista, and L=Santa Clara
        cert_subjects = root_cert_info["certSubject"].split(", ")
        cn_found, o_found, l_found = False, False, False
        skip_str = "Untangle certs allowed on old versions. Skipping test."
        for subject in cert_subjects:
            if "CN=" in subject:
                cn_found = True
                assert(subject in ["CN=www.untangle.com","CN=edge.arista.com"])
            if "O=" in subject:
                o_found = True
                assert(subject in ["O=Untangle","O=Arista","O=Security"])
            if "L=" in subject:
                l_found = True
                assert(subject in ["L=Sunnyvale","L=Santa Clara"])
        assert(cn_found and o_found and l_found)


    #Test to validate import certificate or key with trailing spaces and new line and Upload Cerificate 
    def test_021_validate_import_server_certificate(self):

        certificates_dir = '/usr/share/untangle/settings/untangle-certificates'
        initial_certificates_list = global_functions.uvmContext.certificateManager().getServerCertificateList()
        isDir = os.path.exists(certificates_dir)
        isFile = os.path.exists(f"{certificates_dir}/apache.pem")
        if not isDir and not isFile:
            pytest.skip('%s certificate directory or certificate not present' % self.appName())
        url = global_functions.get_http_url()
        headers = {'accept': 'application/json',}
        output_file_path = '/tmp/uploadcertificate.pem'

        with open(f"{certificates_dir}/apache.pem", 'r') as input_file:
            lines = input_file.readlines()

        # Add trailing spaces to each line
        lines_with_spaces = [line.rstrip() + ' ' + '\n' for line in lines]

        with open(output_file_path, 'w') as output_file:
            output_file.writelines(lines_with_spaces)

        files = {
            'type': (None, 'certificate_upload'),
            'argument': (None, 'upload_server'),
            'filename': ('uploadcert.pem', open(f"{output_file_path}", 'rb') , 'application/x-x509-ca-cert')
        }
        rpc_url = f"{url}/admin/upload"
        s = requests.Session()
        # Log in
        response = s.post(
            f"{url}/auth/login?url=/admin&realm=Administrator",
            data=f"fragment=&username={username}&password={password}",
            verify=False
        )
        # Upload pem file containing cert and key files
        response = s.post(
            f"{rpc_url}",
            headers=headers,
            files=files

        )
        certificate_upload_response = json.loads(response.text)
        files_list = []
        cert_upload_json = json.loads(certificate_upload_response.get('msg', None))
        if (cert_upload_json.get("certData", None)):
            uploaded_response = global_functions.uvmContext.certificateManager().uploadCertificate("SERVER", cert_upload_json.get("certData"), cert_upload_json.get("keyData"), "" )
            assert "Certificate successfully uploaded" in uploaded_response.get("output", None)

            for file in glob(join(certificates_dir,f'*.pem')):
                files_list.append((getctime(file), file))
            files_list = [file for _, file in sorted(files_list, reverse=True)]
        # Need to delete uploaded certificate, compare certificates by creation time
        # Use removeCertificate api to delete the certificate
        if len(files_list) > 1:
            uploaded_file_path = files_list[0]
            global_functions.uvmContext.certificateManager().removeCertificate("SERVER",os.path.basename(uploaded_file_path))
        final_certificates_list = global_functions.uvmContext.certificateManager().getServerCertificateList()
        #certificates list should be unchanged after test execution
        assert(len(initial_certificates_list['list']) == len(final_certificates_list['list']))
        os.remove(output_file_path)

    #Test to validate import certificate or key with space and DOS style CRLF endings, new line and Upload Cerificate 
    def test_022_validate_import_server_certificate(self):

        certificates_dir = '/usr/share/untangle/settings/untangle-certificates'
        initial_certificates_list = global_functions.uvmContext.certificateManager().getServerCertificateList()
        isDir = os.path.exists(certificates_dir)
        isFile = os.path.exists(f"{certificates_dir}/apache.pem")
        if not isDir and not isFile:
            pytest.skip('%s certificate directory or certificate not present' % self.appName())
        url = global_functions.get_http_url()
        headers = {'accept': 'application/json',}
        output_file_path = '/tmp/uploadcertificate.pem'

        with open(f"{certificates_dir}/apache.pem", 'r') as input_file:
            content = input_file.read()

        # Convert LF to add trailing spaces and CRLF line endings
        modified_content = content.replace('\n', ' \r\n')
        #modified_content = content.replace('\n', '\r\n')
        with open(f"{output_file_path}", 'w') as output_file:
            output_file.write(modified_content)

        files = {
            'type': (None, 'certificate_upload'),
            'argument': (None, 'upload_server'),
            'filename': ('uploadcert.pem', open(f"{output_file_path}", 'rb') , 'application/x-x509-ca-cert')
        }
        rpc_url = f"{url}/admin/upload"
        s = requests.Session()
        # Log in
        response = s.post(
            f"{url}/auth/login?url=/admin&realm=Administrator",
            data=f"fragment=&username={username}&password={password}",
            verify=False
        )
        # Upload pem file containing cert and key files
        response = s.post(
            f"{rpc_url}",
            headers=headers,
            files=files

        )

        certificate_upload_response = json.loads(response.text)
        files_list = []
        cert_upload_json = json.loads(certificate_upload_response.get('msg', None))
        if (cert_upload_json.get("certData", None)):
            uploaded_response = global_functions.uvmContext.certificateManager().uploadCertificate("SERVER", cert_upload_json.get("certData"), cert_upload_json.get("keyData"), "" )
            assert "Certificate successfully uploaded" in uploaded_response.get("output", None)

            for file in glob(join(certificates_dir,f'*.pem')):
                files_list.append((getctime(file), file))
            files_list = [file for _, file in sorted(files_list, reverse=True)]
        # Need to delete uploaded certificate, compare certificates by creation time
        # Use removeCertificate api to delete the certificate
        if len(files_list) > 1:
            uploaded_file_path = files_list[0]
            global_functions.uvmContext.certificateManager().removeCertificate("SERVER",os.path.basename(uploaded_file_path))
        final_certificates_list = global_functions.uvmContext.certificateManager().getServerCertificateList()
        #  certificates list should be unchanged after test execution
        assert(len(initial_certificates_list['list']) == len(final_certificates_list['list']))
        os.remove(output_file_path)

    #Test to validate import certificate or key and DOS style CRLF endings, new line and Upload Cerificate 
    def test_023_validate_import_server_certificate(self):

        certificates_dir = '/usr/share/untangle/settings/untangle-certificates'
        initial_certificates_list = global_functions.uvmContext.certificateManager().getServerCertificateList()
        isDir = os.path.exists(certificates_dir)
        isFile = os.path.exists(f"{certificates_dir}/apache.pem")
        if not isDir and not isFile:
            pytest.skip('%s certificate directory or certificate not present' % self.appNameWF())
        url = global_functions.get_http_url()
        headers = {'accept': 'application/json',}
        output_file_path = '/tmp/uploadcertificate.pem'

        with open(f"{certificates_dir}/apache.pem", 'r') as input_file:
            content = input_file.read()

        # Convert LF to add CRLF line endings
        modified_content = content.replace('\n', '\r\n')
        with open(f"{output_file_path}", 'w') as output_file:
            output_file.write(modified_content)

        files = {
            'type': (None, 'certificate_upload'),
            'argument': (None, 'upload_server'),
            'filename': ('uploadcert.pem', open(f"{output_file_path}", 'rb') , 'application/x-x509-ca-cert')
        }
        rpc_url = f"{url}/admin/upload"
        s = requests.Session()
        # Log in
        response = s.post(
            f"{url}/auth/login?url=/admin&realm=Administrator",
            data=f"fragment=&username={username}&password={password}",
            verify=False
        )
        # Upload pem file containing cert and key files
        response = s.post(
            f"{rpc_url}",
            headers=headers,
            files=files

        )

        certificate_upload_response = json.loads(response.text)
        files_list = []
        cert_upload_json = json.loads(certificate_upload_response.get('msg', None))
        if (cert_upload_json.get("certData", None)):
            uploaded_response = global_functions.uvmContext.certificateManager().uploadCertificate("SERVER", cert_upload_json.get("certData"), cert_upload_json.get("keyData"), "" )
            assert "Certificate successfully uploaded" in uploaded_response.get("output", None)

            for file in glob(join(certificates_dir,f'*.pem')):
                files_list.append((getctime(file), file))
            files_list = [file for _, file in sorted(files_list, reverse=True)]
        # Need to delete uploaded certificate, compare certificates by creation time
        # Use removeCertificate api to delete the certificate
        if len(files_list) > 1:
            uploaded_file_path = files_list[0]
            global_functions.uvmContext.certificateManager().removeCertificate("SERVER",os.path.basename(uploaded_file_path))
        final_certificates_list = global_functions.uvmContext.certificateManager().getServerCertificateList()
        #  certificates list should be unchanged after test execution
        assert(len(initial_certificates_list['list']) == len(final_certificates_list['list']))
        os.remove(output_file_path)

    #Test to validate invalid json upload uploadCerificate API
    def test_024_validate_upload_certificate_api(self):
        # This test validates certificate upload to uploadCerificate API
        uploaded_response = global_functions.uvmContext.certificateManager().uploadCertificate("SERVER", invalid_certificate_payload.get("certData"), invalid_certificate_payload.get("keyData"), "" )
        assert "The certificate is not valid" in uploaded_response.get("output", None)


    #Test to validate import invalid certificate or key file
    def test_025_validate_import_invalid_server_certificate(self):
        # This test validates certificate from Text Area in the UI

        certificates_dir = '/usr/share/untangle/settings/untangle-certificates'
        isDir = os.path.exists(certificates_dir)
        isFile = os.path.exists(f"{certificates_dir}/apache.pfx")
        if not isDir and not isFile:
            pytest.skip('%s certificate directory or certificate not present' % self.appNameWF())
        url = global_functions.get_http_url()
        headers = {'accept': 'application/json',}
        files = {
            'type': (None, 'certificate_upload'),
            'argument': (None, 'upload_server'),
            'filename': ('uploadcert.pem', open(f"{certificates_dir}/apache.pfx", 'rb') , 'application/x-x509-ca-cert')
        }
        rpc_url = f"{url}/admin/upload"
        s = requests.Session()
        # Log in
        response = s.post(
            f"{url}/auth/login?url=/admin&realm=Administrator",
            data=f"fragment=&username={username}&password={password}",
            verify=False
        )
        # Upload pem file containing cert and key files
        response = s.post(
            f"{rpc_url}",
            headers=headers,
            files=files

        )
        certificate_upload_response = json.loads(response.text)
        #for invalid certificated files should get following error
        assert "The file does not contain any valid certificates or keys" in certificate_upload_response.get('msg', None)

    #Test to validate chained certificate json upload uploadCerificate API
    def test_025_validate_upload_certificate_api(self):
        certificates_dir = '/usr/share/untangle/settings/untangle-certificates'
        initial_certificates_list = global_functions.uvmContext.certificateManager().getServerCertificateList()
        # This test validates certificate upload to uploadCerificate API
        uploaded_response = global_functions.uvmContext.certificateManager().uploadCertificate("SERVER", chained_certificate_payload.get("certData"), chained_certificate_payload.get("keyData"), chained_certificate_payload.get("extraData"))
        assert "Certificate successfully uploaded" in uploaded_response.get("output", None)
        # The chained certificate is uploaded successfully, delete uploaded certificicate
        files_list = []
        for file in glob(join(certificates_dir,f'*.pem')):
                files_list.append((getctime(file), file))
        files_list = [file for _, file in sorted(files_list, reverse=True)]
        if len(files_list) > 1:
            uploaded_file_path = files_list[0]
            global_functions.uvmContext.certificateManager().removeCertificate("SERVER",os.path.basename(uploaded_file_path))
        final_certificates_list = global_functions.uvmContext.certificateManager().getServerCertificateList()
        #  certificates list should be unchanged after test execution
        assert(len(initial_certificates_list['list']) == len(final_certificates_list['list']))

test_registry.register_module("administration-tests", AdministrationTests)
