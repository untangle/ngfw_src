# -*-ruby-*-

deps = []

%w(smtp ftp http virus-blocker-base).each do |c|
  deps << BuildEnv::SRC[c]['src']
end

NodeBuilder.makeBase(BuildEnv::SRC, 'clam-base', 'clam-base', deps)
