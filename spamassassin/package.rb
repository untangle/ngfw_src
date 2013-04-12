# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']
spam = BuildEnv::SRC['untangle-base-spam'];

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-spamassassin', 'spamassassin', [smtp['src'], spam['src']])
