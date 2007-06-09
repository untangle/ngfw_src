# -*-ruby-*-

http = BuildEnv::SRC['http-casing']
httpblocker = BuildEnv::SRC['httpblocker-node']

NodeBuilder.makeNode(BuildEnv::SRC, 'httpblocker',
                               [http['localapi']], [http['gui']])

deps = [httpblocker['gui'], http['gui']]

ServletBuilder.new(httpblocker, 'com.untangle.node.httpblocker.jsp',
                   "#{SRC_HOME}/tran/httpblocker/servlets/httpblocker", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
