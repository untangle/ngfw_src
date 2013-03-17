# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']
spam = BuildEnv::SRC['untangle-base-spam']
clam = BuildEnv::SRC['untangle-base-clam']
phish = BuildEnv::SRC['untangle-node-phish']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-phish', 'phish',
                     [smtp['api']],
                     [smtp['api']], { 'spam-base' => spam, 'clam-base' => clam })

deps = [phish['impl'], phish['api'], spam['impl']]

