# -*-ruby-*-

mail = BuildEnv::SRC['untangle-casing-mail']

NodeBuilder.makeNode(BuildEnv::SRC, 'spamassassin', [mail['localapi']], [ mail['gui']], [], BuildEnv::SRC['spam-base'])
