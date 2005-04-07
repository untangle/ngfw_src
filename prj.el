;; project file for jdee
;; $Id: prj.el,v 1.16 2005/02/01 09:21:29 amread Exp $

(jde-project-file-version "1.0")

(setq transforms '("airgap" "email" "fprot" "http" "httpblocker" "protofilter"
                   "reporting" "sophos" "spyware" "test" "virus"))
(setq jni-projects '("jnetcap" "jvector"))

(jde-set-variables
 '(jde-sourcepath
   (append
    '("./mvvm/main" "./mvvm/bootstrap")
    (mapcar (lambda (a) (concat "./tran/" a "/main")) transforms)
    (mapcar (lambda (a) (concat "./" a "/src/")) jni-projects)))
 '(jde-make-program "make")
 '(jde-compile-option-directory ".")
 '(jde-make-working-directory ".")
 '(jde-make-args "all")
 '(jde-global-classpath
   '("../downloads/concurrent-1.3.4/lib/concurrent.jar"
     "../downloads/hibernate-2.1.8/hibernate2.jar"
     "../downloads/hibernate-2.1.8/lib/commons-logging-1.0.4.jar"
     "../downloads/hibernate-2.1.8/lib/proxool-0.8.3.jar"
     "../downloads/jakarta-tomcat-5.0.28-embed/lib/catalina-optional.jar"
     "../downloads/jakarta-tomcat-5.0.28-embed/lib/catalina.jar"
     "../downloads/jakarta-tomcat-5.0.28-embed/lib/servlet-api.jar"
     "../downloads/javamail-1.3.1/mail.jar"
     "../downloads/javassist-2.6/javassist.jar"
     "../downloads/junit3.8.1/junit.jar"
     "../downloads/logging-log4j-1.2.9/dist/lib/log4j-1.2.9.jar"
     "../downloads/trove/lib/trove.jar"
     "./mvvm/output/jar/mvvm.jar"
     "./mvvm/output/jar/tranutil.jar"
     "./http/output/jar/http-casing.mar"
     "./jnetcap/output/jar/jnetcap.jar"
     "./jvector/output/jar/jvector.jar")))
