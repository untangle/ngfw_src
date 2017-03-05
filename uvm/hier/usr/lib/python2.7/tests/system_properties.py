# -*- coding: utf-8 -*-
import os
import sys
import subprocess
import simplejson as json

from uvm import Manager
from uvm import Uvm
import remote_control

uvmContext = Uvm().getUvmContext()
prefix = "@PREFIX@"

def get_prefix():
    global prefix
    return prefix

def get_lan_ip():
    ip = uvmContext.networkManager().getFirstNonWanAddress()
    return ip

def get_http_url():
    ip = uvmContext.networkManager().getFirstNonWanAddress()
    httpPort = str(uvmContext.networkManager().getNetworkSettings().get('httpPort'))
    httpAdminUrl = "http://" + ip + ":" + httpPort + "/"
    return httpAdminUrl

def get_https_url():
    ip = uvmContext.networkManager().getFirstNonWanAddress()
    httpsPort = str(uvmContext.networkManager().getNetworkSettings().get('httpsPort'))
    httpsAdminUrl = "https://" + ip + ":" + httpsPort + "/"
    return httpsAdminUrl

