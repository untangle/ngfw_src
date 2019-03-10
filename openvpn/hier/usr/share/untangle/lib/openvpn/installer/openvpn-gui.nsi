; ****************************************************************************
; * Copyright (C) 2002-2010 OpenVPN Technologies, Inc.                       *
; * Copyright (C)      2012 Alon Bar-Lev <alon.barlev@gmail.com>             *
; *  This program is free software; you can redistribute it and/or modify    *
; *  it under the terms of the GNU General Public License version 2          *
; *  as published by the Free Software Foundation.                           *
; ****************************************************************************

; OpenVPN install script for Windows, using NSIS
; WebFooL was here ;-)
; mahotz was here too!

SetCompressor lzma

; Modern user interface
!include "MUI.nsh"
!include "LogicLib.nsh"

; Install for all users. MultiUser.nsh also calls SetShellVarContext to point
; the installer to global directories (e.g. Start menu, desktop, etc.)
!define MULTIUSER_EXECUTIONLEVEL Admin
!include "MultiUser.nsh"
!include "x64.nsh"
!include "DotNetChecker.nsh"
!include "nsProcess.nsh"
; WinMessages.nsh is needed to send WM_CLOSE to the GUI if it is still running
!include "WinMessages.nsh"

; EnvVarUpdate.nsh is needed to update the PATH environment variable
!include "EnvVarUpdate.nsh"

; Read the command-line parameters
!insertmacro GetParameters
!insertmacro GetOptions

; Default service settings
; The at-PREFIX-at tag lets this work in prod and dev dev environments
!define OPENVPN_CONFIG_EXT   "ovpn"
!define UNTANGLE_SETTINGS_DIR "@PREFIX@/usr/share/untangle/settings/openvpn"
!define UNTANGLE_PACKAGE_DIR "/tmp/openvpn/client-packages"
!define OPENVPN_ROOT "openvpn"
!define PACKAGE_NAME "OpenVPN"
!define OPENVPN_VERSION "2.4.7"
!define GUI_VERSION "11.12.0.0"
!define VERSION "${OPENVPN_VERSION}-gui-${GUI_VERSION}"
!define OUTFILE_LABEL ""

;--------------------------------
;Configuration

;General

; Package name as shown in the installer GUI
Name "${PACKAGE_NAME} ${OPENVPN_VERSION}"

; On 64-bit Windows the constant $PROGRAMFILES defaults to
; C:\Program Files (x86) and on 32-bit Windows to C:\Program Files. However,
; the .onInit function (see below) takes care of changing this for 64-bit
; Windows.
InstallDir "$PROGRAMFILES\${PACKAGE_NAME}"

; Installer filename
OutFile "${UNTANGLE_PACKAGE_DIR}/setup-${COMMON_NAME}.exe"

ShowInstDetails show
ShowUninstDetails show

;Remember install folder
InstallDirRegKey HKLM "SOFTWARE\${PACKAGE_NAME}" ""

;--------------------------------
;Modern UI Configuration

; Compile-time constants which we'll need during install
!define MUI_WELCOMEPAGE_TEXT "This wizard will guide you through the installation of ${PACKAGE_NAME} ${OPENVPN_VERSION}, an Open Source VPN package by James Yonan.$\r$\n$\r$\nNote that the Windows version of ${PACKAGE_NAME} will only run on Windows XP, or higher.$\r$\n$\r$\n$\r$\n"

!define MUI_COMPONENTSPAGE_TEXT_TOP "Select the components to install/upgrade.  Stop any ${PACKAGE_NAME} processes or the ${PACKAGE_NAME} service if it is running.  All DLLs are installed locally."

!define MUI_COMPONENTSPAGE_SMALLDESC
!define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\doc\INSTALL-win32.txt"
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_ABORTWARNING
!define MUI_ICON "${OPENVPN_ROOT}\install-win32\openvpn.ico"
!define MUI_UNICON "${OPENVPN_ROOT}\install-win32\openvpn.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP "${OPENVPN_ROOT}\install-win32\modern-wizard.bmp"
!define MUI_HEADERIMAGE 
!define MUI_HEADERIMAGE_BITMAP "${OPENVPN_ROOT}\install-win32\modern-header.bmp"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE


!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${OPENVPN_ROOT}\install-win32\license.txt"
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

LangString DESC_SecOpenVPNUserSpace ${LANG_ENGLISH} "Install ${PACKAGE_NAME} user-space components, including openvpn.exe."

LangString DESC_SecOpenVPNGUI ${LANG_ENGLISH} "Install ${PACKAGE_NAME} New OpenVPN-Gui"

LangString DESC_SecTAP ${LANG_ENGLISH} "Install/upgrade the TAP virtual device driver."

LangString DESC_SecOpenSSLDLLs ${LANG_ENGLISH} "Install OpenSSL DLLs locally (may be omitted if DLLs are already installed globally)."

LangString DESC_SecLZODLLs ${LANG_ENGLISH} "Install LZO DLLs locally (may be omitted if DLLs are already installed globally)."

LangString DESC_SecPKCS11DLLs ${LANG_ENGLISH} "Install PKCS#11 helper DLLs locally (may be omitted if DLLs are already installed globally)."

LangString DESC_SecService ${LANG_ENGLISH} "Install the ${PACKAGE_NAME} service wrapper (openvpnserv.exe)"

LangString DESC_SecOpenSSLUtilities ${LANG_ENGLISH} "Install the OpenSSL Utilities (used for generating public/private key pairs)."

LangString DESC_SecAddPath ${LANG_ENGLISH} "Add ${PACKAGE_NAME} executable directory to the current user's PATH."

LangString DESC_SecAddShortcuts ${LANG_ENGLISH} "Add ${PACKAGE_NAME} shortcuts to the current user's Start Menu."

LangString DESC_SecLaunchGUIOnLogon ${LANG_ENGLISH} "Launch ${PACKAGE_NAME} GUI on user logon."

LangString DESC_SecFileAssociation ${LANG_ENGLISH} "Register ${PACKAGE_NAME} config file association (*.${OPENVPN_CONFIG_EXT})"

;--------------------------------
;Reserve Files

;Things that need to be extracted on first (keep these lines before any File command!)
;Only useful for BZIP2 compression

ReserveFile "${OPENVPN_ROOT}\install-win32\modern-header.bmp"

;--------------------------------
;Macros

!macro SelectByParameter SECT PARAMETER DEFAULT
	${GetOptions} $R0 "/${PARAMETER}=" $0
	${If} ${DEFAULT} == 0
		${If} $0 == 1
			!insertmacro SelectSection ${SECT}
		${EndIf}
	${Else}
		${If} $0 != 0
			!insertmacro SelectSection ${SECT}
		${EndIf}
	${EndIf}
!macroend

!macro WriteRegStringIfUndef ROOT SUBKEY KEY VALUE
	Push $R0
	ReadRegStr $R0 "${ROOT}" "${SUBKEY}" "${KEY}"
	${If} $R0 == ""
		WriteRegStr "${ROOT}" "${SUBKEY}" "${KEY}" '${VALUE}'
	${EndIf}
	Pop $R0
!macroend

!macro DelRegKeyIfUnchanged ROOT SUBKEY VALUE
	Push $R0
	ReadRegStr $R0 "${ROOT}" "${SUBKEY}" ""
	${If} $R0 == '${VALUE}'
		DeleteRegKey "${ROOT}" "${SUBKEY}"
	${EndIf}
	Pop $R0
!macroend

Function CacheServiceState
	; We will set the defaults for a service only if it did not exist before:
	; otherwise we restore the previous state. Startuptype is cached for
	; OpenVPNService as we need to reinstall it, and it might be pointing to
	; the old legacy service.
	Var /GLOBAL iservice_existed
	Var /GLOBAL iservice_was_running
	Var /GLOBAL legacy_service_existed
	Var /GLOBAL legacy_service_was_running
	Var /GLOBAL service_existed
	Var /GLOBAL service_starttype
	Var /GLOBAL service_was_running

	DetailPrint "Caching service states"

	SimpleSC::ExistsService "OpenVPNServiceInteractive"
	Pop $iservice_existed
	SimpleSC::GetServiceStatus "OpenVPNServiceInteractive"
	Pop $0
	Pop $iservice_was_running

	SimpleSC::ExistsService "OpenVPNServiceLegacy"
	Pop $legacy_service_existed
	SimpleSC::GetServiceStatus "OpenVPNServiceLegacy"
	Pop $0
	Pop $legacy_service_was_running

	SimpleSC::ExistsService "OpenVPNService"
	Pop $service_existed
	SimpleSC::GetServiceStartType "OpenVPNService"
	Pop $0
	Pop $service_starttype
	SimpleSC::GetServiceStatus "OpenVPNService"
	Pop $0
	Pop $service_was_running
FunctionEnd

Function RestoreServiceState

	${If} $iservice_was_running == 4
	${OrIf} $iservice_existed != 0
		DetailPrint "Starting OpenVPN Interactive Service"
		SimpleSC::StartService "OpenVPNServiceInteractive" "" 5
	${EndIf}

	${If} $legacy_service_was_running == 4
		DetailPrint "Restarting OpenVPN Legacy Service"
		SimpleSC::StartService "OpenVPNServiceLegacy" "" 10
	${EndIf}

	${If} $service_existed == 0
		DetailPrint "Restoring starttype of OpenVPN Service"
		SimpleSC::SetServiceStartType "OpenVPNService" $service_starttype

		${If} $service_was_running == 4
			DetailPrint "Restarting OpenVPN Service"
			SimpleSC::StartService "OpenVPNService" "" 10
		${EndIf}
	${EndIf}

FunctionEnd

Function StopServices
	DetailPrint "Stopping OpenVPN services..."
	SimpleSC::StopService "OpenVPNServiceInteractive" 0 10
	SimpleSC::StopService "OpenVPNServiceLegacy" 0 10
	SimpleSC::StopService "OpenVPNService" 0 10
FunctionEnd
;--------------------
;Pre-install section

Section -pre

	Push $0 ; for FindWindow
	FindWindow $0 "OpenVPN-GUI"
	StrCmp $0 0 guiNotRunning

	MessageBox MB_YESNO|MB_ICONEXCLAMATION "To perform the specified operation, OpenVPN-GUI needs to be closed. You will have to restart it manually after the installation has completed. Shall I close it?" /SD IDYES IDNO guiEndNo
	Goto guiEndYes

	guiEndNo:
		Quit

	guiEndYes:
		DetailPrint "Closing OpenVPN-GUI..."
		; user wants to close GUI as part of install/upgrade
		FindWindow $0 "OpenVPN-GUI"
		IntCmp $0 0 guiNotRunning
		SendMessage $0 ${WM_CLOSE} 0 0
		Sleep 100
		Goto guiEndYes

	guiNotRunning:
		; Store the current state of OpenVPN services
		Call CacheServiceState
		Call StopServices

		Sleep 3000

		; check for running openvpn.exe processes
		${nsProcess::FindProcess} "openvpn.exe" $R0
		${If} $R0 == 0
			MessageBox MB_OK|MB_ICONEXCLAMATION "The installation cannot continue as OpenVPN is currently running. Please close all OpenVPN instances and re-run the installer."
			Call RestoreServiceState
			Quit
		${EndIf}

		; openvpn.exe + GUI not running/closed successfully, carry on with install/upgrade

		; Delete previous start menu folder
		RMDir /r "$SMPROGRAMS\${PACKAGE_NAME}"

	Pop $0 ; for FindWindow

SectionEnd

Section /o "-workaround" SecAddShortcutsWorkaround
	; this section should be selected as SecAddShortcuts
	; as we don't want to move SecAddShortcuts to top of selection
SectionEnd

Section /o "-launchondummy" SecLaunchGUIOnLogon0
	; this section should be selected as SecLaunchGUIOnLogon
	; this is here as we don't want to move that section to the top
SectionEnd

Section /o "${PACKAGE_NAME} User-Space Components" SecOpenVPNUserSpace

	SetOverwrite on

        ${If} ${RunningX64}
        DetailPrint "Installing 64-bit OpenVPN.exe."
        SetOutPath "$INSTDIR\bin"
        File "${OPENVPN_ROOT}\bin-win64\openvpn.exe"
        ${Else}
        DetailPrint "Installing 32-bit OpenVPN.exe."
        SetOutPath "$INSTDIR\bin"
        File "${OPENVPN_ROOT}\bin-win32\openvpn.exe"
        ${EndIf}


        SetOutPath "$INSTDIR\doc"
	File "${OPENVPN_ROOT}\install-win32\INSTALL-win32.txt"
	File "${OPENVPN_ROOT}\install-win32\openvpn.8.html"

	${If} ${SectionIsSelected} ${SecAddShortcutsWorkaround}
		CreateDirectory "$SMPROGRAMS\${PACKAGE_NAME}\Documentation"
		CreateShortCut "$SMPROGRAMS\${PACKAGE_NAME}\Documentation\${PACKAGE_NAME} Manual Page.lnk" "$INSTDIR\doc\openvpn.8.html"
		CreateShortCut "$SMPROGRAMS\${PACKAGE_NAME}\Documentation\${PACKAGE_NAME} Windows Notes.lnk" "$INSTDIR\doc\INSTALL-win32.txt"
	${EndIf}
	
	Call CoreSetup
	
SectionEnd

Section /o "${PACKAGE_NAME} Service" SecService

SetOverwrite on

	nsExec::ExecToLog '"$INSTDIR\bin\openvpnserv2.exe" -remove'
	Pop $R0 # return value/error/timeout
	SetOutPath "$INSTDIR\bin"


        ${If} ${RunningX64}
        File "${OPENVPN_ROOT}\bin-win64\openvpnserv2.exe"
        ${Else}
        File "${OPENVPN_ROOT}\bin-win32\openvpnserv2.exe"
        ${EndIf}
 	DetailPrint "Installing OpenVPN Service..."

	DotNetChecker::IsDotNet40FullInstalled
	Pop $0
	${If} $0 == "false"
	${OrIf} $0 == "f" ; could be either false or f as per dotnetchecker.nsh
		DetailPrint "NET 4.0 not found. Using sc.exe to install openvpnservice"
		nsExec::ExecToLog '$SYSDIR\sc.exe create OpenVPNService binPath= "$INSTDIR\bin\openvpnserv2.exe" depend= tap0901/dhcp'
	${Else}
		DetailPrint "Running openvpnserv2.exe -install"
		nsExec::ExecToLog '"$INSTDIR\bin\openvpnserv2.exe" -install'
	${EndIf}

	Pop $R0 # return value/error/timeout

SectionEnd

Function CoreSetup

	SetOverwrite on
	
        ${If} ${RunningX64}
        DetailPrint "Installing 64-bit openvpnserv.exe."
        SetOutPath "$INSTDIR\bin"
        File "${OPENVPN_ROOT}\bin-win64\openvpnserv.exe"
        ${Else}
        DetailPrint "Installing 32-bit openvpnserv.exe."
        SetOutPath "$INSTDIR\bin"
        File "${OPENVPN_ROOT}\bin-win32\openvpnserv.exe"
        ${EndIf}

        # Include your custom config file(s) here.
        SetOutPath "$INSTDIR\config"
        File /oname=${SITE_NAME}.ovpn "${UNTANGLE_PACKAGE_DIR}/client-${COMMON_NAME}.ovpn"

        # Copy crt and key files
        SetOutPath "$INSTDIR\config\keys"
        File /oname=${SITE_NAME}-${COMMON_NAME}.crt "${UNTANGLE_SETTINGS_DIR}/remote-clients/client-${COMMON_NAME}.crt"
        File /oname=${SITE_NAME}-${COMMON_NAME}.key "${UNTANGLE_SETTINGS_DIR}/remote-clients/client-${COMMON_NAME}.key"
        File /oname=${SITE_NAME}-${COMMON_NAME}-ca.crt "${UNTANGLE_SETTINGS_DIR}/ca.crt"

	CreateDirectory "$INSTDIR\log"
	FileOpen $R1 "$INSTDIR\log\README.txt" w
	FileWrite $R1 "This directory will contain the log files for ${PACKAGE_NAME}$\r$\n"
	FileWrite $R1 "sessions which are being run as a service.$\r$\n"
	FileClose $R1

        ; set registry parameters for services and GUI
	!insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\${PACKAGE_NAME}" "config_dir" "$INSTDIR\config"
	!insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\${PACKAGE_NAME}" "config_ext"  "${OPENVPN_CONFIG_EXT}"
	WriteRegStr HKLM "SOFTWARE\${PACKAGE_NAME}" "exe_path"    "$INSTDIR\bin\openvpn.exe"
	!insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\${PACKAGE_NAME}" "log_dir"     "$INSTDIR\log"
	!insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\${PACKAGE_NAME}" "priority"    "NORMAL_PRIORITY_CLASS"
	!insertmacro WriteRegStringIfUndef HKLM "SOFTWARE\${PACKAGE_NAME}" "log_append"  "0"

	; install openvpnserv as a service (to be started manually from service control manager)
	DetailPrint "Service INSTALL"
	nsExec::ExecToLog '"$INSTDIR\bin\openvpnserv.exe" -install'
	Pop $R0 # return value/error/timeout

FunctionEnd


Section "TAP Virtual Ethernet Adapter" SecTAP

	SetOverwrite on
	SetOutPath "$TEMP"

	File "${OPENVPN_ROOT}\tap-installer\tap-windows.exe"

	DetailPrint "TAP INSTALL (May need confirmation)"
	nsExec::ExecToLog '"$TEMP\tap-windows.exe" /S /SELECT_UTILITIES=1'
	Pop $R0 # return value/error/timeout

	Delete "$TEMP\tap-windows.exe"

	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "tap" "installed"
SectionEnd

Section "${PACKAGE_NAME} GUI" SecOpenVPNGUI

	SetOverwrite on
	
	SetOutPath "$INSTDIR\bin"

        ${If} ${RunningX64}
        DetailPrint "Installing 64-bit openvpn-gui.exe."
        SetOutPath "$INSTDIR\bin"
        File "${OPENVPN_ROOT}\bin-win64\openvpn-gui.exe"
        ${Else}
        DetailPrint "Installing 32-bit openvpn-gui.exe."
        SetOutPath "$INSTDIR\bin"
        File "${OPENVPN_ROOT}\bin-win32\openvpn-gui.exe"
        ${EndIf}
        
        ; Look for legacy "RUNASADMIN" regkey and remove it as it is not needed any more
        ; If user have hade OpenVPN installed before where this regkey is set and has not uninstalled.
        ReadRegStr $R0 HKLM "Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers\" "$INSTDIR\bin\openvpn-gui.exe"
	${If} $R0 == "RUNASADMIN"
	DeleteRegValue HKLM "Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers" "$INSTDIR\bin\openvpn-gui.exe"
        ${EndIf}

	${If} ${SectionIsSelected} ${SecAddShortcutsWorkaround}
		CreateDirectory "$SMPROGRAMS\${PACKAGE_NAME}"
		CreateShortCut "$SMPROGRAMS\${PACKAGE_NAME}\${PACKAGE_NAME} GUI.lnk" "$INSTDIR\bin\openvpn-gui.exe" ""
		CreateShortcut "$DESKTOP\${PACKAGE_NAME} GUI.lnk" "$INSTDIR\bin\openvpn-gui.exe"
	${EndIf}
	
        ; Using active setup registry entries to set/unset GUI to launch on logon for each user.
	; If the user removes the GUI from startup items it will not be re-added or removed on subsequent
	; installs unless the value of "Version" is updated (do this only if/when really necessary).
	; Ref: https://helgeklein.com/blog/2010/04/active-setup-explained/
	WriteRegStr HKLM "Software\Microsoft\Active Setup\Installed Components\${PACKAGE_NAME}_UserSetup" "" "OpenVPN Setup"
	WriteRegStr HKLM "Software\Microsoft\Active Setup\Installed Components\${PACKAGE_NAME}_UserSetup" "Version" "2,4,7,0"
	WriteRegDword HKLM "Software\Microsoft\Active Setup\Installed Components\${PACKAGE_NAME}_UserSetup" "IsInstalled" 0x1
        ; DontAsk = 2 is used to not prompt the user
	WriteRegDword HKLM "Software\Microsoft\Active Setup\Installed Components\${PACKAGE_NAME}_UserSetup" "DontAsk" 0x2
	${If} ${SectionIsSelected} ${SecLaunchGUIOnLogon0}
		WriteRegStr HKLM "Software\Microsoft\Active Setup\Installed Components\${PACKAGE_NAME}_UserSetup" "StubPath" "reg add HKCU\Software\Microsoft\Windows\CurrentVersion\Run /v OPENVPN-GUI /t REG_SZ /d $\"$INSTDIR\bin\openvpn-gui.exe$\" /f"
	${Else}
		WriteRegStr HKLM "Software\Microsoft\Active Setup\Installed Components\${PACKAGE_NAME}_UserSetup" "StubPath" "reg delete HKCU\Software\Microsoft\Windows\CurrentVersion\Run /v OPENVPN-GUI /f"
	${EndIf}
SectionEnd

Section /o "${PACKAGE_NAME} File Associations" SecFileAssociation
	WriteRegStr HKCR ".${OPENVPN_CONFIG_EXT}" "" "${PACKAGE_NAME}File"
	WriteRegStr HKCR "${PACKAGE_NAME}File" "" "${PACKAGE_NAME} Config File"
	WriteRegStr HKCR "${PACKAGE_NAME}File\shell" "" "open"
	WriteRegStr HKCR "${PACKAGE_NAME}File\DefaultIcon" "" "$INSTDIR\icon.ico,0"
	WriteRegStr HKCR "${PACKAGE_NAME}File\shell\open\command" "" 'notepad.exe "%1"'
	WriteRegStr HKCR "${PACKAGE_NAME}File\shell\run" "" "Start ${PACKAGE_NAME} on this config file"
	WriteRegStr HKCR "${PACKAGE_NAME}File\shell\run\command" "" '"$INSTDIR\bin\openvpn.exe" --pause-exit --config "%1"'
SectionEnd

Section /o "OpenSSL Utilities" SecOpenSSLUtilities

	SetOverwrite on
	
	${If} ${RunningX64}
        DetailPrint "Installing 64-bit openssl.exe."
	SetOutPath "$INSTDIR\bin"
	File "${OPENVPN_ROOT}\bin-win64\openssl.exe"
        ${Else}
        DetailPrint "Installing 32-bit openssl.exe."
	SetOutPath "$INSTDIR\bin"
	File "${OPENVPN_ROOT}\bin-win32\openssl.exe"
        ${EndIf}


SectionEnd

Section /o "Add ${PACKAGE_NAME} to PATH" SecAddPath

	; append our bin directory to end of current user path
	${EnvVarUpdate} $R0 "PATH" "A" "HKLM" "$INSTDIR\bin"

SectionEnd

Section /o "Add Shortcuts to Start Menu" SecAddShortcuts

	SetOverwrite on
	CreateDirectory "$SMPROGRAMS\${PACKAGE_NAME}\Documentation"
	WriteINIStr "$SMPROGRAMS\${PACKAGE_NAME}\Documentation\${PACKAGE_NAME} HOWTO.url" "InternetShortcut" "URL" "http://openvpn.net/howto.html"
	WriteINIStr "$SMPROGRAMS\${PACKAGE_NAME}\Documentation\${PACKAGE_NAME} Web Site.url" "InternetShortcut" "URL" "http://openvpn.net/"
	WriteINIStr "$SMPROGRAMS\${PACKAGE_NAME}\Documentation\${PACKAGE_NAME} Wiki.url" "InternetShortcut" "URL" "https://community.openvpn.net/openvpn/wiki/"
	WriteINIStr "$SMPROGRAMS\${PACKAGE_NAME}\Documentation\${PACKAGE_NAME} Support.url" "InternetShortcut" "URL" "https://community.openvpn.net/openvpn/wiki/GettingHelp"

	CreateShortCut "$SMPROGRAMS\${PACKAGE_NAME}\Uninstall ${PACKAGE_NAME}.lnk" "$INSTDIR\Uninstall.exe"
SectionEnd

Section /o "Launch ${PACKAGE_NAME} GUI on User Logon" SecLaunchGUIOnLogon
	SectionEnd

SectionGroup "!Dependencies (Advanced)"

	Section /o "OpenSSL DLLs" SecOpenSSLDLLs

		SetOverwrite on

	${If} ${RunningX64}
        DetailPrint "Installing 64-bit DLLs."
	SetOutPath "$INSTDIR\bin"
        File "${OPENVPN_ROOT}\bin-win64\libcrypto-1_1-x64.dll"
        File "${OPENVPN_ROOT}\bin-win64\libssl-1_1-x64.dll"
        ${Else}
        DetailPrint "Installing 32-bit DLLs."
	SetOutPath "$INSTDIR\bin"
	File "${OPENVPN_ROOT}\bin-win32\libcrypto-1_1.dll"
	File "${OPENVPN_ROOT}\bin-win32\libssl-1_1.dll"
        ${EndIf}


SectionEnd

Section /o "LZO DLLs" SecLZODLLs

	SetOverwrite on
		
 	${If} ${RunningX64}
        DetailPrint "Installing 64-bit liblzo2-2.dll."
	SetOutPath "$INSTDIR\bin"
	File "${OPENVPN_ROOT}\bin-win64\liblzo2-2.dll"
        ${Else}
        DetailPrint "Installing 32-bit liblzo2-2.dll."
	SetOutPath "$INSTDIR\bin"
	File "${OPENVPN_ROOT}\bin-win32\liblzo2-2.dll"
        ${EndIf}


SectionEnd

Section /o "PKCS#11 DLLs" SecPKCS11DLLs

	SetOverwrite on

	${If} ${RunningX64}
        DetailPrint "Installing 64-bit libpkcs11-helper-1.dll."
	SetOutPath "$INSTDIR\bin"
	File "${OPENVPN_ROOT}\bin-win64\libpkcs11-helper-1.dll"
        ${Else}
        DetailPrint "Installing 32-bit libpkcs11-helper-1.dll."
	SetOutPath "$INSTDIR\bin"
	File "${OPENVPN_ROOT}\bin-win32\libpkcs11-helper-1.dll"
        ${EndIf}

SectionEnd

SectionGroupEnd

;--------------------------------
;Installer Sections

Function .onInit
	${GetParameters} $R0
	ClearErrors

	!insertmacro SelectByParameter ${SecAddShortcutsWorkaround} SELECT_SHORTCUTS 1
	!insertmacro SelectByParameter ${SecOpenVPNUserSpace} SELECT_OPENVPN 1
	!insertmacro SelectByParameter ${SecService} SELECT_SERVICE 1
!ifdef USE_TAP_WINDOWS
	!insertmacro SelectByParameter ${SecTAP} SELECT_TAP 1
!endif
!ifdef USE_OPENVPN_GUI
	!insertmacro SelectByParameter ${SecOpenVPNGUI} SELECT_OPENVPNGUI 1
!endif
	!insertmacro SelectByParameter ${SecFileAssociation} SELECT_ASSOCIATIONS 1
	!insertmacro SelectByParameter ${SecOpenSSLUtilities} SELECT_OPENSSL_UTILITIES 0
	!insertmacro SelectByParameter ${SecAddPath} SELECT_PATH 1
	!insertmacro SelectByParameter ${SecAddShortcuts} SELECT_SHORTCUTS 1
	!insertmacro SelectByParameter ${SecLaunchGUIOnLogon} SELECT_LAUNCH 1
	!insertmacro SelectByParameter ${SecLaunchGUIOnLogon0} SELECT_LAUNCH 1
	!insertmacro SelectByParameter ${SecOpenSSLDLLs} SELECT_OPENSSLDLLS 1
	!insertmacro SelectByParameter ${SecLZODLLs} SELECT_LZODLLS 1
	!insertmacro SelectByParameter ${SecPKCS11DLLs} SELECT_PKCS11DLLS 1

	!insertmacro MULTIUSER_INIT
	SetShellVarContext all

	; Check if we're running on 64-bit Windows
	${If} ${RunningX64}

                SetRegView 64

		; Change the installation directory to C:\Program Files, but only if the
		; user has not provided a custom install location.
		${If} "$INSTDIR" == "$PROGRAMFILES\${PACKAGE_NAME}"
			StrCpy $INSTDIR "$PROGRAMFILES64\${PACKAGE_NAME}"
		${EndIf}
	${EndIf}

	# Delete previous start menu
	RMDir /r "$SMPROGRAMS\${PACKAGE_NAME}"
        
FunctionEnd

;--------------------------------
;Dependencies

Function .onSelChange
	${If} ${SectionIsSelected} ${SecService}
		!insertmacro SelectSection ${SecOpenVPNUserSpace}
	${EndIf}
	${If} ${SectionIsSelected} ${SecAddShortcuts}
		!insertmacro SelectSection ${SecAddShortcutsWorkaround}
	${Else}
		!insertmacro UnselectSection ${SecAddShortcutsWorkaround}
	${EndIf}
	${If} ${SectionIsSelected} ${SecLaunchGUIOnLogon}
		!insertmacro SelectSection ${SecLaunchGUIOnLogon0}
	${Else}
		!insertmacro UnSelectSection ${SecLaunchGUIOnLogon0}
	${EndIf}
FunctionEnd

;--------------------
;Post-install section

Section -post

	SetOverwrite on
	SetOutPath "$INSTDIR"
	File "${OPENVPN_ROOT}\install-win32\openvpn.ico"
	SetOutPath "$INSTDIR\doc"
	File "${OPENVPN_ROOT}\install-win32\license.txt"

	; Store install folder in registry
	WriteRegStr HKLM "SOFTWARE\${PACKAGE_NAME}" "" "$INSTDIR"
	
	; Create uninstaller
	WriteUninstaller "$INSTDIR\Uninstall.exe"

	; Show up in Add/Remove programs
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "DisplayName" "${PACKAGE_NAME} ${OPENVPN_VERSION}"
	WriteRegExpandStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "UninstallString" "$INSTDIR\Uninstall.exe"
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "DisplayIcon" "$INSTDIR\icon.ico"
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "DisplayVersion" "${OPENVPN_VERSION}"

	; Start the interactive service
	DetailPrint "Starting OpenVPN Interactive Service"
	SimpleSC::StartService "OpenVPNServiceInteractive" "" 10

SectionEnd

;--------------------------------
;Descriptions

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
	!insertmacro MUI_DESCRIPTION_TEXT ${SecOpenVPNUserSpace} $(DESC_SecOpenVPNUserSpace)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecService} $(DESC_SecService)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecOpenVPNGUI} $(DESC_SecOpenVPNGUI)
        !insertmacro MUI_DESCRIPTION_TEXT ${SecTAP} $(DESC_SecTAP)
        !insertmacro MUI_DESCRIPTION_TEXT ${SecOpenSSLUtilities} $(DESC_SecOpenSSLUtilities)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecOpenSSLDLLs} $(DESC_SecOpenSSLDLLs)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecLZODLLs} $(DESC_SecLZODLLs)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecPKCS11DLLs} $(DESC_SecPKCS11DLLs)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecAddPath} $(DESC_SecAddPath)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecAddShortcuts} $(DESC_SecAddShortcuts)
	!insertmacro MUI_DESCRIPTION_TEXT ${SecFileAssociation} $(DESC_SecFileAssociation)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

;--------------------------------
;Uninstaller Section

Function un.onInit
	ClearErrors
	!insertmacro MULTIUSER_UNINIT
	SetShellVarContext all
	${If} ${RunningX64}
		SetRegView 64
	${EndIf}
FunctionEnd

Section "Uninstall"

	; Stop OpenVPN-GUI if currently running
	DetailPrint "Stopping OpenVPN-GUI..."
	StopGUI:

	FindWindow $0 "OpenVPN-GUI"
	IntCmp $0 0 guiClosed
	SendMessage $0 ${WM_CLOSE} 0 0
	Sleep 100
	Goto StopGUI

	guiClosed:

	; Services have to be explicitly stopped before they are removed
	DetailPrint "Stopping OpenVPN Services..."
	SimpleSC::StopService "OpenVPNService" 0 10
	SimpleSC::StopService "OpenVPNServiceInteractive" 0 10
	SimpleSC::StopService "OpenVPNServiceLegacy" 0 10
	DetailPrint "Removing OpenVPN Services..."
	SimpleSC::RemoveService "OpenVPNService"
	SimpleSC::RemoveService "OpenVPNServiceInteractive"
	SimpleSC::RemoveService "OpenVPNServiceLegacy"
	Sleep 3000

	 	ReadRegStr $R0 HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}" "tap"
		${If} $R0 == "installed"
			ReadRegStr $R0 HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\TAP-Windows" "UninstallString"
			${If} $R0 != ""
				DetailPrint "TAP UNINSTALL"
				nsExec::ExecToLog '"$R0" /S'
				Pop $R0 # return value/error/timeout
			${EndIf}
		${EndIf}

	${un.EnvVarUpdate} $R0 "PATH" "R" "HKLM" "$INSTDIR\bin"

		Delete "$INSTDIR\bin\openvpn-gui.exe"
		Delete "$DESKTOP\${PACKAGE_NAME} GUI.lnk"

	Delete "$INSTDIR\bin\openvpn.exe"
	Delete "$INSTDIR\bin\openvpnserv.exe"
	Delete "$INSTDIR\bin\openvpnserv2.exe"
	Delete "$INSTDIR\bin\liblzo2-2.dll"
	Delete "$INSTDIR\bin\libpkcs11-helper-1.dll"
	Delete "$INSTDIR\bin\libcrypto-1_1.dll"
	Delete "$INSTDIR\bin\libcrypto-1_1-x64.dll"
	Delete "$INSTDIR\bin\libssl-1_1.dll"
	Delete "$INSTDIR\bin\libssl-1_1-x64.dll"

	Delete "$INSTDIR\config\README.txt"
	Delete "$INSTDIR\log\README.txt"

	Delete "$INSTDIR\bin\openssl.exe"

	Delete "$INSTDIR\doc\license.txt"
	Delete "$INSTDIR\doc\INSTALL-win32.txt"
	Delete "$INSTDIR\doc\openvpn.8.html"
	Delete "$INSTDIR\icon.ico"
	Delete "$INSTDIR\Uninstall.exe"



	RMDir "$INSTDIR\bin"
	RMDir "$INSTDIR\doc"
	RMDir "$INSTDIR\config"
	RMDir /r "$INSTDIR\log"
	RMDir "$INSTDIR"
	RMDir /r "$SMPROGRAMS\${PACKAGE_NAME}"

	!insertmacro DelRegKeyIfUnchanged HKCR ".${OPENVPN_CONFIG_EXT}" "${PACKAGE_NAME}File"
	DeleteRegKey HKCR "${PACKAGE_NAME}File"
	DeleteRegKey HKLM "SOFTWARE\${PACKAGE_NAME}"
	DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PACKAGE_NAME}"
        ; Set installed status to 0 in Active Setup
	WriteRegDword HKLM "Software\Microsoft\Active Setup\Installed Components\${PACKAGE_NAME}_UserSetup" "IsInstalled" 0x0
	WriteRegStr HKLM "Software\Microsoft\Active Setup\Installed Components\${PACKAGE_NAME}_UserSetup" "StubPath" "reg delete HKCU\Software\Microsoft\Windows\CurrentVersion\Run /v OPENVPN-GUI /f"
SectionEnd
