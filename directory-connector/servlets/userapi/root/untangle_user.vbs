
' This script runs at login time to alert the configured Untangle NGFW 
' that the user has logged into this IP. 

' IMPORTANT: The following variables should be customized for your configuration

' Replace this string with the appropriate IP/hostname for server
serverLocation = "%UNTANGLE_REPLACE_WITH_ADDRESS%"

' Replace this string with the appropriate secret for server
secret = "%UNTANGLE_REPLACE_WITH_SECRET%"

' Set this to True if you wish the script to run continuously
keepLooping = True

' Time in milliseconds to sleep between request (if keepLooping is True)
sleepPeriodMs = 300000

' Protocol to be used
urlProtocol = "http"



'Handle or Ignore all errors
On Error Resume Next

For Each strArg in Wscript.Arguments
	If LCase(strArg) = "loop" Then
		keepLooping = True
	Else
		serverLocation = strArg
	End If
Next

'WScript.Echo "keepLooping: " & keepLoooping
'WScript.Echo "serverLocation: " & serverLocation
'WScript.Echo "urlProtocol: " & urlProtocol
'WScript.Echo "sleepPeriodMs: " & sleepPeriodMs

Do While True 
	Set AJAX = CreateObject("MSXML2.ServerXMLHTTP")
	Set wshNetwork = CreateObject("WScript.Network")
	strHostname = wshNetwork.ComputerName
	strDomain = wshNetwork.UserDomain  

	'The next section gets username from function and checks that it isn't null
	sUserName = GetActiveUser(strHostname)
	If sUserName <> "" Then
	    strUser = sUserName
    	'MsgBox "Active user from function: " + sUserName
	Else
    	'will default to user executing script if no active user
    	strUser = wshNetwork.UserName 
	End If

    command = _
        urlProtocol + "://" + serverLocation _
        + "/userapi/registration" _
        + "?username=" + strUser _
        + "&domain=" + strDomain _
        + "&hostname=" + strHostname _
        + "&action=login"

    If secret <> "" Then
        command = command + "&secretKey=" + secret
    End If

	'WScript.Echo "Hitting Url: " & command
	AJAX.Open "GET", command
	AJAX.Send ""
	If keepLooping Then
		'WScript.Echo "Sleeping..."
		WScript.sleep(sleepPeriodMs)
		AJAX.Abort 
		Set AJAX = nothing
	Else
		AJAX.Abort 
		Set AJAX = nothing
		Exit Do
	End If
Loop

' Function will return active user name from QWINSTA.EXE
' Windows XP and higher

Function GetActiveUser(sHost)

    Set oShell = CreateObject("Wscript.Shell")
    Set oFS = CreateObject("Scripting.FileSystemObject")

    sTempFile = oFS.GetSpecialFolder(2).ShortPath & "\" & oFS.GetTempName

    'Run command via Command Prompt and use for /f to extract user names
    'and status, dump results into a temp text file
    oShell.Run "%ComSpec% /c for /f ""skip=1 Tokens=2,4"" %i in ('" _
             & "%SystemRoot%\System32\QWINSTA.EXE /SERVER:" & sHost _
             & "') do echo %i %j>>" & sTempFile, 0, True

    GetActiveUser = "" 'init value

    If oFS.FileExists(sTempFile) Then
        'Open the temp Text File and Read out the Data
        Set oTF = oFS.OpenTextFile(sTempFile)

        'Parse the text file
        strContents = OTF.ReadAll
        arrLines = Split(strContents, vbCrLf)

        For l = 0 To ubound(arrLines)
            strLine = arrlines(l)
            lineimp = split(strLine)
            If lineimp(1) = "Active" Then 
                GetActiveUser = lineimp(0)
                Exit For
            End If
        Next

        'Close it
        oTF.Close
        'Delete It
        oFS.DeleteFile sTempFile
    End If

End Function
