# -*-ruby-*-

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-cpd', 'cpd')
cpd = BuildEnv::SRC["untangle-node-cpd"]

deps = Jars::Base + [BuildEnv::SRC['untangle-libuvm']['api']]
jt = [cpd['api']]
#jt = [JarTarget.build_target(cpd, deps, 'api', 'cpd/api')]

ServletBuilder.new(cpd, 'com.untangle.node.cpd.jsp', "./cpd/servlets/users", [], jt )
                   

