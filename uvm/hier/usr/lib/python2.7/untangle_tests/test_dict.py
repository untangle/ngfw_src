import os
import sys
import subprocess

nodeDict = {}

#
# Global dictionary that stores all known tests
# Each node registers itself
#
class TestDict:
      
    @staticmethod
    def registerNode(nodeName, clz):
        global nodeDict
    	nodeDict[nodeName] = clz
    
    @staticmethod
    def allNodes():
        global nodeDict
        return nodeDict.keys()

    @staticmethod
    def allTests():
        global nodeDict
        return nodeDict.values()

    @staticmethod
    def test(nodeName):
        global nodeDict
        try:
            return nodeDict[nodeName]
        except Exception,e:
            return None
