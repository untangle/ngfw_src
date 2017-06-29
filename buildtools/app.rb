# -*-ruby-*-

class AppBuilder

  @uvm_lib = BuildEnv::SRC['untangle-libuvm']

  def AppBuilder.makeApp(buildEnv, name, location, deps = [], baseHash = {})
    makePackage(buildEnv, name, location, deps, baseHash)
  end

  def AppBuilder.makeCasing(buildEnv, name, location, deps = [], baseHash = {})
    makePackage(buildEnv, name, location, deps, baseHash)
  end

  def AppBuilder.makeBase(buildEnv, name, location, deps = [], baseHash = {})
    makePackage(buildEnv, name, location, deps, baseHash)
  end

  private
  ## Create the necessary packages and targets to build a app
  def AppBuilder.makePackage(buildEnv, name, location, deps = [], baseHash = {})
    home = buildEnv.home

    dirName = location
    app = buildEnv["#{name}"]
    buildEnv.installTarget.register_dependency(app)
    buildEnv['app'].registerTarget("#{name}", app)

    ## If there is an SRC, build it first
    src = FileList["#{home}/#{dirName}/src/**/*.java"]
    baseHash.each_pair do |bd, bn|
      src += FileList["#{bn.buildEnv.home}/#{bd}/src/**/*.java"]
    end

    if (src.length > 0)
      deps  = baseJars + deps

      paths = baseHash.map { |bd, bn| ["#{bn.buildEnv.home}/#{bd}/src", "#{bn.buildEnv.home}/#{bd}/src"] }.flatten

      srcJar = JarTarget.build_target(app, deps, 'src', ["#{home}/#{dirName}/src"] + paths)
      buildEnv.installTarget.install_jars(srcJar, "#{app.distDirectory}/usr/share/untangle/lib", nil, true)
    end

    hierFiles = FileList["#{home}/#{dirName}/hier/**/*"]
    if ( hierFiles.length > 0 )
      ms = MoveSpec.new("#{home}/#{dirName}/hier", hierFiles, app.distDirectory)
      cf = CopyFiles.new(app, ms, 'hier', buildEnv.filterset)
      buildEnv.hierTarget.register_dependency(cf)
    end

    # JS files (if needed)
    if File.directory?(File.join(location, "js")) then
      JsBuilder.new(@uvm_lib, location, "#{location}/js", "admin/script/apps")
      JsLintTarget.new(@uvm_lib, "#{location}/js", "jslint-adminui-#{name}")
    end
  end

  ## Helper to retrieve the standard dependencies
  def AppBuilder.baseJars
    Jars::Base + [@uvm_lib['api']]
  end
end
