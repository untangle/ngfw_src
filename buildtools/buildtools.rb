# -*-ruby-*-
# $HeadURL: svn://chef/work/src/buildtools/untangle-core.rb $
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

# Certified Filthy2008
ENV["JAVA_HOME"] = "/usr/lib/jvm/java-6-sun"

Kernel.system("pwd")
Kernel.system("ls ../../work/src")
SRC_HOME = [ ENV['SRC_HOME'], '../../work/src', '.' ].compact.find do |d|
  File.exist?(d)
end
puts "SRC_HOME = #{SRC_HOME}"

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

# XXX Move this into main rakefile
if File.exist?('./downloads') and not $CleanBuild
  Kernel.system("make -C ./downloads")
end

require "./buildtools/stamp-task.rb"
require "./buildtools/rake-util.rb"
require "./buildtools/target.rb"
require "./buildtools/jars.rb"
require "./buildtools/c-compiler.rb"
require "./buildtools/node.rb"

if SRC_HOME.nil?
  uvm_lib = BuildEnv::SRC['untangle-libuvm']
  ['bootstrap', 'api', 'localapi', 'impl' ].each do |n|
    InstalledJar.get(uvm_lib, "/usr/share/untangle/lib/untangle-libuvm-#{n}/")
  end

  InstalledJar.get(uvm_lib, "/usr/share/java/uvm/untangle-libuvm-taglib.jar")

  buildutil = BuildEnv::SRC['untangle-buildutil']
  ['impl'].each do |n|
    InstalledJar.get(buildutil, "/usr/share/untangle/lib/untangle-buildutil-#{n}.jar")
  end

  [ 'mail', 'ftp', 'http' ].each do |c|
    p =  BuildEnv::SRC["untangle-casing-#{c}"]
    ['localapi'].each do |n|
      InstalledJar.get(p, "/usr/share/untangle/toolbox/untangle-casing-#{c}-#{n}.jar")
    end
  end

  [ 'virus', 'webfilter' ].each do |c|
    p =  BuildEnv::SRC["untangle-base-#{c}"]
    ['impl'].each do |n|
      InstalledJar.get(p, "/usr/share/untangle/toolbox/untangle-base-#{c}-#{n}/")
    end
  end
else
  require "./buildtools/untangle-core.rb"
end
