# -*-ruby-*-

mail = BuildEnv::SRC['mail-casing']
http = BuildEnv::SRC['http-casing']
spam = BuildEnv::SRC['spam-base']
phish = BuildEnv::SRC['clamphish-node']

NodeBuilder.makeNode(BuildEnv::SRC, 'phish',
                               [mail['localapi'], http['localapi']],
                               [mail['gui'], http['gui'] ], [],
                               ['spam', 'clam-base'])

deps = [http['gui'], phish['gui'], spam['gui']]

ServletBuilder.new(phish, 'com.untangle.node.clamphish.jsp',
                   "#{SRC_HOME}/tran/phish/servlets/idblocker", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
