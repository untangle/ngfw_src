/**
 * contains methods for converting the column values to a different form
 * e.g. ids => strings found in mapping file
 * converters help with column sorting and filtering options
 * so instead of sorting by underlaying initial id value,
 * column will be sorted/filtered by converted string value
 */
Ext.define('Ung.util.Converter', {
    singleton: true,
    alternateClassName: 'Converter',

    mapValueFormat: '{0} [{1}]'.t(),

    timestamp: function (value) {
        if(Renderer.timestampOffset === null){
            Renderer.timestampOffset =  (new Date().getTimezoneOffset() * 60000) + rpc.timeZoneOffset;
        }
        if (!value) { return ''; }
        if ((typeof(value) === 'object') && value.time) { value = value.time; }
        if (value < 2696400000){ value *= 1000; }
        var date = new Date(value);
        date.setTime(value + Renderer.timestampOffset);
        return Ext.util.Format.date(date, 'timestamp_fmt'.t());
    },

    interface: function (v) {
        return (Map.interfaces[v] || 'None'.t()) + ' [' + v + ']';
    },
    country: function (v) { return Map.countries[v] || v; },
    policy: function (v) { return Map.policies[v] || 'None'.t(); },
    webReason: function (v) { return Map.webReasons[v] || 'no rule applied'.t(); },
    webCategory: function (v) { return Map.webCategories[v] || v; },
    protocol: function (v) { return Map.protocols[v] || v; },

    icmp: function (v) { return Map.icmps[v] || 'Unasigned'.t(); },
    ipaddr: function (v) { return v || ''; },

    httpMethod: function (v) { return Map.httpMethods[v] || v; },
    loginFailureReason: function (v) { return Map.loginFailureReasons[v] || ''; },

    priority: function (v) { return Map.httpMethods[v] || v; },
    emailAction: function (v) { return Map.emailActions[v] || 'unknown action'.t(); },

    authType: function (v) {
        var types = {
            NONE: 'None'.t(),
            LOCAL_DIRECTORY: 'Local Directory'.t(),
            ACTIVE_DIRECTORY: 'Active Directory'.t(),
            RADIUS: 'RADIUS'.t(),
            GOOGLE: 'Google Account'.t(),
            FACEBOOK: 'Facebook Account'.t(),
            MICROSOFT: 'Microsoft Account'.t(),
            CUSTOM: 'Custom'.t()
        };
        if (Ext.isEmpty(v)) { return ''; }
        return types[v] || 'Unknown'.t();
    },

    directoryConnectorAction: function (v) {
        var actions = {
            I: 'login'.t(),
            U: 'update'.t(),
            O: 'logout'.t(),
            A: 'authenticate'.t()
        };
        if (Ext.isEmpty(v)) { return ''; }
        return actions[v] || 'unknown'.t();
    },

    captivePortalEventInfo: function (v) {
        var events = {
            LOGIN: 'Login Success'.t(),
            FAILED: 'Login Failure'.t(),
            TIMEOUT: 'Session Timeout'.t(),
            INACTIVE: 'Idle Timeout'.t(),
            USER_LOGOUT: 'User Logout'.t(),
            ADMIN_LOGOUT: 'Admin Logout'.t(),
            HOST_CHANGE: 'Host Change Logout'.t(),
        };
        if (Ext.isEmpty(v)) { return ''; }
        return events[v] || 'Unknown'.t();
    },

    quotaAction: function (v) {
        var actions = {
            1: 'Given'.t(),
            2: 'Exceeded'.t()
        };
        if (Ext.isEmpty(v)) { return ''; }
        return actions[v] || 'Unknown'.t();
    },

    // login_type is a non existant field
    // directoryConnectorActionSource: function (v) {
    //     var sources = {
    //         W: 'client'.t(),
    //         A: 'active directory'.t(),
    //         R: 'radius'.t(),
    //         T: 'test'.t(),
    //     }
    //     if (Ext.isEmpty(v)) { return ''; }
    //     return sources[v] || 'unknown'.t();
    // },

    // not implemented, see old Renderer
    webRule: function () {}
});
