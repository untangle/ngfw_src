# -*-ruby-*-

NodeBuilder.makeCasing(BuildEnv::SRC, 'smtp', 'smtp-casing')

smtp = BuildEnv::SRC['smtp']

jt = [smtp['src']]

deps = %w(
           slf4j-1.4.3/slf4j-log4j12-1.4.3.jar
           slf4j-1.4.3/slf4j-api-1.4.3.jar
         ).map { |f| Jars.downloadTarget(f) }
deps +=  Jars::Jabsorb

ServletBuilder.new(smtp, 'com.untangle.node.smtp.quarantine.jsp', "./smtp-casing/servlets/quarantine", deps, jt)

