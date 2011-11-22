# -*-ruby-*-

NodeBuilder.makeCasing(BuildEnv::SRC, 'untangle-casing-mail', 'mail-casing')

mail = BuildEnv::SRC['untangle-casing-mail']

jt = [mail['api']]

deps = %w(
           slf4j-1.4.3/slf4j-log4j12-1.4.3.jar
           slf4j-1.4.3/slf4j-api-1.4.3.jar
         ).map { |f| Jars.downloadTarget(f) }
deps +=  Jars::Jabsorb

ServletBuilder.new(mail, 'com.untangle.node.mail.quarantine.jsp', "./mail-casing/servlets/quarantine", deps, jt)

