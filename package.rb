# -*-ruby-*-

directory_connector = HadesBuildEnv['untangle-node-directory-connector']

deps = [Jars::Base, Jars::JRadius]
NodeBuilder.makeNode(HadesBuildEnv, 'untangle-node-directory-connector', 'directory-connector', deps)

uvm_lib = BuildEnv::SRC['untangle-libuvm']
nodedeps = [directory_connector['src']]

## Servlets
ServletBuilder.new(directory_connector, "com.untangle.node.directory_connector.jsp", "./directory-connector/servlets/userapi", [uvm_lib['taglib']], nodedeps, [], [BuildEnv::SERVLET_COMMON])
ServletBuilder.new(directory_connector, "com.untangle.node.directory_connector.jsp", "./directory-connector/servlets/oauth",   [uvm_lib['taglib']], nodedeps, [], [BuildEnv::SERVLET_COMMON])
