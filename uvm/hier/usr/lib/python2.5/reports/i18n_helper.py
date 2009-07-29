import gettext

def get_translation(domain):
    return gettext.translation('untangle-node-reporting', fallback=True)
