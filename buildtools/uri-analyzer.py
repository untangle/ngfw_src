#!/usr/bin/python3
##
## Analyze source code for uris and determine if they are in translate list
##
import argparse
import glob
import re
import sys

Results = {}
Debug = False

class UriAnalyzer():
    """
    Using the known list of translatable uris from UriManager, see if target
    source files contain unaccounted uris.

    If so, it likely means the URI needs to be added to translations or ignored.
    """

    # Source containing all known translatable uris
    uri_manager_source_filename = "uvm/impl/com/untangle/uvm/UriManagerImpl.java"
    # Regex match on translatable uris
    uri_manager_seturi_re = re.compile(".*\.setUri\(\"([^\"]+)\"\)")
    # List of translatable uris
    uri_manager_uris=[]

    # Source code extensions to analyze
    source_extensions = [
        "java",
        "py",
        "js"
    ]
    # Source code paths to ignore
    source_ignore_paths = [
        "/dist/",
        "/downloads/",
        "/hier/usr/lib/python3/unit_tests/",
        "/hier/usr/lib/python3/dist-packages/tests/",
        "/i18ntools/"
    ]
    # General uri matcher across all source files
    source_uri_re = re.compile(".*[\"\'](([a-zA-Z]+)://[^\"\']+/.+?)[\\\\\"\']")

    # Uri schemes to ignore
    ignore_schemes = ["chrome"]
    # Uris that we don't translate
    ignore_uris = [
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd",
        "http://www.w3.org/1999/xhtml",
        "https://search.kidzsearch.com/kzsearchlmt.php",
        "http://127.0.0.1/reports/?reportChart=1",
        "http://localhost:8002/admin/gridSettings",

        "http://www.securityfocus.com/bid/",
        "http://cve.mitre.org/cgi-bin/cvename.cgi?name=",
        "http://cgi.nessus.org/plugins/dump.php3?id=",
        "http://www.whitehats.com/info/IDS",
        "http://vil.nai.com/vil/content/v",
        "http://osvdb.org/show/osvdb/",
        "http://technet.microsoft.com/en-us/security/bulletin/",

        "https://accounts.google.com/o/oauth2/v2/auth?client_id=365238258169-6k7k0ett96gv2c8392b9e1gd602i88sr.apps.googleusercontent.com&redirect_uri=$.AuthRelayUri.$&response_type=code&scope=email&state=$.GoogleState.$",
        "https://www.facebook.com/v2.9/dialog/oauth?client_id=1840471182948119&redirect_uri=$.AuthRelayUri.$&response_type=code&scope=email&state=$.FacebookState.$",
        "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=f8285e96-b240-4036-8ea5-f37cf6b981bb&redirect_uri=$.AuthRelayUri.$&response_type=code&scope=openid%20User.Read&state=$.MicrosoftState.$",

        "https://www.privateinternetaccess.com/pages/openvpn-ios",

        "http://download.thinkbroadband.com/5MB.zip",
        "http://download.thinkbroadband.com/50MB.zip",
        "http://cachefly.cachefly.net/5mb.test",
        "http://cachefly.cachefly.net/50mb.test",

        "http://standards.ieee.org/cgi-bin/ouisearch?",
        "https://wiki.edge.arista.com/index.php/OpenVPN",
        "https://edge.arista.com/shop/virus-blocker",
        "https://edge.arista.com/shop/web-filter",
        "https://edge.arista.com/shop/Spam-Blocker",
        "https://edge.arista.com/shop/Live-Support",
        "https://edge.arista.com/shop/Application-Control",

        "https://easylist-downloads.adblockplus.org/easylist.txt",
        "https://edge.arista.com/favicon.ico",
        "https://edge.arista.com/legal",

        "https://wiki.edge.arista.com/get.php",
        "https://edge.arista.com/feedback",
        "https://downloads.edge.arista.com/bdam/config",

        # From test_network.py
        "http://1.2.3.4/test/testPage1.html",
        "http://{wan_address}:81/test/testPage1.html",
        "http://{wan_address}/test/testPage1.html",
        "https://1.2.3.4/test/testPage1.html",
        "ftp://{global_functions.ftp_server}/{ftp_file_name}",
        "ftp://{wan_ip}/{ftp_file_name}",
        "https://{wan_address}/test/testPage1.html",

        # DBL Uri's
        "http://opendbl.net/lists/etknown.list",
        "http://opendbl.net/lists/dshield.list"
    ]

    def __init__(self):
        """
        Initialize object
        """
        with open(UriAnalyzer.uri_manager_source_filename,"r") as uri_source:
            for line in uri_source:
                matches = UriAnalyzer.uri_manager_seturi_re.match(line)
                if matches is not None:
                    self.uri_manager_uris.append(matches.group(1))

        if len(self.uri_manager_uris) == 0:
            # We have had some problem reading the file
            raise Exception(f"no uri manager uris found in {UriAnalyzer.uri_manager_source_filename}")

    def scan(self, filename):
        """
        Analyze file, extract uris, and compare against known list.

        Returns True if all urls match the manager.  Otherwise False.
        """
        global Results

        matched = True
        line_number = 0
        with open(filename,"r") as source_file:
            for line in source_file:
                line_number += 1
                matches = UriAnalyzer.source_uri_re.match(line)
                if matches is not None:
                    uri=matches.group(1)
                    scheme=matches.group(2)
                    if uri not in self.uri_manager_uris \
                        and scheme not in UriAnalyzer.ignore_schemes \
                        and uri not in UriAnalyzer.ignore_uris:
                        if filename not in Results:
                            Results[filename] = []
                        Results[filename].append({
                            "line_number": line_number,
                            "line": line.strip(),
                            "uri": uri
                        })
                        matched = False

        return matched

    def scan_all(self):
        """
        Look at all source files and determine if any contains uris that are not managed.

        Returns True if all urls match the manager.  Otherwise False.
        """
        matched = True
        for source_extension in UriAnalyzer.source_extensions:
            file_paths=glob.glob(f"./**/*.{source_extension}",recursive=True)
            for file_path in file_paths:
                ignore_file=False
                for ignore_path in UriAnalyzer.source_ignore_paths:
                    if ignore_path in file_path:
                        ignore_file=True
                        break
                if ignore_file:
                    continue

                if self.scan(file_path) is False:
                    matched = False

        return matched

def usage():
    """
    Show usage
    """
    print("usage")

def main(argv):
    global Debug

    global Result

    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument("-d", "--debug", help="Include DEBUG in result", action="store_true")
    arg_parser.add_argument("-f", "--filename", help="List of filenames to process", nargs="*")

    args = arg_parser.parse_args()

    Debug=args.debug

    uri_analyzer = UriAnalyzer()
    matched = True
    if args.filename is not None and len(args.filename) > 0:
        # Scan each filen
        for filename in args.filename:
            if uri_analyzer.scan(filename) is False:
                matched = False
    else:
        # Scan all files
        matched = uri_analyzer.scan_all()

    if matched is False:
        # We have at least one uri that seems like it should be translated
        for filename in Results:
            print(f"{filename}:")
            for line in Results[filename]:
                print(f" num = {line['line_number']}")
                print(f"line = {line['line']}")
                print(f" uri = {line['uri']}")
                print()
        sys.exit(1)

if __name__ == "__main__":
    main( sys.argv[1:] )
