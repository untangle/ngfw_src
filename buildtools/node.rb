# -*-ruby-*-

class NodeBuilder
  NodeSuffix = 'node'
  CasingSuffix = 'casing'
  BaseSuffix = 'base'

  def NodeBuilder.makeNode(buildEnv, name, depsImpl = [],
                           depsGui = [], depsLocalApi = [],
                           baseNodes = [])
    makePackage(buildEnv, name, NodeSuffix, depsImpl, depsGui,
                depsLocalApi, baseNodes)
  end

  def NodeBuilder.makeCasing(buildEnv, name, depsImpl = [], depsGui = [],
                             depsLocalApi = [], baseNodes = [])
    makePackage(buildEnv, name, CasingSuffix, depsImpl, depsGui, depsLocalApi,
                baseNodes)
  end

  def NodeBuilder.makeBase(buildEnv, name, depsImpl = [], depsGui = [],
                           depsLocalApi = [], baseNodes = [])
    makePackage(buildEnv, name, BaseSuffix, depsImpl, depsGui, depsLocalApi,
                baseNodes)
  end

  private
  ## Create the necessary packages and targets to build a node
  def NodeBuilder.makePackage(buildEnv, name, suffix, depsImpl = [],
                                   depsGui = [], depsLocalApi = [],
                                   baseNodes = [])
    home = buildEnv.home

    uvm = BuildEnv::SRC['uvm']
    gui  = BuildEnv::SRC['untangle-client']
    if (suffix == 'node')
        dirName = name;
    else
        dirName = "#{name}-#{suffix}"
    end
    node = buildEnv["#{name}-#{suffix}"]
    buildEnv['tran'].registerTarget("#{name}-#{suffix}", node)

    localApiJar = nil

    baseNodes = [baseNodes].flatten

    ## If there is a local API, build it first
    localApi = FileList["#{home}/#{dirName}/localapi/**/*.java"]
    baseNodes.each do |bt|
      localApi += FileList["#{home}/#{bt}/localapi/**/*.java"]
    end

    if (localApi.length > 0)
      deps  = baseJarsLocalApi + depsLocalApi

      paths = baseNodes.map { |bt| ["#{home}/#{bt}/api",
          "#{home}/#{bt}/localapi"] }.flatten

      localApiJar = JarTarget.buildTarget(node, deps, 'localapi',
                                          ["#{home}/#{dirName}/api", "#{home}/#{dirName}/localapi"] + paths)
      buildEnv.installTarget.installJars(localApiJar, "#{node.distDirectory}/usr/share/untangle/toolbox")
    end

    ## Build the IMPL jar.
    deps = baseJarsImpl + depsImpl

    ## Make the IMPL dependent on the localapi if a jar exists.
    directories= ["#{home}/#{dirName}/impl"]
    if (localApiJar.nil?)
      ## Only include the API if the localJarApi doesn't exist
      directories << "#{home}/#{dirName}/api"
      baseNodes.each { |bt| directories << "#{home}/#{bt}/api" }
    else
      deps << localApiJar
    end

    baseNodes.each { |bt| directories << "#{home}/#{bt}/impl" }

    ## The IMPL jar depends on the reports
    deps << JasperTarget.buildTarget( node,
                                      "#{buildEnv.staging}/#{node.name}-impl/reports",
                                      directories )

    jt = JarTarget.buildTarget(node, deps, "impl", directories)

    buildEnv.installTarget.installJars(jt, "#{node.distDirectory}/usr/share/untangle/toolbox", nil, false, true)

    ## Only create the GUI api if there are files for the GUI
    if (FileList["#{home}/#{dirName}/gui/**/*.java"].length > 0)
      deps  = baseJarsGui + depsGui
      baseNodes.each do |bt|
        pkg = buildEnv["#{bt}"]
        if pkg.hasTarget?('gui')
          deps << pkg['gui']
        end
      end

      jt = JarTarget.buildTarget(node, deps, 'gui',
                                 ["#{home}/#{dirName}/api", "#{home}/#{dirName}/gui",
                                   "#{home}/#{dirName}/fake"])
      buildEnv.installTarget.installJars(jt, "#{node.distDirectory}/usr/share/untangle/web/webstart",
                                         nil, true)
    end

    hierFiles = FileList["#{home}/#{dirName}/hier/**/*"]
    if (0 < hierFiles.length)
      ms = MoveSpec.new("#{home}/#{dirName}/hier", hierFiles, node.distDirectory)
      cf = CopyFiles.new(node, ms, 'hier', buildEnv.filterset)
      node.registerTarget('hier', cf)
    end
  end

  ## Helper to retrieve the standard dependencies for an impl
  def NodeBuilder.baseJarsImpl
    uvm = BuildEnv::SRC['uvm']
    Jars::Base + [Jars::JFreeChart, Jars::Jasper, uvm['api'], uvm['localapi'],
      uvm['reporting']]
  end

  ## Helper to retrieve the standard dependencies for local API
  def NodeBuilder.baseJarsLocalApi
    ## See no reason to use a different set of jars
    baseJarsImpl
  end

  ## Helper to retrieve the standard dependencies for a GUI jar
  def NodeBuilder.baseJarsGui
    Jars::Base + Jars::Gui + Jars::TomcatEmb +
      [BuildEnv::SRC['uvm']['api'], BuildEnv::SRC['untangle-client']['api']]
  end
end
