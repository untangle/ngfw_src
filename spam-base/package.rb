# -*-ruby-*-

smtp = BuildEnv::SRC['untangle-casing-smtp']

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-spam', 'spam-base', [smtp['src']])
