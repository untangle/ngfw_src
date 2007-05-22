# -*-ruby-*-

TransformBuilder.makeTransform(BuildEnv::ALPINE, 'openvpn')

openvpn = BuildEnv::ALPINE['openvpn-transform']


deps = Jars::Base + [BuildEnv::ALPINE['mvvm']['api']]

jt = [JarTarget.buildTarget(openvpn, deps, 'api', 'tran/openvpn/api')]

ServletBuilder.new(openvpn, 'com.untangle.tran.openvpn.servlet','tran/openvpn/servlets/openvpn', [], jt)

