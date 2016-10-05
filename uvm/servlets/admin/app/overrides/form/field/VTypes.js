Ext.define('Ung.overrides.form.field.VTypes', {
    override: 'Ext.form.field.VTypes',

    validators: function () {

    },

    // ip all
    ipall: function (val) {
        if ( val.indexOf('/') == -1 && val.indexOf(',') == -1 && val.indexOf('-') == -1) {
            switch (val) {
            case 'any':
                return true;
            default:
                return this.ip4(val);
            }
        }
        if (val.indexOf(',') != -1) {
            return this.ipList(val);
        } else {
            if ( val.indexOf('-') != -1) {
                return this.ipRange(val);
            }
            if ( val.indexOf('/') != -1) {
                return this.cidrRange(val) || this.ipNetmask(val);
            }
            console.log('Unhandled case while handling vtype for ipAddr:', val, ' returning true!');
            return true;
        }
    },
    ipallText: 'Invalid IP Address.'.t(),

    // ip any
    ip: function (value) { return this.ip4Re.test(value) || this.ip6Re.test(value); },
    ipText: 'Invalid IP Address.'.t(),

    // ip4 address
    ip4: function (value) { return this.ip4Re.test(value); },
    ip4Re: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
    ip4Text: 'Invalid IPv4 Address.'.t(),

    // ip6 address
    ip6: function (value) { return this.ip6Re.test(value); },
    ip6Re: '/^\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?\s*$/',
    ip6Text: 'Invalid IPv6 Address.'.t(),

    // ip4 list
    ip4List: function (value) {
        var addr = value.split(','), i;
        for (i = 0 ; i < addr.length ; i++) {
            if (!this.ip4Re.test(addr[i]))
                return false;
        }
        return true;
    },
    ip4ListText: 'Invalid IPv4 Address(es).'.t(),

    // mac address
    mac: function (value) { return this.macRe.test(value); },
    macRe: /^[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}$/,
    macText: 'Invalid Mac Address.'.t(),

    // cidr block
    cidr: function (value) { return this.cidrRe.test(value); },
    cidrRe: /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/,
    cidrText: 'Must be a network in CIDR format.'.t() + ' (192.168.123.0/24)',

    // cidr list
    cidrList: function (value) {
        var blocks = value.split(','), i;
        for (i = 0 ; i < blocks.length ; i++) {
            if (!this.cidrRe.test(blocks[i]))
                return false;
        }
        return true;
    },
    cidrListText: 'Must be a comma seperated list of networks in CIDR format.'.t() + ' (192.168.123.0/24,1.2.3.4/24)',

    // cidr block
    cidrBlock: function (value) {
        var blocks = value.split('\n'), i;
        for (i = 0 ; i < blocks.length ; i++) {
            if (!this.cidrRe.test(blocks[i]))
                return false;
        }
        return true;
    },
    cidrBlockText: 'Must be a one-per-line list of networks in CIDR format.'.t() + ' (192.168.123.0/24)',

    // port
    port: function(value) {
        var minValue = 1;
        var maxValue = 65535;
        return (value >= minValue && value <= maxValue);
    },
    portText: Ext.String.format('The port must be an integer number between {0} and {1} or one of the following values: any, all, n/a, none.'.t(), 1, 65535),


    ipRange: function (value) { return this.ipRangeRe.test(value); },
    ipRangeRe: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)-(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,
    ipRangeText: 'Invalid IP range'.t(),

    cidrRange: function (value)  { return this.cidrRangeRe.test(value); },
    cidrRangeRe: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/[0-3]?[0-9]$/,

    ipNetmask: function (value) { return this.cidrRangeRe.test(value); },
    ipNetmaskRe: /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/,


    ipList: function (val) {
        var ipList = val.split(',');
        var retVal = true;
        for ( var i = 0; i < ipList.length;i++) {
            if ( ipList[i].indexOf('-') != -1) {
                retVal = retVal && this.ipRange(ipList[i]);
            } else {
                if ( ipList[i].indexOf('/') != -1) {
                    retVal = retVal && ( this.cidRange(ipList[i]) || this.ipNetmask(ipList[i]));
                } else {
                    retVal = retVal && this.ip4(ipList[i]);
                }
            }
            if (!retVal) {
                return false;
            }
        }
        return true;
    }
});