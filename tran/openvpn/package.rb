# -*-ruby-*-

TransformBuilder.makeTransform('openvpn')

openvpn = Package['openvpn-transform']


deps = Jars::Base + [Package['mvvm']['api']]

jt = [JarTarget.buildTarget(openvpn, deps, 'api', 'tran/openvpn/api')]

ServletBuilder.new(openvpn, 'com.untangle.tran.openvpn.servlet','tran/openvpn/servlets/openvpn', [], jt)

