# -*-ruby-*-

mail = BuildEnv::SRC['untangle-casing-mail']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-spamassassin', 'spamassassin',
                     [mail['api']], 
                     [mail['api']], 
                     { 'spam-base' => BuildEnv::SRC['untangle-base-spam'] })
