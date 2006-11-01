# -*-ruby-*-

TransformBuilder.makeTransform( "openvpn" )

openvpn = Package["openvpn-transform"]


deps = Jars::Base + [Package["mvvm"]["api"]]

jt = JarTarget.buildTarget(openvpn, deps, "api", "tran/openvpn/api")

deps = %w(
         ).map { |f| Jars.downloadTarget(f) } << jt

ServletBuilder.new(openvpn, "com.untangle.tran.openvpn.servlet","tran/openvpn/servlets/openvpn", deps)

