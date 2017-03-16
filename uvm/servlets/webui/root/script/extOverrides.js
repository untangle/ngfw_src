/* TODO extjs5
Ext.Loader.loadScriptFileInitial=Ext.Loader.loadScriptFile;
Ext.Loader.loadScriptFile=Ext.bind(function() {
    var args = arguments;
    args[0]=arguments[0]+"?_dc="+Ext.buildStamp;
    Ext.Loader.loadScriptFileInitial.apply(this, args);
}, Ext.Loader);
*/
Ext.override(Ext.form.field.Base, {
    msgTarget: 'side',
    clearDirty: function() {
        if(this.xtype=='radiogroup') {
            this.items.each(function(item) {
                item.clearDirty();
            });
        } else {
            this.originalValue=this.getValue();
        }
    },
    afterRender: Ext.Function.createSequence(Ext.form.Field.prototype.afterRender,function() {
        if (this.tooltip) {
            var target = null;
            try {
                if(this.xtype=='checkbox') {
                    target = this.labelEl;
                } else {
                    target = this.container.dom.parentApp.childApps[0];
                }
            } catch(exn) {
                //don't bother if there's nothing to target
            }

            if (target) {
                Ext.QuickTips.register({
                    target: target,
                    title: '',
                    text: this.tooltip,
                    enabled: true,
                    showDelay: 20
                });
            }
        }
    })
});

Ext.override( Ext.form.FieldSet, {
    border: 0
});

Ext.override(Ext.grid.column.Column, {
    defaultRenderer: Ext.util.Format.htmlEncode
});

Ext.apply(Ext.data.SortTypes, {
    // Timestamp sorting
    asTimestamp: function(value) {
        return value.time;
    },
    // Ip address sorting. may contain netmask.
    asIp: function(value){
        if(Ext.isEmpty(value)) {
            return null;
        }
        var i, len, parts = (""+value).replace(/\//g,".").split('.');
        for(i = 0, len = parts.length; i < len; i++){
            parts[i] = Ext.String.leftPad(parts[i], 3, '0');
        }
        return parts.join('.');
    }
});

Ext.define('Ung.VTypes', {
    singleton: true,
    init: function(i18n) {
        var macAddrMaskRe = /^[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}$/;
        var ip4AddrMaskRe = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
        var ip6AddrMaskRe = /^\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?\s*$/;
        var email = /^(")?(?:[^\."])(?:(?:[\.])?(?:[\w\-!#$%&'*+/=?^_`{|}~]))*\1@(\w[\-\w]*\.){1,5}([A-Za-z]){2,63}$/;

        var ipAddrRange = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)-(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
        var cidrRange = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/[0-3]?[0-9]$/;
        var ipNetmask = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;

        var validators = {
            isSinglePortValid: function(val) {
                /* check for values between 0 and 65536 */
                if ( val < 0 || val > 65536 )
                    return false;
                /* verify its an integer (not a float) */
                if( ! /^\d{1,5}$/.test( val ) )
                    return false;
                return true;
            },
            isPortRangeValid: function(val) {
                var portRange = val.split('-');
                if ( portRange.length != 2 )
                    return false;
                return this.isSinglePortValid(portRange[0]) && this.isSinglePortValid(portRange[1]);
            },
            isPortListValid: function(val) {
                var portList = val.split(',');
                var retVal = true;
                for ( var i = 0; i < portList.length;i++) {
                    if ( portList[i].indexOf("-") != -1) {
                        retVal = retVal && this.isPortRangeValid(portList[i]);
                    } else {
                        retVal = retVal && this.isSinglePortValid(portList[i]);
                    }
                    if (!retVal) {
                        return false;
                    }
                }
                return true;
            },
            isSingleIpValid: function(val) {
                return ip4AddrMaskRe.test(val);
            },
            isIpRangeValid: function(val) {
                return ipAddrRange.test(val);
            },
            isCIDRValid: function(val) {
                return cidrRange.test(val);
            },
            isIpNetmaskValid:function(val) {
                return ipNetmask.test(val);
            },
            isIpListValid: function(val) {
                var ipList = val.split(',');
                var retVal = true;
                for ( var i = 0; i < ipList.length;i++) {
                    if ( ipList[i].indexOf("-") != -1) {
                        retVal = retVal && this.isIpRangeValid(ipList[i]);
                    } else {
                        if ( ipList[i].indexOf("/") != -1) {
                            retVal = retVal && ( this.isCIDRValid(ipList[i]) || this.isIpNetmaskValid(ipList[i]));
                            } else {
                                retVal = retVal && this.isSingleIpValid(ipList[i]);
                            }
                    }
                    if (!retVal) {
                        return false;
                    }
                }
                return true;
            }
        };
        Ext.apply(Ext.form.VTypes, {
            email: function (v) {
                return email.test(v);
            },
            ipMatcher: function(val) {
                if ( val.indexOf("/") == -1 && val.indexOf(",") == -1 && val.indexOf("-") == -1) {
                    switch(val) {
                      case 'any':
                        return true;
                    default:
                        return validators.isSingleIpValid(val);
                    }
                }
                if ( val.indexOf(",") != -1) {
                    return validators.isIpListValid(val);
                } else {
                    if ( val.indexOf("-") != -1) {
                        return validators.isIpRangeValid(val);
                    }
                    if ( val.indexOf("/") != -1) {
                        var cidrValid = validators.isCIDRValid(val);
                        var ipNetmaskValid = validators.isIpNetmaskValid(val);
                        return cidrValid || ipNetmaskValid;
                    }
                    console.log("Unhandled case while handling vtype for ipAddr:", val, " returning true !");
                    return true;
                }
            },
            ipMatcherText: i18n._('Invalid IP Address.'),

            ip4Address: function(val) {
                return ip4AddrMaskRe.test(val);
            },
            ip4AddressText: i18n._('Invalid IPv4 Address.'),

            ip4AddressList:  function(v) {
                var addr = v.split(",");
                for ( var i = 0 ; i < addr.length ; i++ ) {
                    if ( ! ip4AddrMaskRe.test(addr[i]) )
                        return false;
                }
                return true;
            },
            ip4AddressListText: i18n._('Invalid IPv4 Address(es).'),

            ip6Address: function(val) {
                return ip6AddrMaskRe.test(val);
            },
            ip6AddressText: i18n._('Invalid IPv6 Address.'),

            ipAddress: function(val) {
                return ip4AddrMaskRe.test(val) || ip6AddrMaskRe.test(val);
            },
            ipAddressText: i18n._('Invalid IP Address.'),

            macAddress: function(val) {
                return macAddrMaskRe.test(val);
            },
            macAddressText: i18n._('Invalid Mac Address.'),
            
            cidrBlock:  function(v) {
                return (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(v));
            },
            cidrBlockText: i18n._('Must be a network in CIDR format.') + ' ' + '(192.168.123.0/24)',

            cidrBlockList:  function(v) {
                var blocks = v.split(",");
                for ( var i = 0 ; i < blocks.length ; i++ ) {
                    if ( ! (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(blocks[i])) )
                        return false;
                }
                return true;
            },
            cidrBlockListText: i18n._('Must be a comma seperated list of networks in CIDR format.') + ' ' + '(192.168.123.0/24,1.2.3.4/24)',

            cidrBlockArea:  function(v) {
                var blocks = v.split("\n");
                for ( var i = 0 ; i < blocks.length ; i++ ) {
                    if ( ! (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(blocks[i])) )
                        return false;
                }
                return true;
            },
            cidrBlockAreaText: i18n._('Must be a one-per-line list of networks in CIDR format.') + ' ' + '(192.168.123.0/24)',


            portMatcher: function(val) {
                switch(val) {
                  case 'any':
                    return true;
                default:
                    if ( val.indexOf('>') != -1 && val.indexOf(',') == -1) {
                        return validators.isSinglePortValid( val.substring( val.indexOf('>') + 1 ));
                    }
                    if ( val.indexOf('<') != -1 && val.indexOf(',') == -1) {
                        return validators.isSinglePortValid( val.substring( val.indexOf('<') + 1 ));
                    }
                    if ( val.indexOf('-') == -1 && val.indexOf(',') == -1) {
                        return validators.isSinglePortValid(val);
                    }
                    if ( val.indexOf('-') != -1 && val.indexOf(',') == -1) {
                        return validators.isPortRangeValid(val);
                    }
                    return validators.isPortListValid(val);
                }
            },
            portMatcherText: Ext.String.format(i18n._("The port must be an integer number between {0} and {1}."), 1, 65535),

            port: function(val) {
                var minValue = 1;
                var maxValue = 65535;
                return (minValue <= val && val <= maxValue);
            },
            portText: Ext.String.format(i18n._("The port must be an integer number between {0} and {1} or one of the following values: any, all, n/a, none."), 1, 65535)
        });
    }
});