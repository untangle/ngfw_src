Ext.define('Ung.util.Converter', {
    singleton: true,
    alternateClassName: 'Converter',

    mapValueFormat: '{0} [{1}]'.t(),

    emailActionMap: {
        P: 'pass message'.t(),
        M: 'mark message'.t(),
        D: 'drop message'.t(),
        B: 'block message'.t(),
        Q: 'quarantine message'.t(),
        S: 'pass safelist message'.t(),
        Z: 'pass oversize message'.t(),
        O: 'pass outbound message'.t(),
        F: 'block message (scan failure)'.t(),
        G: 'pass message (scan failure)'.t(),
        Y: 'block message (greylist)'.t(),
        default:  'unknown action'.t()
    },
    emailAction: function(value){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return Ext.String.format(
                Converter.mapValueFormat,
                ( value in Converter.emailActionMap ) ? Converter.emailActionMap[value] : Converter.emailActionMap['default'],
                value
        );
    },

    loginSuccess: function( value ){
        return value ?  'success'.t() : 'failed'.t();
    },

    loginFrom: function( value ){
        return value ?  'local'.t() : 'remote'.t();
    },

    loginFailureReasonMap : {
        U:'invalid username'.t(),
        P: 'invalid password'.t(),
        default: ''

    },
    loginFailureReason: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.loginFailureReasonMap ) ? Converter.loginFailureReasonMap[value] : Converter.loginFailureReasonMap['default'];
    },

    quotaActionMap: {
        1: 'Given'.t(),
        2: 'Exceeded'.t(),
        default: 'Unknown'.t()
    },
    quotaAction: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return Ext.String.format(
                Converter.mapValueFormat,
                ( value in Converter.quotaActionMap ) ? Converter.quotaActionMap[value] : Converter.quotaActionMap['default'],
                value
        );
    },

    captivePortalEventInfoMap: {
        LOGIN: 'Login Success'.t(),
        FAILED: 'Login Failure'.t(),
        TIMEOUT: 'Session Timeout'.t(),
        INACTIVE: 'Idle Timeout'.t(),
        USER_LOGOUT: 'User Logout'.t(),
        ADMIN_LOGOUT: 'Admin Logout'.t(),
        HOST_CHANGE: 'Host Change Logout'.t(),
        default: 'Unknown'.t()
    },
    captivePortalEventInfo: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.captivePortalEventInfoMap ) ? Converter.captivePortalEventInfoMap[value] : Converter.captivePortalEventInfoMap['default'];
    },

    authTypeMap: {
        NONE: 'None'.t(),
        LOCAL_DIRECTORY: 'Local Directory'.t(),
        ACTIVE_DIRECTORY: 'Active Directory'.t(),
        RADIUS: 'RADIUS'.t(),
        GOOGLE: 'Google Account'.t(),
        FACEBOOK: 'Facebook Account'.t(),
        MICROSOFT: 'Microsoft Account'.t(),
        CUSTOM: 'Custom'.t(),
        default: 'Unknown'.t()
    },
    authType: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.authTypeMap ) ? Converter.authTypeMap[value] : Converter.authTypeMap['default'];
    },

    directoryConnectorActionMap: {
        I: 'login'.t(),
        U: 'update'.t(),
        O: 'logout'.t(),
        A: 'authenticate'.t(),
        default: 'unknown'.t()

    },
    directoryConnectorAction: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.directoryConnectorActionMap ) ? Converter.directoryConnectorActionMap[value] : Converter.directoryConnectorActionMap['default'];
    },

    directoryConnectorActionSourceMap: {
        W: 'client'.t(),
        A: 'active directory'.t(),
        R: 'radius'.t(),
        T: 'test'.t(),
        default: 'unknown'.t()

    },
    directoryConnectorActionSource: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.directoryConnectorActionSourceMap ) ? Converter.directoryConnectorActionSourceMap[value] : Converter.directoryConnectorActionSourceMap['default'];
    },

    bandwidthControlRule: function( value ){
        return Ext.isEmpty(value) ? 'none'.t() : value;
    },

    adBlockerActionMap:{
        B: 'block'.t(),
        default: 'pass'.t()
    },
    adBlockerAction: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.adBlockerActionMap ) ? Converter.adBlockerActionMap[value] : Converter.adBlockerActionMap['default'];
    },

    configurationBackupSuccessMap:{
        true: 'success'.t(),
        default: 'failed'.t()
    },
    configurationBackupSuccess: function( value ){
        return ( value in Converter.configurationBackupSuccessMap ) ? Converter.configurationBackupSuccessMap[value] : Converter.configurationBackupSuccessMap['default'];
    }

});
