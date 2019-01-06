import os
import sys
import subprocess

__module_dict = {}

def register_module(module_name, clz):
    global __module_dict
    __module_dict[module_name] = clz

def all_modules():
    global __module_dict
    module_list = sorted(__module_dict.keys())
    return module_list

def all_tests():
    global __module_dict
    return __module_dict.values()

def get_test(module_name):
    global __module_dict
    try:
        return __module_dict[module_name]
    except Exception as e:
        return None
