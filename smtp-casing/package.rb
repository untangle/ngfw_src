# -*-ruby-*-

AppBuilder.makeCasing(BuildEnv::SRC, 'untangle-app-smtp', 'smtp-casing')

smtp = BuildEnv::SRC['untangle-app-smtp']

jt = [smtp['src']]

deps = %w(
           slf4j-1.4.3/slf4j-log4j12-1.4.3.jar
           slf4j-1.4.3/slf4j-api-1.4.3.jar
         ).map { |f| Jars.downloadTarget(f) }
deps +=  Jars::Jabsorb

ServletBuilder.new(smtp, 'com.untangle.app.smtp.quarantine.jsp', "./smtp-casing/servlets/quarantine", deps, jt)

