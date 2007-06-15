# -*-ruby-*-

mail = BuildEnv::SRC['untangle-casing-mail']
http = BuildEnv::SRC['untangle-casing-http']
spam = BuildEnv::SRC['untangle-base-spam']
clam = BuildEnv::SRC['untangle-base-clam']
phish = BuildEnv::SRC['untangle-node-phish']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-phish', 'phish',
                     [mail['localapi'], http['localapi']], 
                     [mail['gui'], http['gui'] ], [],
                     { 'spam-base' => spam, 'clam-base' => clam })

deps = [http['gui'], phish['gui'], spam['gui']]

ServletBuilder.new(phish, 'com.untangle.node.phish.jsp',
                   "#{SRC_HOME}/tran/phish/servlets/idblocker", [],
                   deps, [], [BuildEnv::SERVLET_COMMON])
