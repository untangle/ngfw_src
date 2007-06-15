# -*-ruby-*-

implDeps = []
guiDeps = []

%w(untangle-casing-mail untangle-casing-ftp untangle-casing-http).each do |c|
  implDeps << BuildEnv::SRC[c]['localapi']
  guiDeps << BuildEnv::SRC[c]['gui']
end

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-virus', 'virus-base', implDeps, guiDeps)
