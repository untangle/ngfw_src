;; project file for jdee
;; $Id$

(jde-project-file-version "1.0")

(setq casings '("mail" "http" "ftp"))

(setq transforms '("airgap" "email" "fprot" "httpblocker" "protofilter"
                   "reporting" "sophos" "spyware" "test" "virus" "spam"
                   "portal"))

(setq jni-projects '("jnetcap" "jvector"))

(jde-set-variables
 ;; TODO base on dir listing, include webapps
 '(jde-sourcepath
   (append
    '("./mvvm/main" "./mvvm/bootstrap" "./util"
      "./mvvm/webapps/store/src"
      "./tran/portal/webapps/browser/src"
      "./tran/portal/webapps/proxy/src")
    (mapcar (lambda (a) (concat "./tran/" a "/main")) transforms)
    (mapcar (lambda (a) (concat "./tran/" a "/main")) casings)
    (mapcar (lambda (a) (concat "./" a "/src/")) jni-projects)))
 '(jde-make-program "make")
 '(jde-compile-option-directory ".")
 '(jde-make-working-directory ".")
 '(jde-make-args "all")
 '(jde-global-classpath
   (append
    (mapcar (lambda (a)
              (concat "./tran/output/" a)) casings)
    '("../downloads/output/bcel-5.1/bcel-5.1.jar"
      "../downloads/output/c3p0-0.9.0.2/lib/c3p0-0.9.0.2.jar"
      "../downloads/output/commons-fileupload-1.1/commons-fileupload-1.1.jar"
      "../downloads/output/commons-httpclient-3.0/commons-httpclient-3.0.jar"
      "../downloads/output/concurrent-1.3.4/lib/concurrent.jar"
      "../downloads/output/gnu.regexp-1.1.4/lib/gnu-regexp-1.1.4.jar"
      "../downloads/output/hibernate-3.0/hibernate3.jar"
      "../downloads/output/htmlparser1_6_20060319/htmlparser1_6/lib/htmlparser.jar"
      "../downloads/output/jakarta-tomcat-5.0.28-embed/lib/catalina-optional.jar"
      "../downloads/output/jakarta-tomcat-5.0.28-embed/lib/catalina.jar"
      "../downloads/output/jakarta-tomcat-5.0.28-embed/lib/jsp-api.jar"
      "../downloads/output/jakarta-tomcat-5.0.28-embed/lib/servlet-api.jar"
      "../downloads/output/javamail-1.3.1/mail.jar"
      "../downloads/output/javassist-2.6/javassist.jar"
      "../downloads/output/jcifs_1.1.11/jcifs-1.1.11.jar"
      "../downloads/output/jfreechart-0.9.21/jfreechart-0.9.21.jar"
      "../downloads/output/junit3.8.1/junit.jar"
      "../downloads/output/logging-log4j-1.2.9/dist/lib/log4j-1.2.9.jar"
      "../downloads/output/trove/lib/trove.jar"
      "./jnetcap/output/jar/jnetcap.jar"
      "./jvector/output/jar/jvector.jar"
      "./mvvm/output/jar/mvvm.jar"
      "./mvvm/output/jar/tranutil.jar"))))
