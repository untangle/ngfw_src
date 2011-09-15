# -*-ruby-*-

implDeps = []

%w(untangle-casing-mail).each do |c|
  implDeps << BuildEnv::SRC[c]['localapi']
end

NodeBuilder.makeBase(BuildEnv::SRC, 'untangle-base-spam', 'spam-base', implDeps)
