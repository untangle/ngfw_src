#!/usr/bin/python3
import os
import getopt
import json
import sys
import glob
import re
import string
import http.client
import urllib.request, urllib.parse, urllib.error
import html.parser

UNTANGLE_DIR = '%s/lib/python' % ( os.path.dirname(os.path.realpath(__file__) ) )
sys.path.insert(0, UNTANGLE_DIR)

import i18n

Languages = i18n.Languages()

# !! add comment entry about source

def process_test(po):
    for (record_index, record) in enumerate(po.records):
        if record.msg_id == "":
            continue
        if record.msg_id == "date_fmt":
            value = 'Y|m|d'
        elif record.msg_id == "thousand_sep":
            value = "."
        elif record.msg_id == "decimal_sep":
            value = ","
        elif record.msg_id == "timestamp_fmt":
            value = "Y|m|d g:i:s a"
        else:
            value = "X" + record.msg_id + "X"
        po.records[record_index].msg_str = [value]

def process_google(po):
    for (record_index, record) in enumerate(po.records):
        if record.msg_id == "":
            continue
# 		if len("".join(record.msg_str)) > 0:
# 			continue

# 		# print(record.msg_id)
# 		# print(urllib.urlencode({"q": record.msg_id}))
# 		# continue
# 		# sys.exit(1)

# 		print(po.language)

# 		# lookoup via https
# 		# https://www.googleapis.com/language/translate/v2?key=AIzaSyBlJ_x_7BX0NbbLlbRWdGmGuc4W32jmZzs&source=en&target=fr&q=Press"%"20"%"5C"%"22Add"%"5C"%"22"%"20button" 
# 		# -H "Origin: https://translate.google.com" 
# 		# -H "Referer: https://translate.google.com/toolkit/workbench?did=006kzas00qnqw0jcshkw&hl=en" 
# 		# -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36"
# 		#  --compressed
# 		key = "AIzaSyBlJ_x_7BX0NbbLlbRWdGmGuc4W32jmZzs"
# 		target = po.language
# 		headers = {
# 			"Origin": "https://translate.google.com",
# 			"Referer": "https://translate.google.com/toolkit/workbench?did=006kzas00qnqw0jcshkw&hl=en", 
# 			"User-Agent": "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36"
# 		}
# #		print(record.msg_id)
# 		query = "q=%7B0%7DThis+web+page+is+blocked%7B1%7D+because+it+violates+network+policy."
# 		c = httplib.HTTPSConnection("www.googleapis.com")
# 		c.request("GET", "/language/translate/v2?key="+key+"&source=en&target="+target+"&format=html&"+query, "", headers)
# 		response = c.getresponse()
# 		print(response.status, response.reason)
# 		data = response.read()
# 		print(data)
# 		rjson = json.loads(data)
# 		print(rjson)
# 		print(rjson["data"]["translations"][0]["translatedText"])
# 		parse = HTMLParser.HTMLParser()
# 		print(parse.unescape(u''+rjson["data"]["translations"][0]["translatedText"]))
# 		sys.exit(1)
# 		break

def main(argv):
    language_ids = Languages.get_enabled_ids()

    try:
        opts, args = getopt.getopt(argv, "hl:d", ["help", "languages=", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-l", "--languages"):
            language_ids = arg.split(",")

    for id in language_ids:
        language = Languages.get_by_id(id)
        po = i18n.PoFile(language=language["id"])
        po.load()
        print("\tProcessing: %s, %s," % (language["name"], po.file_name),)
        print("")
        if language["id"] == "xx":
            process_test(po)
        else:
            process_google(po)
        po.save()

    # otherwise, look at languages source and process

 
if __name__ == "__main__":
    main(sys.argv[1:])
