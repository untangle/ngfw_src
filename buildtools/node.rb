# -*-ruby-*-

class NodeBuilder

  def NodeBuilder.makeNode(buildEnv, name, location, depsImpl = [],
                           depsApi = [], baseHash = {})
    makePackage(buildEnv, name, location, depsImpl, depsApi, baseHash)
  end

  def NodeBuilder.makeCasing(buildEnv, name, location, depsImpl = [],
                             depsApi = [], baseHash = {})
    makePackage(buildEnv, name, location, depsImpl, depsApi, baseHash)
  end

  def NodeBuilder.makeBase(buildEnv, name, location, depsImpl = [],
                           depsApi = [], baseHash = {})
    makePackage(buildEnv, name, location, depsImpl, depsApi, baseHash)
  end

  private
  ## Create the necessary packages and targets to build a node
  def NodeBuilder.makePackage(buildEnv, name, location, depsImpl = [],
                              depsApi = [], baseHash = {})
    home = buildEnv.home

    uvm_lib = BuildEnv::SRC['untangle-libuvm']
    dirName = location
    node = buildEnv["#{name}"]
    buildEnv.installTarget.register_dependency(node)
    buildEnv['node'].registerTarget("#{name}", node)

    apiJar = nil

    ## If there is a local API, build it first
    api = FileList["#{home}/#{dirName}/api/**/*.java"]
    baseHash.each_pair do |bd, bn|
      api += FileList["#{bn.buildEnv.home}/#{bd}/api/**/*.java"]
    end

    if (api.length > 0)
      deps  = baseJarsApi + depsApi

      paths = baseHash.map { |bd, bn| ["#{bn.buildEnv.home}/#{bd}/api",
          "#{bn.buildEnv.home}/#{bd}/api"] }.flatten

      apiJar = JarTarget.build_target(node, deps, 'api',
                                          ["#{home}/#{dirName}/api"] + paths)
      buildEnv.installTarget.install_jars(apiJar, "#{node.distDirectory}/usr/share/untangle/toolbox")
    end

    ## Build the impl jar.
    deps = baseJarsImpl + depsImpl

    ## Make the impl dependent on the api if a jar exists.
    directories= ["#{home}/#{dirName}/impl"]
    if (apiJar.nil?)
      ## Only include the API if the localJarApi doesn't exist
      directories << "#{home}/#{dirName}/api"
      baseHash.each_pair { |bd, bn| directories << "#{bn.buildEnv.home}/#{bd}/api" }
    else
      deps << apiJar
    end

    baseHash.each_pair { |bd, bn| directories << "#{bn.buildEnv.home}/#{bd}/impl" }

    jt = JarTarget.build_target(node, deps, "impl", directories)

    po_dir = "#{home}/#{dirName}/po"
    if File.exist? po_dir
      JavaMsgFmtTarget.make_po_targets(node, po_dir,
                                       "#{node.distDirectory}/usr/share/untangle/lang/",
                                       name).each do |t|
        buildEnv.i18nTarget.register_dependency(t)
      end
    end

    buildEnv.installTarget.install_jars(jt, "#{node.distDirectory}/usr/share/untangle/toolbox", nil, true)

    hierFiles = FileList["#{home}/#{dirName}/hier/**/*"]
    if (0 < hierFiles.length)
      ms = MoveSpec.new("#{home}/#{dirName}/hier", hierFiles, node.distDirectory)
      cf = CopyFiles.new(node, ms, 'hier', buildEnv.filterset)
      buildEnv.hierTarget.register_dependency(cf)
    end

    FileList["#{home}/#{dirName}/**/*.js"].each do |f|
      jsl = JsLintTarget.new(f)
      buildEnv.jsLintTarget.register_dependency(jsl)
    end
  end

  ## Helper to retrieve the standard dependencies for an impl
  def NodeBuilder.baseJarsImpl
    uvm_lib = BuildEnv::SRC['untangle-libuvm']
    Jars::Base + [Jars::JFreeChart, uvm_lib['api']]
  end

  ## Helper to retrieve the standard dependencies for local API
  def NodeBuilder.baseJarsApi
    ## See no reason to use a different set of jars
    baseJarsImpl
  end
end
