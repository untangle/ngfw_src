# -*-ruby-*-

TransformBuilder.makeTransform( "openvpn" )

openvpn = Package["openvpn-transform"]

jt = JarTarget.buildTarget(openvpn, [Package["mvvm"]["api"]], "api", "tran/openvpn/api")

deps = %w(
         ).map { |f| Jars.downloadTarget(f) } << jt

ServletBuilder.new(openvpn, "com.metavize.tran.openvpn.servlet","tran/openvpn/servlets/openvpn", deps)
                   
