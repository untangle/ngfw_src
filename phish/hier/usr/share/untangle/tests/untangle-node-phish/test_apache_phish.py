# $HeadURL: svn://chef/work/src/buildtools/rake-util.rb $
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

from untangle.ats.apache_setup import ApacheSetup

class TestApachePhish(ApacheSetup):
    @classmethod
    def setup_class(cls):
        ApacheSetup.setup_class.im_func(cls)
        ApacheSetup.install_node.im_func( cls, "untangle-node-phish", delete_existing = False )

        ## Just use the tid for another node, this way you don't need a nonce but can
        ## tell if it is able to access the blockpage servlet.
        tid = cls.node_manager.im_func( cls ).nodeInstances("untangle-node-router")["list"][0]
        cls.router_tid = str( tid["id"] )

    def test_root_access_outside_admin_enable( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : True })

        base = "http%s://localhost%s/phish/blockpage?tid=" + self.router_tid

        ## This should be run as root, so it should access these
        yield self.check_access, base % ( "", "" ), 406, "Feature is not installed"
        yield self.check_access, base % ( "", ":64156" ), 406, "Feature is not installed"
        yield self.check_access, base % ( "s", "" ), 406, "Feature is not installed"
        yield self.check_access, base % ( "s", ":64157" ), 406, "Feature is not installed"

    def test_root_access_outside_admin_disabled( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : False })

        base = "http%s://localhost%s/phish/blockpage?tid=" + self.router_tid

        ## This should be run as root, so it should access these
        yield self.check_access, base % ( "", "" ), 406, "Feature is not installed"
        yield self.check_access, base % ( "", ":64156" ), 406, "Feature is not installed"
        yield self.check_access, base % ( "s", "" ), 406, "Feature is not installed"
        yield self.check_access, base % ( "s", ":64157" ), 406, "Feature is not installed"

    def test_nonroot_access_outside_admin_enable( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : True })

        base = "http%s://192.0.2.43%s/phish/blockpage?tid=" + self.router_tid

        yield self.check_access, base % ( "", "" ), 406, "Feature is not installed"
        yield self.check_access, base % ( "", ":64156" ), 406, "Feature is not installed"
        yield self.check_access, base % ( "s", "" ), 403, "Off-site administration is disabled"
        yield self.check_access, base % ( "s", ":64157" ), 406, "Feature is not installed"

    def test_nonroot_access_outside_admin_disabled( self ):
        self.set_access_settings({ "isOutsideAdministrationEnabled" : False })

        base = "http%s://192.0.2.43%s/phish/blockpage?tid=" + self.router_tid

        ## This should be run as root, so it should access these
        yield self.check_access, base % ( "", "" ), 406, "Feature is not installed"
        yield self.check_access, base % ( "", ":64156" ), 406, "Feature is not installed"
        yield self.check_access, base % ( "s", "" ), 403, "Off-site administration is disabled"
        yield self.check_access, base % ( "s", ":64157" ), 406, "Feature is not installed"
