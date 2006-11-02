/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: LocalPhoneBookImpl.java 7736 2006-10-31 19:25:54Z rbscott $
 */

!include "MUI.nsh"

## This is the directory where the files to be installed are located
!define HOME "files"

## The directory where the generated certs are for the server
!define CERT_FILES "@MVVM_CONF@/certs"

## Product information
!define PRODUCT_NAME "WMI Mapper"
!define VERSION "2.5.3"

## The service name
!define SERVICE_NAME "wmimapper-mv"

## The name of the process
!define PROCESS_NAME "WMIServer.exe"

## The name of the program
!define PROGRAM_NAME "${PROCESS_NAME}"

## The name of the object to update in the registry
!define SERVICE_REG "SYSTEM\CurrentControlSet\Services\${SERVICE_NAME}"

##--------------------------------
## Configuration

##General  
OutFile "/tmp/setup-wmi.exe"
SetCompressor bzip2
ShowInstDetails show
ShowUninstDetails show

##Folder selection page
InstallDir "$PROGRAMFILES\${PRODUCT_NAME}"
  
##Remember install folder
InstallDirRegKey HKCU "Software\${PRODUCT_NAME}" ""

##--------------------------------
## Modern UI Configuration

Name "${PRODUCT_NAME} ${VERSION}"

!define MUI_COMPONENTSPAGE_SMALLDESC
## XXX we should insert a README of some sort.
## !define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\INSTALL-win32.txt"
!define MUI_FINISHPAGE_NOAUTOCLOSE

!define MUI_FINISHPAGE_SHOWREADME_NOTCHECKED
!define MUI_ABORTWARNING
!define MUI_ICON "${HOME}\wmimapper.ico"
!define MUI_UNICON "${HOME}\wmimapper.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE

!define MUI_WELCOMEPAGE_TITLE "Welcome to the ${PRODUCT_NAME} Setup Wizard"

!define MUI_WELCOMEPAGE_TEXT "This wizard will guide you through the installation of:\r\n\rThe WMI Mapper.\r\n\r\n"

!define MUI_COMPONENTSPAGE_TEXT_TOP "Select the components to install/upgrade."

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${HOME}\license.txt"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH

##--------------------------------
## Languages

!insertmacro MUI_LANGUAGE "English"

##--------------------------------
## Language Strings

  LangString DESC_SecInstallWMIMapper ${LANG_ENGLISH} "Install WMI Mapper, including necessary configuration files."

##--------------------------------
## Functions
Function QuitWMIServer
  push "${PROCESS_NAME}"
  processwork::ExistsProcess
  pop $5
  IntCmp $5 0 killdone

  DetailPrint "Exiting the remaining WMI Mapper processes."
  push "${PROCESS_NAME}"
  processwork::KillProcess 
  pop $5
  Sleep 1000
  IntCmp $5 0 killdone

  DetailPrint "Stopping the process."
  nsExec::ExecToLog '"$INSTDIR\${PROGRAM_NAME}" -stop ${SERVICE_NAME}'

  Sleep 1000

killdone:
FunctionEnd

##--------------------------------
## Installer Sections

Section "WMIMapper" SecInstallWMIMapper
  Call QuitWMIServer

  SetOverwrite on

  SetOutPath "$INSTDIR"
  File "${HOME}\${PROGRAM_NAME}"
  File "${HOME}\cimserver_current.conf"
  File "${HOME}\cimserver_planned.conf"
  File "${HOME}\libeay32.lib"
  File "${HOME}\ssleay32.lib"
  File /oname=server.key "${KEY_FILE}"
  File /oname=server.crt "${CRT_FILE}"
SectionEnd


##--------------------------------
## Descriptions

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecInstallWMIMapper} $(DESC_SecInstallWMIMapper)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

##--------------------------------
## Post-Install section

Section -post
  Call QuitWMIServer
 
## Write the uninstaller
  ; Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"

##installservice:
  DetailPrint "Installing the WMI Mapper Service."
  nsExec::ExecToLog '"$INSTDIR\${PROGRAM_NAME}" -install ${SERVICE_NAME}'
  Pop $R0 # return value/error/timeout

## updateregistry:
  DetailPrint "Updating the registry for the WMI Server"
  ## Set to automatically start at startup
  WriteRegDWORD HKLM "${SERVICE_REG}" "Start" 0x2
  ## Set to the correct home
  WriteRegStr HKLM "${SERVICE_REG}" "home" '"$INSTDIR"'
  ## Set the correct image path
  WriteRegExpandStr HKLM "${SERVICE_REG}" "ImagePath" '"$INSTDIR\${PROGRAM_NAME}" -D "$INSTDIR"'

## Start the service
  DetailPrint "Starting the WMI Mapper Service."
  nsExec::ExecToLog '"$INSTDIR\${PROGRAM_NAME}" -start ${SERVICE_NAME}'
  Pop $R0 # return value/error/timeout

  ; Store install folder in registry
  WriteRegStr HKLM SOFTWARE\WMIMapper "" $INSTDIR
SectionEnd

##--------------------------------
## Uninstaller Section

Section "Uninstall"
  ## Quit the server
  DetailPrint "Stopping the process."
  nsExec::ExecToLog '"$INSTDIR\${PROGRAM_NAME}" -stop ${SERVICE_NAME}'
  
  DetailPrint "Removing the service"
  nsExec::ExecToLog '"$INSTDIR\${PROGRAM_NAME}" -remove ${SERVICE_NAME}'
  Pop $R0 # return value/error/timeout

  RMDir /r "$INSTDIR"
  
  ## Remove the install directory register key
  DeleteRegKey HKCU "Software\${PRODUCT_NAME}"
SectionEnd

##
