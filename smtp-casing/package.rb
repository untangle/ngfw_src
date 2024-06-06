# -*-ruby-*-

AppBuilder.makeCasing(BuildEnv::SRC, 'untangle-app-smtp', 'smtp-casing')

smtp = BuildEnv::SRC['untangle-app-smtp']

jt = [smtp['src']]

deps = %w(
           slf4j-2.0.9/slf4j-reload4j-2.0.9.jar
           slf4j-2.0.9/slf4j-api-2.0.9.jar
         ).map { |f| Jars.downloadTarget(f) }
deps +=  Jars::Jabsorb

ServletBuilder.new(smtp, 'com.untangle.app.smtp.quarantine.jsp', "./smtp-casing/servlets/quarantine", deps, jt)

