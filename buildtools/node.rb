# -*-ruby-*-

class NodeBuilder

  def NodeBuilder.makeNode(buildEnv, name, location, depsApi = [], baseHash = {})
    makePackage(buildEnv, name, location, depsApi, baseHash)
  end

  def NodeBuilder.makeCasing(buildEnv, name, location, depsApi = [], baseHash = {})
    makePackage(buildEnv, name, location, depsApi, baseHash)
  end

  def NodeBuilder.makeBase(buildEnv, name, location, depsApi = [], baseHash = {})
    makePackage(buildEnv, name, location, depsApi, baseHash)
  end

  private
  ## Create the necessary packages and targets to build a node
  def NodeBuilder.makePackage(buildEnv, name, location, depsApi = [], baseHash = {})
    home = buildEnv.home

    dirName = location
    node = buildEnv["#{name}"]
    buildEnv.installTarget.register_dependency(node)
    buildEnv['node'].registerTarget("#{name}", node)

    ## If there is an API, build it first
    api = FileList["#{home}/#{dirName}/api/**/*.java"]
    baseHash.each_pair do |bd, bn|
      api += FileList["#{bn.buildEnv.home}/#{bd}/api/**/*.java"]
    end

    if (api.length > 0)
      deps  = baseJars + depsApi

      paths = baseHash.map { |bd, bn| ["#{bn.buildEnv.home}/#{bd}/api", "#{bn.buildEnv.home}/#{bd}/api"] }.flatten

      apiJar = JarTarget.build_target(node, deps, 'api', ["#{home}/#{dirName}/api"] + paths)
      buildEnv.installTarget.install_jars(apiJar, "#{node.distDirectory}/usr/share/untangle/toolbox", nil, true)
    end

    po_dir = "#{home}/#{dirName}/po"
    if File.exist? po_dir
      JavaMsgFmtTarget.make_po_targets(node, po_dir, "#{node.distDirectory}/usr/share/untangle/lang/", name).each do |t|
        buildEnv.i18nTarget.register_dependency(t)
      end
    end

    hierFiles = FileList["#{home}/#{dirName}/hier/**/*"]
    if (0 < hierFiles.length)
      ms = MoveSpec.new("#{home}/#{dirName}/hier", hierFiles, node.distDirectory)
      cf = CopyFiles.new(node, ms, 'hier', buildEnv.filterset)
      buildEnv.hierTarget.register_dependency(cf)

      # uncomment this to copy all python2.6 to python2.7 (for wheezy support)
      # ms_python = MoveSpec.new("#{home}/#{dirName}/hier/usr/lib/python2.6", FileList["#{home}/#{dirName}/hier/usr/lib/python2.6/**/*"], "#{node.distDirectory}/usr/lib/python2.7/")
      # cf_python = CopyFiles.new(node, ms_python, 'python2.7', buildEnv.filterset)
      # buildEnv.hierTarget.register_dependency(cf_python)
    end

    FileList["#{home}/#{dirName}/**/*.js"].each do |f|
      jsl = JsLintTarget.new(f)
      buildEnv.jsLintTarget.register_dependency(jsl)
    end
  end

  ## Helper to retrieve the standard dependencies
  def NodeBuilder.baseJars
    uvm_lib = BuildEnv::SRC['untangle-libuvm']
    Jars::Base + [Jars::JFreeChart, uvm_lib['api']]
  end
end
