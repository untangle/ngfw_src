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

## This is how you define where the stamp file will go
module Rake
  StampFile = "#{SRC_HOME}/taskstamps.txt"
end

require "#{SRC_HOME}/buildtools/stamp-task.rb"
require "#{SRC_HOME}/buildtools/rake-util.rb"
require "#{SRC_HOME}/buildtools/c-compiler.rb"
require "#{SRC_HOME}/buildtools/jars.rb"
require "#{SRC_HOME}/buildtools/jasper.rb"
require "#{SRC_HOME}/buildtools/node.rb"

## Require all of the sub packages.
## Done manually because order matters.
## XXX Could create a new helper method that sets a prefix directory before
## calling require and then unsets it afterwards.
require "#{SRC_HOME}/util/package.rb"
require "#{SRC_HOME}/libmvutil/package.rb"
require "#{SRC_HOME}/libnetcap/package.rb"
require "#{SRC_HOME}/libvector/package.rb"
require "#{SRC_HOME}/jmvutil/package.rb"
require "#{SRC_HOME}/jnetcap/package.rb"
require "#{SRC_HOME}/jvector/package.rb"
require "#{SRC_HOME}/nfutil/package.rb"
require "#{SRC_HOME}/uvm/package.rb"
require "#{SRC_HOME}/gui/package.rb"

require "#{SRC_HOME}/test/package.rb"
require "#{SRC_HOME}/reporting/package.rb"
require "#{SRC_HOME}/ftp-casing/package.rb"
require "#{SRC_HOME}/http-casing/package.rb"
require "#{SRC_HOME}/mail-casing/package.rb"
require "#{SRC_HOME}/spyware/package.rb"
require "#{SRC_HOME}/router/package.rb"

require "#{SRC_HOME}/shield/package.rb"
require "#{SRC_HOME}/firewall/package.rb"
require "#{SRC_HOME}/openvpn/package.rb"
require "#{SRC_HOME}/protofilter/package.rb"
require "#{SRC_HOME}/sigma/package.rb"
require "#{SRC_HOME}/webfilter/package.rb"
require "#{SRC_HOME}/ips/package.rb"

## Base Nodes
require "#{SRC_HOME}/spam-base/package.rb"
require "#{SRC_HOME}/virus-base/package.rb"
require "#{SRC_HOME}/clam-base/package.rb"

## SPAM based nodes
require "#{SRC_HOME}/phish/package.rb"
require "#{SRC_HOME}/spamassassin/package.rb"

## Virus based nodes
require "#{SRC_HOME}/clam/package.rb"

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

BuildEnv::SRC['untangle-vm']['impl'].registerDependency(libuvmcore_so)

BuildEnv::SRC.installTarget.installFiles(libuvmcore_so, "#{BuildEnv::SRC['untangle-libuvmcore'].distDirectory}/usr/lib/uvm")

# DO IT!
#graphViz('foo.dot')
