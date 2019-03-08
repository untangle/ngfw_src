# -*-ruby-*-

directory_connector = BuildEnv::SRC['untangle-app-directory-connector']

deps = [Jars::Base, Jars::JRadius]
AppBuilder.makeApp(BuildEnv::SRC, 'untangle-app-directory-connector', 'directory-connector', deps)

uvm_lib = BuildEnv::SRC['untangle-libuvm']
appdeps = [directory_connector['src']]

## Servlets
ServletBuilder.new(directory_connector, "com.untangle.app.directory_connector.jsp", "./directory-connector/servlets/userapi", [uvm_lib['taglib']], appdeps, [], [BuildEnv::SERVLET_COMMON])
ServletBuilder.new(directory_connector, "com.untangle.app.directory_connector.jsp", "./directory-connector/servlets/oauth",   [uvm_lib['taglib']], appdeps, [], [BuildEnv::SERVLET_COMMON])
