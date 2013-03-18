# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-spamassassin', 'spamassassin', [smtp['src']], { 'spam-base' => BuildEnv::SRC['untangle-base-spam'] })
