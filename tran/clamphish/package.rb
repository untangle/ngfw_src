# -*-ruby-*-

mail = BuildEnv::SRC['mail-casing']
http = BuildEnv::SRC['http-casing']
spam = BuildEnv::SRC['spam-base']
clamphish = BuildEnv::SRC['clamphish-node']

NodeBuilder.makeNode(BuildEnv::SRC, 'clamphish',
                               [mail['localapi'], http['localapi']],
                               [mail['gui'], http['gui'] ], [],
                               ['spam', 'clam-base'])

deps = [http['gui'], clamphish['gui'], spam['gui']]

ServletBuilder.new(clamphish, 'com.untangle.node.clamphish.jsp',
                   "#{SRC_HOME}/tran/clamphish/servlets/idblocker", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
