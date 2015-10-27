# $Id$
import commands
import logging
import mx
import os
import re
import simplejson as json
import shutil
import reports.sql_helper as sql_helper
import string
import sys
import time
import traceback

from mx.DateTime import DateTimeDelta
from psycopg2.extensions import DateFromMx
from psycopg2.extensions import TimestampFromMx
from reports.sql_helper import print_timing
from reports.log import *
logger = getLogger(__name__)

def get_number_wan_interfaces():
    return len(get_wan_clause().split(','))

def get_wan_clause():
    netSettingsJsonObj = json.loads(open('@PREFIX@/usr/share/untangle/settings/untangle-vm/network.js', 'r').read())
    wans = []
    for intf in netSettingsJsonObj['interfaces']['list']:
        if intf['configType'] == 'DISABLED':
            continue
        if intf.get('isWan'):
            wans.append(str(intf['interfaceId']))
    return "(" + ','.join(wans) + ")"

def get_wan_names_map():
    netSettingsJsonObj = json.loads(open('@PREFIX@/usr/share/untangle/settings/untangle-vm/network.js', 'r').read())
    map = {}
    for intf in netSettingsJsonObj['interfaces']['list']:
        if intf.get('isWan'):
            map[int(intf['interfaceId'])] = intf['name']

    return map

def get_wan_ip():
    netSettingsJsonObj = json.loads(open('@PREFIX@/usr/share/untangle/settings/untangle-vm/network.js', 'r').read())
    wans = []
    for intf in netSettingsJsonObj['interfaces']['list']:
        if intf.get('isWan'):
            try:
                STATUS_JSON = json.loads(open('/var/lib/untangle-netd/interface-' + str(intf.get('interfaceId')) + '-status.js').read())
                addr = STATUS_JSON.get('v4Address')
                if addr != None:
                    return addr
                else:
                    return "unknown.ip"
            except Exception,e:
                traceback.print_exc(e)
                return "unknown.ip"
    return "no.wan.found"

class Node:
    def __init__(self, name, title):
        self.__name = name
        self.__display_title = title
        
    def get_report(self):
        return None

    def get_toc_membership(self):
        return []

    def create_tables(self):
        pass

    def reports_cleanup(self, cutoff):
        pass

    @property
    def name(self):
        return self.__name

    @property
    def display_title(self):
        return self.__display_title

    @property
    def view_position(self):
        return self.__view_position

    def parents(self):
        return []

__nodes = {}

def register_node(node):
    global __nodes

    __nodes[node.name] = node

@sql_helper.print_timing
def reports_cleanup(cutoff):
    logger.info("Cleaning reports data for all dates < %s" % (cutoff,))

    for name in __get_available_nodes():
        try:
            node = __nodes.get(name, None)
            logger.info("Cleaning data for %s" % (name,))
            node.reports_cleanup(cutoff)
        except:
            logger.warn('could not cleanup reports for: %s' % name,
                         exc_info=True)

@sql_helper.print_timing
def init_engine(node_module_dir):
    __get_nodes(node_module_dir)

@sql_helper.print_timing
def create_tables():
    global __nodes

    logger.info('create_tables()')
    for name in __get_available_nodes():
        try:
            logger.info('create_tables() for: %s' % (name))
            node = __nodes.get(name, None)

            if not node:
                logger.warn('could not get node %s' % name)
            else:
                node.create_tables()
        except:
            logger.warn('could not setup for: %s' % name, exc_info=True)

def get_node(name):
    return __nodes[name]

def __get_available_nodes():
    global __nodes
    available = set(__nodes.keys());
    list = []

    while len(available):
        name = available.pop()
        __add_node(name, list, available)

    return list;

def __add_node(name, list, available):
    global __nodes

    node = __nodes.get(name, None)
    if not node:
        logger.warn('node not found %s' % name)
    else:
        for p in node.parents():
            if p in available:
                available.remove(p)
                __add_node(p, list, available)
        list.append(name)

def __get_nodes(node_module_dir):
    for f in os.listdir(node_module_dir):
        if f.endswith('py'):
            (m, e) = os.path.splitext(f)
            __import__('reports.node.%s' % m)


