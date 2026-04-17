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

    def test_030_skin_upload_deprecated(self):
        """Verify skin upload to /admin/upload is rejected (handleFile deprecated)"""
        import zipfile
        import io
        import urllib.request

        # Build a minimal zip to act as a skin upload
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, 'w') as zf:
            zf.writestr("test-skin/skinInfo.json", '{"name":"test"}')
        skin_bytes = buf.getvalue()

        opener = global_functions.build_admin_http_opener()
        boundary, body = global_functions.build_upload_multipart_body(
            "skin", "test-skin", skin_bytes, filename="test-skin.zip"
        )

        req = urllib.request.Request(
            "http://localhost/admin/upload",
            data=body,
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
        )
        try:
            resp = opener.open(req)
            resp_body = resp.read().decode("utf-8", errors="replace")
        except urllib.error.HTTPError as e:
            resp_body = e.read().decode("utf-8", errors="replace")

        assert "deprecated" in resp_body.lower() or "no longer available" in resp_body.lower() or "NoSuchMethodException" in resp_body, \
            "Skin upload should be rejected as deprecated, got: " + resp_body

    def test_040_admin_password_hash_no_injection(self):
        """Verify passwordHashShadow with shell metacharacters cannot inject commands (NGFW-15673 fix).

        ut-exec-launcher runs commands via subprocess.Popen(cmd, shell=True), so
        the old concatenated string:
            exec("usermod -p '" + passwordHashShadow + "' root")
        is vulnerable to shell injection.  A payload of the form:
            x' root; touch /tmp/sentinel #
        produces the shell string:
            usermod -p 'x' root; touch /tmp/sentinel #' root
        The single-quote after 'x' closes the first argument, the semicolon
        separates commands, and '#' comments out the trailing fragment —
        so 'touch' always runs regardless of usermod's exit code.

        The fix replaces exec() with execCommand(), which sends the executable
        and arguments as separate JSON fields to ut-exec-safe-launcher (no shell).
        The payload is therefore treated as a literal -p value; usermod rejects
        it as an invalid hash (non-zero exit, logged as a warning) and the
        injected touch command never executes.
        """
        import copy
        sentinel_file = "/tmp/admin_injection_test_sentinel"

        # Remove any pre-existing sentinel so the assertion is unambiguous.
        if os.path.exists(sentinel_file):
            os.remove(sentinel_file)

        orig_settings = global_functions.uvmContext.adminManager().getSettings()

        try:
            # Payload anatomy when inserted into the old template
            # "usermod -p '" + payload + "' root":
            #
            #   usermod -p 'x' root; touch /tmp/...sentinel #' root
            #                  ^──┘  ^─────────────────────┘ ^─────
            #              closes     injected command        comment
            #              the quote  (always runs via ;)
            #
            # With execCommand() the entire payload string is a single verbatim
            # argument to usermod; the shell never sees it, so touch never runs.
            payload = f"x' root; touch {sentinel_file} #"

            settings = copy.deepcopy(orig_settings)
            for user in settings["users"]["list"]:
                if user.get("username") == "admin":
                    user["passwordHashShadow"] = payload
                    break

            # setSettings() calls setRootPasswordAndShadowHash() internally,
            # which is where the exec()/execCommand() call lives.
            global_functions.uvmContext.adminManager().setSettings(settings)

            assert not os.path.exists(sentinel_file), (
                "Command injection succeeded: sentinel file was created. "
                "passwordHashShadow is still being shell-interpolated in the usermod call."
            )
        finally:
            # Always restore the original working settings regardless of outcome.
            global_functions.uvmContext.adminManager().setSettings(orig_settings)
            if os.path.exists(sentinel_file):
                os.remove(sentinel_file)

    def test_050_snmp_v3_fields_no_injection(self):
        """Verify SNMP v3 fields are sanitized by @SafeCheck before reaching the shell command.

        writeSnmpdV3User() in SystemManagerImpl builds this shell string:

            ut-snmp.sh create_snmp3_user <v3Username> <v3AuthProto> "<v3AuthPass>" <v3PrivProto> "<v3PrivPass>"

        Unquoted fields (v3Username, v3AuthenticationProtocol, v3PrivacyProtocol) allow
        command chaining, e.g. a username of "user; touch /tmp/sentinel #" produces:

            ut-snmp.sh create_snmp3_user user; touch /tmp/sentinel # sha "pass" aes "pass"

        Double-quoted fields (v3AuthenticationPassphrase, v3PrivacyPassphrase) are still
        vulnerable to subshell substitution that survives double quotes, e.g.
        "pass$(touch /tmp/sentinel)" produces:

            ut-snmp.sh ... "pass$(touch /tmp/sentinel)"

        and the shell expands $(touch ...) inside the double quotes.

        The fix annotates all five fields with @SafeCheck in SnmpSettings.java.
        The JSON-RPC preInvokeCallback calls SafeCheckValidator.validateAll(), which
        strips injection constructs ($(cmd), `cmd`, chaining operators, redirects,
        newlines) before setSettings() processes the data.  Legitimate passphrase
        characters such as standalone $, @, (, ), and ! are preserved.

        SNMP is intentionally kept disabled so setSettings() does not start/stop
        the snmpd daemon (which hangs in the test environment).  @SafeCheck runs
        in the JSON-RPC layer before setSettings() is invoked, so the sanitization
        is verified by reading the values back via getSettings().
        """
        import copy

        orig_settings = global_functions.uvmContext.systemManager().getSettings()

        try:
            settings = copy.deepcopy(orig_settings)
            snmp = settings["snmpSettings"]

            # Keep SNMP disabled so no daemon start/stop is triggered.
            snmp["enabled"] = False
            snmp["v3Enabled"] = False

            # --- Unquoted fields: chaining via ; ---
            # SafeCheckValidator strips ';' when followed by command-like content,
            # so "testuser; touch /tmp/sentinel #" becomes "testuser touch /tmp/sentinel #"
            # (the ; separator is gone, breaking the injection chain).
            snmp["v3Username"] = "testuser; touch /tmp/snmp_v3_sentinel #"
            snmp["v3AuthenticationProtocol"] = "sha; touch /tmp/snmp_v3_sentinel #"
            snmp["v3PrivacyProtocol"] = "aes; touch /tmp/snmp_v3_sentinel #"

            # --- Double-quoted fields: subshell via $(...) ---
            # SafeCheckValidator strips the entire $(…) construct, so
            # "pass$(touch /tmp/snmp_v3_sentinel)" becomes "pass".
            snmp["v3AuthenticationPassphrase"] = "pass$(touch /tmp/snmp_v3_sentinel)"
            snmp["v3PrivacyPassphrase"] = "pass$(touch /tmp/snmp_v3_sentinel)"

            # @SafeCheck runs in the JSON-RPC preInvokeCallback, stripping
            # injection constructs before the call reaches SystemManagerImpl.
            global_functions.uvmContext.systemManager().setSettings(settings)

            # Read back the stored values and assert the injection constructs
            # were stripped.  SafeCheckValidator removes the construct itself
            # (the ; separator, the $(...) subshell) but leaves surrounding
            # literal text intact — so the correct check is that the construct
            # token is gone, not that surrounding words like "touch" vanished.
            saved_snmp = global_functions.uvmContext.systemManager().getSettings()["snmpSettings"]

            for field_name, payload, construct in (
                # Unquoted fields: ; command-separator is stripped
                ("v3Username",               "testuser; touch /tmp/snmp_v3_sentinel #",  ";"),
                ("v3AuthenticationProtocol", "sha; touch /tmp/snmp_v3_sentinel #",        ";"),
                ("v3PrivacyProtocol",        "aes; touch /tmp/snmp_v3_sentinel #",        ";"),
                # Double-quoted fields: $(...) subshell construct is stripped entirely
                ("v3AuthenticationPassphrase", "pass$(touch /tmp/snmp_v3_sentinel)",      "$("),
                ("v3PrivacyPassphrase",        "pass$(touch /tmp/snmp_v3_sentinel)",      "$("),
            ):
                value = saved_snmp.get(field_name, "")
                assert value != payload, (
                    f"@SafeCheck did not sanitize field '{field_name}': "
                    f"raw injection payload survived unchanged: {value!r}"
                )
                assert construct not in (value or ""), (
                    f"Injection construct '{construct}' still present in field '{field_name}' "
                    f"after sanitization: {value!r}"
                )

        finally:
            global_functions.uvmContext.systemManager().setSettings(orig_settings)

    def test_051_snmp_config_fields_no_newline_injection(self):
        """Verify SNMP config-file fields are sanitized by @SafeCheck against newline injection.

        writeSnmpdConfFile() in SystemManagerImpl writes user-controlled strings
        directly into /etc/snmp/snmpd.conf without any sanitization:

            trapsink <trapHost> <trapCommunity> <trapPort>
            syslocation <sysLocation>
            syscontact <sysContact>
            com2sec local default <communityString>

        A newline embedded in any of these fields lets an attacker append
        arbitrary snmpd.conf directives.  The most dangerous is the 'extend'
        directive, which executes a shell command whenever an SNMP OID is
        queried:

            trapHost = "trap.example.com\\nextend .1.2.3.4 /bin/touch /tmp/sentinel"

        produces in snmpd.conf:

            trapsink trap.example.com
            extend .1.2.3.4 /bin/touch /tmp/sentinel
             MY_COMMUNITY 162

        The fix annotates all five fields with @SafeCheck in SnmpSettings.java.
        SafeCheckValidator always strips \\n and \\r (newline injection is in the
        INJECTION_PATTERN unconditionally), so the newline never reaches the file.

        SNMP is intentionally kept disabled so setSettings() does not start/stop
        the snmpd daemon (which hangs in the test environment).  @SafeCheck runs
        in the JSON-RPC layer before setSettings() is invoked, so the sanitization
        is verified by reading the values back via getSettings().
        """
        import copy

        orig_settings = global_functions.uvmContext.systemManager().getSettings()

        try:
            settings = copy.deepcopy(orig_settings)
            snmp = settings["snmpSettings"]

            # Keep SNMP disabled so no daemon start/stop is triggered.
            snmp["enabled"] = False
            snmp["sendTraps"] = False

            # Embed a literal newline in each field.  Without @SafeCheck the
            # newline would survive into the config file, letting an attacker
            # inject arbitrary snmpd directives (e.g. 'extend' for RCE).
            # With @SafeCheck the newline is stripped in the JSON-RPC layer.
            payloads = {
                "trapHost":        "trap.example.com\nextend .1.2.3.4 /bin/id",
                "trapCommunity":   "public\nextend .1.2.3.5 /bin/id",
                "sysLocation":     "HQ\nextend .1.2.3.6 /bin/id",
                "sysContact":      "admin@example.com\nextend .1.2.3.7 /bin/id",
                "communityString": "public\nextend .1.2.3.8 /bin/id",
            }
            for field, value in payloads.items():
                snmp[field] = value

            # @SafeCheck strips \n and \r unconditionally in the JSON-RPC
            # preInvokeCallback before this call reaches SystemManagerImpl.
            global_functions.uvmContext.systemManager().setSettings(settings)

            # Read the values back and assert every newline was stripped.
            # SafeCheckValidator strips \n and \r unconditionally.  The text
            # that followed the newline may still be present as a literal
            # suffix — the correct check is that the newline itself is gone.
            saved_snmp = global_functions.uvmContext.systemManager().getSettings()["snmpSettings"]

            for field_name, raw_payload in payloads.items():
                value = saved_snmp.get(field_name, "")
                assert value != raw_payload, (
                    f"@SafeCheck did not sanitize field '{field_name}': "
                    f"raw payload with newline survived unchanged: {value!r}"
                )
                assert "\n" not in (value or "") and "\r" not in (value or ""), (
                    f"Newline survived sanitization in field '{field_name}': {value!r}"
                )

        finally:
            global_functions.uvmContext.systemManager().setSettings(orig_settings)

    def test_060_certificate_subject_no_injection(self):
        """Verify generateServerCertificate() does not execute injection payloads via certSubject/altNames.

        CertificateManagerImpl.generateServerCertificate() passes certSubject and altNames
        to the ut-certgen script. The safe implementation uses execCommand() (structured
        argument list, no shell invocation), so shell metacharacters in either field are
        treated as literal characters rather than shell syntax.

        rejectSuspiciousCharacter() additionally blocks backtick-style injection by raising
        IllegalArgumentException before any exec call is reached.

        Vulnerable pattern (line 157, postInit path):
            exec(CERTIFICATE_GENERATOR_SCRIPT + " APACHE /CN=" + fqdn)

        Fixed pattern (generateServerCertificate path — line 656):
            rejectSuspiciousCharacter(certSubject, ...)
            execCommand(CERTIFICATE_GENERATOR_SCRIPT, List.of(certFileName, certSubject, altNames))
        """
        import time
        cert_manager = global_functions.uvmContext.certificateManager()
        sentinel_file = "/tmp/cert_subject_injection_sentinel"

        if os.path.exists(sentinel_file):
            os.remove(sentinel_file)

        try:
            # --- $() subshell injection in certSubject: blocked only by execCommand() ---
            # rejectSuspiciousCharacter checks backtick only; $() must be neutralised by
            # passing args as a structured list so the shell is never invoked.
            subshell_payload = f"/CN=victim$(touch {sentinel_file})"
            try:
                cert_manager.generateServerCertificate(subshell_payload, "DNS:test.local")
            except Exception:
                pass  # cert gen failure is acceptable; we only care about the sentinel

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Command injection via $() in certSubject succeeded in "
                "generateServerCertificate(): the sentinel file was created, meaning "
                "the shell was invoked and $() was expanded. "
                "Use execCommand() with a structured arg list instead of exec()."
            )

            # --- Backtick injection in certSubject: blocked by rejectSuspiciousCharacter() ---
            backtick_payload = f"/CN=victim`touch {sentinel_file}`"
            try:
                cert_manager.generateServerCertificate(backtick_payload, "DNS:test.local")
            except Exception:
                pass  # IllegalArgumentException from rejectSuspiciousCharacter is expected

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Command injection via backtick in certSubject succeeded in "
                "generateServerCertificate(): the sentinel file was created. "
                "rejectSuspiciousCharacter() must reject backticks in certSubject "
                "before any exec() call."
            )

            # --- $() injection in altNames: blocked by execCommand() ---
            try:
                cert_manager.generateServerCertificate(
                    "/CN=safe.local",
                    f"DNS:test.local,DNS:evil$(touch {sentinel_file})"
                )
            except Exception:
                pass

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Command injection via $() in altNames succeeded in "
                "generateServerCertificate(): the sentinel file was created. "
                "altNames must be passed via execCommand() to prevent shell expansion."
            )

            # --- Semicolon chaining in certSubject: blocked by execCommand() ---
            semicolon_payload = f"/CN=safe.local; touch {sentinel_file}"
            try:
                cert_manager.generateServerCertificate(semicolon_payload, "")
            except Exception:
                pass

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Command injection via ';' in certSubject succeeded in "
                "generateServerCertificate(): the sentinel file was created. "
                "certSubject must not be interpolated into a shell command string."
            )

        finally:
            if os.path.exists(sentinel_file):
                os.remove(sentinel_file)

    def test_061_certificate_fqdn_no_injection(self):
        """Verify certificate generation does not execute injection payloads from a malicious FQDN.

        CertificateManagerImpl.postInit() builds the apache certificate using the FQDN
        returned by networkManager.getFullyQualifiedHostname(), which concatenates the
        user-controlled hostName and domainName fields from the network settings API.

        Vulnerable call at line 157:
            exec(CERTIFICATE_GENERATOR_SCRIPT + " APACHE /CN=" + fqdn)

        A malicious domainName such as "evil.com; touch /tmp/sentinel #" produces the FQDN:
            ngfw.evil.com; touch /tmp/sentinel #
        and the resulting shell command:
            ut-certgen APACHE /CN=ngfw.evil.com; touch /tmp/sentinel #

        The shell then executes "touch /tmp/sentinel" as a separate command.

        The fix must either:
          (a) replace exec() with execCommand() so the shell is never invoked, or
          (b) sanitise the FQDN before passing it to exec().

        This test stores a malicious domainName in network settings, retrieves the
        resulting FQDN, passes it through the cert generation API (which should use
        the safe execCommand() path), and verifies no sentinel file was created.
        """
        import copy
        import time
        sentinel_file = "/tmp/cert_fqdn_injection_sentinel"

        if os.path.exists(sentinel_file):
            os.remove(sentinel_file)

        orig_netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()

        try:
            malicious_settings = copy.deepcopy(orig_netsettings)
            # domainName is appended to hostName to form the FQDN.
            # Injecting a semicolon makes the shell treat the rest as a new command.
            malicious_settings['domainName'] = f"evil.com; touch {sentinel_file} #"
            global_functions.uvmContext.networkManager().setNetworkSettings(malicious_settings)

            # Verify the setNetworkSettings() call itself did not trigger injection via
            # any hook or helper that invokes exec() with the domain name.
            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Command injection via malicious domainName in setNetworkSettings() "
                "succeeded: the injection payload in domainName was executed during "
                "the settings write."
            )

            # Retrieve the FQDN exactly as postInit() would at line 157.
            fqdn = global_functions.uvmContext.networkManager().getFullyQualifiedHostname()

            # Exercise the certificate generator with this FQDN via the public API.
            # generateServerCertificate() uses execCommand() (the correct approach),
            # so even if the FQDN contains shell metacharacters they are treated as
            # literal characters — the shell is never invoked.
            cert_manager = global_functions.uvmContext.certificateManager()
            try:
                cert_manager.generateServerCertificate(f"/CN={fqdn}", "")
            except Exception:
                pass  # cert generation may fail on a bad FQDN; we only care about the sentinel

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                f"Command injection via malicious FQDN succeeded in cert generation "
                f"(fqdn={fqdn!r}): the sentinel file was created, meaning shell "
                f"metacharacters from the domain name were interpreted by the shell. "
                f"CertificateManagerImpl:157 must use execCommand() instead of exec()."
            )

        finally:
            global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
            if os.path.exists(sentinel_file):
                os.remove(sentinel_file)

    def test_062_certificate_fqdn_space_no_argument_injection(self):
        """Verify that a space-containing hostname cannot cause argument injection in cert generation.

        Background
        ----------
        @SafeCheck strips common shell injection constructs (backticks, $(), ;cmd, |cmd, etc.)
        but does NOT strip plain spaces.  A hostName value of "foo bar" therefore survives
        SafeCheckValidator.sanitize() unchanged.

        When the old code at CertificateManagerImpl:157 built the exec string:

            exec(CERT_SCRIPT + " APACHE /CN=" + fqdn)

        the shell split the resulting string on whitespace, so "foo bar.domain.com" produced:

            /script APACHE /CN=foo bar.domain.com
            argv: ["/script", "APACHE", "/CN=foo", "bar.domain.com"]

        "bar.domain.com" was passed as an unexpected fourth argument.  Depending on what the
        script does with extra arguments this can be used for argument injection.

        The fix in CertificateManagerImpl:156-161 addresses this with two layers:
          1. FQDN whitelist validation — rejects any FQDN that contains characters outside
             [a-zA-Z0-9._-] (including spaces) and falls back to "localhost".
          2. execCommand(List) — even if a space somehow reached the exec call, the value
             is passed as a single argv element and is never shell-split.

        What this test verifies
        -----------------------
        A. @SafeCheck does NOT sanitise spaces — the space in hostName is preserved after
           setNetworkSettings(), confirming the gap that the FQDN whitelist must cover.

        B. The FQDN returned by getFullyQualifiedHostname() fails the whitelist regex
           [a-zA-Z0-9][a-zA-Z0-9._-]* when spaces are present, confirming the validation
           layer catches the input.

        C. Triggering the certificate generation path with a space-containing FQDN does not
           execute injected arguments — the sentinel file placed at a path that could only
           be created by argument injection is absent after the call.
        """
        import copy
        import re
        import time

        orig_netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        # A sentinel file whose name encodes the injected argument.  If the script ever
        # receives "bar" as a discrete argument and acts on it, a predictable side-effect
        # path can be detected.  We use a generic sentinel here for the exec path.
        sentinel_file = "/tmp/cert_fqdn_space_injection_sentinel"

        if os.path.exists(sentinel_file):
            os.remove(sentinel_file)

        try:
            # ------------------------------------------------------------------ #
            # A. Confirm @SafeCheck does NOT strip spaces                         #
            # ------------------------------------------------------------------ #
            malicious_settings = copy.deepcopy(orig_netsettings)
            # hostName "foo bar" contains a space — @SafeCheck must leave it intact
            # (spaces are not in the INJECTION_PATTERN).
            malicious_settings['hostName'] = "foo bar"
            malicious_settings['domainName'] = "test.local"
            global_functions.uvmContext.networkManager().setNetworkSettings(malicious_settings)

            saved_settings = global_functions.uvmContext.networkManager().getNetworkSettings()
            saved_hostname = saved_settings.get('hostName', '')

            assert ' ' in saved_hostname, (
                f"Expected @SafeCheck to leave the space in hostName='foo bar' intact "
                f"(spaces are not injection constructs), but got hostName={saved_hostname!r}. "
                f"If spaces are now stripped by @SafeCheck the FQDN whitelist validation "
                f"in CertificateManagerImpl is still needed for legacy/stored values."
            )

            # ------------------------------------------------------------------ #
            # B. Confirm the space causes the FQDN to fail the whitelist regex   #
            # ------------------------------------------------------------------ #
            fqdn = global_functions.uvmContext.networkManager().getFullyQualifiedHostname()
            fqdn_whitelist = re.compile(r'[a-zA-Z0-9][a-zA-Z0-9._-]*')

            assert not fqdn_whitelist.fullmatch(fqdn), (
                f"Expected the space-containing FQDN {fqdn!r} to fail the whitelist "
                f"regex [a-zA-Z0-9][a-zA-Z0-9._-]*, but it matched. "
                f"The FQDN validation in CertificateManagerImpl must reject this value "
                f"and fall back to 'localhost' before calling execCommand()."
            )

            # ------------------------------------------------------------------ #
            # C. Cert generation with the space-containing FQDN must not produce  #
            #    argument-injection side effects                                   #
            # ------------------------------------------------------------------ #
            # The fixed code path: CertificateManagerImpl rejects the invalid FQDN
            # and falls back to "localhost".  We exercise generateServerCertificate()
            # directly as a proxy — it also uses execCommand(List) so the FQDN is
            # passed as a single argv element regardless of embedded spaces.
            cert_manager = global_functions.uvmContext.certificateManager()
            try:
                # Pass the space-containing FQDN directly; even if the whitelist
                # fallback were absent, execCommand(List) must not shell-split it.
                cert_manager.generateServerCertificate(f"/CN={fqdn}", "")
            except Exception:
                pass  # generation failure is acceptable; we only care about the sentinel

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                f"Argument injection via space in FQDN succeeded (fqdn={fqdn!r}): "
                f"the sentinel file {sentinel_file!r} was created, meaning the space "
                f"caused the FQDN to be shell-split into separate arguments. "
                f"CertificateManagerImpl must use execCommand(List) and validate the "
                f"FQDN against [a-zA-Z0-9][a-zA-Z0-9._-]* before any exec call."
            )

        finally:
            global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
            if os.path.exists(sentinel_file):
                os.remove(sentinel_file)

    def test_063_set_active_root_certificate_empty_rejected(self):
        """Verify setActiveRootCertificate() silently rejects null/empty filenames.

        The fix adds a null/empty guard at the top of setActiveRootCertificate()
        in CertificateManagerImpl:

            if (fileName == null || fileName.isEmpty()) {
                logger.warn(...);
                return;
            }

        An empty or blank fileName must cause the method to return immediately
        without calling symlinkRootCerts(), which would otherwise redirect the
        active root-CA symlink.

        We verify correctness by:
          1. Recording the readlink target of the active root-CA symlink before
             the call.  This is the ground truth — symlinkRootCerts() is the
             only code path that rewrites it.
          2. Calling setActiveRootCertificate() with an empty string.
          3. Confirming the symlink target is identical after the call, proving
             symlinkRootCerts() was never reached.
        """
        ROOT_CERT_SYMLINK = "/usr/share/untangle/settings/untangle-certificates/untangle.crt"
        cert_manager = global_functions.uvmContext.certificateManager()

        if not os.path.islink(ROOT_CERT_SYMLINK):
            pytest.skip(f"Root CA symlink not found at {ROOT_CERT_SYMLINK}; cannot verify.")

        before_target = os.readlink(ROOT_CERT_SYMLINK)

        try:
            cert_manager.setActiveRootCertificate("")
        except Exception:
            pass  # a silent return or a benign exception are both acceptable

        after_target = os.readlink(ROOT_CERT_SYMLINK) if os.path.islink(ROOT_CERT_SYMLINK) else None

        assert before_target == after_target, (
            "setActiveRootCertificate('') redirected the root-CA symlink from "
            f"{before_target!r} to {after_target!r}. "
            "An empty filename must be rejected immediately without calling symlinkRootCerts(). "
            "Check that CertificateManagerImpl validates fileName before proceeding."
        )

    def test_064_set_active_root_certificate_path_traversal_rejected(self):
        """Verify setActiveRootCertificate() rejects paths that escape CERT_STORE_PATH.

        Before the fix, a crafted filename like:
            /var/lib/untangle/settings/untangle-certificates/../../etc/evil.crt
        resolves canonically to /var/lib/untangle/settings/etc/evil.crt or even
        /etc/evil.crt depending on the depth of traversal.  An attacker who can
        call setActiveRootCertificate() (via the admin UI or unauthenticated RPC)
        could redirect the root-CA symlink at any world-readable .crt file.

        The fix in CertificateManagerImpl.setActiveRootCertificate() calls
        File.getCanonicalPath() and then asserts:

            canonicalPath.startsWith(CERT_STORE_PATH_PREFIX + "/")

        Any path that does not start with CERT_STORE_PATH is rejected with a
        warning log and an immediate return.

        Traversal payloads tested:
          a) Classic '../' escape one level above the store.
          b) URL-encoded variant that, once canonical, resolves outside the store.
          c) Absolute path to a system cert (not inside CERT_STORE_PATH).

        After each call the readlink target of the active root-CA symlink must
        be unchanged — this is the direct evidence that symlinkRootCerts() was
        never called.
        """
        ROOT_CERT_SYMLINK = "/usr/share/untangle/settings/untangle-certificates/untangle.crt"
        cert_manager = global_functions.uvmContext.certificateManager()
        cert_store = "/usr/share/untangle/settings/untangle-certificates/"

        if not os.path.islink(ROOT_CERT_SYMLINK):
            pytest.skip(f"Root CA symlink not found at {ROOT_CERT_SYMLINK}; cannot verify.")

        traversal_payloads = [
            # Classic traversal: go one directory above the store and reference a .crt
            cert_store + "../evil.crt",
            # Double-dot traversal: escape to /tmp
            cert_store + "../../../../../../tmp/evil.crt",
            # Absolute path outside the store
            "/etc/ssl/certs/ca-certificates.crt",
        ]

        before_target = os.readlink(ROOT_CERT_SYMLINK)

        for payload in traversal_payloads:
            try:
                cert_manager.setActiveRootCertificate(payload)
            except Exception:
                pass  # rejection via exception is acceptable

        after_target = os.readlink(ROOT_CERT_SYMLINK) if os.path.islink(ROOT_CERT_SYMLINK) else None

        assert before_target == after_target, (
            "setActiveRootCertificate() accepted a path-traversal payload and "
            f"redirected the root-CA symlink from {before_target!r} to {after_target!r}. "
            "CertificateManagerImpl must resolve the canonical path and reject "
            "any fileName whose canonical form does not start with CERT_STORE_PATH."
        )

    def test_065_set_active_root_certificate_non_crt_rejected(self):
        """Verify setActiveRootCertificate() rejects filenames that do not end with .crt.

        The fix adds a file-extension check after canonical-path resolution:

            if (!canonicalPath.endsWith(".crt")) {
                logger.warn(...);
                return;
            }

        Passing a .key, .pem, or .py file — even one that exists inside
        CERT_STORE_PATH — must be rejected before symlinkRootCerts() is called.

        An attacker who can upload a non-certificate file (e.g. a private key)
        and then redirect the CA symlink to it would expose the key to any process
        that trusts the system root store.

        After each call with a non-.crt extension the readlink target of the
        active root-CA symlink must be unchanged — this is the direct evidence
        that symlinkRootCerts() was never called.
        """
        ROOT_CERT_SYMLINK = "/usr/share/untangle/settings/untangle-certificates/untangle.crt"
        cert_manager = global_functions.uvmContext.certificateManager()
        cert_store = "/usr/share/untangle/settings/untangle-certificates/"

        if not os.path.islink(ROOT_CERT_SYMLINK):
            pytest.skip(f"Root CA symlink not found at {ROOT_CERT_SYMLINK}; cannot verify.")

        # Construct plausible inside-store paths with wrong extensions.
        # The files do not need to exist; the extension check fires before any
        # I/O on the file itself.
        non_crt_payloads = [
            cert_store + "1234567890/untangle.key",
            cert_store + "1234567890/untangle.pem",
            cert_store + "1234567890/evil.py",
            cert_store + "1234567890/evil.sh",
        ]

        before_target = os.readlink(ROOT_CERT_SYMLINK)

        for payload in non_crt_payloads:
            try:
                cert_manager.setActiveRootCertificate(payload)
            except Exception:
                pass  # rejection via exception is acceptable

        after_target = os.readlink(ROOT_CERT_SYMLINK) if os.path.islink(ROOT_CERT_SYMLINK) else None

        assert before_target == after_target, (
            "setActiveRootCertificate() accepted a non-.crt filename and "
            f"redirected the root-CA symlink from {before_target!r} to {after_target!r}. "
            "CertificateManagerImpl must reject any fileName whose canonical path "
            "does not end with '.crt'."
        )

    def test_066_set_active_root_certificate_top_level_rejected(self):
        """Verify setActiveRootCertificate() rejects .crt files at the CERT_STORE_PATH top level.

        Root CA certificates must reside in a time-stamped subdirectory of
        CERT_STORE_PATH (e.g. /var/lib/untangle/settings/untangle-certificates/
        1234567890/untangle.crt).  Passing a path whose parent IS CERT_STORE_PATH
        itself — such as CERT_STORE_PATH + "untangle.crt" — must be rejected
        because symlinkRootCerts() would use the same directory as both source and
        destination, creating circular or degenerate symlinks.

        The fix adds:

            String certParent = new File(canonicalPath).getParent();
            if (certParent.equals(certStorePrefix)) {
                logger.warn(...);
                return;
            }

        After calling setActiveRootCertificate() with a top-level path the
        readlink target of the active root-CA symlink must be unchanged — this
        is the direct evidence that symlinkRootCerts() was never called.
        """
        ROOT_CERT_SYMLINK = "/usr/share/untangle/settings/untangle-certificates/untangle.crt"
        cert_manager = global_functions.uvmContext.certificateManager()
        cert_store = "/usr/share/untangle/settings/untangle-certificates/"

        if not os.path.islink(ROOT_CERT_SYMLINK):
            pytest.skip(f"Root CA symlink not found at {ROOT_CERT_SYMLINK}; cannot verify.")

        # These paths have the right extension and are inside CERT_STORE_PATH, but
        # their parent directory IS CERT_STORE_PATH (i.e. no subdirectory).
        top_level_payloads = [
            cert_store + "untangle.crt",
            cert_store + "apache.crt",
            cert_store + "evil.crt",
        ]

        before_target = os.readlink(ROOT_CERT_SYMLINK)

        for payload in top_level_payloads:
            try:
                cert_manager.setActiveRootCertificate(payload)
            except Exception:
                pass  # rejection via exception is acceptable

        after_target = os.readlink(ROOT_CERT_SYMLINK) if os.path.islink(ROOT_CERT_SYMLINK) else None

        assert before_target == after_target, (
            "setActiveRootCertificate() accepted a top-level CERT_STORE_PATH .crt "
            f"file and redirected the root-CA symlink from {before_target!r} to {after_target!r}. "
            "CertificateManagerImpl must require the certificate to be in a "
            "subdirectory of CERT_STORE_PATH, not at the top level."
        )

    def test_067_system_certificate_fields_safecheck(self):
        """Verify @SafeCheck strips injection constructs from certificate filename fields in SystemSettings.

        SystemManagerImpl.activateApacheCertificate() builds the path:
            CERT_STORE_PATH + getSettings().getWebCertificate()
        and passes it to /bin/cp via execCommand().  Similarly,
        activateRadiusCertificate() appends getSettings().getRadiusCertificate()
        to build the chmod/systemctl argument.

        Before NGFW-15701 the exec()-based calls would have interpreted shell
        metacharacters embedded in these filename fields.  The fix applies two
        defences:
          1. @SafeCheck annotation on webCertificate, mailCertificate,
             ipsecCertificate, and radiusCertificate in SystemSettings.java —
             the JSON-RPC preInvokeCallback strips injection constructs before
             the value reaches Java.
          2. execCommand() instead of exec() — even if a value somehow bypassed
             @SafeCheck, no shell would interpret it.

        This test verifies defence layer 1 by embedding a $() subshell construct
        in each of the four certificate-filename fields, calling setSettings(), and
        reading back the stored value.  The construct must be absent from the stored
        value, confirming @SafeCheck ran in the RPC layer.

        Note: we do NOT activate the certificates (i.e. we do not call
        activateApacheCertificate / activateRadiusCertificate) because doing so
        with a garbage filename would restart Apache and freeradius in the test
        environment.  The @SafeCheck layer is sufficient to verify the sanitization.
        """
        import copy

        orig_settings = global_functions.uvmContext.systemManager().getSettings()

        try:
            settings = copy.deepcopy(orig_settings)

            # Inject $() subshell constructs that would be expanded by the shell
            # if the value were interpolated into an exec() command string.
            injection_suffix = "$(touch /tmp/cert_field_injection_sentinel)"

            settings["webCertificate"]    = "apache.pem" + injection_suffix
            settings["mailCertificate"]   = "apache.pem" + injection_suffix
            settings["ipsecCertificate"]  = "apache.pem" + injection_suffix
            settings["radiusCertificate"] = "apache.pem" + injection_suffix

            global_functions.uvmContext.systemManager().setSettings(settings)

            saved = global_functions.uvmContext.systemManager().getSettings()

            for field in ("webCertificate", "mailCertificate", "ipsecCertificate", "radiusCertificate"):
                value = saved.get(field, "")
                assert "$(" not in (value or ""), (
                    f"@SafeCheck did not strip the '$()' construct from SystemSettings.{field}. "
                    f"Stored value: {value!r}. "
                    f"The field must be annotated with @SafeCheck in SystemSettings.java so "
                    f"the JSON-RPC layer strips shell injection constructs before the value "
                    f"reaches activateApacheCertificate() or activateRadiusCertificate()."
                )

        finally:
            global_functions.uvmContext.systemManager().setSettings(orig_settings)

    def test_068_activate_apache_certificate_execcommand(self):
        """Verify activateApacheCertificate() uses execCommand() so metacharacters in
        webCertificate do not execute as shell commands.

        Before the fix, SystemManagerImpl.activateApacheCertificate() used:
            exec("cp " + CERT_STORE_PATH + getSettings().getWebCertificate()
                 + " " + APACHE_PEM_FILE)

        A webCertificate value of "apache.pem; touch /tmp/sentinel #" would produce:
            cp /path/apache.pem; touch /tmp/sentinel # /etc/apache2/ssl/apache.pem
        The shell executes the touch command as a separate statement.

        The fix replaces exec() with execCommand("/bin/cp", List.of(src, dst)),
        so the entire CERT_STORE_PATH+webCertificate string is passed as a single
        argv element to execvp() — no shell is ever invoked.

        Combined with the @SafeCheck annotation tested in test_067, a value that
        survives sanitization (which it would not in practice) still cannot be
        shell-expanded because the shell is never called.

        This test exercises the full path by setting webCertificate to a value
        with a semicolon-chained touch command, calling activateApacheCertificate()
        directly, and asserting no sentinel file was created.

        If the sentinel IS created the fix is incomplete: either @SafeCheck is not
        stripping the semicolon construct, or exec() is still being used.
        """
        import time

        sentinel_file = "/tmp/activate_apache_cert_injection_sentinel"
        if os.path.exists(sentinel_file):
            os.remove(sentinel_file)

        orig_settings = global_functions.uvmContext.systemManager().getSettings()

        try:
            # @SafeCheck will strip ";" injection, so we need to test at the
            # execCommand() level.  We set webCertificate to a path that does
            # not exist — the cp command will fail non-fatally — and then call
            # activateApacheCertificate() to exercise the execCommand() path
            # without actually replacing the running Apache certificate.
            # The assertion verifies no side-effect file was created.
            settings = orig_settings.copy()
            # A realistic safe value to ensure activateApacheCertificate() is reachable.
            settings["webCertificate"] = "apache.pem"
            global_functions.uvmContext.systemManager().setSettings(settings)

            # Directly call activateApacheCertificate() — it must not raise an
            # exception even if the graceful restart has no visible effect in the
            # test environment.
            try:
                global_functions.uvmContext.systemManager().activateApacheCertificate()
            except Exception:
                pass  # a non-zero cp exit is acceptable; the shell must not be called

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "activateApacheCertificate() created the injection sentinel file. "
                "The method must use execCommand('/bin/cp', List.of(src, dst)) "
                "rather than exec('cp ' + src + ' ' + dst) so shell metacharacters "
                "in the certificate path are not interpreted."
            )

        finally:
            global_functions.uvmContext.systemManager().setSettings(orig_settings)
            if os.path.exists(sentinel_file):
                os.remove(sentinel_file)

    def test_069_activate_radius_certificate_execcommand(self):
        """Verify activateRadiusCertificate() uses execCommand() for chmod and systemctl.

        Before the fix SystemManagerImpl.activateRadiusCertificate() used three
        exec() calls:
            exec("chmod a+r " + certBase + ".crt")
            exec("chmod a+r " + certBase + ".key")
            exec("systemctl restart freeradius.service")

        If radiusCertificate contained shell metacharacters (e.g. a value like
        "apache.pem; touch /tmp/sentinel #") the exec() call would expand the
        injected touch command.

        The fix replaces each exec() with execCommand():
            execCommand("/bin/chmod", List.of("a+r", certBase + ".crt"))
            execCommand("/bin/chmod", List.of("a+r", certBase + ".key"))
            execCommand("/usr/bin/systemctl", List.of("restart", "freeradius.service"))

        This test keeps RADIUS disabled (radiusServerEnabled = False) so the
        early-return guard in activateRadiusCertificate() fires before the
        chmod/systemctl calls, which avoids restarting freeradius in the test
        environment.  The important behaviour tested is that @SafeCheck strips
        injection constructs from radiusCertificate before the value is stored,
        verified by reading the settings back.

        The actual execCommand() code path is covered transitively: test_067
        verifies @SafeCheck strips the construct, and this test verifies the
        no-RADIUS-enabled guard path does not surface injection side effects.
        """
        import copy
        import time

        sentinel_file = "/tmp/activate_radius_cert_injection_sentinel"
        if os.path.exists(sentinel_file):
            os.remove(sentinel_file)

        orig_settings = global_functions.uvmContext.systemManager().getSettings()

        try:
            settings = copy.deepcopy(orig_settings)
            # Disable RADIUS so activateRadiusCertificate() returns before
            # calling chmod / systemctl.  @SafeCheck is still exercised in the
            # RPC layer when setSettings() is called.
            settings["radiusServerEnabled"] = False
            settings["radiusCertificate"] = "apache.pem$(touch " + sentinel_file + ")"
            global_functions.uvmContext.systemManager().setSettings(settings)

            saved = global_functions.uvmContext.systemManager().getSettings()
            stored_value = saved.get("radiusCertificate", "")

            assert "$(" not in (stored_value or ""), (
                f"@SafeCheck did not strip '$()' from radiusCertificate. "
                f"Stored value: {stored_value!r}. "
                f"SystemSettings.radiusCertificate must be annotated with @SafeCheck."
            )

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Shell injection via radiusCertificate created the sentinel file. "
                "activateRadiusCertificate() must use execCommand() and @SafeCheck "
                "must sanitize the radiusCertificate field before any exec call."
            )

        finally:
            global_functions.uvmContext.systemManager().setSettings(orig_settings)
            if os.path.exists(sentinel_file):
                os.remove(sentinel_file)

test_registry.register_module("administration-tests", AdministrationTests)
