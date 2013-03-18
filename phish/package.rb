# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']
spam = BuildEnv::SRC['untangle-base-spam']
clam = BuildEnv::SRC['untangle-base-clam']
phish = BuildEnv::SRC['untangle-node-phish']

deps = [spam['src'], smtp['src']]

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-phish', 'phish', deps, { 'spam-base' => spam, 'clam-base' => clam })


