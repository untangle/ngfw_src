#
# This Script is for Installing Untangle Root Certificates
# Based on installer Script Written by Björn Gustavsson (WebFooL) For Untangle Inc.
# 
# The installer supports /S (Silent mode installation)
# Written by Kichik http://nsis.sourceforge.net/Import_Root_Certificate
#
# SetCompression Before packeting Incudes
SetCompressor /FINAL lzma 

# Includes Needed
!addincludedir "/usr/share/nsis/Include"
!addincludedir ".\include"
!addplugindir ".\plugin"
!include "MUI.nsh"
!include "x64.nsh"
!include "nsProcess.nsh"
!include "EnumUsersReg.nsh"
!include "StrFunc.nsh"

#!define MULTIUSER_EXECUTIONLEVEL Admin
!define PACKAGE_NAME "Untangle Root CA Installer"
!define VERSION "1.1.0.0"
!define FILENAME "RootCAInstaller.exe"
!define UNTANGLE_SETTINGS_DIR "./"
!define PUBLISHER "Untangle"
!define UNTANGLE_ROOTCA_DIR "/usr/share/untangle/settings/untangle-certificates"
!define UNTANGLE_FF_CFG "${UNTANGLE_SETTINGS_DIR}\untangle-firefox-certificate.cfg"
!define UNTANGLE_FF_JS "${UNTANGLE_SETTINGS_DIR}\untangle-firefox-preferences.js"
#!define ROOTCANAME "www.untangle.com - Untangle"
!define ROOTCANAME "www.untangle.com"

# Function AddCertificateToStore Defines
!define CERT_QUERY_OBJECT_FILE          1
!define CERT_QUERY_CONTENT_FLAG_ALL     16382
!define CERT_QUERY_FORMAT_FLAG_ALL      14
!define CERT_STORE_PROV_SYSTEM          10
!define CERT_STORE_OPEN_EXISTING_FLAG   0x4000
!define CERT_SYSTEM_STORE_LOCAL_MACHINE 0x20000
!define CERT_STORE_MAXIMUM_ALLOWED_FLAG 0x00001000
!define CERT_STORE_ADD_ALWAYS           4

# CertGetNameString Defines
!define CERT_STORE_CERTIFICATE_CONTEXT  1
!define CERT_NAME_SIMPLE_DISPLAY_TYPE   4

# MUI Defines
!define MUI_WELCOMEPAGE_TEXT "This wizard will guide you through the installation of ${PACKAGE_NAME} ${VERSION}. $\r$\n$\r$\nNote that the Windows version of ${PACKAGE_NAME} will only run on Windows XP, or higher.$\r$\n$\r$\n$\r$\n"
!define MUI_ABORTWARNING
!define MUI_ICON "${UNTANGLE_SETTINGS_DIR}\untangle.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "images\modern-wizard.bmp"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "images\modern-header.bmp"
!define MUI_WELCOMEPAGE_TITLE "$(^NameDA) Setup Wizard"
!define MUI_TEXT_FINISH_INFO_TITLE "$(^NameDA) Setup Wizard"
!define MUI_UNTEXT_FINISH_INFO_TITLE "$(^NameDA)"

!define MUI_FINISHPAGE_NOAUTOCLOSE
#!define MUI_FINISHPAGE_RUN
#!define MUI_FINISHPAGE_RUN_TEXT "Root certificates installed!"

var /GLOBAL FIREFOX_INSTALL_DIRECTORY
VIProductVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} "FileVersion" "${VERSION}"

# MUI Macros
# Install
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${UNTANGLE_SETTINGS_DIR}\SoftwareLicense.txt"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

# Uninstall
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH
# Languages
!insertmacro MUI_LANGUAGE "English"

!define SF_USELECTED  0
!macro SecUnSelect SecId
  Push $0
  IntOp $0 ${SF_USELECTED} | ${SF_RO}
  SectionSetFlags ${SecId} $0
  SectionSetText  ${SecId} ""
  Pop $0
!macroend
 
!define UnSelectSection '!insertmacro SecUnSelect'

# Added Descrption to Options for Component page.
LangString DESC_SecWin ${LANG_ENGLISH} "Import Untangle's Root CA to Windows Keystore (used by Internet Explorer, Chrome, and others)"
LangString DESC_SecFireFox ${LANG_ENGLISH} "Import Untangle's Root CA to Firefox's Default Profile"
LangString DESC_Install ${LANG_ENGLISH} "Install Untangle Root Certificates"

# Name for the packet uses Defines Packet_Name and Version.
Name "${PACKAGE_NAME}-${Version}"

# If Details should be showed or not
ShowInstDetails show
ShowUninstDetails show
#RequestExecutionLevel admin

# Filename for generated Installer
#OutFile "${UNTANGLE_PACKAGE_DIR}/${FILENAME}"

# Filename for generated Installer
OutFile "${FILENAME}"

# The "!" infront of Install makes it a required option.
Section "Untangle Root Certificates" -Install
    SectionIn RO
    SetOverwrite on
    DetailPrint "Installing Untangle Root CA Certificates"
    SetOutPath $INSTDIR
    File "${UNTANGLE_ROOTCA_DIR}\untangle.crt"
    File "${UNTANGLE_SETTINGS_DIR}\ut.ico"
SectionEnd

# Section Add to IE/Windows will push the Root CA in to windows keystore.
Section "Add to Windows" SecWin

    DetailPrint "Pushing Root CA to Windows Keystore"
    Push "$INSTDIR\untangle.crt"

    Call AddCertificateToStore
    Pop $0
    ${If} $0 != success
        MessageBox MB_OK "import failed: $0"
        DetailPrint "$0"
    ${EndIf}

    SetOutPath "$INSTDIR\bin"
    File "${UNTANGLE_SETTINGS_DIR}\bin\certmgr.exe"
        
SectionEnd

# Section Add to Firefox will import the Root CA to Firefoxs standard profile.
# /o infront of "Add to Firefox" makes it a Option if it should be on by default remove /0
Section /o "Add to Firefox" SecFirefoxUnchecked

    SetOutPath "$PROGRAMFILES\Mozilla Firefox"
    File "${UNTANGLE_SETTINGS_DIR}\untangle-firefox-certificate.cfg"
    SetOutPath "$PROGRAMFILES\Mozilla Firefox\defaults\pref"
    File "${UNTANGLE_SETTINGS_DIR}\untangle-firefox-preferences.js"

    SetOutPath "$INSTDIR\bin"
    File "${UNTANGLE_SETTINGS_DIR}\bin\freebl3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\libnspr4.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\libplc4.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\libplds4.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\msvcr100.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\nss3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\nssckbi.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\nssdbm3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\nssutil3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\smime3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\softokn3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\sqlite3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\ssl3.dll"

SectionEnd

# Section Add to Firefox will import the Root CA to Firefoxs standard profile.
# /o infront of "Add to Firefox" makes it a Option if it should be on by default remove /0
Section "Add to Firefox" SecFirefoxChecked

    SetOutPath "$PROGRAMFILES\Mozilla Firefox"
    File "${UNTANGLE_SETTINGS_DIR}\untangle-firefox-certificate.cfg"
    SetOutPath "$PROGRAMFILES\Mozilla Firefox\defaults\pref"
    File "${UNTANGLE_SETTINGS_DIR}\untangle-firefox-preferences.js"

    SetOutPath "$INSTDIR\bin"
    File "${UNTANGLE_SETTINGS_DIR}\bin\freebl3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\libnspr4.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\libplc4.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\libplds4.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\msvcr100.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\nss3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\nssckbi.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\nssdbm3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\nssutil3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\smime3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\softokn3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\sqlite3.dll"
    File "${UNTANGLE_SETTINGS_DIR}\bin\ssl3.dll"

SectionEnd

Function AddCertificateToStore
    Exch $0
    Push $1
    Push $R0

    System::Call "crypt32::CryptQueryObject(i ${CERT_QUERY_OBJECT_FILE}, w r0, \
        i ${CERT_QUERY_CONTENT_FLAG_ALL}, i ${CERT_QUERY_FORMAT_FLAG_ALL}, \
        i 0, i 0, i 0, i 0, i 0, i 0, *i .r0) i .R0"

    ${If} $R0 <> 0
        System::Call "crypt32::CertOpenStore(i ${CERT_STORE_PROV_SYSTEM}, i 0, i 0, \
        i ${CERT_STORE_OPEN_EXISTING_FLAG}|${CERT_SYSTEM_STORE_LOCAL_MACHINE}, \
        w 'ROOT') i .r1"

        ${If} $1 <> 0
            System::Call "crypt32::CertAddCertificateContextToStore(i r1, i r0, \
                i ${CERT_STORE_ADD_ALWAYS}, i 0) i .R0"
            System::Call "crypt32::CertFreeCertificateContext(i r0)"

            ${If} $R0 = 0
                StrCpy $0 "Unable to add certificate to certificate store"
            ${Else}
                StrCpy $0 "success"
            ${EndIf}

            System::Call "crypt32::CertCloseStore(i r1, i 0)"
        ${Else}
            System::Call "crypt32::CertFreeCertificateContext(i r0)"

            StrCpy $0 "Unable to open certificate store"
        ${EndIf}
    ${Else}
        StrCpy $0 "Unable to open certificate file"
    ${EndIf}

    Pop $R0
    Pop $1
    Exch $0

FunctionEnd

InstallDir "$PROGRAMFILES\${PACKAGE_NAME}"

# StrFuncs we want to use
${UnStrLoc}

# Function before installation start verify is application already exist.
Function .onInit
    SetShellVarContext all
 
	# Check if we're running on 64-bit Windows
	${If} ${RunningX64}
        SetRegView 64

        # Change the installation directory to C:\Program Files, but only if the
        # user has not provided a custom install location.
        ${If} "$INSTDIR" == "$PROGRAMFILES\${PACKAGE_NAME}"
            StrCpy $INSTDIR "$PROGRAMFILES64\${PACKAGE_NAME}"
        ${EndIf}
    ${EndIf}

    # Determine if Firefox is installer.
    ReadRegStr $FIREFOX_INSTALL_DIRECTORY HKLM \
        "SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\firefox.exe" \
        "Path"

    ${If} $FIREFOX_INSTALL_DIRECTORY == ''
        ${UnSelectSection} ${SecFirefoxChecked}
    ${Else}
        ${UnSelectSection} ${SecFirefoxUnchecked}
    ${EndIf}

    # Delete previous start menu
    RMDir /r "$SMPROGRAMS\${PACKAGE_NAME}"

    # Verify if Uninstall regvalue exist
    ReadRegStr $R0 HKLM \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" \
        "UninstallString"
        StrCmp $R0 "" done

    # Verify if Version regvalue is the same as installer.
    ReadRegStr $R1 HKLM \
        "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" \
        "DisplayVersion"
        StrCmp $R1 "${VERSION}" Reinstall

    MessageBox MB_OKCANCEL|MB_ICONEXCLAMATION \
        "${PACKAGE_NAME}-${VERSION} is not matching the installed version. $\n$\nClick `OK` to remove the \
        Current version or `Cancel` to cancel this upgrade/downgrade." \
        IDOK uninst
        Abort

    # -Reinstall if Verion is the same.
Reinstall:
    MessageBox MB_OKCANCEL|MB_ICONEXCLAMATION \
        "${PACKAGE_NAME}-${VERSION} is already installed. $\n$\nClick `OK` to reinstall the \
        current version or `Cancel` to cancel this reinstallation." \
        IDOK uninst
        Abort

#  Run the uninstaller
uninst:
    ClearErrors
    ExecWait '$R0 _?=$INSTDIR' # Do not copy the uninstaller to a temp file

    IfErrors no_remove_uninstaller done
    no_remove_uninstaller:

done:

FunctionEnd

# Post-install section
Section -post
	SetOverwrite on
	SetOutPath "$INSTDIR"

	# Store install folder in registry
	WriteRegStr HKLM "SOFTWARE\${PACKAGE_NAME}" "" "$INSTDIR"

	# Create uninstaller
	WriteUninstaller "$INSTDIR\Uninstall.exe"

    # Show up in Add/Remove programs
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "DisplayName" "${PACKAGE_NAME} ${VERSION}"
	WriteRegExpandStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "UninstallString" "$\"$INSTDIR\Uninstall.exe$\""
    WriteRegExpandStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "QuietUninstallString" "$\"$INSTDIR\Uninstall.exe$\" /S"
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "DisplayVersion" "${VERSION}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "Install_Dir" "$INSTDIR"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "Publisher" "Untangle"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "UrlInfoAbout" "http://www.untangle.com"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "InstallLocation" "$INSTDIR"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "DisplayIcon" "$INSTDIR\ut.ico"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "Helplink" "http://support.untangle.com"
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "NoModify" "1"
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "NoRepair" "1"        
SectionEnd

# Uninstaller Section
Section "Uninstall"

    Delete "$INSTDIR\Uninstall.exe"

    SetShellVarContext all
    ${If} ${RunningX64}
        SetRegView 64
    ${Endif}  

    DetailPrint "Uninstalling Untangle Root Certificates"

    ${un.EnumUsersReg} un.removeWindows temp.key

    ${un.EnumUsersReg} un.removeFirefox temp.key

    Delete "$INSTDIR\untangle.crt"
    Delete "$INSTDIR\ut.ico"
    Delete "$INSTDIR\bin\certmgr.exe"
    Delete "$INSTDIR\bin\freebl3.dll"
    Delete "$INSTDIR\bin\libnspr4.dll"
    Delete "$INSTDIR\bin\libplc4.dll"
    Delete "$INSTDIR\bin\libplds4.dll"
    Delete "$INSTDIR\bin\msvcr100.dll"
    Delete "$INSTDIR\bin\nss3.dll"
    Delete "$INSTDIR\bin\nssckbi.dll"
    Delete "$INSTDIR\bin\nssdbm3.dll"
    Delete "$INSTDIR\bin\nssutil3.dll"
    Delete "$INSTDIR\bin\smime3.dll"
    Delete "$INSTDIR\bin\softokn3.dll"
    Delete "$INSTDIR\bin\sqlite3.dll"
    Delete "$INSTDIR\bin\ssl3.dll"
    RMDir "$INSTDIR\bin"
    RMDir "$INSTDIR"

    DetailPrint "Remove Regvalues for Untangle Root Certificates"
    DeleteRegKey HKLM  "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}"
    DeleteRegKey HKLM  "Software\${PACKAGE_NAME}"

SectionEnd

Function un.removeWindows

    DetailPrint "Uninstalling Untangle Root Certificates from Windows keystore"

    Exch $0
    System::Call "crypt32::CertOpenStore(i ${CERT_STORE_PROV_SYSTEM}, i 0, i 0, \
    i ${CERT_STORE_OPEN_EXISTING_FLAG}|${CERT_SYSTEM_STORE_LOCAL_MACHINE}, \
    w 'ROOT') i .r1"

    ${If} $1 <> 0
        StrCpy $2 0
        ${Do}
            System::Call "crypt32::CertEnumCertificatesInStore(i r1, i r2) i .r2"
            ${If} $2 != 0
                System::Call "crypt32::CertGetNameString(i r2, \ 
                    i ${CERT_NAME_SIMPLE_DISPLAY_TYPE}, i 0, i 0, \ 
                    t .r4, i ${NSIS_MAX_STRLEN}) i.r3" 
                ${If} $3 != 0
                    ## Compare names here
                    ${UnStrLoc} $5 $4 "Untangle" ">"
                    ${If} $5 != ""
                        System::Call "crypt32::CertDuplicateCertificateContext(i r2) i.r6"
                        System::Call "crypt32::CertDeleteCertificateFromStore(i r6) i.r7"
                    ${Endif}
                ${EndIf}
            ${Else}
                ${ExitDo}
            ${EndIf}
        ${Loop}
        System::Call "crypt32::CertCloseStore(i r1, i 0)"
    ${Else}
        StrCpy $0 "Unable to open certificate store"
    ${EndIf}
FunctionEnd

Function un.removeFirefox
    Pop $0

    DetailPrint "Uninstalling Untangle Root Certificates from Firefox"
#    Delete "$PROGRAMFILES\Mozilla Firefox\untangle-firefox-certificate.cfg"
#    Delete "$PROGRAMFILES\Mozilla Firefox\defaults\pref\untangle-firefox-preferences.js"

    ReadINIStr $1 "$APPDATA\Mozilla\Firefox\profiles.ini" "Profile0" "Path"
    DetailPrint "Uninstalling Untangle Root Certificates from Firefox keystore"

    var /GLOBAL APPDATA_PATH
    ReadRegStr $APPDATA_PATH HKU \
        "$0\Software\Microsoft\Windows\CurrentVersion\Explorer\Shell Folders" \
        "AppData"

FunctionEnd

#  Function for Closeing running DirectoryLoginMonitorService.exe before Uninstall.
Function un.onInit
    ClearErrors

    ${nsProcess::Unload}
FunctionEnd

# Add Description text from Langstring Part
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SecWin} $(DESC_SecWin)
    !insertmacro MUI_DESCRIPTION_TEXT ${SecFireFoxUnchecked} $(DESC_SecFireFox)
    !insertmacro MUI_DESCRIPTION_TEXT ${SecFireFoxChecked} $(DESC_SecFireFox)
    !insertmacro MUI_DESCRIPTION_TEXT ${-Install} $(DESC_Install)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

