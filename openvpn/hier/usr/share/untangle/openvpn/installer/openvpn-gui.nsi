; ****************************************************************************
; * Copyright (C) 2002-2005 OpenVPN Solutions LLC                            *
; *               2004-2005 Updated by Mathias Sundman <mathias@nilings.se>  * 
; *  This program is free software; you can redistribute it and/or modify    *
; *  it under the terms of the GNU General Public License as published by    *
; *  the Free Software Foundation; either version 2 of the License, or       *
; *  (at your option) any later version.                                     *
; ****************************************************************************

; OpenVPN install script for Windows, using NSIS

!include "MUI.nsh"
!include "setpath.nsi"
!include "GetWindowsVersion.nsi"

!define HOME "openvpn"
!define MV_FILES "@UVM_CONF@/openvpn"
!define MV_PACKAGE_DIR "${MV_FILES}/client-packages"
!define MV_PKI_DIR "${MV_FILES}/pki"
!define BIN "${HOME}\bin"

!define PRODUCT_NAME "OpenVPN"
!define OPENVPN_VERSION "2.1.3"
!define GUI_VERSION "1.0.3"
!define VERSION "${OPENVPN_VERSION}-gui-${GUI_VERSION}"

!define TAP "tap0901"
!define TAPDRV "${TAP}.sys"
!define TAPDRVCAT "${TAP}.cat"

; something like "-DBG2"
!define OUTFILE_LABEL ""

; Default OpenVPN Service registry settings
!define SERV_CONFIG_DIR   "$INSTDIR\config"
!define SERV_CONFIG_EXT   "ovpn"
!define SERV_EXE_PATH     "$INSTDIR\bin\openvpn.exe"
!define SERV_LOG_DIR      "$INSTDIR\log"
!define SERV_PRIORITY     "NORMAL_PRIORITY_CLASS"
!define SERV_LOG_APPEND   "0"

; Default OpenVPN GUI registry settings
!define GUI_CONFIG_DIR    "$INSTDIR\config"
!define GUI_CONFIG_EXT    "ovpn"
!define GUI_EXE_PATH      "$INSTDIR\bin\openvpn.exe"
!define GUI_LOG_DIR       "$INSTDIR\log"
!define GUI_PRIORITY      "NORMAL_PRIORITY_CLASS"
!define GUI_LOG_APPEND    "0"
!define GUI_ALLOW_EDIT    "1"
!define GUI_ALLOW_SERVICE "0"
!define GUI_ALLOW_PROXY   "1"
!define GUI_ALLOW_PASSWORD "1"
!define GUI_SERVICE_ONLY  "0"
!define GUI_PSW_ATTEMPTS  "3"
!define GUI_UP_TIMEOUT    "15"
!define GUI_DOWN_TIMEOUT  "10"
!define GUI_PRE_TIMEOUT   "10"
!define GUI_SHOW_BALLOON  "1"
!define GUI_SHOW_SCRIPT   "1"
!define GUI_LOG_VIEWER    "$WINDIR\notepad.exe"
!define GUI_EDITOR        "$WINDIR\notepad.exe"
!define GUI_SUSPEND       "1"
!define GUI_SILENT_CONN   "0"

;--------------------------------
;Configuration

  ;General

  OutFile "${MV_PACKAGE_DIR}/setup-${COMMON_NAME}.exe"

  SetCompressor bzip2

  ShowInstDetails show
  ShowUninstDetails show

  ;Folder selection page
  InstallDir "$PROGRAMFILES\${PRODUCT_NAME}"
  
  ;Remember install folder
  InstallDirRegKey HKCU "Software\${PRODUCT_NAME}" ""

;--------------------------------
;Modern UI Configuration

  Name "${PRODUCT_NAME} ${VERSION}"

  !define MUI_COMPONENTSPAGE_SMALLDESC
  !define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\INSTALL-win32.txt"
  !define MUI_FINISHPAGE_NOAUTOCLOSE
  !define MUI_ABORTWARNING
  !define MUI_ICON "${HOME}\install-win32\openvpn.ico"
  !define MUI_UNICON "${HOME}\install-win32\openvpn.ico"
  !define MUI_HEADERIMAGE
  !define MUI_HEADERIMAGE_BITMAP "${HOME}\install-win32\install-whirl.bmp"
  !define MUI_UNFINISHPAGE_NOAUTOCLOSE

  !define MUI_WELCOMEPAGE_TITLE "Welcome to the ${PRODUCT_NAME} Setup Wizard"

  !define MUI_WELCOMEPAGE_TEXT "This wizard will guide you through the installation of:\r\n\r\nOpenVPN -  an Open Source VPN package by James Yonan.\r\n\r\nOpenVPN GUI - A Graphical User Interface for OpenVPN by Mathias Sundman\r\n\r\nNote that the Windows version of OpenVPN will only run on Win 2000, XP, or higher.\r\n\r\nVista and 64-Bit Support added by Björn Gustavsson\r\n\r\n\r\n"

  !define MUI_COMPONENTSPAGE_TEXT_TOP "Select the components to install/upgrade.  Stop any OpenVPN or OpenVPN GUI processes or the OpenVPN service if it is running."

  !insertmacro MUI_PAGE_WELCOME
  !insertmacro MUI_PAGE_LICENSE "${HOME}\install-win32\license.txt"
  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY
  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_PAGE_FINISH
  
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES  
  !insertmacro MUI_UNPAGE_FINISH

;--------------------------------
;Languages

  !insertmacro MUI_LANGUAGE "English"
  
;--------------------------------
;Language Strings
  
  LangString DESC_SecOpenVPNUserSpace ${LANG_ENGLISH} "Install OpenVPN user-space components, including openvpn.exe."
 
  LangString DESC_SecOpenSSLDLLs ${LANG_ENGLISH} "Install OpenSSL DLLs locally (may be omitted if DLLs are already installed globally)."

  LangString DESC_SecTAP ${LANG_ENGLISH} "Install/Upgrade the TAP-Win32/Win64 virtual device driver.  Will not interfere with CIPE."

  LangString DESC_SecTAPHidden ${LANG_ENGLISH} "Install the TAP device as hidden. The TAP device will not be visible under Network Connections."

  LangString DESC_SecService ${LANG_ENGLISH} "Install the OpenVPN service wrapper (openvpnserv.exe)"

  LangString DESC_SecOpenSSLUtilities ${LANG_ENGLISH} "Install the OpenSSL Utilities (used for generating public/private key pairs)."

  LangString DESC_SecAddPath ${LANG_ENGLISH} "Add OpenVPN executable directory to the current user's PATH."

  LangString DESC_SecAddShortcuts ${LANG_ENGLISH} "Add shortcuts to the current user's Start Menu."

  LangString DESC_SecFileAssociation ${LANG_ENGLISH} "Register OpenVPN config file association (*.${SERV_CONFIG_EXT})"

  LangString DESC_SecGUI ${LANG_ENGLISH} "Install OpenVPN GUI (A System tray application to control OpenVPN)"

  LangString DESC_SecGUIAuto ${LANG_ENGLISH} "Automatically start OpenVPN GUI at system startup"

;--------------------------------
;Data
  
;  LicenseData "${HOME}\install-win32\license.txt"

;--------------------------------
;Reserve Files
  
  ;Things that need to be extracted on first (keep these lines before any File command!)
  ;Only useful for BZIP2 compression
  
  ReserveFile "${HOME}\install-win32\install-whirl.bmp"

;--------------------------------
;Macros

!macro WriteRegStringIfUndef ROOT SUBKEY KEY VALUE
Push $R0
ReadRegStr $R0 "${ROOT}" "${SUBKEY}" "${KEY}"
StrCmp $R0 "" +1 +2
WriteRegStr "${ROOT}" "${SUBKEY}" "${KEY}" '${VALUE}'
Pop $R0
!macroend

!macro DelRegStringIfUnchanged ROOT SUBKEY KEY VALUE
Push $R0
ReadRegStr $R0 "${ROOT}" "${SUBKEY}" "${KEY}"
StrCmp $R0 '${VALUE}' +1 +2
DeleteRegValue "${ROOT}" "${SUBKEY}" "${KEY}"
Pop $R0
!macroend

!macro DelRegKeyIfUnchanged ROOT SUBKEY VALUE
Push $R0
ReadRegStr $R0 "${ROOT}" "${SUBKEY}" ""
StrCmp $R0 '${VALUE}' +1 +2
DeleteRegKey "${ROOT}" "${SUBKEY}"
Pop $R0
!macroend

!macro DelRegKeyIfEmpty ROOT SUBKEY
Push $R0
EnumRegValue $R0 "${ROOT}" "${SUBKEY}" 1
StrCmp $R0 "" +1 +2
DeleteRegKey /ifempty "${ROOT}" "${SUBKEY}"
Pop $R0
!macroend

;------------------------------------------
;Set reboot flag based on tapinstall return

Function CheckReboot
  IntCmp $R0 1 "" noreboot noreboot
  IntOp $R0 0 & 0
  SetRebootFlag true
  DetailPrint "REBOOT flag set"
 noreboot:
FunctionEnd

;--------------------------------
;Installer Sections

!ifndef SF_SELECTED
!define SF_SELECTED   1
!endif

!ifndef SF_RO
!define SF_RO         16
!endif

!define SF_NOT_RO     0xFFFFFFEF

Section "-Check User Rights and System" 
# Verify that user has admin privs
  UserInfo::GetName
  IfErrors ok
  Pop $R0
  UserInfo::GetAccountType
  Pop $R1
  StrCmp $R1 "Admin" ok
    Messagebox MB_OK "Administrator privileges required to install ${PRODUCT_NAME} [$R0/$R1]"
    Abort

ok:

# Check windows version
  Call GetWindowsVersion
  Pop $1
  StrCmp $1 "2000" goodwinver
  StrCmp $1 "XP" goodwinver
  StrCmp $1 "2003" goodwinver
  StrCmp $1 "VISTA" goodwinver
  StrCmp $1 "7" goodwinver

  Messagebox MB_OK "Sorry, ${PRODUCT_NAME} does not currently support Windows $1"
  Abort

goodwinver:

SectionEnd

Section "OpenVPN User-Space Components" SecOpenVPNUserSpace

  SetOverwrite on
  SetOutPath "$INSTDIR\bin"

  File "${HOME}\openvpn.exe"

SectionEnd

Section "OpenVPN GUI" SecGUI

  SetOverwrite on
  SetOutPath "$INSTDIR\bin"
  File "${HOME}\openvpn-gui.exe"

  # Include your custom config file(s) here.
  SetOutPath "$INSTDIR\config"
  File /oname=${SITE_NAME}.ovpn "${MV_PACKAGE_DIR}/client-${COMMON_NAME}.ovpn"

  # Named untangle-vpn so it is safe to overwrite the files in it.
  SetOutPath "$INSTDIR\config\untangle-vpn"
  File /oname=${SITE_NAME}-${COMMON_NAME}.crt "${MV_PKI_DIR}/client-${COMMON_NAME}.crt"
  File /oname=${SITE_NAME}-${COMMON_NAME}.key "${MV_PKI_DIR}/client-${COMMON_NAME}.key"
  File /oname=${SITE_NAME}-ca.crt "${MV_PKI_DIR}/ca.crt"

  SetOutPath "$INSTDIR"
  File "${HOME}\install-win32\OpenVPN_GUI_ReadMe.txt"

  CreateDirectory "$INSTDIR\log"
  CreateDirectory "$INSTDIR\config"
  CreateDirectory "$INSTDIR\config\untangle-vpn"


SectionEnd

Section "AutoStart OpenVPN GUI" SecGUIAuto
SectionEnd

Section "Hide the TAP-Win32/Win64 Virtual Ethernet Adapter" SecTAPHidden
SectionEnd

Section "OpenVPN Service" SecService

  SetOverwrite on

  SetOutPath "$INSTDIR\bin"
  File "${HOME}\service-win32\openvpnserv.exe"

  FileOpen $R0 "$INSTDIR\config\README.txt" w
  FileWrite $R0 "This directory should contain OpenVPN configuration files$\r$\n"
  FileWrite $R0 "each having an extension of .${SERV_CONFIG_EXT}$\r$\n"
  FileWrite $R0 "$\r$\n"
  FileWrite $R0 "When OpenVPN is started as a service, a separate OpenVPN$\r$\n"
  FileWrite $R0 "process will be instantiated for each configuration file.$\r$\n"
  FileClose $R0

  CreateDirectory "$INSTDIR\log"
  FileOpen $R0 "$INSTDIR\log\README.txt" w
  FileWrite $R0 "This directory will contain the log files for OpenVPN$\r$\n"
  FileWrite $R0 "sessions which are being run as a service.$\r$\n"
  FileClose $R0

SectionEnd

Section "OpenVPN File Associations" SecFileAssociation
SectionEnd

Section "OpenSSL DLLs" SecOpenSSLDLLs

  SetOverwrite on
  SetOutPath "$INSTDIR\bin"
  File "${BIN}\libeay32.dll"
  File "${BIN}\libssl32.dll"
  File "${BIN}\libpkcs11-helper-1.dll"

SectionEnd

Section "OpenSSL Utilities" SecOpenSSLUtilities

  SetOverwrite on
  SetOutPath "$INSTDIR\bin"
  File "${BIN}\openssl.exe"

SectionEnd

Section "TAP-Win32/Win64 Virtual Ethernet Adapter" SecTAP

SetOverwrite on
FileOpen $R0 "$INSTDIR\bin\addtap.bat" w
FileWrite $R0 "rem Add a new TAP-Win32/Win64 virtual ethernet adapter$\r$\n"
FileWrite $R0 '"$INSTDIR\bin\tapinstall.exe" install "$INSTDIR\driver\OemWin2k.inf" ${TAP}$\r$\n'
FileWrite $R0 "pause$\r$\n"
FileClose $R0

FileOpen $R0 "$INSTDIR\bin\deltapall.bat" w
FileWrite $R0 "echo WARNING: this script will delete ALL TAP-Win32/Win64 virtual adapters (use the device manager to delete adapters one at a time)$\r$\n"
FileWrite $R0 "pause$\r$\n"
FileWrite $R0 '"$INSTDIR\bin\tapinstall.exe" remove ${TAP}$\r$\n'
FileWrite $R0 "pause$\r$\n"
FileClose $R0

 GetVersion::WindowsPlatformArchitecture
  Pop $R0

StrCmp $R0 '64' 0 +3
 DetailPrint "64 is  $R0"
  Goto W64
StrCmp $R0 '32' 0 +3
 DetailPrint "32 is  $R0"
  Goto W32
# else
goto end

W32:
DetailPrint "We are running on a 32-bit system."
SetOutPath "$INSTDIR\bin"
File "${BIN}\ti3790\32\tapinstall.exe"
SetOutPath "$INSTDIR\driver"
File "${HOME}\tap-win32\i386\${TAPDRV}"
File "${HOME}\tap-win32\i386\${TAPDRVCAT}"
SectionGetFlags ${SecTAPHidden} $R0
IntOp $R0 $R0 & ${SF_SELECTED}
IntCmp $R0 ${SF_SELECTED} "" nohiddentap32 nohiddentap32
File "${HOME}\tap-win32-hiddentap\i386\OemWin2k.inf"
goto end
nohiddentap32:
File "${HOME}\tap-win32\i386\OemWin2k.inf"
goto end

W64:
DetailPrint "We are running on a 64-bit system."
SetOutPath "$INSTDIR\bin"
File "${BIN}\ti3790\64\tapinstall.exe"
SetOutPath "$INSTDIR\driver"
File "${HOME}\tap-win64\i386\${TAPDRV}"
File "${HOME}\tap-win64\i386\${TAPDRVCAT}"
SectionGetFlags ${SecTAPHidden} $R0
IntOp $R0 $R0 & ${SF_SELECTED}
IntCmp $R0 ${SF_SELECTED} "" nohiddentap64 nohiddentap64
File "${HOME}\tap-win64-hiddentap\i386\OemWin2k.inf"
goto end
nohiddentap64:
File "${HOME}\tap-win64\i386\OemWin2k.inf"
goto end

end:
SectionEnd

Section "Add OpenVPN to PATH" SecAddPath

  ; remove previously set path (if any)
  Push "$INSTDIR\bin"
  Call RemoveFromPath

  ; append our bin directory to end of current user path
  Push "$INSTDIR\bin"
  Call AddToPath

SectionEnd

Section "Add Shortcuts to Start Menu" SecAddShortcuts

  SetOverwrite on
  CreateDirectory "$SMPROGRAMS\OpenVPN"
;;---  CreateShortCut "$SMPROGRAMS\OpenVPN\OpenVPN Win32 README.lnk" "$INSTDIR\INSTALL-win32.txt" ""
;;---  WriteINIStr "$SMPROGRAMS\OpenVPN\OpenVPN Manual Page.url" "InternetShortcut" "URL" "http://openvpn.sourceforge.net/man.html"
  WriteINIStr "$SMPROGRAMS\OpenVPN\OpenVPN Web Site.url" "InternetShortcut" "URL" "http://openvpn.sourceforge.net/"
  CreateShortCut "$SMPROGRAMS\OpenVPN\Uninstall OpenVPN.lnk" "$INSTDIR\Uninstall.exe"

  SectionGetFlags ${SecGUI} $R0
  IntOp $R0 $R0 & ${SF_SELECTED}
  IntCmp $R0 ${SF_SELECTED} "" nogui nogui

  CreateShortCut "$SMPROGRAMS\OpenVPN\OpenVPN GUI.lnk" "$INSTDIR\bin\openvpn-gui.exe"
  CreateShortCut "$SMPROGRAMS\OpenVPN\OpenVPN GUI ReadMe.lnk" "$INSTDIR\OpenVPN_GUI_ReadMe.txt"

nogui:

SectionEnd


;--------------------
;Post-install section

Section -post

  ; delete old devcon.exe
  Delete "$INSTDIR\bin\devcon.exe"

  ;
  ; install/upgrade TAP-Win32 driver if selected, using tapinstall.exe
  ;
  SectionGetFlags ${SecTAP} $R0
  IntOp $R0 $R0 & ${SF_SELECTED}
  IntCmp $R0 ${SF_SELECTED} "" notap notap
    ; TAP install/update was selected.
    ; Should we install or update?
    ; If tapinstall error occurred, $5 will
    ; be nonzero.
    IntOp $5 0 & 0
    nsExec::ExecToStack '"$INSTDIR\bin\tapinstall.exe" hwids ${TAP}'
    Pop $R0 # return value/error/timeout
    IntOp $5 $5 | $R0
    DetailPrint "tapinstall hwids returned: $R0"

    ; If tapinstall output string contains "${TAP}" we assume
    ; that TAP device has been previously installed,
    ; therefore we will update, not install.
    Push "${TAP}"
    Call StrStr
    Pop $R0

    IntCmp $5 0 "" tapinstall_check_error tapinstall_check_error
    IntCmp $R0 -1 tapinstall

 ;tapupdate:
    DetailPrint "TAP-Win32 UPDATE"
    nsExec::ExecToLog '"$INSTDIR\bin\tapinstall.exe" update "$INSTDIR\driver\OemWin2k.inf" ${TAP}'
    Pop $R0 # return value/error/timeout
    Call CheckReboot
    IntOp $5 $5 | $R0
    DetailPrint "tapinstall update returned: $R0"
    Goto tapinstall_check_error

 tapinstall:
    DetailPrint "TAP-Win32 REMOVE OLD TAP"
    nsExec::ExecToLog '"$INSTDIR\bin\tapinstall.exe" remove TAP'
    Pop $R0 # return value/error/timeout
    DetailPrint "tapinstall remove TAP returned: $R0"
    nsExec::ExecToLog '"$INSTDIR\bin\tapinstall.exe" remove TAPDEV'
    Pop $R0 # return value/error/timeout
    DetailPrint "tapinstall remove TAPDEV returned: $R0"

    DetailPrint "TAP-Win32 INSTALL (${TAP})"
    nsExec::ExecToLog '"$INSTDIR\bin\tapinstall.exe" install "$INSTDIR\driver\OemWin2k.inf" ${TAP}'
    Pop $R0 # return value/error/timeout
    Call CheckReboot
    IntOp $5 $5 | $R0
    DetailPrint "tapinstall install returned: $R0"

 tapinstall_check_error:
    DetailPrint "tapinstall cumulative status: $5"
    IntCmp $5 0 notap
    MessageBox MB_OK "An error occurred installing the TAP-Win32 device driver."

 notap:

  ; Store install folder in registry
  WriteRegStr HKLM SOFTWARE\OpenVPN "" $INSTDIR

  ; install as a service if requested
  SectionGetFlags ${SecService} $R0
  IntOp $R0 $R0 & ${SF_SELECTED}
  IntCmp $R0 ${SF_SELECTED} "" noserv noserv

    ; set registry parameters for openvpnserv	
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN" "config_dir"  "${SERV_CONFIG_DIR}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN" "config_ext"  "${SERV_CONFIG_EXT}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN" "exe_path"    "${SERV_EXE_PATH}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN" "log_dir"     "${SERV_LOG_DIR}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN" "priority"    "${SERV_PRIORITY}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN" "log_append"  "${SERV_LOG_APPEND}"

    ; install openvpnserv as a service
    DetailPrint "Previous Service REMOVE (if exists)"
    nsExec::ExecToLog '"$INSTDIR\bin\openvpnserv.exe" -remove'
    Pop $R0 # return value/error/timeout
    DetailPrint "Service INSTALL"
    nsExec::ExecToLog '"$INSTDIR\bin\openvpnserv.exe" -install'
    Pop $R0 # return value/error/timeout

 noserv:
  ; Store install folder in registry
  WriteRegStr HKLM SOFTWARE\OpenVPN-GUI "" $INSTDIR

  ; Set registry keys for openvpn-gui if gui is requested
  SectionGetFlags ${SecGUI} $R0
  IntOp $R0 $R0 & ${SF_SELECTED}
  IntCmp $R0 ${SF_SELECTED} "" nogui nogui

    ; set registry parameters for openvpn-gui	
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "config_dir"      "${GUI_CONFIG_DIR}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "config_ext"      "${GUI_CONFIG_EXT}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "exe_path"        "${GUI_EXE_PATH}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "log_dir"         "${GUI_LOG_DIR}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "priority"        "${GUI_PRIORITY}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "log_append"      "${GUI_LOG_APPEND}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "allow_edit"      "${GUI_ALLOW_EDIT}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "allow_service"   "${GUI_ALLOW_SERVICE}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "allow_proxy"     "${GUI_ALLOW_PROXY}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "allow_password"  "${GUI_ALLOW_PASSWORD}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "service_only"    "${GUI_SERVICE_ONLY}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "log_viewer"      "${GUI_LOG_VIEWER}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "passphrase_attempts"   "${GUI_PSW_ATTEMPTS}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "editor"                "${GUI_EDITOR}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "connectscript_timeout" "${GUI_UP_TIMEOUT}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "disconnectscript_timeout" "${GUI_DOWN_TIMEOUT}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "preconnectscript_timeout" "${GUI_PRE_TIMEOUT}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "silent_connection"     "${GUI_SILENT_CONN}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "show_balloon"          "${GUI_SHOW_BALLOON}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "show_script_window"    "${GUI_SHOW_SCRIPT}"
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\OpenVPN-GUI" "disconnect_on_suspend" "${GUI_SUSPEND}"

  ; AutoStart OpenVPN GUI if requested
  SectionGetFlags ${SecGUIAuto} $R0
  IntOp $R0 $R0 & ${SF_SELECTED}
  IntCmp $R0 ${SF_SELECTED} "" nogui nogui

    ; set registry parameters for openvpn-gui	
    !insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Run" "openvpn-gui"  "$INSTDIR\bin\openvpn-gui.exe"

 nogui:
  ; Store README, license, icon
  SetOverwrite on
  SetOutPath $INSTDIR
  File "${HOME}\install-win32\INSTALL-win32.txt"
  File "${HOME}\install-win32\license.txt"
  File "${HOME}\install-win32\openvpn.ico"

  ; Create file association if requested
  SectionGetFlags ${SecFileAssociation} $R0
  IntOp $R0 $R0 & ${SF_SELECTED}
  IntCmp $R0 ${SF_SELECTED} "" noass noass
    !insertmacro WriteRegStringIfUndef HKCR ".${SERV_CONFIG_EXT}" "" "OpenVPNFile"
    !insertmacro WriteRegStringIfUndef HKCR "OpenVPNFile" "" "OpenVPN Config File"
    !insertmacro WriteRegStringIfUndef HKCR "OpenVPNFile\shell" "" "open"
    !insertmacro WriteRegStringIfUndef HKCR "OpenVPNFile\DefaultIcon" "" "$INSTDIR\openvpn.ico,0"
    !insertmacro WriteRegStringIfUndef HKCR "OpenVPNFile\shell\open\command" "" 'notepad.exe "%1"'
    !insertmacro WriteRegStringIfUndef HKCR "OpenVPNFile\shell\run" "" "Start OpenVPN on this config file"
    !insertmacro WriteRegStringIfUndef HKCR "OpenVPNFile\shell\run\command" "" '"$INSTDIR\bin\openvpn.exe" --pause-exit --config "%1"'

 noass:
; These shortcuts are just a little too complicated.
;;---    IfFileExists "$INSTDIR\bin\addtap.bat" "" trydeltap
;;---      CreateShortCut "$SMPROGRAMS\OpenVPN\Add a new TAP-Win32 virtual ethernet adapter.lnk" "$INSTDIR\bin\addtap.bat" ""

;;--- trydeltap:
;;---    IfFileExists "$INSTDIR\bin\deltapall.bat" "" config_shortcut
;;---      CreateShortCut "$SMPROGRAMS\OpenVPN\Delete ALL TAP-Win32 virtual ethernet adapters.lnk" "$INSTDIR\bin\deltapall.bat" ""

    ; Create start menu shortcuts for config and log directories
;;--- config_shortcut:
    IfFileExists "$INSTDIR\config" "" log_shortcut
      CreateShortCut "$SMPROGRAMS\OpenVPN\OpenVPN configuration file directory.lnk" "$INSTDIR\config" ""

 log_shortcut:
;;---    IfFileExists "$INSTDIR\log" "" samp_shortcut
;;---      CreateShortCut "$SMPROGRAMS\OpenVPN\OpenVPN log file directory.lnk" "$INSTDIR\log" ""

;;--- genkey_shortcut:
;;---    IfFileExists "$INSTDIR\bin\openvpn.exe" "" noshortcuts
;;---      IfFileExists "$INSTDIR\config" "" noshortcuts
;;---        CreateShortCut "$SMPROGRAMS\OpenVPN\Generate a static OpenVPN key.lnk" "$INSTDIR\bin\openvpn.exe" '--pause-exit --verb 3 --genkey --secret "$INSTDIR\config\key.txt"' "$INSTDIR\openvpn.ico" 0

;;--- noshortcuts:
  ; Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  ; Show up in Add/Remove programs
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OpenVPN" "DisplayName" "OpenVPN ${VERSION}"
  WriteRegExpandStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OpenVPN" "UninstallString" "$INSTDIR\Uninstall.exe"

  ; Advise a reboot
  ;Messagebox MB_OK "IMPORTANT: Rebooting the system is advised in order to finalize TAP-Win32 driver installation/upgrade (this is an informational message only, pressing OK will not reboot)."

  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayIcon" "$INSTDIR\openvpn.ico"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayVersion" "${VERSION}"

SectionEnd

;--------------------------------
;Descriptions

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecOpenVPNUserSpace} $(DESC_SecOpenVPNUserSpace)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecGUI} $(DESC_SecGUI)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecGUIAuto} $(DESC_SecGUIAuto)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecTAP} $(DESC_SecTAP)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecTAPHidden} $(DESC_SecTAPHidden)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecOpenSSLUtilities} $(DESC_SecOpenSSLUtilities)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecOpenSSLDLLs} $(DESC_SecOpenSSLDLLs)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecAddPath} $(DESC_SecAddPath)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecAddShortcuts} $(DESC_SecAddShortcuts)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecService} $(DESC_SecService)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecFileAssociation} $(DESC_SecFileAssociation)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Function .onInit
  ClearErrors
  UserInfo::GetName
  IfErrors ok
  Pop $R0
  UserInfo::GetAccountType
  Pop $R1
  StrCmp $R1 "Admin" ok
    Messagebox MB_OK "Administrator privileges are required to install OpenVPN [$R0/$R1]"
    Abort
  ok:

  Push $R0
  ReadRegStr $R0 HKLM SOFTWARE\OpenVPN-GUI ""
  StrCmp $R0 "" goon

    Messagebox MB_YESNO "It seems the package ${PRODUCT_NAME} (OpenVPN GUI) is already installed.$\r$\nWe recommend you to uninstall it in the standard way before proceeding. Continue installing?" IDYES goon
    Abort

  goon:
  Pop $R0

  Push $R0
  Push $R1
  FindWindow $R0 "openvpn-gui"
  IntCmp $R0 0 donerun

    Messagebox MB_YESNO|MB_ICONEXCLAMATION "OpenVPN GUI is currently running.$\r$\nUntil you terminate it, all files that belong to it cannot be updated.$\r$\nShall this program be killed now? If true, all existing connections will be closed." IDNO donerun

    SendMessage $R0 ${WM_DESTROY} 0 0 $R1 /TIMEOUT=7000
    IntCmp $R1 0 donerun

      Messagebox MB_OK|MB_ICONEXCLAMATION "Trouble terminating OpenVPN GUI, please close it and then click OK."

  donerun:
  Pop $R1
  Pop $R0

  ; Don't install the TAP driver as hiddden as default.
;; Commented out to hide by default
;;  SectionSetFlags ${SecTAPHidden} 0
FunctionEnd

Function .onSelChange
  Push $0

  ;Check if Section OpenVPN GUI is selected.
  SectionGetFlags ${SecGUI} $0
  IntOp $0 $0 & ${SF_SELECTED}
  IntCmp $0 ${SF_SELECTED} "" noautogui noautogui

  ;GUI was selected so set GUIAuto to Not-ReadOnly.
  SectionGetFlags ${SecGUIAuto} $0
  IntOp $0 $0 & ${SF_NOT_RO}
  SectionSetFlags ${SecGUIAuto} $0
  goto CheckTAP

  noautogui:
  SectionSetFlags ${SecGUIAuto} ${SF_RO}


  CheckTAP:
  ;Check if Section Install-TAP is selected.
  SectionGetFlags ${SecTAP} $0
  IntOp $0 $0 & ${SF_SELECTED}
  IntCmp $0 ${SF_SELECTED} "" notap notap

  ;TAP was selected so set TAPHidden to Not-ReadOnly.
  SectionGetFlags ${SecTAPHidden} $0
  IntOp $0 $0 & ${SF_NOT_RO}
  SectionSetFlags ${SecTAPHidden} $0
  goto end

  notap:
  SectionSetFlags ${SecTAPHidden} ${SF_RO}

  end:
  Pop $0

FunctionEnd

Function .onInstSuccess
  IfFileExists "$INSTDIR\bin\openvpn-gui.exe" "" nogui
    ExecShell open "$INSTDIR\bin\openvpn-gui.exe"
  nogui:

FunctionEnd

;--------------------------------
;Uninstaller Section

Function un.onInit
  ClearErrors
  UserInfo::GetName
  IfErrors ok
  Pop $R0
  UserInfo::GetAccountType
  Pop $R1
  StrCmp $R1 "Admin" ok
    Messagebox MB_OK "Administrator privileges required to uninstall OpenVPN [$R0/$R1]"
    Abort
  ok:
  Push $R0
  Push $R1
  FindWindow $R0 "openvpn-gui"
  IntCmp $R0 0 donerun

    Messagebox MB_YESNO|MB_ICONEXCLAMATION "OpenVPN GUI is currently running.$\r$\nUntil you terminate it, all files that belong to it cannot be removed.$\r$\nShall this program be killed now? If true, all existing connections will be closed." IDNO donerun

    SendMessage $R0 ${WM_DESTROY} 0 0 $R1 /TIMEOUT=7000
    IntCmp $R1 0 donerun

      Messagebox MB_OK|MB_ICONEXCLAMATION "Trouble terminating OpenVPN GUI, please close it and then click OK."

  donerun:
  Pop $R1
  Pop $R0


FunctionEnd

Section "Uninstall"

  DetailPrint "Service REMOVE"
  nsExec::ExecToLog '"$INSTDIR\bin\openvpnserv.exe" -remove'
  Pop $R0 # return value/error/timeout

  Sleep 2000

  DetailPrint "TAP-Win32/Win64 REMOVE"
  nsExec::ExecToLog '"$INSTDIR\bin\tapinstall.exe" remove ${TAP}'
  Pop $R0 # return value/error/timeout
  DetailPrint "tapinstall remove returned: $R0"

  Push "$INSTDIR\bin"
  Call un.RemoveFromPath

  RMDir /r $SMPROGRAMS\OpenVPN

  Delete "$INSTDIR\bin\openvpn.exe"
  Delete "$INSTDIR\bin\openvpnserv.exe"
  Delete "$INSTDIR\bin\openvpn-gui.exe"
  Delete "$INSTDIR\bin\libeay32.dll"
  Delete "$INSTDIR\bin\libssl32.dll"
  Delete "$INSTDIR\bin\libpkcs11-helper-1.dll"
  Delete "$INSTDIR\bin\tapinstall.exe"
  Delete "$INSTDIR\bin\addtap.bat"
  Delete "$INSTDIR\bin\deltapall.bat"
  
  
  Delete "$INSTDIR\config\README.txt"

  Delete "$INSTDIR\log\README.txt"

  Delete "$INSTDIR\driver\OemWin2k.inf"
  Delete "$INSTDIR\driver\${TAPDRV}"
  Delete "$INSTDIR\driver\${TAPDRVCAT}"

  Delete "$INSTDIR\bin\openssl.exe"

  Delete "$INSTDIR\OpenVPN_GUI_ReadMe.txt"
  Delete "$INSTDIR\INSTALL-win32.txt"
  Delete "$INSTDIR\openvpn.ico"
  Delete "$INSTDIR\license.txt"
  Delete "$INSTDIR\Uninstall.exe"

  RMDir "$INSTDIR\bin"
  RMDir "$INSTDIR\driver"
  RMDir "$INSTDIR"

  !insertmacro DelRegKeyIfUnchanged HKCR ".${SERV_CONFIG_EXT}" "OpenVPNFile"
  DeleteRegKey HKCR "OpenVPNFile"
  DeleteRegKey HKLM SOFTWARE\OpenVPN
  DeleteRegKey HKLM SOFTWARE\OpenVPN-GUI
  DeleteRegKey HKCU "Software\${PRODUCT_NAME}"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OpenVPN"
  DeleteRegValue HKLM "Software\Microsoft\Windows\CurrentVersion\Run" "openvpn-gui"

SectionEnd
