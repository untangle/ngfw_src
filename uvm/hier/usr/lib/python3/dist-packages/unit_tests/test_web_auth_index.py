from uvm import login_tools
import pytest
import crypt
import hashlib
import base64

def test_valid_login_crypt(monkeypatch):
    password = 'passw0rd'
    shadow_hash = crypt.crypt(password)
    uvm_settings = {
        'admin': {'users':
                  {'list':[
                      {'username': 'admin',
                       'passwordHashShadow': shadow_hash}
                      ]
                   }
                  }
    }
    def mock_get_app_settings_item(app, item):
        raise RuntimeError("get_app_settings_item() "
                           "shouldn't be called")
    def mock_get_uvm_settings_item(key, subkey):
        return uvm_settings[key][subkey]

    monkeypatch.setattr(login_tools, 'get_app_settings_item',
                        mock_get_app_settings_item)
    monkeypatch.setattr(login_tools, 'get_uvm_settings_item',
                        mock_get_uvm_settings_item)

    assert login_tools.valid_login(None, 'Administrator', 'admin', password)


    assert not login_tools.valid_login(None, 'Administrator', 'admin',
                                       'wrongPassword')


def test_valid_login_md5(monkeypatch):
    password = 'passw0rd'
    salt = b'\x0f' * 8    
    md5_hash_bytes = hashlib.md5(password.encode('utf-8') + salt).digest()
    password_hash = base64.b64encode(md5_hash_bytes + salt).decode('utf-8')
    
    uvm_settings = {
        'admin': {'users':
                  {'list':[
                      {'username': 'admin',
                       'passwordHashBase64': password_hash}
                      ]
                   }
                  }
    }
    def mock_get_app_settings_item(app, item):
        raise RuntimeError("get_app_settings_item() "
                           "shouldn't be called")
    def mock_get_uvm_settings_item(key, subkey):
        return uvm_settings[key][subkey]

    monkeypatch.setattr(login_tools, 'get_app_settings_item',
                        mock_get_app_settings_item)
    monkeypatch.setattr(login_tools, 'get_uvm_settings_item',
                        mock_get_uvm_settings_item)

    assert login_tools.valid_login(None, 'Administrator', 'admin',
                                   password)

    assert not login_tools.valid_login(None, 'Administrator', 'admin',
                                       'wrongPassword')
    




def test_logins_reports_md5(monkeypatch):
    password = 'passw0rd'
    salt = b'\x0f' * 8
    md5_hash_bytes = hashlib.md5(password.encode('utf-8') + salt).digest()
    password_hash = base64.b64encode(md5_hash_bytes + salt).decode('utf-8')

    reports_settings = {
        'reports': {'reportsUsers':
                  {'list':[
                      {'username': 'reporter1',
                       'emailAddress': 'reporter1',
                       'passwordHashBase64': password_hash}
                      ]
                   }
                  }
    }
    def mock_get_app_settings_item(app, item):
        return reports_settings[app][item]
    def mock_get_uvm_settings_item(key, subkey):
        return None


    monkeypatch.setattr(login_tools, 'get_app_settings_item',
                        mock_get_app_settings_item)
    monkeypatch.setattr(login_tools, 'get_uvm_settings_item',
                        mock_get_uvm_settings_item)

    assert login_tools.valid_login(None, 'Reports', 'reporter1',
                                   password)

    assert not login_tools.valid_login(None, 'Reports', 'reporter1',
                                       'wrongPassword')
    
