# -*-ruby-*-

implDeps = []
guiDeps = []

%w(untangle-casing-mail).each do |c|
  implDeps << BuildEnv::SRC[c]['localapi']
  guiDeps << BuildEnv::SRC[c]['gui']
end

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-spam', 'spam-base', implDeps, guiDeps)
