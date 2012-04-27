Ung.RuleValidator = function() {
}

Ung.RuleValidator.prototype = {

    isSinglePortValid: function(val) {
        return 1 <= val && val <= 65536;
    },
    
    isPortRangeValid: function(val) {
        var portRange = val.split('-');
        var portRe =/^\d{1,5}$/;
        if ( portRe.test(portRange[0]) && portRe.test(portRange[1])) {
            return (1 <= portRange[0] && portRange[0] <= 65536) && (1 <= portRange[1] && portRange[1] <= 65536);
        } else {
           // console.log("port regex fail for" , portRange);
            return false;
        }
    },
    
    isPortListValid:function(val) {
        var portList = val.split(',');
       // console.log("portList=",portList);
        var retVal = true;
        for ( var i = 0; i < portList.length;i++) {
            if ( portList[i].indexOf("-") != -1) {
                retVal = retVal && this.isPortRangeValid(portList[i]);
            } else {
                retVal = retVal && this.isSinglePortValid(portList[i]);
            }
            if (!retVal) {
                //console.log("portList=",portList," NOT VALID !");
                return false;
            }
        }
        return true;
    },
    
    isSingleIpValid:function(val) {
        var ipAddrMaskRe = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
        return ipAddrMaskRe.test(val);
    },
    
    isIpRangeValid:function(val) {
        var ipAddrRange = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)-(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
        return ipAddrRange.test(val);
    },
    
    isCIDRValid:function(val) {
        var cidrRange = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/[0-3]?[0-9]$/;
        return cidrRange.test(val);
    },
    
    isIpNetmaskValid:function(val) {
        var ipNetmask = /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
        return ipNetmask.test(val);
    },
    
    isIpListValid:function(val) {
        var ipList = val.split(',');
       //console.log("ipList=",ipList);
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
                //console.log("ipList=",ipList[i]," NOT VALID !");
                return false;
            }
        }
        return true;

    }

    
};