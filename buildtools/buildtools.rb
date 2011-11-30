# Robert Scott <rbscott@untangle.com>
# Aaron Read <amread@untangle.com>
# Sebastien Delafond <seb@untangle.com>

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
