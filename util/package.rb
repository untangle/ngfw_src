# -*-ruby-*-

buildutil = Package["buildutil"]

JarTarget.buildTarget(buildutil, [ Jars::Becl , Jars::Reporting], 'impl', [ 'util/impl' ] )
