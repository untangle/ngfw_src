/**
 * MailAddrs model definition
 * matching the mail_addrs sql table fields
 */
Ext.define ('Ung.model.mail_addrs', {
    extend: 'Ext.data.Model',
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    },
    fields: [
        { name: 'time_stamp', type: 'auto', convert: Converter.timestamp },
        { name: 'session_id', type: 'string' },
        { name: 'server_intf', type: 'integer', convert: Converter.interface },
        { name: 'c_client_addr', type: 'string' },
        { name: 'c_server_addr', type: 'string' },
        { name: 'c_client_port', type: 'integer' },
        { name: 'c_server_port', type: 'integer' },
        { name: 's_client_addr', type: 'string' },
        { name: 's_server_addr', type: 'string' },
        { name: 's_client_port', type: 'integer' },
        { name: 's_server_port', type: 'integer' },
        { name: 'policy_id', type: 'integer', convert: Converter.policy },
        { name: 'username', type: 'string' },

        { name: 'msg_id', type: 'string' },
        { name: 'subject', type: 'string' },
        { name: 'addr', type: 'string' },
        { name: 'addr_name', type: 'string' },
        { name: 'addr_kind', type: 'string' },

        { name: 'hostname', type: 'string' },

        { name: 'event_id', type: 'string' },
        { name: 'sender', type: 'string' },

        { name: 'virus_blocker_lite_clean', type: 'boolean' },
        { name: 'virus_blocker_lite_name', type: 'string' },
        { name: 'virus_blocker_clean', type: 'boolean' },
        { name: 'virus_blocker_name', type: 'string' },

        { name: 'spam_blocker_lite_score', type: 'number' },
        { name: 'spam_blocker_lite_is_spam', type: 'boolean' },
        { name: 'spam_blocker_lite_action', type: 'string' },
        { name: 'spam_blocker_lite_tests_string', type: 'string' },
        { name: 'spam_blocker_score', type: 'number' },
        { name: 'spam_blocker_is_spam', type: 'boolean' },
        { name: 'spam_blocker_action', type: 'string' },
        { name: 'spam_blocker_tests_string', type: 'string' },

        { name: 'phish_blocker_score', type: 'number' },
        { name: 'phish_blocker_is_spam', type: 'boolean' },
        { name: 'phish_blocker_tests_string', type: 'string' },
        { name: 'phish_blocker_action',  type: 'string' }
    ]
});
