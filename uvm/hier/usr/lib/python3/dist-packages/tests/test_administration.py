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

test_registry.register_module("administration-tests", AdministrationTests)
