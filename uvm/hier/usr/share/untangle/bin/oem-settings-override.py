#!/usr/bin/python3

"""
SCRIPT NAME: oem-settings-override.py

#------------------------------------------------------------------------------
This script is used to make minor modifications to Untangle NGFW settings
stored in simple JSON files. It is called just after creating the default
settings from each of the core component managers in the Uvm context. The
purpose is to provide a simple mechanism to allow customers and partners to
customize the initial system configuration via a simple and supported
mechanism.

The script takes four arguments. The first is the settings file to be
modified. The second is a control file where the override criteria are
defined. The third is the name of the settings class which selects the
override section to be applied. The fourth is the output file for the
modified settings. The input and output files can be the same.

The example control json below is for NetworkSettings and contains examples
for using the update, find/change, and insert features. The update example
replaces several of the values in the base network settings. The find/change
example looks for systemDev=eth1 in the interfaces section and changes
the IP address and DHCP server range. The insert example adds two new
entries to the staticEntries in the dnsSettings section.

{
  "NetworkSettings": {
    "update":
      { "hostName": "myserver", "domainName": "mydomain.com", "publicUrlAddress": "myserver.mydomain.com" },
    "interfaces": {
      "list": [{
        "find":
          { "systemDev": "eth1" },
        "change":
          { "v4StaticAddress": "192.168.1.1", "dhcpRangeStart": "192.168.1.10", "dhcpRangeEnd": "192.168.1.250" }
      }]
    },
    "dnsSettings": {
      "staticEntries": {
        "insert": {
          "list": [
            { "address": "192.168.1.1", "javaClass": "com.untangle.uvm.network.DnsStaticEntry", "name": "routerlogin.com" },
            { "address": "192.168.1.1", "javaClass": "com.untangle.uvm.network.DnsStaticEntry", "name": "routerlogin.net" }
          ]
        }
      }
    }
  }
}

"""

# for interactive debugging add 'import pdb' below and use pdb.set_trace()
# anywhere in the script to pause execution and break into the debugger

import logging
import json
import time
#import pdb
import sys
import os

# This sets the logging level
logging_level = logging.DEBUG

# Can be set by passing debug as the fifth argument to the script
debug_flag = False

# These globals hold the JSON for the config settings to be changed and the
# list of modifications to be applied.
config_json = None
modify_json = None

# These globals track recursion into the config and modify dictionaries
# in the config_scanner and modify_scanner functions
config_stack = []
modify_stack = []

# This global holds the class name of the settings being modified
class_name = ""

#------------------------------------------------------------------------------
# The config_scanner recursively walks and processes the config json
def config_scanner(argjson):
	# process everything in the argumented json
	for key, value in list(argjson.items()):
		# for dictionaries we put the key on the config stack and process
		# recursively and then pass the dictionary to the insert_worker
		if isinstance(value, dict):
			config_stack.append(key)
			config_scanner(value)
			insert_worker(value)
			config_stack.pop()

		# for lists we process each item recursively and then pass the list to
		# the modify_scanner where we handle the search and modify logic
		elif isinstance(value, list):
			# if we have an empty list just ignore
			if len(value) == 0:
				continue

			# recursively process each item in the list that is a dict or list
			for item in value:
				if isinstance(item, dict) or isinstance(item, list):
					config_scanner(item)

			# pass the modify_json and the current list to the modify_scanner
			modify_scanner(modify_json,value)

		# everything else should be a simple key/value pair that we pass to
		# the update_worker where we handle the update section for each
		# dictionary defined in the modify_json file
		else:
			update_worker(key, value)

#------------------------------------------------------------------------------
# The modify_scanner is called for every list we find in the config_json. It
# recursively walks the modify_json looking for instances where the config_json
# and modify_json stacks match. When found we then search the config_json for
# the 'find' items listed in the modify_json. If those are found we apply
# the 'change' items from the modify_json to the corresponding fields
# in config_json.
def modify_scanner(argjson,config_chunk):
	# process everything in the argumented json
	for key, value in list(argjson.items()):

		# for dictionaries we put the key on the modify stack and process recursively
		if isinstance(value, dict):
			modify_stack.append(key)
			modify_scanner(value,config_chunk)
			modify_stack.pop()

		# we have a list so first make sure the config_stack and modify_stack are in sync
		elif isinstance(value, list):
			# make sure they are the same size
			if len(config_stack) != len(modify_stack):
				continue

			# make sure each level of recursion matches
			section_name = class_name
			match_count = 0
			for x in range(0,len(config_stack)):
				if (config_stack[x] == modify_stack[x]):
					section_name += "-->"
					section_name += config_stack[x]
					match_count += 1
			if (match_count != len(modify_stack)):
				continue

			# the config and modify dictionary stacks match so now walk the list of find/change records
			for modrec in range(len(argjson['list'])):

				# walk the config_chunk list
				for cfgrec in range(len(config_chunk)):

					# for each modify record make sure each find record exists in the target config_chunk
					found_count = 0
 					for find_key,find_val in list(argjson['list'][modrec]['find'].items()):
						search = config_chunk[cfgrec].get(find_key)
						if search != None and search == find_val:
							logging.info("FOUND %s=%s IN %s config_chunk[%d]", find_key, find_val, section_name, cfgrec)
							found_count +=1

					# if we found all of the find records then update the config_chunk with the change records
					if found_count == len(argjson['list'][modrec]['find']):
						for edit_key,edit_val in list(argjson['list'][modrec]['change'].items()):
							logging.info("CHANGE KEY:%s VAL:%s", edit_key, edit_val)
							fixer = {}
							fixer[edit_key] = edit_val
							config_chunk[cfgrec].update(fixer)

#------------------------------------------------------------------------------
# The insert_worker is called for each dictionary we encounter in config_json
# following the recursive procesing. We look for an applicable insert
# dictionary in the modify_json by following the config stack where the
# passed dictionary is located. If found, we append the insert records
# from the modify_json to the passed dictionary.
def insert_worker(config_chunk):
	# start with the modify_json
	find_stack = modify_json

	# walk the config stack looking for matching modify dictionaries
	for item in config_stack:
		if find_stack != None:
			find_stack = find_stack.get(item)

	# if there is no modify dictionary we are done
	if find_stack == None:
		return

	# if there is no insert section in the modify dictionary we are done
	insert_dict = find_stack.get("insert")
	if insert_dict == None:
		return

	# create section_name for logging messages
	section_name = class_name
	for item in config_stack:
		section_name += "-->"
		section_name += item

	# add each of the insert records to the passed dictionary
	for item in insert_dict['list']:
		logging.info("INSERT %s %s", section_name, item)
		config_chunk['list'].append(item)

#------------------------------------------------------------------------------
# The update_worker is called for every dictionary key/value pair. We first
# look for an applicable update dictionary in the modify_json by following the
# config_stack where the key/value pair is located. If we find an update
# dictionary and it contains a key/value pair for the argumented key, we find
# and modify the key/value pair in the corresponding config_json dictionary.
def update_worker(key, value):
	# start with the modify_json
	find_stack = modify_json
	section_name = class_name

	# walk the config stack looking for matching modify dictionaries
	for item in config_stack:
		if find_stack != None:
			find_stack = find_stack.get(item)
			section_name += "-->"
			section_name += item

	# if there is no modify dictionary we are done
	if find_stack == None:
		return

	# if there is no update section in the modify dictionary we are done
	update_dict = find_stack.get("update")
	if update_dict == None:
		return

	# look for an update key that matches the argumented config key
	# and if not found we are done
	update_value = update_dict.get(key)
	if update_value == None:
		return

	# we have an update key/value pair for the argumented key/value pair
	# so now we have to find the corresponding dictionary in config_json
	local_stack = config_json
	for item in config_stack:
		if local_stack != None:
			local_stack = local_stack.get(item)

	# this should never happen since we were called for the key/value pair
	# we are now searching for but we check just to be safe
	if local_stack == None:
		return

	# this should also never happen but we check just to be safe
	local_value = local_stack.get(key)
	if local_value == None:
		return

	# update the key/value pair in the config dictionary
	fixer = {}
	fixer[key] = update_value
	local_stack.update(fixer)

	logging.info("UPDATE SECTION:%s KEY:%s OLD:%s NEW:%s", section_name, key, local_value, update_value)

#------------------------------------------------------------------------------
# SCRIPT EXECUTION BEGINS HERE
#------------------------------------------------------------------------------

# set the logging configuration
logging.basicConfig(format='%(asctime)s %(levelname)s %(message)s', datefmt='%m/%d/%Y %H:%M:%S', level=logging_level, filename='/tmp/oem-settings-override.log')

# check for the minimum number of arguments which is five since
# the script name is zero and we need the next four
if len(sys.argv) < 5:
	print("  ERROR: This script requires four arguments")
	print("  USAGE: %s input_settings_file.js override_file.js SettingsClass output_settings_file.js" % sys.argv[0])
	exit(1)

# look for the debug argument and set the flag if found
if len(sys.argv) > 5:
	if sys.argv[5].lower() == "debug":
		debug_flag = True

# get a timestamp we can use for the start/end log messages
jobtime = time.time()

# save the settings class name
class_name = sys.argv[3]

# load the override file
try:
	modify_file = open(sys.argv[2], "r")
	modify_data = json.load(modify_file)
	modify_file.close()
except Exception as exn:
	print("Unable to read override file: %s { %s }" % (sys.argv[2], exn))
	exit(3)

# look for the modify section that matches the settings class name
if class_name in list(modify_data.keys()):
	modify_json = modify_data[class_name]
else:
	print("No settings overrides for class = %s" % class_name)
	exit(2)

# load the input settings file
try:
	config_file = open(sys.argv[1], "r")
	config_json = json.load(config_file)
	config_file.close()
except Exception as exn:
	print("Unable to read input file: %s { %s }" % (sys.argv[1], exn))
	exit(4)

# If the debug_flag is set create /tmp/oem_pristine.js before applying
# the overrides. This is useful for comparing the original and modified
# settings to see the changes actually made, since Python and Java don't
# write the JSON in the same order which makes using diff challenging.
if debug_flag:
	target_file = open("/tmp/oem_pristine.js", "w")
	json.dump(config_json,target_file,indent=4,sort_keys=True)
	target_file.close()

logging.info("========== SETTINGS OVERRIDE %d STARTING ==========", jobtime)
logging.info("= INPUT: %s", sys.argv[1])
logging.info("= OUTPUT: %s", sys.argv[4])
logging.info("= OVERRIDE: %s", sys.argv[2])
logging.info("= CLASS: %s", sys.argv[3])
logging.info("= DEBUG: %s", debug_flag)

# process the settings
config_scanner(config_json)

# save the modified settings to the output file
try:
	target_file = open(sys.argv[4], "w")
	json.dump(config_json,target_file,indent=4,sort_keys=True)
	target_file.close()
except Exception as exn:
	logging.error("Unable to write output file: %s { %s }", (sys.argv[4], exn))
	logging.warning("========== SETTINGS OVERRIDE %d FAILED ==========\n", jobtime)
	exit(5)

logging.info("========== SETTINGS OVERRIDE %d FINISHED ==========\n", jobtime)
exit(0)
