# $HeadURL: https://untangle.svn.beanstalkapp.com/ngfw/work/src/uvm/hier/usr/lib/python2.7/uvm/i18n_helper.py $
# Copyright (c) 2003-2009 Untangle, Inc.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License, version 2,
# as published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful, but
# AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
# NONINFRINGEMENT.  See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.

import gettext
import sys
import os

if "@PREFIX@" != '':
    sys.path.insert(0, '@PREFIX@/usr/lib/python2.7/dist-packages')


def get_uvm_settings_item(a,b):
    return None

try:
    from uvm.settings_reader import get_uvm_settings_item
except ImportError:
    pass

def get_translation(domain):
#    return gettext.translation(domain, fallback=True)
    lang = get_uvm_language()
    return gettext.translation(domain, fallback=True, codeset='utf-8',languages=[lang])
        
def get_uvm_language():
    lang = 'us'

    setval = get_uvm_settings_item('language','language')
    if (setval != None):
        lang = setval

    return lang
