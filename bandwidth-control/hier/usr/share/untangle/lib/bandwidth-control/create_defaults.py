#!/usr/bin/python3
import simplejson as json
import copy
import os

# This script for maintaining the default (wizard) rulesets
# It parses through defaults_master.json and builds the other default files
# This is only run by the developer when updating the master set of rules

# defaults_master.json is the master file used to create the individual starting configs
# this contains all the possible rules for the wizard default rulesets

# There is a set of "standard rules"
# There is a set of "business rules"
# There is a set of "school rules"
# There is a set of "metered rules"
# There is a set of "home rules"

# Business is standard+business
# Educational is standard+school
# Metered is standard+metered
# Home is standard+home


master_file = open('defaults_master.json').read()
master_json = json.loads(master_file)

ruleset = master_json['rules']['list']
rulesets = {}

for profile in "school","business","metered","home":
    i = 1
    # build ruleset for this name
    rulelist = []

    # add rule add it to the list if its standard or in this one
    for rule in ruleset:
        if profile in rule['set'] or "standard" in rule['set']:
            thisrule = copy.deepcopy(rule)
            thisrule['id'] = i
            del thisrule['set']
            rulelist.append(thisrule)
            i = i+1

    jsonobj = copy.deepcopy(master_json)
    jsonobj['rules']['list'] = rulelist

    print("Writing \"%s\" profile to \"defaults_%s.json\" file" % (profile, profile))
    outfile = open('defaults_%s.json.tmp'%profile,'wb')
    json.dump(jsonobj,outfile)
    outfile.close()
    os.system('python -m simplejson.tool defaults_%s.json.tmp > defaults_%s.json'%(profile,profile))
    os.system('rm -f \'defaults_%s.json.tmp\'' % profile)
