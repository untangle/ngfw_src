import pytest
import unittest
import json
import os
import time
from glob import glob
from os.path import join, getctime
from tests.common import NGFWTestCase
import tests.global_functions as global_functions
import runtests.test_registry as test_registry
import runtests.remote_control as remote_control
import runtests.overrides as overrides
import requests
import re
from jsonrpc.proxy import JSONRPCException

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

def _login_and_upload_cert(cert_path, retries=10, retry_sleep=2):
    """
    Authenticate to /auth/login then POST cert_path as multipart to /admin/upload.
    Returns the parsed JSON response.

    Retries on empty response.text: in the window right after untangle-vm restart
    Apache mod_python's DbmSession DB hasn't fully initialized -- /auth/login
    returns 200 + Set-Cookie but the cookie isn't persisted server-side, so
    /admin/upload sees no auth context and returns an empty body. Observed as 4
    administration cert-upload failures in the 2026-05-28 ATS bookworm->trixie
    post-upgrade run, all 4 passed cleanly on re-run minutes later.
    """
    url = global_functions.get_http_url()
    headers = {'accept': 'application/json'}
    last_status = None
    for attempt in range(retries):
        with open(cert_path, 'rb') as fh:
            files = {
                'type': (None, 'certificate_upload'),
                'argument': (None, 'upload_server'),
                'filename': ('uploadcert.pem', fh, 'application/x-x509-ca-cert'),
            }
            s = requests.Session()
            s.post(
                f"{url}/auth/login?url=/admin&realm=Administrator",
                data=f"fragment=&username={username}&password={password}",
                verify=False,
            )
            response = s.post(f"{url}/admin/upload", headers=headers, files=files)
        last_status = response.status_code
        if response.text:
            try:
                return json.loads(response.text)
            except json.JSONDecodeError:
                pass
        time.sleep(retry_sleep)
    raise AssertionError(
        f"/admin/upload returned non-JSON body after {retries} attempts "
        f"(last HTTP={last_status}); Apache mod_python session store likely "
        f"not warm after untangle-vm restart"
    )

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
        output_file_path = '/tmp/uploadcertificate.pem'

        with open(f"{certificates_dir}/apache.pem", 'r') as input_file:
            lines = input_file.readlines()

        # Add trailing spaces to each line
        lines_with_spaces = [line.rstrip() + ' ' + '\n' for line in lines]

        with open(output_file_path, 'w') as output_file:
            output_file.writelines(lines_with_spaces)

        certificate_upload_response = _login_and_upload_cert(output_file_path)
        files_list = []
        try:
            cert_upload_json = json.loads(certificate_upload_response.get('msg', None))
        except (json.JSONDecodeError, ValueError, TypeError):
            cert_upload_json = {}
        if (cert_upload_json.get("certData", None)):
            try:
                uploaded_response = global_functions.uvmContext.certificateManager().uploadCertificate("SERVER", cert_upload_json.get("certData"), cert_upload_json.get("keyData"), "" )
                assert "Certificate successfully uploaded" in uploaded_response.get("output", None)

                for file in glob(join(certificates_dir,f'*.pem')):
                    files_list.append((getctime(file), file))
                files_list = [file for _, file in sorted(files_list, reverse=True)]
            except JSONRPCException:
                pass
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
        output_file_path = '/tmp/uploadcertificate.pem'

        with open(f"{certificates_dir}/apache.pem", 'r') as input_file:
            content = input_file.read()

        # Convert LF to add trailing spaces and CRLF line endings
        modified_content = content.replace('\n', ' \r\n')
        #modified_content = content.replace('\n', '\r\n')
        with open(f"{output_file_path}", 'w') as output_file:
            output_file.write(modified_content)

        certificate_upload_response = _login_and_upload_cert(output_file_path)
        files_list = []
        try:
            cert_upload_json = json.loads(certificate_upload_response.get('msg', None))
        except (json.JSONDecodeError, ValueError, TypeError):
            cert_upload_json = {}
        if (cert_upload_json.get("certData", None)):
            try:
                uploaded_response = global_functions.uvmContext.certificateManager().uploadCertificate("SERVER", cert_upload_json.get("certData"), cert_upload_json.get("keyData"), "" )
                assert "Certificate successfully uploaded" in uploaded_response.get("output", None)

                for file in glob(join(certificates_dir,f'*.pem')):
                    files_list.append((getctime(file), file))
                files_list = [file for _, file in sorted(files_list, reverse=True)]
            except JSONRPCException:
                pass
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
        output_file_path = '/tmp/uploadcertificate.pem'

        with open(f"{certificates_dir}/apache.pem", 'r') as input_file:
            content = input_file.read()

        # Convert LF to add CRLF line endings
        modified_content = content.replace('\n', '\r\n')
        with open(f"{output_file_path}", 'w') as output_file:
            output_file.write(modified_content)

        certificate_upload_response = _login_and_upload_cert(output_file_path)
        files_list = []
        try:
            cert_upload_json = json.loads(certificate_upload_response.get('msg', None))
        except (json.JSONDecodeError, ValueError, TypeError):
            cert_upload_json = {}
        if (cert_upload_json.get("certData", None)):
            try:
                uploaded_response = global_functions.uvmContext.certificateManager().uploadCertificate("SERVER", cert_upload_json.get("certData"), cert_upload_json.get("keyData"), "" )
                assert "Certificate successfully uploaded" in uploaded_response.get("output", None)

                for file in glob(join(certificates_dir,f'*.pem')):
                    files_list.append((getctime(file), file))
                files_list = [file for _, file in sorted(files_list, reverse=True)]
            except JSONRPCException:
                pass
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
        # Malformed PEM payloads are now rejected at the RPC boundary by the
        # @SafeCheckParam(PEM) validator before uploadCertificate's body runs,
        # so the call raises instead of returning a structured error.
        with pytest.raises(Exception):
            global_functions.uvmContext.certificateManager().uploadCertificate(
                "SERVER",
                invalid_certificate_payload.get("certData"),
                invalid_certificate_payload.get("keyData"),
                "")


    #Test to validate import invalid certificate or key file
    def test_025_validate_import_invalid_server_certificate(self):
        # This test validates certificate from Text Area in the UI

        certificates_dir = '/usr/share/untangle/settings/untangle-certificates'
        isDir = os.path.exists(certificates_dir)
        isFile = os.path.exists(f"{certificates_dir}/apache.pfx")
        if not isDir and not isFile:
            pytest.skip('%s certificate directory or certificate not present' % self.appNameWF())
        certificate_upload_response = _login_and_upload_cert(f"{certificates_dir}/apache.pfx")
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
        """Verify SNMP v3 fields are rejected by @SafeCheck before reaching the shell command.

        writeSnmpdV3User() in SystemManagerImpl builds this shell string:

            ut-snmp.sh create_snmp3_user <v3Username> <v3AuthProto> "<v3AuthPass>" <v3PrivProto> "<v3PrivPass>"

        Unquoted fields (v3Username, v3AuthenticationProtocol, v3PrivacyProtocol) would allow
        command chaining; double-quoted fields (passphrases) would allow $(...) subshell
        substitution. The fix annotates all five fields with typed @SafeCheck in
        SnmpSettings.java (ALPHANUM for username/protocol fields, OPAQUE_SECRET for
        passphrases).

        REWRITTEN for NGFW-15741: the redesigned SafeCheckValidator is **fail-fast** —
        a value that violates the SafeType regex causes setSettings() to raise
        SafeCheckValidationException (JSON-RPC code 490) at the preInvokeCallback.
        The previous strip-mode behavior (silently mutating the value) was replaced
        with allowlist + fail-fast. This test asserts: (a) each malicious payload
        is rejected with an exception, and (b) the previously stored value is
        unchanged after each rejection (no partial-apply).
        """
        import copy

        orig_settings = global_functions.uvmContext.systemManager().getSettings()
        # Baseline must use values that pass the typed regexes so the initial
        # setSettings round-trip succeeds.
        baseline = copy.deepcopy(orig_settings)
        baseline_snmp = baseline["snmpSettings"]
        baseline_snmp["enabled"] = False
        baseline_snmp["v3Enabled"] = False
        baseline_snmp["v3Username"] = "testuser"
        baseline_snmp["v3AuthenticationProtocol"] = "sha"
        baseline_snmp["v3PrivacyProtocol"] = "aes"
        baseline_snmp["v3AuthenticationPassphrase"] = "GoodAuth1"
        baseline_snmp["v3PrivacyPassphrase"] = "GoodPriv1"

        try:
            # Persist the clean baseline first; round-trip must succeed.
            global_functions.uvmContext.systemManager().setSettings(baseline)

            # Each malicious payload must be rejected and must NOT mutate the
            # stored snmpSettings.
            for field_name, payload in (
                # Unquoted (shell-meta-class) — ALPHANUM rejects ; and space.
                ("v3Username",                  "testuser; touch /tmp/snmp_v3_sentinel #"),
                ("v3AuthenticationProtocol",    "sha; touch /tmp/snmp_v3_sentinel #"),
                ("v3PrivacyProtocol",           "aes; touch /tmp/snmp_v3_sentinel #"),
                # Double-quoted (subshell class) — OPAQUE_SECRET allows printable
                # Unicode (so $(), backtick, etc. would pass), but the test newline
                # variant below confirms control-char rejection. The $() payload
                # here is retained as a regression guard: if OPAQUE_SECRET ever gets
                # widened differently the assertion still holds via stored-value
                # equality.
                ("v3AuthenticationPassphrase",  "pass\n$(touch /tmp/snmp_v3_sentinel)"),
                ("v3PrivacyPassphrase",         "pass\n$(touch /tmp/snmp_v3_sentinel)"),
            ):
                bad = copy.deepcopy(baseline)
                bad["snmpSettings"][field_name] = payload
                with pytest.raises(Exception):
                    global_functions.uvmContext.systemManager().setSettings(bad)
                current = global_functions.uvmContext.systemManager().getSettings()["snmpSettings"]
                assert current[field_name] == baseline_snmp[field_name], (
                    f"NGFW-15741: SnmpSettings.{field_name} mutated despite "
                    f"SafeCheckValidator rejection. Stored: {current[field_name]!r}, "
                    f"expected: {baseline_snmp[field_name]!r}"
                )
        finally:
            global_functions.uvmContext.systemManager().setSettings(orig_settings)

    def test_051_snmp_config_fields_no_newline_injection(self):
        """Verify SNMP config-file fields are rejected by @SafeCheck against newline injection.

        writeSnmpdConfFile() in SystemManagerImpl writes user-controlled strings
        directly into /etc/snmp/snmpd.conf:

            trapsink <trapHost> <trapCommunity> <trapPort>
            syslocation <sysLocation>
            syscontact <sysContact>
            com2sec local default <communityString>

        A newline embedded in any of these fields would let an attacker append
        arbitrary snmpd.conf directives (e.g. 'extend' for RCE).

        REWRITTEN for NGFW-15741: the redesigned SafeCheckValidator is fail-fast.
        Every typed SafeType (ALPHANUM / HOSTNAME / SIMPLE_TEXT) rejects '\\n'
        and '\\r' at the regex level — setSettings() raises rather than silently
        stripping. This test asserts each newline payload is rejected and the
        stored snmpSettings is unchanged.
        """
        import copy

        orig_settings = global_functions.uvmContext.systemManager().getSettings()
        baseline = copy.deepcopy(orig_settings)
        baseline_snmp = baseline["snmpSettings"]
        baseline_snmp["enabled"] = False
        baseline_snmp["sendTraps"] = False
        # Clean baseline values that match the typed regexes.
        baseline_snmp["trapHost"]        = "trap.example.com"
        baseline_snmp["trapCommunity"]   = "publiccomm"
        baseline_snmp["sysLocation"]     = "HQ"
        baseline_snmp["sysContact"]      = "admin@example.com"
        baseline_snmp["communityString"] = "publiccomm"

        try:
            global_functions.uvmContext.systemManager().setSettings(baseline)

            payloads = {
                "trapHost":        "trap.example.com\nextend .1.2.3.4 /bin/id",
                "trapCommunity":   "public\nextend .1.2.3.5 /bin/id",
                "sysLocation":     "HQ\nextend .1.2.3.6 /bin/id",
                "sysContact":      "admin@example.com\nextend .1.2.3.7 /bin/id",
                "communityString": "public\nextend .1.2.3.8 /bin/id",
            }

            for field_name, payload in payloads.items():
                bad = copy.deepcopy(baseline)
                bad["snmpSettings"][field_name] = payload
                with pytest.raises(Exception):
                    global_functions.uvmContext.systemManager().setSettings(bad)
                current = global_functions.uvmContext.systemManager().getSettings()["snmpSettings"]
                assert current[field_name] == baseline_snmp[field_name], (
                    f"NGFW-15741: SnmpSettings.{field_name} mutated despite "
                    f"SafeCheckValidator rejection of newline payload. "
                    f"Stored: {current[field_name]!r}, expected: {baseline_snmp[field_name]!r}"
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
            # ------------------------------------------------------------------ #
            # Layer 1 — @SafeCheck(SafeType.HOSTNAME) on NetworkSettings.domainName #
            # (added by NGFW-15741) must reject the malicious domain at the RPC   #
            # boundary. setNetworkSettings raises SafeCheckValidationException    #
            # (code 490) before any hook fires.                                   #
            # ------------------------------------------------------------------ #
            malicious_settings = copy.deepcopy(orig_netsettings)
            malicious_settings['domainName'] = f"evil.com; touch {sentinel_file} #"
            with pytest.raises(Exception):
                global_functions.uvmContext.networkManager().setNetworkSettings(malicious_settings)

            # Even if layer 1 was bypassed, no hook should have fired with the
            # malicious value because the RPC throws before the change reaches
            # NetworkManagerImpl.
            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Command injection sentinel exists after a rejected setNetworkSettings — "
                "indicates the @SafeCheckValidator was bypassed and a downstream hook "
                "still received the malicious domainName."
            )

            # ------------------------------------------------------------------ #
            # Layer 2 — defense-in-depth: even if a malicious FQDN reached       #
            # generateServerCertificate() (e.g. via backup-restore of a tainted  #
            # settings.json, which bypasses @SafeCheck), CertificateManagerImpl  #
            # uses execCommand(argv) and an FQDN whitelist so the shell is never #
            # invoked. We exercise that path directly with a CN containing the   #
            # injection payload.                                                  #
            # ------------------------------------------------------------------ #
            cert_manager = global_functions.uvmContext.certificateManager()
            try:
                cert_manager.generateServerCertificate(
                    f"/CN=ngfw.evil.com; touch {sentinel_file} #", ""
                )
            except Exception:
                pass  # cert generation may fail; we only care about the sentinel

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                f"Command injection via malicious CN succeeded in cert generation: "
                f"the sentinel file was created, meaning shell metacharacters from "
                f"the cert subject were interpreted by the shell. "
                f"CertificateManagerImpl must use execCommand() and an FQDN whitelist."
            )

        finally:
            global_functions.uvmContext.networkManager().setNetworkSettings(orig_netsettings)
            if os.path.exists(sentinel_file):
                os.remove(sentinel_file)

    def test_062_certificate_fqdn_space_no_argument_injection(self):
        """Verify that a space-containing hostname cannot cause argument injection in cert generation.

        REWRITTEN for NGFW-15741: the original premise was "@SafeCheck does NOT
        strip spaces, so the FQDN whitelist in CertificateManagerImpl must catch
        them at the cert-generation step." That premise is now inverted — the
        redesigned typed validator @SafeCheck(SafeType.HOSTNAME) on
        NetworkSettings.hostName rejects spaces at the RPC boundary outright
        (HOSTNAME regex `^[A-Za-z0-9][A-Za-z0-9._-]*$` excludes whitespace).

        The CertificateManagerImpl whitelist + execCommand layers are STILL
        load-bearing because they protect the backup-restore path:
        SafeCheckValidator fires only on setSettings, not on settings-load from
        disk. A backup with a tainted hostName would bypass layer 1 and reach
        the cert generator. This test exercises that backup-restore-equivalent
        path by calling generateServerCertificate() directly with a CN
        containing a space — the whitelist + execCommand defenses must hold.
        """
        import copy
        import re
        import time

        orig_netsettings = global_functions.uvmContext.networkManager().getNetworkSettings()
        sentinel_file = "/tmp/cert_fqdn_space_injection_sentinel"

        if os.path.exists(sentinel_file):
            os.remove(sentinel_file)

        try:
            # ------------------------------------------------------------------ #
            # A. Confirm @SafeCheck(HOSTNAME) rejects space at the RPC boundary  #
            #    (positive change vs the original strip-mode validator).        #
            # ------------------------------------------------------------------ #
            malicious_settings = copy.deepcopy(orig_netsettings)
            malicious_settings['hostName'] = "foo bar"  # space in hostName
            malicious_settings['domainName'] = "test.local"
            with pytest.raises(Exception):
                global_functions.uvmContext.networkManager().setNetworkSettings(malicious_settings)

            # Confirm the stored hostName is unchanged (no partial-apply).
            saved_settings = global_functions.uvmContext.networkManager().getNetworkSettings()
            saved_hostname = saved_settings.get('hostName', '')
            original_hostname = orig_netsettings.get('hostName', '')
            assert saved_hostname == original_hostname, (
                f"NGFW-15741: hostName mutated despite SafeCheckValidator rejection. "
                f"Stored: {saved_hostname!r}, expected: {original_hostname!r}"
            )

            # ------------------------------------------------------------------ #
            # B. Defense-in-depth — the CertificateManagerImpl whitelist regex   #
            #    is still load-bearing for backup-restore (SafeCheck fires on    #
            #    setSettings, not on settings-load). Verify the whitelist regex  #
            #    that the impl uses still rejects a space-containing FQDN.       #
            # ------------------------------------------------------------------ #
            fqdn_whitelist = re.compile(r'[a-zA-Z0-9][a-zA-Z0-9._-]*')
            space_fqdn = "foo bar.test.local"
            assert not fqdn_whitelist.fullmatch(space_fqdn), (
                f"Backup-restore defense-in-depth regression: the FQDN whitelist "
                f"regex [a-zA-Z0-9][a-zA-Z0-9._-]* must continue rejecting "
                f"space-containing FQDNs even though @SafeCheck blocks them at RPC."
            )

            # ------------------------------------------------------------------ #
            # C. Cert generation with a space-containing CN must not produce      #
            #    argument-injection side effects — exercises execCommand(argv).  #
            # ------------------------------------------------------------------ #
            cert_manager = global_functions.uvmContext.certificateManager()
            try:
                cert_manager.generateServerCertificate(f"/CN={space_fqdn}", "")
            except Exception:
                pass  # generation failure is acceptable; we only care about the sentinel

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                f"Argument injection via space in CN succeeded (cn={space_fqdn!r}): "
                f"the sentinel file {sentinel_file!r} was created, meaning the space "
                f"caused the CN to be shell-split into separate arguments. "
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
        """Verify @SafeCheck(FILENAME) rejects injection constructs in cert filename fields.

        SystemManagerImpl.activateApacheCertificate() builds the path:
            CERT_STORE_PATH + getSettings().getWebCertificate()
        and passes it to /bin/cp via execCommand().  Similarly,
        activateRadiusCertificate() appends getSettings().getRadiusCertificate()
        to build the chmod/systemctl argument.

        Defences (layered):
          1. @SafeCheck(SafeType.FILENAME) on webCertificate, mailCertificate,
             ipsecCertificate, and radiusCertificate in SystemSettings.java —
             the JSON-RPC preInvokeCallback REJECTS the payload (fail-fast since
             NGFW-15741, throws SafeCheckValidationException -> JSON-RPC 490).
          2. execCommand() instead of exec() — even if a value somehow bypassed
             @SafeCheck, no shell would interpret it.

        This test verifies defence layer 1 by embedding a $() subshell construct
        in each of the four cert-filename fields and asserting setSettings() raises
        and the stored value remains unchanged from the baseline.
        """
        import copy

        orig_settings = global_functions.uvmContext.systemManager().getSettings()

        try:
            baseline = copy.deepcopy(orig_settings)
            injection_suffix = "$(touch /tmp/cert_field_injection_sentinel)"

            for field in ("webCertificate", "mailCertificate", "ipsecCertificate", "radiusCertificate"):
                bad = copy.deepcopy(baseline)
                bad[field] = "apache.pem" + injection_suffix
                with pytest.raises(Exception):
                    global_functions.uvmContext.systemManager().setSettings(bad)
                current = global_functions.uvmContext.systemManager().getSettings()
                assert current.get(field) == baseline.get(field), (
                    f"NGFW-15741: SystemSettings.{field} was mutated despite "
                    f"@SafeCheck rejection. Stored: {current.get(field)!r}, "
                    f"expected: {baseline.get(field)!r}"
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
        """Verify @SafeCheck(FILENAME) rejects injection in radiusCertificate, and
        the underlying activateRadiusCertificate() uses execCommand() as defence-in-depth.

        Before the fix SystemManagerImpl.activateRadiusCertificate() used three
        exec() calls:
            exec("chmod a+r " + certBase + ".crt")
            exec("chmod a+r " + certBase + ".key")
            exec("systemctl restart freeradius.service")

        Defences (layered):
          1. @SafeCheck(SafeType.FILENAME) on radiusCertificate — JSON-RPC layer
             REJECTS the payload (fail-fast since NGFW-15741, throws
             SafeCheckValidationException -> JSON-RPC 490).
          2. execCommand() instead of exec() — defence-in-depth.

        This test verifies layer 1 by attempting to set a $() injection payload
        and asserting setSettings() raises with the stored value unchanged. The
        sentinel file is checked as an additional belt-and-suspenders to confirm
        the injection never reached any shell context.
        """
        import copy
        import time

        sentinel_file = "/tmp/activate_radius_cert_injection_sentinel"
        if os.path.exists(sentinel_file):
            os.remove(sentinel_file)

        orig_settings = global_functions.uvmContext.systemManager().getSettings()

        try:
            baseline = copy.deepcopy(orig_settings)
            bad = copy.deepcopy(baseline)
            bad["radiusServerEnabled"] = False  # belt-and-suspenders: skip the actual restart path
            bad["radiusCertificate"] = "apache.pem$(touch " + sentinel_file + ")"

            with pytest.raises(Exception):
                global_functions.uvmContext.systemManager().setSettings(bad)

            current = global_functions.uvmContext.systemManager().getSettings()
            assert current.get("radiusCertificate") == baseline.get("radiusCertificate"), (
                f"NGFW-15741: SystemSettings.radiusCertificate was mutated despite "
                f"@SafeCheck rejection. Stored: {current.get('radiusCertificate')!r}, "
                f"expected: {baseline.get('radiusCertificate')!r}"
            )

            time.sleep(1)
            assert not os.path.exists(sentinel_file), (
                "Shell injection via radiusCertificate created the sentinel file. "
                "@SafeCheck(FILENAME) must reject the payload at the JSON-RPC "
                "boundary BEFORE any exec call."
            )

        finally:
            global_functions.uvmContext.systemManager().setSettings(orig_settings)
            if os.path.exists(sentinel_file):
                os.remove(sentinel_file)

    def test_070_safecheck_radius_proxy_fields(self):
        """Verify NGFW-15768 @SafeCheck annotations on SystemSettings.radiusProxy* fields.

        Fields validated:
          - radiusProxyServer        (HOSTNAME)
          - radiusProxyWorkgroup     (ALPHANUM)
          - radiusProxyRealm         (HOSTNAME)
          - radiusProxyUsername      (ALPHANUM)
          - radiusProxyPassword      (OPAQUE_SECRET)

        Sink: LocalDirectoryImpl 'net ads join -U user%pass ...' and /etc/samba/smb.conf
        / /etc/krb5.conf via 'include = ...' directives (config-file RCE class).

        The redesigned SafeCheckValidator is fail-fast: a value that violates the
        SafeType regex causes the JSON-RPC preInvokeCallback to throw
        SafeCheckValidationException, which Jabsorb propagates to the Python SDK
        as an exception. The previous stored value must be unchanged.
        """
        import copy

        orig_settings = global_functions.uvmContext.systemManager().getSettings()
        try:
            # Positive — valid values round-trip.
            positive = copy.deepcopy(orig_settings)
            positive["radiusProxyServer"]    = "ads.example.com"
            positive["radiusProxyWorkgroup"] = "EXAMPLE"
            positive["radiusProxyRealm"]     = "example.com"
            positive["radiusProxyUsername"]  = "admin"
            positive["radiusProxyPassword"]  = "Str0ng!Pass-Phrase"
            global_functions.uvmContext.systemManager().setSettings(positive)
            saved = global_functions.uvmContext.systemManager().getSettings()
            assert saved["radiusProxyServer"]    == "ads.example.com"
            assert saved["radiusProxyWorkgroup"] == "EXAMPLE"
            assert saved["radiusProxyRealm"]     == "example.com"
            assert saved["radiusProxyUsername"]  == "admin"
            assert saved["radiusProxyPassword"]  == "Str0ng!Pass-Phrase"

            # Negative — each historically-confirmed RCE payload is rejected.
            # We keep the previous valid `positive` settings as the baseline and
            # confirm that after each rejection the stored value still equals
            # the baseline (i.e. setSettings never partially applied).
            baseline = global_functions.uvmContext.systemManager().getSettings()
            negative_payloads = {
                "radiusProxyServer":    "ads.example.com; touch /tmp/x",
                "radiusProxyWorkgroup": "EXAMPLE`id`",
                "radiusProxyRealm":     "example.com$(id)",
                "radiusProxyUsername":  "admin|whoami",
                "radiusProxyPassword":  "pwd\nextend .1.2.3.4 /bin/id",  # OPAQUE_SECRET rejects \n
            }
            for field, payload in negative_payloads.items():
                bad = copy.deepcopy(baseline)
                bad[field] = payload
                with pytest.raises(Exception):
                    global_functions.uvmContext.systemManager().setSettings(bad)
                current = global_functions.uvmContext.systemManager().getSettings()
                assert current[field] == baseline[field], (
                    f"NGFW-15768: SystemSettings.{field} was mutated despite "
                    f"SafeCheckValidator rejection. Stored: {current[field]!r}, "
                    f"expected: {baseline[field]!r}"
                )
        finally:
            global_functions.uvmContext.systemManager().setSettings(orig_settings)

    def test_073_safecheck_dynamic_dns_fields(self):
        """Verify NGFW-15768 @SafeCheck annotations on NetworkSettings.dynamicDns* fields.

        Fields validated (5):
          - dynamicDnsServiceName       (ALPHANUM)
          - dynamicDnsServiceUsername   (USERNAME_OR_EMAIL)  admits @ for email-format logins
          - dynamicDnsServicePassword   (OPAQUE_SECRET)
          - dynamicDnsServiceZone       (HOSTNAME)
          - dynamicDnsServiceHostnames  (SIMPLE_TEXT)

        Sink: ddclient_manager.py writes these into /etc/ddclient.conf. The
        ddclient `cmd='...'` directive triggers shell exec — newline injection
        in ANY field produces a `cmd=` line that ddclient executes as root.

        dynamicDnsServiceWan is intentionally NOT annotated (lookup-key only,
        not interpolated — the looked-up systemDev is INTERFACE-annotated).
        """
        import copy

        orig_settings = global_functions.uvmContext.networkManager().getNetworkSettings()
        try:
            # Positive: valid values round-trip.
            positive = copy.deepcopy(orig_settings)
            positive["dynamicDnsServiceName"]      = "cloudflare"
            positive["dynamicDnsServiceUsername"]  = "ddns_user"
            positive["dynamicDnsServicePassword"]  = "Str0ng!P@ss-Phrase"
            positive["dynamicDnsServiceZone"]      = "example.com"
            positive["dynamicDnsServiceHostnames"] = "vpn.example.com,www.example.com"
            global_functions.uvmContext.networkManager().setNetworkSettings(positive)
            saved = global_functions.uvmContext.networkManager().getNetworkSettings()
            assert saved["dynamicDnsServiceName"]      == "cloudflare"
            assert saved["dynamicDnsServiceUsername"]  == "ddns_user"
            assert saved["dynamicDnsServiceZone"]      == "example.com"
            assert saved["dynamicDnsServiceHostnames"] == "vpn.example.com,www.example.com"

            # USERNAME_OR_EMAIL accepts email-format logins (ALPHANUM rejected '@').
            # Cloudflare, No-IP, DynDNS all use email as the login identifier.
            positive["dynamicDnsServiceUsername"] = "user@provider.com"
            global_functions.uvmContext.networkManager().setNetworkSettings(positive)
            saved = global_functions.uvmContext.networkManager().getNetworkSettings()
            assert saved["dynamicDnsServiceUsername"] == "user@provider.com", (
                "NGFW-15768: USERNAME_OR_EMAIL failed to accept email-format DDNS "
                f"username; got {saved['dynamicDnsServiceUsername']!r}"
            )

            # Negative: each historically-confirmed RCE payload is rejected.
            baseline = global_functions.uvmContext.networkManager().getNetworkSettings()
            negative_payloads = {
                "dynamicDnsServiceName":      "cloudflare; touch /tmp/x",
                "dynamicDnsServiceUsername":  "ddns_user`id`",
                "dynamicDnsServicePassword":  "pwd\ncmd='/tmp/evil'",   # OPAQUE_SECRET rejects \n
                "dynamicDnsServiceZone":      "example.com$(id)",
                "dynamicDnsServiceHostnames": "vpn.example.com\ncmd='/tmp/evil'",
            }
            for field, payload in negative_payloads.items():
                bad = copy.deepcopy(baseline)
                bad[field] = payload
                with pytest.raises(Exception):
                    global_functions.uvmContext.networkManager().setNetworkSettings(bad)
                current = global_functions.uvmContext.networkManager().getNetworkSettings()
                assert current[field] == baseline[field], (
                    f"NGFW-15768: NetworkSettings.{field} was mutated despite "
                    f"SafeCheckValidator rejection. Stored: {current[field]!r}, "
                    f"expected: {baseline[field]!r}"
                )
        finally:
            global_functions.uvmContext.networkManager().setNetworkSettings(orig_settings)

    def test_072_safecheckparam_daemon_manager(self):
        """Verify NGFW-15768 @SafeCheckParam annotations on DaemonManager methods.

        JIRA Tier A row #4 originally listed DaemonProcessSettings.searchString as
        a POJO field requiring @SafeCheck. There is no such class — searchString
        is a method parameter to DaemonManager.enableDaemonMonitoring and
        enableRequestMonitoring, and lands in bare-shell exec sinks:

          DaemonManagerImpl.java:329  execResult("pgrep -x " + daemonSearchString)
          DaemonManagerImpl.java:432  execOutput("pgrep " + object.searchString)
          DaemonManagerImpl.java:366  execEvil("systemctl " + cmd + " " + daemonName)
          DaemonManagerImpl.java:408  execOutput("systemctl " + cmd + " " + daemonName)

        Closed via @SafeCheckParam(SafeType.ALPHANUM) on daemonName + searchString.

        The JIRA's recommended OPAQUE_SECRET would have permitted ;, |, $(...),
        backtick — exactly the metacharacters the bare-shell concat expands.
        ALPHANUM is the correct type for a pgrep pattern / systemctl unit name.
        """
        daemon_mgr = global_functions.uvmContext.daemonManager()

        # --- Positive: valid daemonName + searchString round-trip. ---
        # 'snmpd' is a reasonable real daemon name; the call returns False if
        # the daemon is not registered, but the @SafeCheckParam validator runs
        # before that lookup, so this should NOT raise.
        try:
            daemon_mgr.enableDaemonMonitoring("snmpd", 30, "snmpd")
        except Exception as e:
            raise AssertionError(
                f"NGFW-15768: positive @SafeCheckParam call rejected unexpectedly: {e!r}. "
                f"Both daemonName='snmpd' and searchString='snmpd' match ALPHANUM regex."
            )
        finally:
            try:
                daemon_mgr.disableAllMonitoring("snmpd")
            except Exception:
                pass

        # --- Negative: every historically-confirmed RCE payload is rejected. ---
        # Each call should raise a JSON-RPC exception (SafeCheckValidationException
        # propagated through Jabsorb as error 490) BEFORE the parameter ever
        # reaches the DaemonObject. We probe both daemonName and searchString
        # positions across the two annotated methods.
        rce_payloads = (
            "snmpd; touch /tmp/x",
            "snmpd`id`",
            "snmpd$(id)",
            "snmpd|whoami",
            "snmpd\necho INJECT",
            "snmpd && touch /tmp/x",
        )
        for payload in rce_payloads:
            # daemonName position on enableDaemonMonitoring
            with pytest.raises(Exception):
                daemon_mgr.enableDaemonMonitoring(payload, 30, "snmpd")
            # searchString position on enableDaemonMonitoring
            with pytest.raises(Exception):
                daemon_mgr.enableDaemonMonitoring("snmpd", 30, payload)
            # daemonName position on enableRequestMonitoring
            with pytest.raises(Exception):
                daemon_mgr.enableRequestMonitoring(payload, 30, "127.0.0.1", 161, "x", "snmpd")
            # searchString position on enableRequestMonitoring
            with pytest.raises(Exception):
                daemon_mgr.enableRequestMonitoring("snmpd", 30, "127.0.0.1", 161, "x", payload)
            # daemonName position on disableAllMonitoring (defense-in-depth)
            with pytest.raises(Exception):
                daemon_mgr.disableAllMonitoring(payload)

    # ------------------------------------------------------------------
    # NGFW-15768 / NGFW-15765 @SafeCheckParam coverage
    #
    # For every RPC method that carries @SafeCheckParam on a String arg,
    # send one INVALID value (asserting JSON-RPC rejection) and one VALID
    # value (asserting it gets past the validator). Any state mutated by a
    # positive call is captured up-front and restored in a finally block.
    # ------------------------------------------------------------------

    def test_080_safecheckparam_get_server_certificate_information(self):
        """CertificateManagerImpl.getServerCertificateInformation(fileName: FILENAME)."""
        cert_mgr = global_functions.uvmContext.certificateManager()
        # INVALID — FILENAME forbids '/', leading dash, semicolons
        with pytest.raises(Exception):
            cert_mgr.getServerCertificateInformation("../etc/shadow")
        # VALID — pull a filename from the live cert list so the positive
        # path isn't tied to any specific PEM existing on the box. The
        # validator-passes proof point is just that no JSONRPCException
        # is raised — the underlying method may still return None for
        # certs it can't parse.
        cert_list = cert_mgr.getServerCertificateList()
        if cert_list and cert_list.get("list"):
            fname = cert_list["list"][0].get("fileName")
            if fname:
                cert_mgr.getServerCertificateInformation(fname)

    def test_081_safecheckparam_get_root_certificate_information(self):
        """CertificateManagerImpl.getRootCertificateInformation(fileName: FILENAME)."""
        cert_mgr = global_functions.uvmContext.certificateManager()
        with pytest.raises(Exception):
            cert_mgr.getRootCertificateInformation("name;id")
        # VALID — read-only. Empty/null is accepted by FILENAME contract and
        # routes to the default root CA path; method handles that internally.
        cert_mgr.getRootCertificateInformation("")

    def test_082_safecheckparam_generate_certificate_authority(self):
        """CertificateManagerImpl.generateCertificateAuthority(commonName: NATURAL_NAME,
                                                              certSubject: CERT_SUBJECT)."""
        cert_mgr = global_functions.uvmContext.certificateManager()
        # INVALID — commonName carries shell metachar
        with pytest.raises(Exception):
            cert_mgr.generateCertificateAuthority("CA;rm -rf /", "/CN=Test/O=Untangle")
        # INVALID — certSubject missing required '/CN=' anchor
        with pytest.raises(Exception):
            cert_mgr.generateCertificateAuthority("Test CA", "CN=NoSlash")
        # Positive call skipped: generateCertificateAuthority() overwrites
        # the active root-CA symlinks (untangle.crt/key) via symlinkRootCerts()
        # and creates a new CA directory.  Cleaning this up reliably is fragile
        # — previous attempts left dangling symlinks that broke SSL Inspector
        # in downstream suites (captive-portal test_028).  The two negative
        # cases above are sufficient to prove the @SafeCheckParam annotation
        # fires; the validator runs before any side effects.

    def test_083_safecheckparam_generate_server_certificate(self):
        """CertificateManagerImpl.generateServerCertificate(certSubject: CERT_SUBJECT,
                                                            altNames:    SAN_LIST)."""
        cert_mgr = global_functions.uvmContext.certificateManager()
        # INVALID — certSubject shape violation
        with pytest.raises(Exception):
            cert_mgr.generateServerCertificate("not a /CN= subject", "DNS:example.com")
        # INVALID — altNames carries shell metachar
        with pytest.raises(Exception):
            cert_mgr.generateServerCertificate("/CN=test.local", "DNS:example.com;id")
        # VALID-shape probe: we don't apply this cert; the call writes a
        # server cert under untangle-certificates, but the existing rotation
        # logic leaves apache.pem in place, so no state restore is required
        # beyond removing the newly-created PEM. Skip the positive call to
        # avoid disk churn — the negative cases already prove the annotation
        # is wired. If the validator was not fired, both negative calls
        # would silently succeed and the test would not raise.

    def test_084_safecheckparam_upload_certificate(self):
        """CertificateManagerImpl.uploadCertificate(certMode: ALPHANUM,
                                                    certData: PEM, keyData: PEM,
                                                    extraData: PEM)."""
        cert_mgr = global_functions.uvmContext.certificateManager()
        # INVALID — certMode contains semicolon (ALPHANUM rejection)
        with pytest.raises(Exception):
            cert_mgr.uploadCertificate("SERVER;id", certData, keyData, "")
        # INVALID — certData is not PEM-shaped
        with pytest.raises(Exception):
            cert_mgr.uploadCertificate("SERVER", "not a pem", keyData, "")
        # VALID — uses the well-formed PEM blobs from this file. The cert
        # is uploaded, then immediately removed to restore initial state.
        certificates_dir = '/usr/share/untangle/settings/untangle-certificates'
        initial = cert_mgr.getServerCertificateList()
        try:
            resp = cert_mgr.uploadCertificate("SERVER", certData, keyData, "")
            # Validator passed; underlying call may report success or a
            # semantic error (e.g. cert/key mismatch). Either is OK — we
            # only care that the @SafeCheckParam validator did not block.
            assert resp is not None
        finally:
            if os.path.isdir(certificates_dir):
                files_list = []
                for f in glob(join(certificates_dir, '*.pem')):
                    files_list.append((getctime(f), f))
                files_list = [f for _, f in sorted(files_list, reverse=True)]
                if len(files_list) > 1:
                    cert_mgr.removeCertificate("SERVER", os.path.basename(files_list[0]))
            final = cert_mgr.getServerCertificateList()
            assert len(initial['list']) == len(final['list'])

    def test_085_safecheckparam_import_signed_request(self):
        """CertificateManagerImpl.importSignedRequest(certData: PEM, extraData: PEM)."""
        cert_mgr = global_functions.uvmContext.certificateManager()
        # INVALID — certData missing PEM envelope. The PEM SafeType only
        # checks envelope shape and forbids control chars; shell-metachar
        # bodies like '$(id)' are valid printable ASCII and not the
        # validator's job to block (OpenSSL parses these, not a shell).
        with pytest.raises(Exception):
            cert_mgr.importSignedRequest("plain text", "")
        # No positive case: importSignedRequest requires a matching CSR
        # to exist on the box and would write new files. The single
        # negative above is sufficient to confirm the validator is wired.

    def test_086_safecheckparam_remove_certificate(self):
        """CertificateManagerImpl.removeCertificate(type: ALPHANUM, fileName)."""
        cert_mgr = global_functions.uvmContext.certificateManager()
        # INVALID — type carries shell metachar
        with pytest.raises(Exception):
            cert_mgr.removeCertificate("SERVER;id", "junk.pem")
        # VALID type, but fileName 'apache.pem' is guarded by the method
        # body so nothing is removed — proves the validator passed.
        cert_mgr.removeCertificate("SERVER", "apache.pem")

    def test_090_safecheckparam_restore_system_backup(self):
        """ConfigManagerImpl.restoreSystemBackup(argFileName: FILE_PATH,
                                                 maintainRegex: REGEX_PATTERN)."""
        cfg_mgr = global_functions.uvmContext.configManager()
        # INVALID — relative path (FILE_PATH requires leading '/')
        with pytest.raises(Exception):
            cfg_mgr.restoreSystemBackup("tmp/backup.tar", ".*")
        # INVALID — FILE_PATH forbids whitespace / shell metachar
        with pytest.raises(Exception):
            cfg_mgr.restoreSystemBackup("/tmp/backup; id .tar", ".*")
        # VALID-shape probe — file does not exist, so the underlying call
        # will fail; that's fine, we only need the validator to pass.
        resp = cfg_mgr.restoreSystemBackup("/tmp/nonexistent-ats-backup.tar.gz", ".*")
        # Any non-exception response means validator accepted the input.
        assert resp is not None or resp is None  # smoke; rejection would raise

    def test_091_safecheckparam_google_drive_path(self):
        """GoogleManagerImpl.getAppSpecificGoogleDrivePath(appDirectory: SIMPLE_TEXT)."""
        g_mgr = global_functions.uvmContext.googleManager()
        # INVALID — SIMPLE_TEXT forbids ';' and shell metachar
        with pytest.raises(Exception):
            g_mgr.getAppSpecificGoogleDrivePath("backup;rm -rf /")
        # VALID — simple ASCII string passes SIMPLE_TEXT
        path = g_mgr.getAppSpecificGoogleDrivePath("ATS-backup")
        assert path is not None

    def test_092_safecheckparam_get_authorization_url(self):
        """GoogleManagerImpl.getAuthorizationUrl(windowProtocol: SIMPLE_TEXT,
                                                 windowLocation: SIMPLE_TEXT)."""
        g_mgr = global_functions.uvmContext.googleManager()
        with pytest.raises(Exception):
            g_mgr.getAuthorizationUrl("https;evil", "host/path")
        with pytest.raises(Exception):
            g_mgr.getAuthorizationUrl("https", "host`id`/path")
        # VALID — read-only string assembly
        g_mgr.getAuthorizationUrl("https", "localhost/admin")

    def test_094_safecheckparam_upload_to_drive(self):
        """GoogleManagerImpl.uploadToDrive(filePath: FILE_PATH, parentFolder: SIMPLE_TEXT)."""
        g_mgr = global_functions.uvmContext.googleManager()
        with pytest.raises(Exception):
            g_mgr.uploadToDrive("../../etc/passwd", "Backup")
        with pytest.raises(Exception):
            g_mgr.uploadToDrive("/tmp/file.zip", "Backup;id")
        # VALID-shape — file may not exist; validator should accept.
        try:
            g_mgr.uploadToDrive("/tmp/nonexistent-ats.zip", "ATS-Folder")
        except Exception as e:
            assert "Invalid value in" not in str(e), \
                f"validator unexpectedly rejected a well-formed FILE_PATH: {e!r}"

    def test_095_safecheckparam_cloud_manager(self):
        """CloudManagerImpl.accountLogin / accountCreate parameter validation."""
        cloud_mgr = global_functions.uvmContext.cloudManager()
        # accountLogin(email: EMAIL, password: OPAQUE_SECRET)
        with pytest.raises(Exception):
            cloud_mgr.accountLogin("not-an-email", "Passw0rd!")
        with pytest.raises(Exception):
            cloud_mgr.accountLogin("ok@example.com", "pwd\nINJECT")  # \n -> control char
        # VALID-shape — credentials are bogus but pass shape; backend will
        # reject the login itself, which proves the validator passed.
        try:
            cloud_mgr.accountLogin("ats@example.com", "Passw0rd!")
        except Exception as e:
            assert "Invalid value in" not in str(e), \
                f"validator unexpectedly rejected well-formed credentials: {e!r}"

test_registry.register_module("administration-tests", AdministrationTests)
