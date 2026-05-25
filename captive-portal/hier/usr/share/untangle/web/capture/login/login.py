from mod_python import apache
from mod_python import util
from mod_python import Cookie



import sys
import os

# Add parent directory (/usr/share/untangle/web/capture) to sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import handler  # Now this works

def index(req):
    #handle the condition for appid being passed.
    if req.args:
       req.args = f"{req.args}&CP=Direct"
    else:
        req.args = f"CP=Direct"
    return handler.index(req)
