# -*-ruby-*-
# $HeadURL$
# Copyright (c) 2003-2007 Untangle, Inc.
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
#

# Robert Scott <rbscott@untangle.com>
# Aaron Read <amread@untangle.com>

## Require all of the sub packages.
## Done manually because order matters.
## XXX Could create a new helper method that sets a prefix directory before
## calling require and then unsets it afterwards.
require "./libmvutil/package.rb"
require "./libnetcap/package.rb"
require "./libvector/package.rb"
require "./jmvutil/package.rb"
require "./jnetcap/package.rb"
require "./jvector/package.rb"
require "./nfutil/package.rb"
require "./uvm/package.rb"
require "./uvm-lib/package.rb"
require "./gui/package.rb"

require "./test/package.rb"
require "./reporting/package.rb"
require "./ftp-casing/package.rb"
require "./http-casing/package.rb"
require "./mail-casing/package.rb"
require "./spyware/package.rb"
require "./router/package.rb"

require "./shield/package.rb"
require "./firewall/package.rb"
require "./openvpn/package.rb"
require "./protofilter/package.rb"
require "./sigma/package.rb"
require "./webfilter/package.rb"
require "./ips/package.rb"

## Base Nodes
require "./spam-base/package.rb"
require "./virus-base/package.rb"
require "./clam-base/package.rb"

## SPAM based nodes
require "./phish/package.rb"
require "./spamassassin/package.rb"

## Virus based nodes
require "./clam/package.rb"

## Other packages
require "./util/package.rb"

libuvmcore_so = "#{BuildEnv::SRC.staging}/libuvmcore.so"

archives = ['libmvutil', 'libnetcap', 'libvector', 'jmvutil', 'jnetcap', 'jvector']

## Make the so dependent on each archive
archives.each do |n|
  file libuvmcore_so => BuildEnv::SRC[n]['archive']
end

file libuvmcore_so do
  compilerEnv = CCompilerEnv.new( { "flags" => "-pthread #{CCompilerEnv.defaultDebugFlags}" } )
  archivesFiles = archives.map { |n| BuildEnv::SRC[n]['archive'].filename }

  CBuilder.new(BuildEnv::SRC, compilerEnv).makeSharedLibrary(archivesFiles, libuvmcore_so, [],
                                                             ['xml2', 'sysfs', 'netfilter_queue'], ['ipq'])
end

BuildEnv::SRC['untangle-libuvm']['impl'].register_dependency(libuvmcore_so)

BuildEnv::SRC.installTarget.install_files(libuvmcore_so, "#{BuildEnv::SRC['untangle-libuvmcore'].distDirectory}/usr/lib/uvm")

# DO IT!
#graphViz('foo.dot')
