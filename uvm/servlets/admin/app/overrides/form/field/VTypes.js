Ext.define('Ung.overrides.form.field.VTypes', {
    override: 'Ext.form.field.VTypes',

    // init: function() {
    //     this.initBundleLoader();
    // },

    mask: {
        macAddrMaskRe: /^[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}$/,
        ip4AddrMaskRe: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
        ip6AddrMaskRe: /^\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?\s*$/,
        email: /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/,
        ipAddrRange: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)-(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
        cidrRange: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/[0-3]?[0-9]$/,
        ipNetmask: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
        openvpnName: /^[A-Za-z0-9]([-.0-9A-Za-z]*[0-9A-Za-z])?$/,
        positiveInteger: /^[0-9]+$/,
        domainNameRe: /^[a-zA-Z0-9\-_.]+$/,
        urlAddrRe: /^(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,63}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/,
        cidrAddrRe: /^([0-9]{1,3}\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[1-9])(\/([0-9]|[1-2][0-9]|3[0-2]))?$/,
        cidrBlockRe: /^([0-9]{1,3}\.){3}[0-9]{1,3}(\/([0-9]|[1-2][0-9]|3[0-2]))?$/,
        cidrBlockOnlyRangeRe: /^([0-9]{1,3}\.){3}[0-9]{1,3}(\/([0-9]|[1-2][0-9]|3[0-1]))$/,
        hostnameRe: /^[a-zA-Z0-9\-_.]+$/
    },

    isSinglePortValid: function(val) {
        /* check for values between 0 and 65536 */
        if (val < 0 || val > 65536) { return false; }
        /* verify its an integer (not a float) */
        if (!/^\d{1,5}$/.test(val)) { return false; }
        return true;
    },
    isSinglePortValidText: 'Invalid port (number between 0 and 65536)'.t(),

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
    isSingleIpValidText: 'Invalid IP address.'.t(),

    isSingleIpValidOrEmpty: function(val){
        if(val == ''){
            return true;
        }
        return this.mask.ip4AddrMaskRe.test(val);
    },
    isSingleIpValidOrEmptyText: 'Valut must either empty or IP address'.t(),

    isNotSingleIp: function(val) {
        return !this.isSingleIpValid(val);
    },
    isNotSingleIpText: 'Value can not be an IP address, it must be a valid hostname'.t(),

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
    emailText: 'You must provide a valid login name or email address.'.t(),

    // matchers
    emailwildcard: function (v) {
        return v == '*' || this.mask.email.test(v);
    },
    emailwildcardText: 'You must provide an email address or wildcard.'.t(),
    
    /**
     * The ipOrUrl VType can validate IP addresses and also URLs
     * 
     * @param {string} val - the input URL or  IP Address
     */
    ipOrUrl: function(val) {
        return this.ip4Address(val) || this.ip6Address(val) || this.url(val);
    },
    ipOrUrlText: 'Not a valid IP or URL'.t(),

    /**
     * 
     * the url VType can validate URL addresses with or without http/https
     * 
     * @param {string} val - the input url 
     */
    url: function(val) {
        return this.mask.urlAddrRe.test(val);
    },
    urlText: 'Not a valid URL'.t(),

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
        return this.mask.ip4AddrMaskRe.test(val);
    },
    ip4AddressText: 'Invalid IPv4 Address.'.t(),

    ip4AddressList:  function (v) {
        var addr = v.split(','), i;
        for (i = 0 ; i < addr.length ; i += 1) {
            if (!this.mask.ip4AddrMaskRe.test(addr[i])) {
                return false;
            }
        }
        return true;
    },
    ip4AddressListText: 'Invalid IPv4 Address(es).'.t(),

    ip6Address: function (val) {
        return this.mask.ip6AddrMaskRe.test(val);
    },
    ip6AddressText: 'Invalid IPv6 Address.'.t(),

    ipAddress: function (val) {
        return this.mask.ip4AddrMaskRe.test(val) || this.mask.ip6AddrMaskRe.test(val);
    },
    ipAddressText: 'Invalid IP Address.'.t(),

    macAddress: function (val) {
        return this.mask.macAddrMaskRe.test(val);
    },
    macAddressText: 'Invalid Mac Address.'.t(),

    openvpnName: function (val) {
        return this.mask.openvpnName.test(val);

    },
    openvpnNameText: 'A name should only contains numbers, letters, dashes and periods.  Spaces are not allowed.'.t(),

    cidrBlockOnlyRanges: function(v) {
        return (this.mask.cidrBlockOnlyRangeRe.test(v));
    },

    cidrBlockOnlyRangesText: 'Must be a network in CIDR format, excluding /32 addresses.'.t() + ' (192.168.123.0/24)',

    domainName: function(value) {
        if (value.charAt(0) === '.') {
            return(false);
        }
        return this.mask.domainNameRe.test(value);
    },
    domainNameText: 'A domain can only contain numbers, letters, dashes and periods, and must not start with a period.'.t(),

    cidrBlock:  function (v) {
        return (this.mask.cidrBlockRe.test(v));
    },
    cidrBlockText: 'Must be a network in CIDR format.'.t() + ' ' + '(192.168.123.0/24)',

    cidrBlockList: function (v) {
        var blocks = v.split(','), i;
        for (i = 0 ; i < blocks.length; i += 1) {
            if (!(this.mask.cidrBlockRe.test(blocks[i]))) {
                return false;
            }
        }
        return true;
    },
    cidrBlockListText: 'Must be a comma seperated list of networks in CIDR format.'.t() + ' ' + '(192.168.123.0/24,1.2.3.4/24)',

    cidrBlockArea:  function (v) {
        var blocks = v.split('\n'), i;
        for (i = 0 ; i < blocks.length; i += 1) {
            if (!(this.mask.cidrBlockRe.test(blocks[i]))) {
                return false;
            }
        }
        return true;
    },
    cidrBlockAreaText: 'Must be a one-per-line list of networks in CIDR format.'.t() + ' ' + '(192.168.123.0/24)',

    cidrAddr:  function (v) {
        return (this.mask.cidrAddrRe.test(v));
    },
    cidrAddrText: 'Must be an address in CIDR format where the last octet is not zero.'.t() + ' ' + '(192.168.123.1/24)',

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
    portText: Ext.String.format('The port must be an integer number between {0} and {1} or one of the following values: any, all, n/a, none.'.t(), 1, 65535),

    x500attributes: function(val){
        if(val == ''){
            return false;
        }
        var valid = true;
        val.split(',').forEach(function(attribute){
            /*
             * Various LDAP implementations may have attribute keys beyond
             * what's defined in RFC 2253.  Instead of hunting them all
             * down, verify they are of a <key>=<value> format.
             */
            if(attribute.split('=').length != 2 ){
                valid = false;
            }
        });
        return valid;
    },
    x500attributesText: 'Invalid x500 attribute found'.t(),

    // BPP/OSPF router id
    routerId: function(value){
        return this.mask.ip4AddrMaskRe.test(value);
    },
    routerIdText: 'Invalid Router ID'.t(),

    // OSPF area id
    routerArea: function(value){
        return this.mask.ip4AddrMaskRe.test(value);
    },
    routerAreaText: 'Invalid Area'.t(),

    // BPP/OSPF router AS
    routerAs: function(value){
        if(isNaN(value) || ( this.mask.positiveInteger.test(value) == false ) ){
            return false;
        }
        var number = parseInt(value, 10);
        return (number > 0) && (number <= 4294967295);
    },
    routerAsText: 'Invalid Router AS'.t(),

    metric: function(value){
        if(isNaN(value) || ( this.mask.positiveInteger.test(value) == false ) ){
            return false;
        }
        var number = parseInt(value, 10);
        return (number >= 0) && (number <= 16777214);
    },
    metricText: 'Invalid metric number'.t(),

    routerInterval: function(value){
        if(isNaN(value) || ( this.mask.positiveInteger.test(value) == false ) ){
            return false;
        }
        var number = parseInt(value, 10);
        return (number > 0) && (number <= 65535);
    },
    routerIntervalText: 'Invalid interval'.t(),

    routerPriority: function(value){
        if(isNaN(value) || ( this.mask.positiveInteger.test(value) == false ) ){
            return false;
        }
        var number = parseInt(value, 10);
        return (number >= 0) && (number <= 65535);
    },
    routerPriorityText: 'Invalid router priority'.t(),

    routerAutoCost: function(value){
        if(isNaN(value) || ( this.mask.positiveInteger.test(value) == false ) ){
            return false;
        }
        var number = parseInt(value, 10);
        return (number >= 0) && (number <= 4294967);
    },
    routerAutoCostText: 'Invalid auto cost reference bandwidth'.t(),

    mtu: function(value){
        if(isNaN(value) || ( this.mask.positiveInteger.test(value) == false ) ){
            return false;
        }
        var number = parseInt(value, 10);
        return (number == 0 || number >= 68);
    },
    mtuText: 'Invalid value (integer above 68, 0 for default)'.t(),

    keepalive: function(value) {
        if(isNaN(value) || ( this.mask.positiveInteger.test(value) == false ) ){
            return false;
        }
        return true;
    },
    keepaliveText: 'Keepalive must be a number or 0 to disable.'.t(),

    wireguardPublicKey: function(value){
        return (value.length == 44) ? true : false;
    },
    wireguardPublicKeyText: 'Public key must be 44 characters long.'.t(),

    isHostnameValid: function(value) {
        if (value.charAt(0) === '.') {
            return false;
        }
        if (!this.mask.hostnameRe.test(value)){
            return false;
        }
        return true;
    },
    isHostnameValidText: 'A hostname can only contain numbers, letters, dashes and periods and cannot begin with a period'.t(),
});
