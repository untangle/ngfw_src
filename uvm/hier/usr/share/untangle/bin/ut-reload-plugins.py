#!@PREFIX@/usr/share/untangle/bin/ut-pycli -f 

# Reloads any plugins

import sys

pluginManager = uvm.pluginManager()

pluginManager.loadPlugins()

