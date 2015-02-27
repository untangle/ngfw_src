import os
import sys
import subprocess

__nodeDict = {}

def registerNode(nodeName, clz):
    global __nodeDict
    __nodeDict[nodeName] = clz

def allNodes():
    global __nodeDict
    return __nodeDict.keys()

def allTests():
    global __nodeDict
    return __nodeDict.values()

def getTest(nodeName):
    global __nodeDict
    try:
        return __nodeDict[nodeName]
    except Exception,e:
        return None
