# -*-ruby-*-

mail = BuildEnv::SRC['untangle-casing-mail']
http = BuildEnv::SRC['untangle-casing-http']
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
