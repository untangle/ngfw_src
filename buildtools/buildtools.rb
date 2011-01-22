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

## This is how you define where the stamp file will go
module Rake
  StampFile = "./taskstamps.txt"
end

# Certified Filthy2008
ENV["JAVA_HOME"] = "/usr/lib/jvm/java-6-sun"

POTENTIAL_SRC_HOMES = [ ENV['SRC_HOME'], '../../work/src', '../../src' ]
POTENTIAL_SRC_HOMES << '.' unless `pwd` =~ /hades/
SRC_HOME = POTENTIAL_SRC_HOMES.compact.find do |d|
  File.exist?(d)
end
puts "SRC_HOME = #{SRC_HOME}"

# FIXME: ugly, but will do for now
$DevelBuild = ARGV.grep(/install/).empty?
puts "DevelBuild = #{$DevelBuild}"

require "./buildtools/stamp-task.rb"
require "./buildtools/rake-util.rb"
require "./buildtools/target.rb"
require "./buildtools/jars.rb"
require "./buildtools/c-compiler.rb"
require "./buildtools/node.rb"
