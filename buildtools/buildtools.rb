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

# XXX Move this into main rakefile
if File.exist?('./downloads') and not $CleanBuild
  Kernel.system("make -C ./downloads")
end

require "./buildtools/stamp-task.rb"
require "./buildtools/rake-util.rb"
require "./buildtools/jars.rb"
require "./buildtools/c-compiler.rb"
require "./buildtools/jasper.rb"
require "./buildtools/node.rb"
