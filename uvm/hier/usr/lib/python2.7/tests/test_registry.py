import os
import sys
import subprocess

__appDict = {}

def registerApp(appName, clz):
    global __appDict
    __appDict[appName] = clz

def allApps():
    global __appDict
    appList = sorted(__appDict.keys())

    # move these to front
    if 'web-filter' in appList:
        appList.insert(0, appList.pop(appList.index('web-filter')))
    if 'firewall' in appList:
        appList.insert(0, appList.pop(appList.index('firewall')))
    if 'spam-blocker' in appList:
        appList.append(appList.pop(appList.index('spam-blocker')))
    if 'spam-blocker-lite' in appList:
        appList.append(appList.pop(appList.index('spam-blocker-lite')))
    if 'intrusion-prevention' in appList:
        appList.append(appList.pop(appList.index('intrusion-prevention')))
        
    return appList

def allTests():
    global __appDict
    return __appDict.values()

def getTest(appName):
    global __appDict
    try:
        return __appDict[appName]
    except Exception,e:
        return None
