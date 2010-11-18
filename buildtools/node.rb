# -*-ruby-*-
# $HeadURL$
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

class NodeBuilder

  def NodeBuilder.makeNode(buildEnv, name, location, depsImpl = [],
                           depsLocalApi = [], baseHash = {})
    makePackage(buildEnv, name, location, depsImpl, depsLocalApi, baseHash)
  end

  def NodeBuilder.makeCasing(buildEnv, name, location, depsImpl = [],
                             depsLocalApi = [], baseHash = {})
    makePackage(buildEnv, name, location, depsImpl, depsLocalApi,
                baseHash)
  end

  def NodeBuilder.makeBase(buildEnv, name, location, depsImpl = [],
                           depsLocalApi = [], baseHash = {})
    makePackage(buildEnv, name, location, depsImpl, depsLocalApi,
                baseHash)
  end

  private
  ## Create the necessary packages and targets to build a node
  def NodeBuilder.makePackage(buildEnv, name, location, depsImpl = [],
                              depsLocalApi = [], baseHash = {})
    home = buildEnv.home

    uvm_lib = BuildEnv::SRC['untangle-libuvm']
    dirName = location
    node = buildEnv["#{name}"]
    buildEnv.installTarget.register_dependency(node)
    buildEnv['node'].registerTarget("#{name}", node)

    localApiJar = nil

    ## If there is a local API, build it first
    localApi = FileList["#{home}/#{dirName}/localapi/**/*.java"]
    baseHash.each_pair do |bd, bn|
      localApi += FileList["#{bn.buildEnv.home}/#{bd}/localapi/**/*.java"]
    end

    if (localApi.length > 0)
      deps  = baseJarsLocalApi + depsLocalApi

      paths = baseHash.map { |bd, bn| ["#{bn.buildEnv.home}/#{bd}/api",
          "#{bn.buildEnv.home}/#{bd}/localapi"] }.flatten

      localApiJar = JarTarget.build_target(node, deps, 'localapi',
                                          ["#{home}/#{dirName}/api", "#{home}/#{dirName}/localapi"] + paths)
      buildEnv.installTarget.install_jars(localApiJar, "#{node.distDirectory}/usr/share/untangle/toolbox")
    end

    ## Build the IMPL jar.
    deps = baseJarsImpl + depsImpl

    ## Make the IMPL dependent on the localapi if a jar exists.
    directories= ["#{home}/#{dirName}/impl"]
    if (localApiJar.nil?)
      ## Only include the API if the localJarApi doesn't exist
      directories << "#{home}/#{dirName}/api"
      baseHash.each_pair { |bd, bn| directories << "#{bn.buildEnv.home}/#{bd}/api" }
    else
      deps << localApiJar
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

    buildEnv.installTarget.install_jars(jt, "#{node.distDirectory}/usr/share/untangle/toolbox", nil, false, true)

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

    script_dir = "#{home}/#{dirName}/hier/usr/share/untangle/web/webui/script"
    if File.exist? script_dir
      
      YuiCompressorTarget.make_min_targets(node, script_dir,
                                       "#{node.distDirectory}/usr/share/untangle/web/webui/script",
                                       "js").each do |t|
        buildEnv.installTarget.register_dependency(t)
      end
    end
  end

  ## Helper to retrieve the standard dependencies for an impl
  def NodeBuilder.baseJarsImpl
    uvm_lib = BuildEnv::SRC['untangle-libuvm']
    Jars::Base + [Jars::JFreeChart, uvm_lib['api'], uvm_lib['localapi']]
  end

  ## Helper to retrieve the standard dependencies for local API
  def NodeBuilder.baseJarsLocalApi
    ## See no reason to use a different set of jars
    baseJarsImpl
  end
end
