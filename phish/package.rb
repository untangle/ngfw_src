# -*-ruby-*-

mail = BuildEnv::SRC['mail-casing']
http = BuildEnv::SRC['http-casing']
spam = BuildEnv::SRC['spam-base']
phish = BuildEnv::SRC['phish-node']

NodeBuilder.makeNode(BuildEnv::SRC, 'phish',
                     [mail['localapi'], http['localapi']],
                     [mail['gui'], http['gui'] ], [],
                     [BuildEnv::SRC['spam-base'], BuildEnv::SRC['clam-base']])

deps = [http['gui'], phish['gui'], spam['gui']]

ServletBuilder.new(phish, 'com.untangle.node.phish.jsp',
                   "#{SRC_HOME}/phish/servlets/idblocker", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
