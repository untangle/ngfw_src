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

# Certified Filthy2007
ENV["JAVA_HOME"] = "/usr/lib/jvm/java-1.5.0-sun"

## This is how you define where the stamp file will go
module Rake
  StampFile = "./taskstamps.txt"
end

$DevelBuild = true
$CleanBuild = false
ARGV.each do |arg|
  if arg =~ /install/
    $DevelBuild = false
  end

  if arg =~ /clean/
    $CleanBuild = true
  end
end

require "./buildtools/stamp-task.rb"
require "./buildtools/rake-util.rb"
require "./buildtools/c-compiler.rb"
require "./buildtools/jars.rb"
require "./buildtools/jasper.rb"
require "./buildtools/node.rb"

## Require all of the sub packages.
## Done manually because order matters.
## XXX Could create a new helper method that sets a prefix directory before
## calling require and then unsets it afterwards.
require "./buildtools/package_util.rb"
require "./buildtools/package_libmvutil.rb"
require "./buildtools/package_libnetcap.rb"
require "./buildtools/package_libvector.rb"
require "./buildtools/package_jmvutil.rb"
require "./buildtools/package_jnetcap.rb"
require "./buildtools/package_jvector.rb"
require "./buildtools/package_nfutil.rb"
require "./buildtools/package_uvm.rb"
require "./buildtools/package_uvm-lib.rb"
require "./buildtools/package_gui.rb"

require "./buildtools/package_test.rb"
require "./buildtools/package_reporting.rb"
require "./buildtools/package_ftp-casing.rb"
require "./buildtools/package_http-casing.rb"
require "./buildtools/package_mail-casing.rb"
require "./buildtools/package_spyware.rb"
require "./buildtools/package_router.rb"

require "./buildtools/package_shield.rb"
require "./buildtools/package_firewall.rb"
require "./buildtools/package_openvpn.rb"
require "./buildtools/package_protofilter.rb"
require "./buildtools/package_sigma.rb"
require "./buildtools/package_webfilter.rb"
require "./buildtools/package_ips.rb"

## Base Nodes
require "./buildtools/package_spam-base.rb"
require "./buildtools/package_virus-base.rb"
require "./buildtools/package_clam-base.rb"

## SPAM based nodes
require "./buildtools/package_phish.rb"
require "./buildtools/package_spamassassin.rb"

## Virus based nodes
require "./buildtools/package_clam.rb"

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

BuildEnv::SRC['untangle-libuvm']['impl'].registerDependency(libuvmcore_so)

BuildEnv::SRC.installTarget.installFiles(libuvmcore_so, "#{BuildEnv::SRC['untangle-libuvmcore'].distDirectory}/usr/lib/uvm")

# DO IT!
#graphViz('foo.dot')
