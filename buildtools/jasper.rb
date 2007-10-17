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

class JasperCompiler
  JavaCommand = "#{BuildEnv::JAVA_HOME}/bin/java"
  BuildUtilPkg = "com.untangle.buildutil"
  ReportGenerator = "#{BuildUtilPkg}.ReportGenerator"
  JRCompiler = "#{BuildUtilPkg}.JRCompiler"

  TemplateDirectory = "./uvm-lib/resources/reports"

  include Singleton

  ## Convert an rpd file
  def JasperCompiler.rpdToJrxml( fileList, destination )
    if ( fileList.size > 0 )
      raise "ReportGenerator failed" unless
        Kernel.system( JavaCommand, "-cp", classpath, ReportGenerator, "-t", TemplateDirectory,
                       "-o", destination, *fileList )
    end
  end

  def JasperCompiler.jrxmlToJasper( fileList, destination = "" )
    if ( fileList.size > 0 )
      raise "JRCompiler failed" unless
        Kernel.system( JavaCommand, "-cp", classpath, JRCompiler, "-o", destination, *fileList )
    end
  end

  private
  def JasperCompiler.classpath
    [Jars::Reporting + Jars::JDKTools + [BuildEnv::SRC['buildutil']['impl'].filename ]].flatten.join(":")
  end
end

## This is a target for converting a list of RPD files into JRXML files
class JRXMLTarget < Target
  def initialize( package, buildDirectory, basepaths )
    @targetName = "rpdtojrxml:#{package.name}"

    ## Force basepath to be an array
    @basepaths = [ basepaths ].flatten

    @buildDirectory = buildDirectory

    @rpdFiles = FileList[ @basepaths.map { |basepath| "#{basepath}/**/*.rpd"} ]

    ## There are no dependencies for these files
    super( package )
  end

  def makeDependencies
    ## ReportGenerator is built inside of the build utils
    buildutil = BuildEnv::SRC['buildutil']

    @rpdFiles.each do |f|
      jrxmlFile =  "#{@buildDirectory}/#{File.basename( f ).gsub(/.rpd$/,".jrxml")}"
      debug jrxmlFile

      ## Make the classfile depend on the source itself
      file jrxmlFile => f do
        directory = File.dirname jrxmlFile
        ## XXX Hokey way to update the timestamp XXX
        mkdir_p directory if !File.exist?(directory)
        Kernel.system("touch",jrxmlFile)
      end

      file jrxmlFile => buildutil

      # Make the stamp task
      stamptask self => jrxmlFile
    end
  end

  def build
    JasperCompiler.rpdToJrxml( @rpdFiles, @buildDirectory )
  end

  def to_s
    @targetName
  end

end

## This is a target for converting a list of JRXML files into a Jasper file
class JasperTarget < Target
  def initialize(package, deps, buildDirectory, basepaths )
    @targetName = "jrxmltojasper:#{package.name}"

    ## Force basepath to be an array
    @basepaths = [ basepaths ].flatten

    @buildDirectory = buildDirectory

    @jrxmlFiles = FileList[ @basepaths.map { |basepath| "#{basepath}/**/*.jrxml"} ]

    super(package,deps)
  end

  def makeDependencies
    @jrxmlFiles.each { |f| stamptask self => f }
  end

  def build
    ## Have to re-evaluate in order to get the RPD files
    jrxmlFiles = FileList[ @basepaths.map { |basepath| "#{basepath}/**/*.jrxml"} ]

    JasperCompiler.jrxmlToJasper( jrxmlFiles, @buildDirectory )
  end

  def to_s
    @targetName
  end

  def JasperTarget.buildTarget( package, destination, basepaths )
    basepaths = [ basepaths ].flatten

    ## Build all of the JRXML files from the RPDs
    rpdDirectory = "#{package.buildEnv.staging}/#{package.name}-rpd"
    deps = [ JRXMLTarget.new( package, rpdDirectory, basepaths ) ]

    ## Append the Generated JRXML files to the base paths
    basepaths <<  rpdDirectory
    JasperTarget.new( package, deps, destination, basepaths )
  end
end
