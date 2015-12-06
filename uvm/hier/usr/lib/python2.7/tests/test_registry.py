import os
import sys
import subprocess

__nodeDict = {}

def registerNode(nodeName, clz):
    global __nodeDict
    __nodeDict[nodeName] = clz

def allNodes():
    global __nodeDict
    nodeList = sorted(__nodeDict.keys())

    # move these to front
    if 'web-filter' in nodeList:
        nodeList.insert(0, nodeList.pop(nodeList.index('web-filter')))
    if 'firewall' in nodeList:
        nodeList.insert(0, nodeList.pop(nodeList.index('firewall')))

    return nodeList

def allTests():
    global __nodeDict
    return __nodeDict.values()

def getTest(nodeName):
    global __nodeDict
    try:
        return __nodeDict[nodeName]
    except Exception,e:
        return None
