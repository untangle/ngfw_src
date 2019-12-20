/**
 * HttpEvents model definition
 * matching the http-events sql table fields
 */
Ext.define ('Ung.model.HttpEvents', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'request_id', type: 'string' },
        { name: 'time_stamp', type: 'auto' },
        { name: 'session_id', type: 'string' },
        { name: 'client_intf', type: 'integer', convert: function (v) { return Map.interfaces[v] || v; } },
        { name: 'server_intf', type: 'integer', convert: function (v) { return Map.interfaces[v] || v; } },
        { name: 'c_client_addr', type: 'string' },
        { name: 's_client_addr', type: 'string' },
        { name: 'c_server_addr', type: 'string' },
        { name: 's_server_addr', type: 'string' },
        { name: 'c_client_port', type: 'integer' },
        { name: 's_client_port', type: 'integer' },
        { name: 'c_server_port', type: 'integer' },
        { name: 's_server_port', type: 'integer' },
        { name: 'client_country', type: 'string', convert: function (v) { return Map.countries[v] || v; } },
        { name: 'client_latitude', type: 'string' },
        { name: 'client_longitude', type: 'string' },
        { name: 'server_country', type: 'string', convert: function (v) { return Map.countries[v] || v; } },
        { name: 'server_latitude', type: 'string' },
        { name: 'server_longitude', type: 'string' },
        { name: 'policy_id', type: 'integer', convert: function (v) { return Map.policies[v] || v; } },
        { name: 'username', type: 'string' },
        { name: 'hostname', type: 'string' },
        { name: 'method', type: 'string' },
        { name: 'uri', type: 'string' },
        { name: 'host', type: 'string' },
        { name: 'domain', type: 'string' },
        { name: 'referer', type: 'string' },

        { name: 'c2s_content_length', type: 'integer' },
        { name: 's2c_content_length', type: 'integer' },
        { name: 's2c_content_type', type: 'string' },
        { name: 's2c_content_filename', type: 'string' },

        { name: 'ad_blocker_cookie_ident', type: 'string' },
        { name: 'ad_blocker_action', type: 'string' },

        { name: 'web_filter_reason', type: 'string', convert: function (v) { return Map.webReasons[v] || 'no rule applied'.t(); } },
        { name: 'web_filter_category_id', type: 'integer', convert: function (v) { return Map.webCategories[v] || v; } },
        { name: 'web_filter_rule_id', type: 'integer' },
        { name: 'web_filter_blocked', type: 'boolean' },
        { name: 'web_filter_flagged', type: 'boolean' },

        { name: 'virus_blocker_lite_clean', type: 'boolean' },
        { name: 'virus_blocker_lite_name', type: 'string' },
        { name: 'virus_blocker_clean', type: 'boolean' },
        { name: 'virus_blocker_name', type: 'string' },

        { name: 'threat_prevention_blocked', type: 'boolean' },
        { name: 'threat_prevention_flagged', type: 'boolean' },
        { name: 'threat_prevention_rule_id', type: 'integer' },
        { name: 'threat_prevention_reputation', type: 'integer' },
        { name: 'threat_prevention_categories', type: 'integer' },

        // converted fields,
        {
            name: '_r_time_stamp',
            calculate: function (data) {
                var value = data.time_stamp;
                if(Renderer.timestampOffset === null){
                    Renderer.timestampOffset =  (new Date().getTimezoneOffset() * 60000) + rpc.timeZoneOffset;
                }
                if (!value) { return ''; }
                if ((typeof(value) === 'object') && value.time) { value = value.time; }
                if (value < 2696400000){ value *= 1000; }
                var date = new Date(value);
                date.setTime(value + Renderer.timestampOffset);
                return Ext.util.Format.date(date, 'timestamp_fmt'.t());
            }
        },
        // {
        //     name: '_r_client_intf',
        //     convert: function (v, rec) {
        //         return Renderer2.interface(rec.data.client_intf);
        //     }
        // },
        // {
        //     name: '_r_server_intf',
        //     convert: function (v, rec) {
        //         return Map.interfaces[rec.data.server_intf] || rec.data.server_intf;
        //     }
        // },
        // {
        //     name: '_r_client_country',
        //     convert: function (v, rec) {
        //         return Map.countries[rec.data.client_country] || rec.data.client_country;
        //     }
        // },
        // {
        //     name: '_r_server_country',
        //     convert: function (v, rec) {
        //         return Map.countries[rec.data.server_country] || rec.data.server_country;
        //     }
        // },
        // {
        //     name: '_r_policy_id',
        //     convert: function (v, rec) {
        //         return Map.policies[rec.data.policy_id] || rec.data.policy_id;
        //     }
        // },
        // {
        //     name: '_r_web_filter_reason',
        //     convert: function (v, rec) {
        //         return Map.httpReason[rec.data.web_filter_reason] || 'no rule applied'.t();
        //     }
        // },
        // {
        //     name: '_r_web_filter_category_id',
        //     convert: function (v, rec) {
        //         return Map.webCategory[rec.data.web_filter_category_id];
        //     }
        // }

    ]
});
