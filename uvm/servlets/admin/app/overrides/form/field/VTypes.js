Ext.define('Ung.overrides.form.field.VTypes', {
    override: 'Ext.form.field.VTypes',

    // init: function() {
    //     this.initBundleLoader();
    // },

    mask: {
        macAddrMaskRe: /^[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}$/,
        ip4AddrMaskRe: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
        ip6AddrMaskRe: /^\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?\s*$/,
        email: /^(")?(?:[^\."])(?:(?:[\.])?(?:[\w\-!#$%&'*+/=?^_`{|}~]))*\1@(\w[\-\w]*\.){1,5}([A-Za-z]){2,63}$/,
        ipAddrRange: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)-(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
        cidrRange: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/[0-3]?[0-9]$/,
        ipNetmask: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
    },

    isSinglePortValid: function(val) {
        /* check for values between 0 and 65536 */
        if (val < 0 || val > 65536) { return false; }
        /* verify its an integer (not a float) */
        if (!/^\d{1,5}$/.test(val)) { return false; }
        return true;
    },
    isPortRangeValid: function(val) {
        var portRange = val.split('-');
        if (portRange.length !== 2) { return false; }
        return this.isSinglePortValid(portRange[0]) && this.isSinglePortValid(portRange[1]);
    },
    isPortListValid: function(val) {
        var portList = val.split(','),
            retVal = true, i;
        for (i = 0; i < portList.length; i++) {
            if (portList[i].indexOf('-') !== -1) {
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
        return this.mask.ip4AddrMaskRe.test(val);
    },
    isIpRangeValid: function(val) {
        return this.mask.ipAddrRange.test(val);
    },
    isCIDRValid: function(val) {
        return this.mask.cidrRange.test(val);
    },
    isIpNetmaskValid:function(val) {
        return this.mask.ipNetmask.test(val);
    },
    isIpListValid: function(val) {
        var ipList = val.split(','),
            retVal = true, i;
        for (i = 0; i < ipList.length; i += 1) {
            if (ipList[i].indexOf('-') !== -1) {
                retVal = retVal && this.isIpRangeValid(ipList[i]);
            } else {
                if (ipList[i].indexOf('/') !== -1) {
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
    },


    // matchers
    email: function (v) {
        return this.mask.email.test(v);
    },
    ipMatcher: function(val) {
        if (val.indexOf('/') === -1 && val.indexOf(',') === -1 && val.indexOf('-') === -1) {
            switch (val) {
                case 'any':
                return true;
            default:
                return this.isSingleIpValid(val);
            }
        }
        if (val.indexOf(',') !== -1) {
            return this.isIpListValid(val);
        } else {
            if (val.indexOf('-') !== -1) {
                return this.isIpRangeValid(val);
            }
            if (val.indexOf('/') !== -1) {
                var cidrValid = this.isCIDRValid(val);
                var ipNetmaskValid = this.isIpNetmaskValid(val);
                return cidrValid || ipNetmaskValid;
            }
            console.log('Unhandled case while handling vtype for ipAddr:', val, ' returning true !');
            return true;
        }
    },
    ipMatcherText: 'Invalid IP Address.'.t(),


    ip4Address: function (val) {
        return this.ip4AddrMaskRe.test(val);
    },
    ip4AddressText: 'Invalid IPv4 Address.'.t(),


    ip4AddressList:  function (v) {
        var addr = v.split(','), i;
        for (i = 0 ; i < addr.length ; i += 1) {
            if (!this.ip4AddrMaskRe.test(addr[i])) {
                return false;
            }
        }
        return true;
    },
    ip4AddressListText: 'Invalid IPv4 Address(es).'.t(),

    ip6Address: function (val) {
        return this.ip6AddrMaskRe.test(val);
    },
    ip6AddressText: 'Invalid IPv6 Address.'.t(),

    ipAddress: function (val) {
        return this.ip4AddrMaskRe.test(val) || this.ip6AddrMaskRe.test(val);
    },
    ipAddressText: 'Invalid IP Address.'.t(),

    macAddress: function (val) {
        return this.macAddrMaskRe.test(val);
    },
    macAddressText: 'Invalid Mac Address.'.t(),

    cidrBlock:  function (v) {
        return (/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(v));
    },
    cidrBlockText: 'Must be a network in CIDR format.'.t() + ' ' + '(192.168.123.0/24)',

    cidrBlockList: function (v) {
        var blocks = v.split(','), i;
        for (i = 0 ; i < blocks.length; i += 1) {
            if (!(/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(blocks[i]))) {
                return false;
            }
        }
        return true;
    },
    cidrBlockListText: 'Must be a comma seperated list of networks in CIDR format.'.t() + ' ' + '(192.168.123.0/24,1.2.3.4/24)',

    cidrBlockArea:  function (v) {
        var blocks = v.split('\n'), i;
        for (i = 0 ; i < blocks.length; i += 1) {
            if (!(/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/.test(blocks[i]))) {
                return false;
            }
        }
        return true;
    },
    cidrBlockAreaText: 'Must be a one-per-line list of networks in CIDR format.'.t() + ' ' + '(192.168.123.0/24)',


    portMatcher: function (val) {
        switch (val) {
            case 'any':
            return true;
        default:
            if (val.indexOf('>') !== -1 && val.indexOf(',') === -1) {
                return this.isSinglePortValid( val.substring( val.indexOf('>') + 1 ));
            }
            if (val.indexOf('<') !== -1 && val.indexOf(',') === -1) {
                return this.isSinglePortValid( val.substring( val.indexOf('<') + 1 ));
            }
            if (val.indexOf('-') === -1 && val.indexOf(',') === -1) {
                return this.isSinglePortValid(val);
            }
            if (val.indexOf('-') !== -1 && val.indexOf(',') === -1) {
                return this.isPortRangeValid(val);
            }
            return this.isPortListValid(val);
        }
    },
    portMatcherText: Ext.String.format('The port must be an integer number between {0} and {1}.'.t(), 1, 65535),

    port: function (val) {
        var minValue = 1,
            maxValue = 65535;
        return (minValue <= val && val <= maxValue);
    },
    portText: Ext.String.format('The port must be an integer number between {0} and {1} or one of the following values: any, all, n/a, none.'.t(), 1, 65535)





    // // ip all
    // ipall: function (val) {
    //     console.log(this.validators.isSinglePortValid(150000000));
    //     if ( val.indexOf('/') === -1 && val.indexOf(',') === -1 && val.indexOf('-') === -1) {
    //         switch (val) {
    //         case 'any':
    //             return true;
    //         default:
    //             return this.ip4(val);
    //         }
    //     }
    //     if (val.indexOf(',') !== -1) {
    //         return this.ipList(val);
    //     } else {
    //         if ( val.indexOf('-') !== -1) {
    //             return this.ipRange(val);
    //         }
    //         if ( val.indexOf('/') !== -1) {
    //             return this.cidrRange(val) || this.ipNetmask(val);
    //         }
    //         console.log('Unhandled case while handling vtype for ipAddr:', val, ' returning true!');
    //         return true;
    //     }
    // },
    // ipallText: 'Invalid IP Address.'.t(),

    // // ip any
    // ip: function (value) { return this.ip4Re.test(value) || this.ip6Re.test(value); },
    // ipText: 'Invalid IP Address.'.t(),

    // // ip4 address
    // ip4: function (value) { return this.ip4Re.test(value); },
    // ip4Re: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
    // ip4Text: 'Invalid IPv4 Address.'.t(),

    // // ip6 address
    // ip6: function (value) { return this.ip6Re.test(value); },
    // ip6Re: '/^\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?\s*$/',
    // ip6Text: 'Invalid IPv6 Address.'.t(),

    // // ip4 list
    // ip4List: function (value) {
    //     var addr = value.split(','), i;
    //     for (i = 0 ; i < addr.length ; i++) {
    //         if (!this.ip4Re.test(addr[i])) {
    //             return false;
    //         }
    //     }
    //     return true;
    // },
    // ip4ListText: 'Invalid IPv4 Address(es).'.t(),

    // // mac address
    // mac: function (value) { return this.macRe.test(value); },
    // macRe: /^[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}$/,
    // macText: 'Invalid Mac Address.'.t(),

    // // cidr block
    // cidr: function (value) { return this.cidrRe.test(value); },
    // cidrRe: /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/,
    // cidrText: 'Must be a network in CIDR format.'.t() + ' (192.168.123.0/24)',

    // // cidr list
    // cidrList: function (value) {
    //     var blocks = value.split(','), i;
    //     for (i = 0 ; i < blocks.length ; i++) {
    //         if (!this.cidrRe.test(blocks[i])) {
    //             return false;
    //         }
    //     }
    //     return true;
    // },
    // cidrListText: 'Must be a comma seperated list of networks in CIDR format.'.t() + ' (192.168.123.0/24,1.2.3.4/24)',

    // // cidr block
    // cidrBlock: function (value) {
    //     var blocks = value.split('\n'), i;
    //     for (i = 0 ; i < blocks.length ; i++) {
    //         if (!this.cidrRe.test(blocks[i])) {
    //             return false;
    //         }
    //     }
    //     return true;
    // },
    // cidrBlockText: 'Must be a one-per-line list of networks in CIDR format.'.t() + ' (192.168.123.0/24)',

    // // port
    // port: function(value) {
    //     var minValue = 1;
    //     var maxValue = 65535;
    //     return (value >= minValue && value <= maxValue);
    // },
    // portText: Ext.String.format('The port must be an integer number between {0} and {1} or one of the following values: any, all, n/a, none.'.t(), 1, 65535),


    // ipRange: function (value) { return this.ipRangeRe.test(value); },
    // ipRangeRe: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)-(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
    // ipRangeText: 'Invalid IP range'.t(),

    // cidrRange: function (value)  { return this.cidrRangeRe.test(value); },
    // cidrRangeRe: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/[0-3]?[0-9]$/,

    // ipNetmask: function (value) { return this.cidrRangeRe.test(value); },
    // ipNetmaskRe: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,


    // ipList: function (val) {
    //     var ipList = val.split(',');
    //     var retVal = true;
    //     for ( var i = 0; i < ipList.length;i++) {
    //         if ( ipList[i].indexOf('-') !== -1) {
    //             retVal = retVal && this.ipRange(ipList[i]);
    //         } else {
    //             if ( ipList[i].indexOf('/') !== -1) {
    //                 retVal = retVal && ( this.cidRange(ipList[i]) || this.ipNetmask(ipList[i]));
    //             } else {
    //                 retVal = retVal && this.ip4(ipList[i]);
    //             }
    //         }
    //         if (!retVal) {
    //             return false;
    //         }
    //     }
    //     return true;
    // }
});