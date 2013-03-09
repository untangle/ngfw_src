# -*-ruby-*-

mail = BuildEnv::SRC['untangle-casing-mail']
spam = BuildEnv::SRC['untangle-base-spam']
clam = BuildEnv::SRC['untangle-base-clam']
phish = BuildEnv::SRC['untangle-node-phish']

NodeBuilder.makeNode(BuildEnv::SRC, 'untangle-node-phish', 'phish',
                     [mail['api']],
                     [mail['api']], { 'spam-base' => spam, 'clam-base' => clam })

deps = [phish['impl'], phish['api'], spam['impl']]

