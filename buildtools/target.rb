# -*-ruby-*-
# $HeadURL: svn://chef/work/src/buildtools/rake-util.rb $
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

# Dirk Morris <dmorris@untangle.com>

require 'gettext/utils'

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
    make_dependencies

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

  def register_dependency(dep)
    stamptask self => dep
  end

  protected #------------------------------------------------------------------

  def make_dependencies
  end

  def build
  end
end

class InstallTarget < Target
  def initialize(package, deps, targetName)
    super(package, deps, targetName)
    @targetName=targetName
  end

  def register_install_targets(movespecs)
    [movespecs].flatten.each do |ms|
      ms.each_move do |src, dest|
        unless File.directory?(src)
          register_dependency(dest)

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

  def install_dirs(dirnames)
    [dirnames].flatten.each do |f|
      file f do
        ensureDirectory(f)
      end

      stamptask self => f
    end
  end

  def install_files(filenames, dest, name = nil)
    is = []

    [filenames].flatten.each do |f|
      is << MoveSpec.fileMove(f, dest, name)
    end

    register_install_targets(is)
  end

  def install_jars(jarTargets, dest, name = nil, sign = false, explode = false)

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

      [jarTargets].flatten.compact.each do |jt|
        is << MoveSpec.fileMove(jt.filename, dest, name)
      end

      register_install_targets(is) do |f|
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

## This is a precompiled third-party JAR
class InstalledJar < Target
  def initialize(package, path)
    @fullpath = "#{path}"
    name = File.basename(@fullpath, '.jar').split('-').last
    super(package, [], name)
  end

  ## Retrieve a third party jar, returning the cached value if it else
  ## or a new one otherwise
  def InstalledJar.get(package, path)
    return package[path] if (package.hasTarget?(path))

    ## Otherwise return a new instances(This will automatically get
    ## registered in package)
    InstalledJar.new(package, path)
  end

  def make_dependencies()
    stamptask self => @fullpath
  end

  def build()
    debug "Nothing required to build #{@fullpath}"
  end

  def file?
    true
  end

  def filename
    @fullpath
  end

  def to_s
    "installedjar:#{@fullpath}"
  end
end

unless SRC_HOME.nil?
  BuildEnv::SRC = BuildEnv.new(SRC_HOME, 'src')
else
  BuildEnv::SRC = BuildEnv.new('.', 'src')
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

  def make_dependencies()
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

class EmptyTarget < Target
  include Singleton
  def initialize
    super(BuildEnv::SRC['none'])
  end

  def to_s
    ''
  end
end

task '' # XXX hack attack

class CopyFiles < Target
  @@ignored_extensions = /(jpe?g|png|gif|exe|ico|lib|jar|sys|bmp|dll)$/

  def initialize(package, moveSpecs, taskName, filterset = nil, destBase = nil)

    @targetName = "copyfiles:#{package.name}-#{taskName}"
    deps = [];

    logged = false      
    [moveSpecs].flatten.each do |moveSpec|
      moveSpec.each_move(destBase) do |src, dest|
        if not logged then
          info "[#{taskName.ljust(8)}] #{package.name}"
          logged = true
        end

        if File.symlink?(src)
          deps << dest

          ## Handling symbolic links that don't resolve until in place.
          file dest => src if File.exists?( src )

          file dest do
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
              filter_copy(src, dest, filterset)
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

  def filter_copy(src, dest, filterset)
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
    "#{BuildEnv::downloads}/#{n}"
  } + ["#{BuildEnv::JAVA_HOME}/lib/tools.jar"];

  def initialize(package, pkgname, path, libdeps = [], nodedeps = [], ms = [],
                 common = [BuildEnv::SERVLET_COMMON], jsp_list = nil)
    @pkgname = pkgname
    @path = path
    @nodedeps = nodedeps
    @jsp_list = Set.new(jsp_list)
    name = File.basename(path)
    @destRoot = package.getWebappDir(name)

    suffix = "servlet-#{name}"
    @targetName = "servlet-builder:#{package.name}-#{name}"

    deps = []

    deps << CopyFiles.new(package, MoveSpec.new("#{path}/root/", "**/*",
                                                @destRoot), "#{suffix}-root")

    if File.exist? "#{path}/apache.conf"
      deps << CopyFiles.new(package,
                            MoveSpec.fileMove("#{path}/apache.conf",
                                              "#{package.distDirectory}/usr/share/untangle/apache2/conf.d/",
                                              "#{name}.conf"),
                            "#{suffix}-apache")
    end

    if File.exist? "#{path}/unrestricted.conf"
      deps << CopyFiles.new(package,
                            MoveSpec.fileMove("#{path}/unrestricted.conf",
                                              "#{package.distDirectory}/usr/share/untangle/apache2/unrestricted-conf.d/",
                                              "#{name}.conf"),
                            "#{suffix}-apache")
    end

    unless 0 == ms.length
      deps << CopyFiles.new(package, ms, "#{suffix}-ms", nil, @destRoot)
    end

    libMoveSpecs = libdeps.compact.map do |d|
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
    uvm_lib = BuildEnv::SRC['untangle-libuvm']

    jardeps = libdeps + @nodedeps + Jars::Base + FileList["#{@destRoot}/WEB-INF/lib/*.jar"]
    jardeps << uvm_lib["api"] << uvm_lib["localapi"]

    @srcJar = JarTarget.build_target(package, jardeps, name, "#{path}/src",
                                     false)
    deps << @srcJar

    po_dir = "#{path}/po"
    if File.exist? po_dir
      JavaMsgFmtTarget.make_po_targets(package, po_dir,
                                       @srcJar.javac_dir,
                                       "#{pkgname}.Messages").each do |t|
        @srcJar.register_dependency(t)
      end
    end

    script_dir = "#{path}/root/script"
    if File.exist? script_dir
      YuiCompressorTarget.make_min_targets(package, script_dir,
                                           "#{@destRoot}/script",
                                           "js").each do |t|
        @srcJar.register_dependency(t)
      end
    end

    super(package, deps + jardeps, suffix)
  end

  def build
    ensureDirectory("#{@destRoot}/WEB-INF/lib")

    if (@srcJar.file?)
      srcJarName = @srcJar.filename()
      if File.exist? srcJarName
        FileUtils.cp(srcJarName, "#{@destRoot}/WEB-INF/lib")
      end
    end

    classroot = File.join(@destRoot, "WEB-INF", "classes")

    webfrag = Tempfile.new("file-list")
    webfrag.close

    uvm_lib = BuildEnv::SRC['untangle-libuvm']
    cp = @nodedeps.map { |j| j.filename }
    cp += JspcClassPath
    cp += Jars::Base.map { |j| j.filename }
    cp += [uvm_lib["api"], uvm_lib["localapi"]].map { |t| t.filename }
    cp += Jars::Base.map {|f| f.filename }
    cp += [SRC_HOME+"/buildtools"] unless SRC_HOME.nil?

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

    if @jsp_list.empty?
      debug( "Empty JSP file list." )
      return
    end

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
          webXml.puts(l.sub(/@JSP_PRE_COMPILED_SERVLETS@/, repl).sub(/@BUILD_STAMP@/, Time.now.to_i.to_s))
        end
      end
    end
  end

  def to_s
    @targetName
  end
end

class JsLintTarget
  JS_LINT_COMMAND = "/usr/bin/jslint"
  
  attr_reader :filename
  
  def initialize(package, filename)
    @filename = filename
    task self do
      build
    end
  end

  def to_s
    "jslint:#{@filename}"
  end

  protected

  def build()
    info "[jslint  ] #{@filename}"
    Kernel.system(JS_LINT_COMMAND, @filename)
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

  def make_dependencies
    if 0 == @javaFiles.length
      ## info "#{self} has no input files."
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

class JavaMsgFmtTarget < Target
  def initialize(src, package, lang, po_file, dest, basename)
    @po_file = po_file
    @dest = dest
    @basename = basename
    @lang = lang

    @filename = "#{dest}/#{@basename.gsub(/-/, '_')}_#{@lang}.class"

    @src = src
    @mo_dest = "#{package.distDirectory()}/usr/share/locale"
    
    super(package, [@po_file], @filename)
  end

  def JavaMsgFmtTarget.make_po_targets(package, src, dest, basename)
    ts = []

    Dir.new(src).select { |f| not f =~ /^\./ and File.directory?("#{src}/#{f}") }.each do |dir|
      Dir.new("#{src}/#{dir}").select { |f| /\.po$/ =~ f }.each do |f|
        ts << JavaMsgFmtTarget.new(src, package, dir, "#{src}/#{dir}/#{f}", dest, basename)
      end
    end

    ts
  end

  def file?
    true
  end

  def filename
    @filename
  end

  def to_s
    "msgfmt:#{@filename}"
  end

  protected

  def build()
    ensureDirectory @dest

    # ignore output of create_mofiles
    $stderr2 = $stderr.clone
    $stderr.reopen('/dev/null')

    GetText::create_mofiles(false, @src, @mo_dest)

    # reenable stderr
    $stderr.reopen($stderr2)

    info "[msgfmt  ] #{@po_file} => #{@dest}official/#{@lang}/#{@basename}.mo"

    raise "msgfmt failed" unless
      Kernel.system <<CMD
msgfmt --java2 -d #{@dest} -r "i18n.official.#{@basename.gsub('-', '_')}" -l #{@lang} #{@po_file}
CMD

    ensureDirectory "#{@dest}/official/#{@lang}"
    raise "msgfmt failed" unless
      Kernel.system <<CMD
msgfmt -o #{@dest}official/#{@lang}/#{@basename}.mo #{@po_file}
CMD

  end
end

# Compresses js and css files
class YuiCompressorTarget < Target
  def initialize(package, script_file, src, dest, type)
    @script_file = "#{src}/#{script_file}"
    @dest = dest
    @type = type

    @filename = "#{dest}/#{script_file.sub(/\.#{type}/, '-min.'+type)}"

    super(package, [@script_file], @filename)
  end

  def YuiCompressorTarget.make_min_targets(package, src, dest, type)
    ts = []
    YuiCompressorTarget.listFiles(src, type).each do |f|
      ts << YuiCompressorTarget.new(package, "#{f}", src, dest, type)
    end

    ts
  end

  def file?
    true
  end

  def filename
    @filename
  end

  def to_s
    "yui-compressor:#{@filename}"
  end

  protected

  def build()
    info "[compress] #{@filename}"

    ensureDirectory(File.dirname(@filename))
    args = [@script_file, "--type", @type, "-o", @filename]
    yuiCompressorJar = Jars.downloadTarget('yuicompressor-2.4.2/yuicompressor-2.4.2/build/yuicompressor-2.4.2.jar').filename
    raise "YUI compress failed" unless
      JavaCompiler.runJar([], yuiCompressorJar, *args )
  end

  private

  def YuiCompressorTarget.listFiles(src, type, relative_path = nil)
    files = []
    Dir.new("#{src}").each do |f|
      
      if not f =~ /^\./ and File.directory?("#{src}/#{f}")
        files = files + YuiCompressorTarget.listFiles("#{src}/#{f}", type, (relative_path ? "#{relative_path}/#{f}" : f))
        next
      end

      file_path = "#{src}/#{f}"
      if ( /\.#{type}$/ =~ f )
        files << (relative_path ? "#{relative_path}/#{f}" : f) 
      end
    end
    files
  end

end

## This is a JAR that must be built from Java Files
class JarTarget < Target
  def initialize(package, deps, suffix, build_dir, registerTarget=true)
    @package = package
    @suffix = suffix
    @targetName = "jar:#{package.name}-#{@suffix}"
    @build_dir = build_dir

    suffix = nil unless registerTarget
    super(package, deps, suffix)

    file jar_file => self
  end

  def javac_dir
    @build_dir
  end

  def jar_file
    "#{package.buildEnv.grabbag}/#{@package.name}-#{@suffix}.jar"
  end

  def make_dependencies
  end

  def build
    JavaCompiler.jar(self)
  end

  def file?
    true
  end

  def filename
    jar_file
  end

  def to_s
    @targetName
  end

  def JarTarget.build_target(package, jars, suffix, basepaths,
                             registerTarget = true)
    build_dir = "#{package.buildEnv.staging}/#{package.name}-#{suffix}"

    deps = []

    jc = buildJavaCompilerTarget(package, jars, build_dir,
                                 suffix, basepaths)

    #return EmptyTarget.instance if javaCompiler.isEmpty

    ap = basepaths.map do |bp|
      ac = "#{bp}/resources/META-INF/annotated-classes"
      ac if File.exist?(ac)
    end.reject { |e| !e }
    if 0 < ap.length then
      tgt = "#{build_dir}/META-INF/annotated-classes"
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

    deps += [jc, buildCopyFilesTargets(package, basepaths, suffix,
                                       build_dir)]

    JarTarget.new(package, deps.flatten, suffix, build_dir, registerTarget)
  end

  private

  def JarTarget.buildJavaCompilerTarget(package, jars, destination, suffix,
                                        basepaths)
    JavaCompilerTarget.new(package, jars, destination, suffix, basepaths)
  end

  def JarTarget.buildCopyFilesTargets(package, basepaths, suffix,
                                      build_dir)
    moveSpecs = basepaths.map do |path|
      ms = []

      f = FileList.new("#{path}/com/**/*") { |fl| fl.exclude(/.*\.java/) }
      ms << MoveSpec.new("#{path}", f, "")

      f = FileList.new("#{path}/resources/**/*") { |fl| fl.exclude(/(.*\.java)|(META-INF\/annotated-classes)/) }
      ms << MoveSpec.new("#{path}/resources", f, "")
    end

    ## Copy any files in before building the JAR
    CopyFiles.new(package, moveSpecs.flatten, "jar-#{suffix}", nil,
                  build_dir)
  end
end
