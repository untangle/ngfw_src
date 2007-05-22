#! /usr/bin/ruby

def usage
  puts "Usage: #{$0} <directory> <numberToKeep> (delete|move|nothing)"
  exit 1
end

class Package

  attr_accessor :fullPath, :baseDir, :fileName, :name, :version

  def initialize(fullPath)
    @fullPath = fullPath
    @baseDir = File.dirname(fullPath)
    @fileName = File.basename(fullPath)
    @name, @version = @fileName.split(/_/)[0..1]
  end

end

def isVersionHigher?(v1, v2)
  return 0 if v1 == v2

  system "dpkg --compare-versions #{v1} gt #{v2}"
  if $? == 0
    return 1
  else
    return -1
  end
end

def findPackages(baseDir)
  pkgs = {}

  for path in `find #{baseDir} -name "*.deb"`
    pkg = Package.new(path.chomp)
    if pkgs.key?(pkg.name)
      pkgs[pkg.name].push(pkg)
    else
      pkgs[pkg.name] = [ pkg ]
    end
  end
  pkgs.each { |name, all|
    all.sort! { |x,y| isVersionHigher?(x.version,y.version) }
  }
  return pkgs
end

# main
if ARGV.length != 3
  usage
end

baseDir, countToKeep, method = ARGV
countToKeep = countToKeep.to_i

if method == "move"
  system "mkdir -p /pkgbackup"
end

pkgs = findPackages(baseDir)

# pkgs.each { |pkg, all|
#   print "#{pkg} ->"
#   all.each { |p|
#     print " #{p.version},"
#   }
#   print "\n"
# }

pkgs.each { |pkg, version|
  toRemove = version[0...-countToKeep]

  if toRemove
    toRemove.each { |x|
      if method == "nothing"
        puts "rm #{x.fullPath}"
      elsif method == "move"
        puts "mv #{x.fullPath} /pkgbackup"
        system "mv -f #{x.fullPath} /pkgbackup"
      elsif method == "delete"
        puts "rm #{x.fullPath}"
        system "rm -f #{x.fullPath}"
      end
    }
  end
}

