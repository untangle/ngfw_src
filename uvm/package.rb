# -*-ruby-*-

jnetcap = BuildEnv::SRC['jnetcap']
jvector = BuildEnv::SRC['jvector']
uvm = BuildEnv::SRC['untangle-uvm']
BuildEnv::SRC.installTarget.registerDependency(uvm)

jts = []

## Bootstrap
jts << JarTarget.buildTarget(uvm, Jars::Base, 'bootstrap', "#{SRC_HOME}/uvm/bootstrap")

## API
jts << (jt = JarTarget.buildTarget(uvm, Jars::Base, 'api', ["#{SRC_HOME}/uvm/api", '../version']))
BuildEnv::SRC.installTarget.installJars(jt, uvm.getWebappDir('webstart'), nil, true)

## Local API
jts << JarTarget.buildTarget(uvm, Jars::Base + [ uvm['api']], 'localapi', "#{SRC_HOME}/uvm/localapi")

## Reporting
deps  = Jars::Base + Jars::Jasper + Jars::JFreeChart + [ uvm['api']]
jts << JarTarget.buildTarget(uvm, deps, 'reporting', "#{SRC_HOME}/uvm/reporting")

## Implementation
deps  = Jars::Base + Jars::TomcatEmb + Jars::JavaMail + Jars::Jcifs +
  Jars::Dom4j + Jars::Activation + Jars::Trove + Jars::Jasper + Jars::JFreeChart +
  [ uvm['bootstrap'], uvm['api'], uvm['localapi'], uvm['reporting'], jnetcap['impl'], jvector['impl']]

jts << JarTarget.buildTarget(uvm, deps, 'impl', "#{SRC_HOME}/uvm/impl")

# servlets
ServletBuilder.new(uvm, 'com.untangle.uvm.invoker.jsp',
                   "#{SRC_HOME}/uvm/servlets/http-invoker", [], [], [],
                   [BuildEnv::SERVLET_COMMON])

deps = %w( commons-httpclient-3.0/commons-httpclient-3.0.jar
           commons-codec-1.3/commons-codec-1.3.jar
           commons-fileupload-1.1/commons-fileupload-1.1.jar
         ).map { |n| ThirdpartyJar.get("#{BuildEnv::DOWNLOADS}/#{n}") }
ServletBuilder.new(uvm, 'com.untangle.uvm.store.jsp',
                   "#{SRC_HOME}/uvm/servlets/onlinestore", deps)

ServletBuilder.new(uvm, 'com.untangle.uvm.reports.jsp',
                   "#{SRC_HOME}/uvm/servlets/reports")

# wmi installer
ServletBuilder.new(uvm, "com.untangle.uvm.user.servlet","uvm/servlets/wmi", [])


deps = FileList["#{BuildEnv::DOWNLOADS}/Ajax/jars/*jar"].exclude(/.*servlet-api.jar/).map { |n| ThirdpartyJar.get(n) }
ms = [ MoveSpec.new("#{BuildEnv::DOWNLOADS}/Ajax/WebRoot/js", '**/*', 'AjaxTk')]
ServletBuilder.new(uvm, 'com.untangle.uvm.root.jsp',
                   "#{SRC_HOME}/uvm/servlets/ROOT", deps, [], ms)

ajaxTkList =
  [
  'core/AjxCore.js',
  'core/AjxEnv.js',
  'util/AjxUtil.js',
  'util/AjxText.js',
  'core/AjxException.js',
  'util/AjxCookie.js',
  'soap/AjxSoapException.js',
  'soap/AjxSoapFault.js',
  'soap/AjxSoapDoc.js',
  'net/AjxRpcRequest.js',
  'net/AjxRpc.js',
  'util/AjxWindowOpener.js',
  'util/AjxVector.js',
  'util/AjxStringUtil.js',
  'debug/AjxDebug.js',
  'debug/AjxDebugXmlDocument.js',
  'xml/AjxXmlDoc.js',
  'core/AjxEnv.js',
  'core/AjxImg.js',
  'core/AjxException.js',
  'util/AjxCallback.js',
  'util/AjxTimedAction.js',
  'events/AjxEvent.js',
  'events/AjxEventMgr.js',
  'events/AjxListener.js',
  'util/AjxDateUtil.js',
  'util/AjxStringUtil.js',
  'util/AjxVector.js',
  'util/AjxSelectionManager.js',
  'net/AjxPost.js',
  'util/AjxBuffer.js',
  'util/AjxCache.js',
  'dwt/core/DwtImg.js',
  'dwt/core/Dwt.js',
  'dwt/core/DwtException.js',
  'dwt/core/DwtDraggable.js',
  'dwt/core/DwtDragTracker.js',
  'dwt/graphics/DwtCssStyle.js',
  'dwt/graphics/DwtPoint.js',
  'dwt/graphics/DwtRectangle.js',
  'dwt/graphics/DwtUnits.js',
  'dwt/events/DwtEvent.js',
  'dwt/events/DwtEventManager.js',
  'dwt/events/DwtDateRangeEvent.js',
  'dwt/events/DwtDisposeEvent.js',
  'dwt/events/DwtUiEvent.js',
  'dwt/events/DwtControlEvent.js',
  'dwt/events/DwtKeyEvent.js',
  'dwt/events/DwtMouseEvent.js',
  'dwt/events/DwtMouseEventCapture.js',
  'dwt/events/DwtListViewActionEvent.js',
  'dwt/events/DwtSelectionEvent.js',
  'dwt/events/DwtHtmlEditorStateEvent.js',
  'dwt/events/DwtTreeEvent.js',
  'dwt/events/DwtHoverEvent.js',
  'dwt/dnd/DwtDragEvent.js',
  'dwt/dnd/DwtDragSource.js',
  'dwt/dnd/DwtDropEvent.js',
  'dwt/dnd/DwtDropTarget.js',
  'dwt/widgets/DwtHoverMgr.js',
  'dwt/widgets/DwtControl.js',
  'dwt/widgets/DwtComposite.js',
  'dwt/widgets/DwtShell.js',
  'dwt/widgets/DwtColorPicker.js',
  'dwt/widgets/DwtBaseDialog.js',
  'dwt/widgets/DwtDialog.js',
  'dwt/widgets/DwtLabel.js',
  'dwt/widgets/DwtListView.js',
  'dwt/widgets/DwtButton.js',
  'dwt/widgets/DwtMenuItem.js',
  'dwt/widgets/DwtMenu.js',
  'dwt/widgets/DwtMessageDialog.js',
  'dwt/widgets/DwtHtmlEditor.js',
  'dwt/widgets/DwtInputField.js',
  'dwt/widgets/DwtSash.js',
  'dwt/widgets/DwtToolBar.js',
  'dwt/graphics/DwtBorder.js',
  'dwt/widgets/DwtToolTip.js',
  'dwt/widgets/DwtStickyToolTip.js',
  'dwt/widgets/DwtTreeItem.js',
  'dwt/widgets/DwtTree.js',
  'dwt/widgets/DwtCalendar.js',
  'dwt/widgets/DwtPropertyPage.js',
  'dwt/widgets/DwtTabView.js',
  'dwt/widgets/DwtWizardDialog.js',
  'dwt/widgets/DwtSelect.js',
  'dwt/widgets/DwtAddRemove.js',
  'dwt/widgets/DwtAlert.js',
  'dwt/widgets/DwtText.js',
  'dwt/widgets/DwtIframe.js',
  'dwt/xforms/DwtXFormDialog.js',
  'dwt/widgets/DwtPropertySheet.js',
  'dwt/widgets/DwtGrouper.js',
  'dwt/widgets/DwtProgressBar.js',
  'dwt/events/DwtXFormsEvent.js',
  'dwt/xforms/XFormGlobal.js',
  'dwt/xforms/XModel.js',
  'dwt/xforms/XModelItem.js',
  'dwt/xforms/XForm.js',
  'dwt/xforms/XFormItem.js',
  'dwt/xforms/XFormChoices.js',
  'dwt/xforms/OSelect_XFormItem.js',
  'dwt/xforms/ButtonGrid.js',
].map { |e| "#{BuildEnv::DOWNLOADS}/Ajax/WebRoot/js/#{e}" }

bundledAjx = "#{uvm.getWebappDir("ROOT")}/AjaxTk/BundledAjx.js"

file bundledAjx => ajaxTkList do
  File.open(bundledAjx, 'w') do |o|
    ajaxTkList.each do |e|
      File.open(e, 'r') do |i|
        i.each_line { |l| o.puts(l) }
      end
    end
  end

  Kernel.system('sed', '-i', '-e', '/\/\/if (AjxEnv.isGeckoBased)/,+1s/\/\///', bundledAjx);
end
BuildEnv::SRC.installTarget.registerDependency(bundledAjx)

ServletBuilder.new(uvm, 'com.untangle.uvm.sessiondumper.jsp',
                   "#{SRC_HOME}/uvm/servlets/session-dumper")

ms = MoveSpec.new("#{SRC_HOME}/uvm/hier", FileList["#{SRC_HOME}/uvm/hier/**/*"], uvm.distDirectory)
cf = CopyFiles.new(uvm, ms, 'hier', BuildEnv::SRC.filterset)
uvm.registerTarget('hier', cf)

BuildEnv::SRC.installTarget.installJars(jts, "#{uvm.distDirectory}/usr/share/untangle/lib",
                                        nil, false, true)

thirdparty = BuildEnv::SRC['libuvm-thirdparty']

BuildEnv::SRC.installTarget.installJars(Jars::Base, "#{thirdparty.distDirectory}/usr/share/java/uvm")

BuildEnv::SRC.installTarget.installJars(Jars::Reporting, "#{thirdparty.distDirectory}/usr/share/java/reports")

BuildEnv::SRC.installTarget.installDirs("#{uvm.distDirectory}/usr/share/untangle/toolbox")

uvm_cacerts = "#{uvm.distDirectory}/usr/share/untangle/conf/cacerts"
java_cacerts = "#{BuildEnv::JAVA_HOME}/jre/lib/security/cacerts"
mv_ca = "#{SRC_HOME}/uvm/resources/mv-ca.pem"
ut_ca = "#{SRC_HOME}/uvm/resources/ut-ca.pem"

file uvm_cacerts => [ java_cacerts, mv_ca, ut_ca ] do
  ensureDirectory(File.dirname(uvm_cacerts))
  FileUtils.cp(java_cacerts, uvm_cacerts)
  FileUtils.chmod(0666, uvm_cacerts)
  Kernel.system("#{BuildEnv::JAVA_HOME}/bin/keytool", '-import', '-noprompt',
                '-keystore', uvm_cacerts, '-storepass', 'changeit', '-file',
                mv_ca, '-alias', 'metavizeprivateca')
  Kernel.system("#{BuildEnv::JAVA_HOME}/bin/keytool", '-import', '-noprompt',
                '-keystore', uvm_cacerts, '-storepass', 'changeit', '-file',
                ut_ca, '-alias', 'untangleprivateca')
end

if BuildEnv::SRC.isDevel
  BuildEnv::SRC.installTarget.installFiles("#{SRC_HOME}/debian/control",
                                           "#{uvm.distDirectory}/tmp",
                                           'pkg-list-main')

  activationKey = "#{uvm.distDirectory}/usr/share/untangle/activation.key"

  ## Insert the activation key if necessary.  Done here to not include
  ## The file inside of packages
  file activationKey do
    File.open( activationKey, "w" ) { |f| f.puts( "0000-0000-0000-0000" ) }
  end

  BuildEnv::SRC.installTarget.registerDependency(activationKey)
end

BuildEnv::SRC.installTarget.registerDependency(uvm_cacerts)

deps  = Jars::Base + Jars::TomcatEmb + Jars::JavaMail + Jars::Jcifs +
  Jars::Dom4j + Jars::Activation + Jars::Trove + Jars::Junit + Jars::WBEM +
  [ uvm['bootstrap'], uvm['api'], uvm['localapi'], uvm['impl'], jnetcap['impl'], jvector['impl']]

JarTarget.buildTarget(BuildEnv::SRC["unittest"], deps, 'untangle-uvm', "#{SRC_HOME}/uvm/unittest")
