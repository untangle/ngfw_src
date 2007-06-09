# -*-ruby-*-

require 'find'
require 'ftools'
require 'set'
require 'tempfile'

def ensureDirectory(t)
  mkdir_p t unless File.exist?(t)
end

## This is overly complicated
class DebugLevel
  include Comparable
  @@cache =  {}

  def initialize(level, name)
    @level = level
    @name  = name

    @@cache[name] = self
  end

  DEBUG = DebugLevel.new(10, 'debug')
  INFO = DebugLevel.new(5, 'info')
  WARN = DebugLevel.new(3, 'warn')
  DEFAULT = INFO

  def <=>(other)
    self.level <=> other.level
  end

  def DebugLevel.[](name)
    return DEFAULT if @@cache[name] == nil
    @@cache[name]
  end

  attr_reader :level, :name
  attr_writer :level, :name
end

module Debug
  Level=DebugLevel[ENV['RAKE_DEBUG_LEVEL']]

  def isDebugEnabled
    Level >= DebugLevel::DEBUG
  end

  def debug(msg)
    puts msg if Level >= DebugLevel::DEBUG
  end

  def isInfoEnabled
    Level >= DebugLevel::Info
  end

  def info(msg)
    puts msg if Level >= DebugLevel::INFO
  end

  def isWarnEnabled
    Level >= DebugLevel::WARN
  end

  def warn(msg)
    puts msg if Level >= DebugLevel::WARN
  end

  def error(msg)
    puts msg
  end
end

include Debug

# XXX make this class immutable
class BuildEnv
  JAVA_HOME = '/usr/lib/jvm/java-1.5.0-sun'
  THIRD_PARTY_JAR = 'usr/share/java/uvm'

  # XXX XXX should these live here???
  DOWNLOADS = "#{SRC_HOME}/downloads/output"
  SERVLET_COMMON = "#{SRC_HOME}/servlet/common"

  attr_reader :home, :prefix, :staging, :devel, :deb, :isDevel, :grabbag, :downloads, :servletcommon, :include, :installTarget
  attr_writer :prefix, :target, :isDevel

  def initialize(home, name)
    @home = home

    ## Prefix is the value used in substitutions
    @prefix = $DevelBuild ? File.expand_path("#{home}/dist") : '';

    ## Flag indicating whether or not this is a development or
    ## package/production build.
    @isDevel = $DevelBuild

    ## Devel is the development environment
    @devel   =  File.expand_path("#{SRC_HOME}/dist")

    ## This is the staging ground for debian packages
    @deb    = "#{home}/debian"

    ## This is the staging area for compilation
    @staging = "#{home}/staging"

    ## This is an directory where all of the includes are stored
    @include = "#{@staging}/include"

    ## Collection of all available jars built so far
    @grabbag = "#{@staging}/grabbag"

    @mutex = Mutex.new
    @cache = {}

    [@devel, @devel, @grabbag].each { |t| ensureDirectory(t) }

    @installTarget = InstallTarget.new(self['install'], [], "#{name}-install")
  end

  def [](name)
    @mutex.synchronize do
      package = @cache[name]
      if (package.nil?)
        package = Package.new(self, name)
        @cache[name] = package
      end
      package
    end
  end

  def filterset
    {
      /@PREFIX@/ => @prefix,
      /@DEFAULT_JAVA_HOME@/ => '/usr/lib/jvm/java-1.5.0-sun',
      /@USR_BIN@/ => "#{@prefix}/usr/bin",
      /@UVM_HOME@/ => "#{@prefix}/usr/share/untangle",
      /@UVM_DUMP@/ => "#{@prefix}/usr/share/untangle/dump",
      /@UVM_CONF@/ => "#{@prefix}/usr/share/untangle/conf",
      /@UVM_LIB@/ => "#{@prefix}/usr/share/untangle/lib",
      /@UVM_LOG@/ => "#{@prefix}/usr/share/untangle/log",
      /@UVM_TOOLBOX@/ => "#{@prefix}/usr/share/untangle/toolbox",
      /@UVM_SCHEMA@/ => "#{@prefix}/usr/share/untangle/schema",
      /@UVM_WEB@/ => "#{@prefix}/usr/share/untangle/web",
      /@UVM_REPORTS@/ => "#{@prefix}/usr/share/untangle/web/reports",
      /@THIRDPARTY_UVM_LIB@/ => "#{@prefix}/usr/share/java/uvm",
      /@THIRDPARTY_TOMCAT_LIB@/ => "#{@prefix}/usr/share/java/tomcat",
      /@THIRDPARTY_REPORTS_LIB@/ => "#{@prefix}/usr/share/java/reports",
      /@ENDORSED_LIB@/ => "#{@prefix}/usr/share/java/endorsed",
      /@FAKE_PKG_LIST@/ => "#{@prefix}/tmp/pkg-list",
      /@SRC_LIB@/ => "#{@prefix}/usr/lib/uvm",
      /@IS_DEVEL@/ => "#{@isDevel}"
    }
  end
end

## XXX This should be initialized lazily so that BuildEnv doesn't have
## to be defined
class JavaCompiler
  JavacCommand = "#{BuildEnv::JAVA_HOME}/bin/javac"
  JarCommand   = "#{BuildEnv::JAVA_HOME}/bin/jar"
  JarSignerCommand = "#{BuildEnv::JAVA_HOME}/bin/jarsigner"
  JavaCommand  = "#{BuildEnv::JAVA_HOME}/bin/java"
  JavahCommand = "#{BuildEnv::JAVA_HOME}/bin/javah"
  KeyToolCommand = "#{BuildEnv::JAVA_HOME}/bin/keytool"

  def JavaCompiler.compile(dstdir, classpath, fileList)
    ## ... could move the tempfile into an open block, it would be a
    ## little cleaner
    files = Tempfile.new("file-list")
    begin
      fileList.each { |f| files.puts(f) }
    ensure
      files.close unless files.nil?
    end

    ensureDirectory dstdir

    cp = classpath.join(":")

    debug "javac classpath: #{cp}"
    info "javac -d #{dstdir}"

    raise "javac failed" unless
      Kernel.system(JavacCommand, "-classpath", cp, "-d", dstdir, "@" + files.path)
  end

  def JavaCompiler.jar(jarTarget)
    src = jarTarget.javacDirectory
    dst = jarTarget.jarFile

    info "Jar #{src} -> #{dst}"
    raise "jar failed" unless  Kernel.system(JarCommand, "cf", dst, "-C", src, ".")
    dst
  end

  def JavaCompiler.unjar(jt, dest)
    ensureDirectory(dest)
    src = File.expand_path(jt.jarFile)

    info "UnJar #{src} -> #{dest}"
    wd = Dir.pwd
    Dir.chdir(dest)
    raise "unjar failed" unless  Kernel.system(JarCommand, "xf", src)
    Dir.chdir(wd)
    dest
  end

  def JavaCompiler.jarSigner(jar)
    ks = ENV['HADES_KEYSTORE']
    if ks.nil?
      ks = "#{BuildEnv::SRC.staging}/keystore"
      a = 'hermes'
      pw = 'hermes'

      unless File.exists?(ks)
        JavaCompiler.selfSignedCert(ks, a, pw)
      end
    else
      a = ENV['HADES_KEY_ALIAS']
      pw = ENV['HADES_KEY_PASS']
    end

    raise "JarSigner failed" unless
      Kernel.system(JarSignerCommand, '-keystore', ks, '-storepass', pw, jar, a)
  end

  def JavaCompiler.selfSignedCert(keystore, aliaz, passwd)
    raise "KeyTool failed" unless
      Kernel.system(KeyToolCommand, '-genkey', '-alias', aliaz,
                    '-keypass', passwd, '-storepass', passwd,
                    '-keystore', keystore, '-dname', 'cn=snakeoil')
  end

  def JavaCompiler.javah(jar, destination, classes)
    ensureDirectory destination
    raise "javah failed" unless
      Kernel.system(JavahCommand, "-d", destination, "-classpath", jar, *classes)
  end

  def JavaCompiler.run(classpath, classname, *args)
    cp = classpath.join(':')
    raise "java #{classname} failed" unless
      Kernel.system(JavaCommand, "-cp", cp, classname, *args)
  end
end

class MoveSpec
  def initialize(dir, pattern, dest = nil, name = nil)
    @dir = dir
    @pattern = pattern
    @dest = dest
    @name = name
  end

  def MoveSpec.fileMove(filename, dest, name = nil)
    return MoveSpec.new(File.dirname(filename), File.basename(filename), dest, name)
  end

  def move_dest(destBase = nil)
    if destBase.nil?
      @dest
    elsif @dest.nil?
      destBase
    else
      File.join(destBase, @dest)
    end
  end

  def each_move(destBase = nil)
    moveDest = move_dest(destBase)

    ## Provide the option of passing in a filelist
    fl = (@pattern.kind_of?(FileList)) ? @pattern : FileList["#{@dir}/#{@pattern}"]

    fl.each do |f|
      if @name.nil?
        f =~ %r|#{@dir}|;
        dest = File.join(moveDest, $')
      else
        dest = File.join(moveDest, @name)
      end
      yield(f, dest)
    end
  end
end

class Package
  attr_reader :name, :buildEnv

  # public methods ------------------------------------------------------------

  def initialize(buildEnv, name)
    @buildEnv = buildEnv;
    @name = name
    @targets = {}
    @mutex = Mutex.new
  end

  def distDirectory()
    @buildEnv.isDevel ? @buildEnv.devel : (@buildEnv.deb + "/" + name)
  end

  def getWebappDir(servletName)
    "#{distDirectory()}/usr/share/untangle/web/#{servletName}"
  end

  def registerTarget(name, target)
    @mutex.synchronize do
      ## Lookup the target in the hash to test
      raise "Target #{name} registered twice" if (@targets[name] != nil)

      ## Make the package depend on the target
      stamptask self => target

      ## Insert the package into the hash
      @targets[name] = target

      debug "Registered target #{name} as '#{target}'"

      ## Return the new target
      target
    end
  end

  def [](name)
    getTarget(name)
  end

  def hasTarget?(name)
    @mutex.synchronize do
      return true if (@targets[name] != nil)
      return false
    end
  end

  def getTarget(name)
    @mutex.synchronize do
      ## This is where the virtual target comes into play!! XXXX ##
      target = @targets[name]
      raise "Target #{@name}['#{name}'] is not registered" if (target == nil)

      target
    end
  end

  def to_s
    "package.#{name}"
  end

  def installThirdpartyJars(jars, type = "uvm")
    targetDir = @buildEnv.getTargetDirectory(self)

    jars.each do |f|
      File.copy(f, @buildEnv.grabbag)
      File.copy(f, "#{targetDir}/#{BuildEnv::THIRD_PARTY_JAR}/#{type}")
    end
  end
end

class Target
  attr_reader :package, :dependencies

  def initialize(package, dependencies = [], targetname = nil)
    @package = package
    dependencies = [dependencies].flatten
    @dependencies = dependencies.uniq.delete_if { |d| d.kind_of? EmptyTarget }

    ## Register the target with the package
    @package.registerTarget(targetname, self) unless targetname.nil?

    ## Build dependencies to the other Targets
    @dependencies.each { |d| stamptask self => d }

    ## Create all of the other dependencies
    makeDependencies

    ## Define the task
    stamptask self do
      build
    end
  end

  ## Return true if this target generates a file This is useful for
  ## targets that have file depenendencies on other files, Such as
  ## jars
  def file?
    false
  end

  ## Name of the file
  def filename
    ""
  end

  def registerDependency(dep)
    stamptask self => dep
  end

  protected #------------------------------------------------------------------

  def makeDependencies
  end

  def build
  end
end

class InstallTarget < Target
  def initialize(package, deps, targetName)
    super(package, deps, targetName)
    @targetName=targetName
  end

  def registerInstallTargets(movespecs)
    [movespecs].flatten.each do |ms|
      ms.each_move do |src, dest|
        unless File.directory?(src)
          registerDependency(dest)

          file dest => src do
            ensureDirectory(File.dirname(dest))
            FileUtils.cp(src, dest)
            if block_given?
              yield dest
            end
          end
        end
      end
    end
  end

  def installDirs(dirnames)
    [dirnames].flatten.each do |f|
      file f do
        ensureDirectory(f)
      end

      stamptask self => f
    end
  end

  def installFiles(filenames, dest, name = nil)
    is = []

    [filenames].flatten.each do |f|
      is << MoveSpec.fileMove(f, dest, name)
    end

    registerInstallTargets(is)
  end

  def installJars(jarTargets, dest, name = nil, sign = false, explode = false)

    if (explode) then
      [jarTargets].flatten.each do |jt|
        d = "#{dest}/#{File.basename(jt.filename, '.jar')}"
        file d => jt.filename do
          JavaCompiler.unjar(jt, d)
        end
        stamptask self => d
      end
    else
      is = []

      [jarTargets].flatten.each do |jt|
        is << MoveSpec.fileMove(jt.filename, dest, name)
      end

      registerInstallTargets(is) do |f|
        if sign
          JavaCompiler.jarSigner(f)
        end
      end
    end
  end

  def to_s
    "install-target:#{@targetName}"
  end
end

BuildEnv::SRC = BuildEnv.new(SRC_HOME, 'src')
%w(uvm untangle-client tran).each do |d|
  BuildEnv::SRC.installTarget.registerDependency(BuildEnv::SRC[d]);
end

class EmptyTarget < Target
  include Singleton
  def initialize
    super(BuildEnv::SRC['none'])
  end

  def to_s
    "the-empty-target"
  end
end

class CopyFiles < Target
  @@ignored_extensions = /(jpe?g|png|gif|exe|ico|lib|jar|sys|bmp|dll)$/

  def initialize(package, moveSpecs, taskName, filterset = nil, destBase = nil)

    @targetName = "copyfiles:#{package.name}-#{taskName}"
    deps = [];

    [moveSpecs].flatten.each do |moveSpec|

      moveSpec.each_move(destBase) do |src, dest|
        if File.symlink?(src)
          deps << dest

          file dest => src do
            ensureDirectory(File.dirname(dest))
            File.symlink(File.readlink(src), dest) if !File.exist?(dest)
          end
        elsif File.directory?(src)
          deps << dest

          file dest => src do
            ensureDirectory(dest)
          end
        else
          deps << dest

          file dest => src do
            FileUtils.mkdir_p(File.dirname(dest))
            if (filterset && ( src.to_s !~ @@ignored_extensions))
              filtercopy(src, dest, filterset)
            else
              FileUtils.cp(src, dest)
            end
            FileUtils.chmod(File.stat(src).mode, dest)
          end
        end
      end
    end

    super(package, deps)
  end

  def to_s
    @targetName
  end

  private

  def filtercopy(src, dest, filterset)
    File.open(dest, 'w') do |d|
      File.open(src, 'r') do |s|
        s.each_line do |l|
          filterset.each_key { |pat| l.gsub!(pat, filterset[pat]) }
          d.puts(l)
        end
      end
    end
  end
end

class ServletBuilder < Target
  JspcClassPath = ['apache-ant-1.6.5/lib/ant.jar'].map { |n|
    "#{BuildEnv::DOWNLOADS}/#{n}"
  } + ["#{BuildEnv::JAVA_HOME}/lib/tools.jar"];

  def initialize(package, pkgname, path, libdeps = [], trandeps = [], ms = [],
                 common = [BuildEnv::SERVLET_COMMON], jsp_list = nil)
    @pkgname = pkgname
    @path = path
    @trandeps = trandeps
    @jsp_list = Set.new(jsp_list)
    name = File.basename(path)
    @destRoot = package.getWebappDir(name)

    suffix = "servlet-#{name}"
    @targetName = "servlet-builder:#{package.name}-#{name}"

    deps = []

    deps << CopyFiles.new(package, MoveSpec.new("#{path}/root/", "**/*",
                                                @destRoot), "#{suffix}-root")

    unless 0 == ms.length
      deps << CopyFiles.new(package, ms, "#{suffix}-ms", nil, @destRoot)
    end

    libMoveSpecs = libdeps.map do |d|
      file = d.filename
      MoveSpec.new(File.dirname(file), File.basename(file),
                   "#{@destRoot}/WEB-INF/lib")
    end

    unless 0 == libMoveSpecs.length
      deps << CopyFiles.new(package, libMoveSpecs, "#{suffix}-libdeps")
    end

    commonMoveSpecs = common.map do |c|
      MoveSpec.new("#{c}/", "**/*", "#{@destRoot}")
    end


    unless 0 == commonMoveSpecs.length
      deps << CopyFiles.new(package, commonMoveSpecs, "#{suffix}-common")
    end
    uvm = BuildEnv::SRC['uvm']

    jardeps = libdeps + @trandeps + Jars::Base + FileList["#{@destRoot}/WEB-INF/lib/*.jar"]
    jardeps << uvm["api"] << uvm["localapi"]

    # XXX make name nil?
    @srcJar = JarTarget.buildTarget(package, jardeps, name, "#{path}/src",
                                    false);
    deps << @srcJar

    super(package, deps, suffix)
  end

  def build
    ensureDirectory("#{@destRoot}/WEB-INF/lib")

    if (@srcJar.file?)
      srcJarName = @srcJar.filename()
      FileUtils.cp(srcJarName, "#{@destRoot}/WEB-INF/lib")
    end

    classroot = File.join(@destRoot, "WEB-INF", "classes")

    webfrag = Tempfile.new("file-list")
    webfrag.close

    uvm = BuildEnv::SRC['uvm']
    cp = @trandeps.map { |j| j.filename } +
      JspcClassPath + Jars::Base.map { |j| j.filename } +
      [uvm["api"], uvm["localapi"]].map { |t| t.filename } +
      Jars::Base.map {|f| f.filename }

    args = ["-s", "-die", "-l", "-v", "-compile", "-d", classroot,
            "-p", @pkgname, "-webinc", webfrag.path, "-source", "1.5",
            "-target", "1.5", "-uriroot", @destRoot]

    Dir.chdir(@destRoot) do |d|
      Find.find('.') do |f|
        if /\.jsp$/ =~ f
          @jsp_list << f
        end
      end
    end

    @jsp_list.map! do |e|
      if /^\.\// =~ e then $' else e end
    end

    args += @jsp_list.to_a

    JavaCompiler.run(cp, "org.apache.jasper.JspC", *args)

    FileList["#{@destRoot}/**/*.java"].each { |f| FileUtils.rm(f) }

    tmp = Tempfile.new("web")
    tmp.close
    webXmlFilename = "#{@destRoot}/WEB-INF/web.xml"
    FileUtils.cp(webXmlFilename, tmp.path)

    repl = File.open(webfrag.path) { |f| f.read }

    File.open(tmp.path) do |tmp|
      File.open(webXmlFilename, 'w') do |webXml|
        tmp.each_line do |l|
          webXml.puts(l.sub(/@JSP_PRE_COMPILED_SERVLETS@/, repl))
        end
      end
    end
  end

  def to_s
    @targetName
  end
end

class JavaCompilerTarget < Target
  def initialize(package, jars, destination, suffix, basepaths)
    @targetName = "javac:#{package.name}-#{suffix}"

    ## Force basepath to be an array
    @basepaths = [basepaths].flatten

    @destination = destination

    ## The com directory is like our source directory.
    ## we don't do any .net or .org stuff here.
    @javaFiles = FileList[@basepaths.map { |basepath| "#{basepath}/com/**/*.java"}]

    @isEmpty = false

    super(package,jars)
  end

  def makeDependencies
    if 0 == @javaFiles.length
      info "#{self} has no input files."
      @isEmpty = true
      return
    end

    @javaFiles.each do |f|
      classFile =  f.gsub(/.*com/,"#{@destination}/com").gsub(/.java$/,".class")
      debug classFile

      ## Make the classfile depend on the source itself
      file classFile => f do
        directory = File.dirname classFile
        ## XXX Hokey way to update the timestamp XXX
        mkdir_p directory if !File.exist?(directory)
        Kernel.system("touch #{classFile}")
      end

      # Make the stamp task
      stamptask self => classFile
    end
  end

  def build
    return if 0 == @javaFiles.length

    cp = []
    jars.each do |d|
      next unless d.kind_of? Target
      cp << d.filename if d.file?
    end

    debug jars
    debug cp
    JavaCompiler.compile(@destination, cp, @javaFiles)
  end

  def jars
    @dependencies
  end

  def to_s
    @targetName
  end

  attr_reader :isEmpty
end

## This is a precompiled third-party JAR
class ThirdpartyJar < Target
  @@package = BuildEnv::SRC['thirdpartyjars']

  def initialize(path)
    @fullpath = "#{path}"
    super(@@package, [], path)
  end

  ## Retrieve a third party jar, returning the cached value if it else
  ## or a new one otherwise
  def ThirdpartyJar.get(path)
    return @@package[path] if (@@package.hasTarget?(path))

    ## Otherwise return a new instances(This will automatically get
    ## registered in package)
    ThirdpartyJar.new(path)
  end

  def makeDependencies()
    stamptask self => @fullpath
  end

  def build()
    debug "Nothing required to build the THIRD_PARTY_JAR #{@fullpath}"
  end

  def file?
    true
  end

  def filename
    @fullpath
  end

  def to_s
    "thirdpartyjar:#{@fullpath}"
  end
end

## This is a JAR that must be built from Java Files
class JarTarget < Target
  def initialize(package, deps, suffix, buildDirectory, registerTarget=true)
    @package = package
    @suffix = suffix
    @targetName = "jar:#{package.name}-#{@suffix}"
    @buildDirectory = buildDirectory

    suffix = nil unless registerTarget
    super(package,deps,suffix)

    file jarFile => self
  end

  def javacDirectory
    @buildDirectory
  end

  def jarFile
    "#{package.buildEnv.grabbag}/#{@package.name}-#{@suffix}.jar"
  end

  def makeDependencies
  end

  def build
    JavaCompiler.jar(self)
  end

  def file?
    true
  end

  def filename
    jarFile
  end

  def to_s
    @targetName
  end

  def JarTarget.buildTarget(package, jars, suffix, basepaths,
                            registerTarget = true)
    buildDirectory = "#{package.buildEnv.staging}/#{package.name}-#{suffix}"

    deps = []

    javaCompiler = buildJavaCompilerTarget(package, jars, buildDirectory,
                                           suffix, basepaths)

    return EmptyTarget.instance if javaCompiler.isEmpty

    ap = basepaths.map do |bp|
      ac = "#{bp}/resources/META-INF/annotated-classes"
      ac if File.exist?(ac)
    end.reject { |e| !e }
    if 0 < ap.length then
      tgt = "#{buildDirectory}/META-INF/annotated-classes"
      file tgt => ap do
        ensureDirectory(File.dirname(tgt))
        File.open(tgt, "w") do |o|
          ap.each do |f|
            File.open(f) do |i|
              i.each_line { |l| o.puts(l) }
            end
          end
        end
      end
      deps += [ file(tgt) ]
    end

    deps += [javaCompiler, buildCopyFilesTargets(package, basepaths, suffix,
                                                 buildDirectory)]

    JarTarget.new(package, deps.flatten, suffix, buildDirectory, registerTarget)
  end

  private
  def JarTarget.buildJavaCompilerTarget(package, jars, destination, suffix,
                                        basepaths)
    JavaCompilerTarget.new(package, jars, destination, suffix, basepaths)
  end

  def JarTarget.buildCopyFilesTargets(package, basepaths, suffix,
                                      buildDirectory)
    moveSpecs = basepaths.map do |path|
      ms = []

      f = FileList.new("#{path}/com/**/*") { |fl| fl.exclude(/.*\.java/) }
      ms << MoveSpec.new("#{path}", f, "")

      f = FileList.new("#{path}/resources/**/*") { |fl| fl.exclude(/(.*\.java)|(META-INF\/annotated-classes)/) }
      ms << MoveSpec.new("#{path}/resources", f, "")
    end

    ## Copy any files in before building the JAR
    CopyFiles.new(package, moveSpecs.flatten, "jar-#{suffix}", nil,
                  buildDirectory)
  end
end

def graphViz(filename)
  i = 0;
  m = {};

  File.open(filename, "w") { |f|
    f.puts <<EOF
digraph packages {
size="1600,1200";
EOF
    Rake.application.tasks.each do |t|
      if t.class != Rake::FileTask
        n = m[t.name]
        if n.nil?
          i = i + 1
          n = i
          m[t.name] = n
        end

        if 0 == t.prerequisites.length
          f.puts("\"#{n}\"")
          #f.puts("\"#{t.name}\"[fontsize=8]")
        else
          t.prerequisites.each do |prereq|
            if Rake.application[prereq].class != Rake::FileTask
              p = m[prereq]
              if p.nil?
                i = i + 1
                p = i
                m[prereq] = p
              end
              #f.puts("\"#{n}\" -> \"#{p}\"")
              f.puts("\"#{t.name}\" -> \"#{prereq}\"[fontsize=8]")
            end
          end
        end
      end
    end

    f.puts "}"
  }
end
