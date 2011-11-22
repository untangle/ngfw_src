# -*-ruby-*-

mail = BuildEnv::SRC['untangle-casing-mail']

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-spam', 'spam-base', [mail["api"]], [mail["api"]])
