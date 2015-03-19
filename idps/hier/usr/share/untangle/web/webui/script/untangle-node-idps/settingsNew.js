/* TODO ext5,  make standard
Ext.define('Ung.RuleEditorGrid', {
    extend: 'Ung.grid.Panel',
    requires: [
        //'Ext.ux.statusbar.StatusBar' //TODO: ext5
    ],

     // search value initialization
    searchValue: null,
    // The generated regular expression used for searching.
    searchRegExp: null,
    defaultStatusText: i18n._('Loading...'),
    // store fields to search
    searchFields:[
        'category',
        'rule'
    ],
     // Minimum number of characters to start search
    searchMinimumCharacters: 2,

    // Component initialization override: adds the top and bottom toolbars and setup headers renderer.
    initComponent: function() {
        var me = this;
        me.bbar = [
            i18n._('Search'), {
                xtype: 'textfield',
                name: 'searchField',
                hideLabel: true,
                width: 200,
                listeners: {
                    change: {
                        fn: me.searchFilter,
                        scope: this,
                        buffer: 100
                    }
                }
            },{
                xtype: 'checkbox',
                name: 'searchLog',
                boxLabel: i18n._("Log"),
                listeners: {
                    change: {
                        fn: me.searchFilter,
                        scope: this
                    }
                }
            },{
                xtype: 'checkbox',
                name: 'searchBlock',
                boxLabel: i18n._("Block"),
                listeners: {
                    change: {
                        fn: me.searchFilter,
                        scope: this
                    }
                }
            },{
                xtype: 'statusbar',
                defaultText: me.defaultStatusText,
                name: 'searchStatusBar',
                border: 0
            }
        ];

        me.fields.push({
            name: "originalId",
            mapping: null
        });

        me.fields.push({
            name: "path",
            mapping: null
        });

        this.referencesStore = Ext.create( 'Ext.data.ArrayStore', {
            fields: [ 'system', 'prefix'],
            data: [
                [ "bugtraq", "http://www.securityfocus.com/bid/" ],
                [ "cve","http://cve.mitre.org/cgi-bin/cvename.cgi?name=" ],
                [ "nessus", "http://cgi.nessus.org/plugins/dump.php3?id="],
                [ "arachnids", "http://www.whitehats.com/info/IDS"],
                [ "mcafee", "http://vil.nai.com/vil/content/v" ],
                [ "osvdb", "http://osvdb.org/show/osvdb/" ],
                [ "msb", "http://technet.microsoft.com/en-us/security/bulletin/" ],
                [ "url","http://" ]
            ]
        });

        me.callParent(arguments);

    },

    // afterRender override: it adds textfield and statusbar reference and start monitoring keydown events in textfield input
    afterRender: function() {
        var me = this;
        me.callParent(arguments);

        me.searchTextField = me.down('textfield[name=searchField]');
        me.searchStatusBar = me.down('statusbar[name=searchStatusBar]');

    },

    regexRuleVariable :  /^\$([A-Za-z0-9\_]+)/,
    regexRule: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)\s+(tcp|udp|icmp|ip)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+\((.+)\)$/,
    regexRuleReference: /\s+reference:\s*([^\;]+)\;/g,

    afterDataBuild: function(handler){
        var me = this;
        me.callParent(arguments);

        me.updateRulesStatus();

        me.storeCategories = Ext.create('Ext.data.Store', {
            fields: ['id', 'value']
        });
        me.storeClasstypes = Ext.create('Ext.data.Store', {
            fields: ['id', 'value']
        });
        me.storeActiveVariables = Ext.create('Ext.data.Store', {
            fields: ['id', 'ruleIds']
        });

        me.store.each(
            function( record ){
                var category = record.get("category");
                if( this.storeCategories.find( 'id', category ) == -1 ){
                    this.storeCategories.add( { id: category, value: category } );
                }
                var classtype = record.get("classtype");
                if( this.storeClasstypes.find( 'id', classtype ) == -1 ){
                    this.storeClasstypes.add( { id: classtype, value: classtype } );
                }
            },
            this
        );

        me.rowEditor.down('combo[name=Classtype]').bindStore(me.storeClasstypes);
        me.rowEditor.down('combo[name=Category]').bindStore(me.storeCategories);
    },

    buildActiveVariableStore: function(){
        me = this;
        me.store.each(
            function( record ){
                var ruleMatches, variableMatches;
                var i, j;
                var variableRecord;
                var ruleId;
                if( me.regexRule.test( record.get("rule") ) ){
                    ruleMatches = me.regexRule.exec( record.get("rule") );
                    for( i = 1; i < ruleMatches.length; i++ ){
                        if( me.regexRuleVariable.test( ruleMatches[i] ) ){
                            variableMatches = me.regexRuleVariable.exec( ruleMatches[i] );
                            for( j = 1; j < variableMatches.length; j++ ){
                                ruleId = me.getRuleId( record.get( "rule" ) );
                                variableRecord = this.storeActiveVariables.findRecord("id", variableMatches[j]);
                                if( variableRecord == null ){
                                    this.storeActiveVariables.add( {
                                        id: variableMatches[j],
                                        ruleIds: [ruleId]
                                    });
                                }else{
                                    variableRecord.set(variableRecord.get("ruleIds").push(ruleId));
                                }
                            }
                        }
                    }
                }
            },
            this
        );
    },

    // Update status for rules to show total # of rules and total enabled.
    updateRulesStatus: function(){
        var totalEnabled = 0;
        this.store.each(
            function( record ){
                if( ( record.get('log') === true ) || ( record.get('block') === true ) ){
                    totalEnabled++;
                }
            }
        );
        this.searchStatusBar.setStatus({
            text:  this.store.count() + ' ' + i18n._('available rules') + ', ' + totalEnabled + ' ' +i18n._("logging or blocking"),
            iconCls: 'x-status-valid'
        });

    },

    // DEL ASCII code
    searchTagsProtect: '\x0f',

    // detects regexp reserved word
    searchRegExpProtect: /\\|\/|\+|\\|\.|\[|\]|\{|\}|\?|\$|\*|\^|\|/gm,
    // In normal mode it returns the value with protected regexp characters.
    // In regular expression mode it returns the raw value except if the regexp is invalid.
    // @return {String} The value to process or null if the textfield value is blank or invalid.
    getSearchValue: function() {
        var me = this,
            value = me.searchTextField.getValue();

        if (value === '') {
            return null;
        }
        value = value.replace(me.searchRegExpProtect, function(m) {
            return '\\' + m;
        });

        var length = value.length,
            resultArray = [me.searchTagsProtect + '*'],
            i = 0,
            c;

        for(; i < length; i++) {
            c = value.charAt(i);
            resultArray.push(c);
            if (c !== '\\') {
                resultArray.push(me.searchTagsProtect + '*');
            }
        }
        return resultArray.join('');
    },

    // Finds all strings that matches the searched value in each grid cells as well as Log or Block action.
    searchFilter: function() {
        var me = this;

        var findLog = me.down('checkbox[name=searchLog]').getValue();
        var findBlock = me.down('checkbox[name=searchBlock]').getValue();

        me.store.clearFilter(false);
        me.searchValue = me.getSearchValue();

        // It's too expensive to search with less than minimum characters.
        if( ( me.searchValue !== null ) &&
            ( me.searchTextField.getValue().length < ( me.searchMinimumCharacters + 1 ) ) ){
            me.searchStatusBar.setStatus({
                text: i18n._("(type more than 2 characters)"),
                iconCls: 'x-status-valid'
            });
        }

        if( ( ( me.searchValue !== null ) &&
              ( me.searchTextField.getValue().length > me.searchMinimumCharacters ) ) ||
            ( findLog === true ) ||
            ( findBlock === true ) ){

            me.searchRegExp = new RegExp(me.searchValue, 'g' + 'i');

            // Build store filter based on Log, Block, and/or search field values.
            me.store.filterBy( function( record, id ) {
                me = this;
                var logMatch = true;
                if( findLog === true ){
                    logMatch = ( record.get( "log" ) === findLog );
                }
                var blockMatch = true;
                if( findBlock === true ){
                    blockMatch = ( record.get( "block" ) === findBlock );
                }
                var searchMatch = true;
                if( ( ( me.searchValue !== null ) &&
                    ( me.searchTextField.getValue().length > me.searchMinimumCharacters ) ) ){
                    searchMatch = false;
                    for( var i = 0 ; i < me.searchFields.length; i++){
                        searchMatch = me.searchRegExp.test( record.get( me.searchFields[i] ) );
                        if( searchMatch == true ){
                            break;
                        }
                    }
                }
                return logMatch && blockMatch && searchMatch;
            }, me );

            // If Log/Block is checked then it's evident that the matching rules are
            // set to either Log/Block.  But if neither are checked, we want to know  how many are enabled.
            var statusText = me.store.count() + ' ' + i18n._(' matching rules(s) found');
            if( findLog === false && findBlock === false ){
                var totalEnabled = 0;
                me.store.each(
                    function( record ){
                        if( ( record.get('log') === true ) || ( record.get('block') === true ) ){
                            totalEnabled++;
                        }
                    }
                );
                statusText += ', ' + totalEnabled + ' ' + i18n._("logging or blocking");
            }
            me.searchStatusBar.setStatus({
                text: me.store.count() ? statusText : i18n._('No matching rules found') ,
                iconCls: 'x-status-valid'
            });
         }else{
            me.updateRulesStatus();
         }

         // force textfield focus
         me.searchTextField.focus();
    },

    getList: function(useId, useInternalId) {
        this.store.clearFilter(true);
        return this.callSuper( useId, useInternalId );
    },

    // Create rule id (sid_gid) from raw rule
    regexRuleSid: /\s+sid:\s*([^;]+);/,
    regexRuleGid: /\s+gid:\s*([^;]+);/,
    getRuleId: function( rule ){
        var gid = "1";
        var sid = "1";
        if( this.regexRuleGid.test( rule ) === true ){
            gid = this.regexRuleGid.exec( rule )[1];
        }
        if( this.regexRuleSid.test( rule ) === true ){
            sid = this.regexRuleSid.exec( rule )[1];
        }
        return sid + "_" + gid;
    },

    // Modify to include original rule identiifer
    getData: function( data ){
        this.data = this.callSuper( data );

        for( var i = 0; i < this.data.length; i++ ){
            this.data[i].originalId = this.getRuleId( this.data[i].rule );
            this.data[i].path = this.data[i].path;
        }
        return this.data;
    }
});
*/
Ext.define('Webui.untangle-node-idps.settings', {
    extend:'Ung.NodeWin',
    statics: {
        preloadSettings: function(node){
            Ext.Ajax.request({
                url: "/webui/download",
                method: 'POST',
                params: {
                    type: "IdpsSettings",
                    arg1: "load",
                    arg2: node.nodeId
                },
                scope: node,
                timeout: 600000,
                success: function(response){
                    this.openSettings.call(this, Ext.decode( response.responseText ) );
                },
                failure: function(response){
                    this.openSettings.call(this, null );
                }
            });
        }
    },
    panelStatus: null,
    gridRules: null,
    gridVariables: null,
    gridEventLog: null,
    statistics: null,
    initComponent: function() {
        this.classtypesStore = Ext.create('Ext.data.ArrayStore', {
            fields: [ 'name', 'description', 'priority' ],
            data: [
                [ "attempted-admin", this.i18n._("Attempted Administrator Privilege Gain"), "high"],
                [ "attempted-user", this.i18n._("Attempted User Privilege Gain"), "high" ],
                [ "inappropriate-content", this.i18n._("Inappropriate Content was Detected"), "high" ],
                [ "policy-violation", this.i18n._("Potential Corporate Privacy Violation"), "high" ],
                [ "shellcode-detect", this.i18n._("Executable code was detected"), "high" ],
                [ "successful-admin", this.i18n._("Successful Administrator Privilege Gain"), "high" ],
                [ "successful-user", this.i18n._("Successful User Privilege Gain"), "high" ],
                [ "trojan-activity", this.i18n._("A Network Trojan was detected"), "high" ],
                [ "unsuccessful-user", this.i18n._("Unsuccessful User Privilege Gain"), "high" ],
                [ "web-application-attack", this.i18n._("Web Application Attack"), "high" ],

                [ "attempted-dos", this.i18n._("Attempted Denial of Service"), "medium" ],
                [ "attempted-recon", this.i18n._("Attempted Information Leak"), "medium" ],
                [ "bad-unknown", this.i18n._("Potentially Bad Traffic"), "medium" ],
                [ "default-login-attempt", this.i18n._("Attempt to login by a default username and password"), "medium" ],
                [ "denial-of-service", this.i18n._("Detection of a Denial of Service Attack"), "medium" ],
                [ "misc-attack", this.i18n._("Misc Attack"), "medium" ],
                [ "non-standard-protocol", this.i18n._("Detection of a non-standard protocol or event"), "medium" ],
                [ "rpc-portmap-decode", this.i18n._("Decode of an RPC Query"), "medium" ],
                [ "successful-dos", this.i18n._("Denial of Service"), "medium" ],
                [ "successful-recon-largescale", this.i18n._("Large Scale Information Leak"), "medium" ],
                [ "successful-recon-limited", this.i18n._("Information Leak"), "medium" ],
                [ "suspicious-filename-detect", this.i18n._("A suspicious filename was detected"), "medium" ],
                [ "suspicious-login", this.i18n._("An attempted login using a suspicious username was detected"), "medium" ],
                [ "system-call-detect", this.i18n._("A system call was detected"), "medium" ],
                [ "unusual-client-port-connection", this.i18n._("A client was using an unusual port"), "medium" ],
                [ "web-application-activity", this.i18n._("Access to a potentially vulnerable web application"), "medium" ],

                [ "icmp-event", this.i18n._("Generic ICMP event"), "low" ],
                [ "misc-activity", this.i18n._("Misc activity"), "low" ],
                [ "network-scan", this.i18n._("Detection of a Network Scan"), "low" ],
                [ "not-suspicious", this.i18n._("Not Suspicious Traffic"), "low" ],
                [ "protocol-command-decode", this.i18n._("Generic Protocol Command Decode"), "low" ],
                [ "string-detect", this.i18n._("A suspicious string was detected"), "low" ],
                [ "unknown", this.i18n._("Unknown Traffic"), "low" ],

                [ "tcp-connection", this.i18n._("A TCP connection was detected"), "low" ]
            ]
        });

        this.categoriesStore = Ext.create('Ext.data.ArrayStore', {
            fields: [ 'name', 'description' ],
            data: [
            [ "app-detect", this.i18n._("This category contains rules that look for, and control, the traffic of certain applications that generate network activity. This category will be used to control various aspects of how an application behaves.") ],
            [ "blacklist", this.i18n._("This category contains URI, USER-AGENT, DNS, and IP address rules that have been determined to be indicators of malicious activity. These rules are based on activity from the Talos virus sandboxes, public list of malicious URLs, and other data sources.") ],
            [ "browser-chrome", this.i18n._("This category contains detection for vulnerabilities present in the Chrome browser. (This is separate from the 'browser-webkit' category, as Chrome has enough vulnerabilities to be broken out into it's own, and while it uses the Webkit rendering engine, there's a lot of other features to Chrome.)") ],
            [ "browser-firefox", this.i18n._("This category contains detection for vulnerabilities present in the Firefox browser, or products that have the 'Gecko' engine. (Thunderbird email client, etc)") ],
            [ "browser-ie", this.i18n._("This category contains detection for vulnerabilities present in the Internet Explorer browser (Trident or Tasman engines)") ],
            [ "browser-webkit", this.i18n._("This category contains detection of vulnerabilities present in the Webkit browser engine (aside from Chrome) this includes Apple's Safari, RIM's mobile browser, Nokia, KDE, Webkit itself, and Palm.") ],
            [ "browser-other", this.i18n._("This category contains detection for vulnerabilities in other browsers not listed above.") ],
            [ "browser-plugins", this.i18n._("This category contains detection for vulnerabilities in browsers that deal with plugins to the browser. (Example: Active-x)") ],
            [ "content-replace", this.i18n._("This category containt any rule that utilizes the 'replace' functionality inside of Snort.") ],
            [ "deleted", this.i18n._("When a rule has been deprecated or replaced it is moved to this categories. Rules are never totally removed from the ruleset, they are moved here.") ],
            [ "exploit", this.i18n._("This is an older category which will be deprecated soon. This category looks for exploits against software in a generic form.") ],
            [ "exploit-kit", this.i18n._("This category contains rules that are specifically tailored to detect exploit kit activity. This does not include 'post-compromise' rules (as those would be in indicator-compromise). Files that are dropped as result of visiting an exploit kit would be in their respective file category.") ],
            [ "file-executable", this.i18n._("This category contains rules for vulnerabilities that are found or are delivered through executable files, regardless of platform.") ],
            [ "file-flash", this.i18n._("This category contains rules for vulnerabilities that are found or are delivered through flash files. Either compressed or uncompressed, regardless of delivery method platform being attacked.") ],
            [ "file-image", this.i18n._("This category contains rules for vulnerabilities that are found inside of images files. Regardless of delivery method, software being attacked, or type of image. (Examples include: jpg, png, gif, bmp, etc)") ],
            [ "file-identify", this.i18n._("This category is to identify files through file extension, the content in the file (file magic), or header found in the traffic. This information is usually used to then set a flowbit to be used in a different rule.") ],
            [ "file-multimedia", this.i18n._("This category contains rules for vulnerabilities present inside of multimedia files (mp3, movies, wmv)") ],
            [ "file-office", this.i18n._("This category contains rules for vulnerabilities present inside of files belonging to the Microsoft Office suite of software. (Excel, PowerPoint, Word, Visio, Access, Outlook, etc)") ],
            [ "file-pdf", this.i18n._("This category contains rules for vulnerabilities found inside of PDF files. Regardless of method of creation, delivery method, or which piece of software the PDF affects (for example, both Adobe Reader and FoxIt Reader)") ],
            [ "file-other", this.i18n._("This category contains rules for vulnerabilities present inside a file, that doesn't fit into the other categories above.") ],
            [ "indicator-compromise", this.i18n._("This category contains rules that are clearly to be used only for the detection of a positively compromised system, false positives may occur.") ],
            [ "indicator-obfuscation", this.i18n._("This category contains rules that are clearly used only for the detection of obfuscated content. Like encoded JavaScript rules.") ],
            [ "indicator-shellcode", this.i18n._("This category contains rules that are simply looking for simple identification markers of shellcode in traffic. This replaces the old 'shellcode.rules'.") ],
            [ "malware-backdoor", this.i18n._("This category contains rules for the detection of traffic destined to known listening backdoor command channels. If a piece of malicious soft are opens a port and waits for incoming commands for its control functions, this type of detection will be here. A simple example would be the detection for BackOrifice as it listens on a specific port and then executes the commands sent.") ],
            [ "malware-cnc", this.i18n._("This category contains known malicious command and control activity for identified botnet traffic. This includes call home, downloading of dropped files, and ex-filtration of data. Actual commands issued from 'Master to Zombie' type stuff will also be here.") ],
            [ "malware-tools", this.i18n._("This category contains rules that deal with tools that can be considered malicious in nature. For example, LOIC.") ],
            [ "malware-other", this.i18n._("This category contains rules that are malware related, but don't fit into one of the other 'malware' categories.") ],
            [ "os-linux", this.i18n._("This category contains rules that are looking for vulnerabilities in Linux based OSes. Not for browsers or any other software on it, but simply against the OS itself.") ],
            [ "os-solaris", this.i18n._("This category contains rules that are looking for vulnerabilities in Solaris based OSes. Not for any browsers or any other software on top of the OS.") ],
            [ "os-windows", this.i18n._("This category contains rules that are looking for vulnerabilities in Windows based OSes. Not for any browsers or any other software on top of the OS.") ],
            [ "os-other", this.i18n._("This category contains rules that are looking for vulnerabilities in an OS that is not listed above.") ],
            [ "policy-multimedia", this.i18n._("This category contains rules that detect potential violations of policy for multimedia. Examples like the detection of the use of iTunes on the network. This is not for vulnerabilities found within multimedia files, as that would be in file-multimedia.") ],
            [ "policy-social", this.i18n._("This category contains rules for the detection potential violations of policy on corporate networks for the use of social media. (p2p, chat, etc)") ],
            [ "policy-other", this.i18n._("This category is for rules that may violate the end-users corporate policy bud do not fall into any of the other policy categories first.") ],
            [ "policy-spam", this.i18n._("This category is for rules that may indicate the presence of spam on the network.") ],
            [ "protocol-finger", this.i18n._("This category is for rules that may indicate the presence of the finger protocol or vulnerabilities in the finger protocol on the network.") ],
            [ "protocol-ftp", this.i18n._("This category is for rules that may indicate the presence of the ftp protocol or vulnerabilities in the ftp protocol on the network.") ],
            [ "protocol-icmp", this.i18n._("This category is for rules that may indicate the presence of icmp traffic or vulnerabilities in icmp on the network.") ],
            [ "protocol-imap", this.i18n._("This category is for rules that may indicate the presence of the imap protocol or vulnerabilities in the imap protocol on the network.") ],
            [ "protocol-pop", this.i18n._("This category is for rules that may indicate the presence of the pop protocol or vulnerabilities in the pop protocol on the network.") ],
            [ "protocol-services", this.i18n._("This category is for rules that may indicate the presence of the rservices protocol or vulnerabilities in the rservices protocols on the network.") ],
            [ "protocol-voip", this.i18n._("This category is for rules that may indicate the presence of voip services or vulnerabilities in the voip protocol on the network.") ],
            [ "pua-adware", this.i18n._("This category deals with 'pua' or Potentially Unwanted Applications that deal with adware or spyware.") ],
            [ "pua-p2p", this.i18n._("This category deals with 'pua' or Potentially Unwanted Applications that deal with p2p.") ],
            [ "pua-toolbars", this.i18n._("This category deals with 'pua' or Potentially Unwanted Applications that deal with toolbars installed on the client system. (Google Toolbar, Yahoo Toolbar, Hotbar, etc)") ],
            [ "pua-other", this.i18n._("This category deals with 'pua' or Potentially Unwanted Applications that don't fit into one of the categories shown above.") ],
            [ "server-apache", this.i18n._("This category deals with vulnerabilities in or attacks against the Apache Web Server.") ],
            [ "server-iis", this.i18n._("This category deals with vulnerabilities in or attacks against the Microsoft IIS Web server.") ],
            [ "server-mssql", this.i18n._("This category deals with vulnerabilities in or attacks against the Microsoft SQL Server.") ],
            [ "server-mysql", this.i18n._("This category deals with vulnerabilities in or attacks against Oracle's MySQL server.") ],
            [ "server-oracle", this.i18n._("This category deals with vulnerabilities in or attacks against Oracle's Oracle DB Server.") ],
            [ "server-webapp", this.i18n._("This category deals with vulnerabilities in or attacks against Web based applications on servers.") ],
            [ "server-mail", this.i18n._("This category contains rules that detect vulnerabilities in mail servers. (Exchange, Courier). These are separate from the protocol categories, as those deal with the traffic going to the mail servers itself.") ],
            [ "server-other", this.i18n._("This category contains rules that detect vulnerabilities in or attacks against servers that are not detailed in the above list.") ]
            ]
        });

        this.lastUpdate = this.getRpcNode().getLastUpdate();
        this.lastUpdateCheck = this.getRpcNode().getLastUpdateCheck();

        this.buildStatus();
        this.buildRules();
        this.buildVariables();
        this.buildEventLog();
        this.buildTabPanel([this.panelStatus, this.gridRules, this.gridVariables, this.gridEventLog]);
        this.callParent(arguments);

        this.forceReload = false;

        if( this.settings.configured === false && !this.wizardWindow ){
            Ext.defer(function(){
                this.setupWizard();
            }, 100, this);
            this.disable();
        }
    },
    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ext.panel.Panel',{
            name: 'Status',
            // helpSource: 'intrusion_detection_prevention_status', //FIXME disabled for now so it doesnt break test - uncomment me when docs exist
            parentId: this.getId(),
            title: this.i18n._('Status'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset',
                buttonAlign: 'left'
            },
            items: [{
            //     title: this.i18n._('Statistics'),
            //     defaults: {
            //         xtype: "displayfield",
            //         labelWidth:200
            //     },
            //     items: [{
            //         fieldLabel: this.i18n._('Total Signatures Available'),
            //         name: 'Total Signatures Available',
            //         value: this.statistics.totalAvailable
            //     }, {
            //         fieldLabel: this.i18n._('Total Signatures Logging'),
            //         name: 'Total Signatures Logging',
            //         value: this.statistics.totalLogging
            //     }, {
            //         fieldLabel: this.i18n._('Total Signatures Blocking'),
            //         name: 'Total Signatures Blocking',
            //         value: this.statistics.totalBlocking
            //     }]
            // }, {
                title: this.i18n._("Setup Wizard"),
                items: [
                    {
                        xtype: "button",
                        name: 'setup_wizard_button',
                        text: this.i18n._("Run Intrusion Detection/Prevention Setup Wizard"),
                        iconCls: "action-icon",
                        handler: Ext.bind(function() {
                            this.setupWizard();
                        }, this)
                    }]
            }, {
                title: this.i18n._('Note'),
                cls: 'description',
                html: Ext.String.format(this.i18n._("{0} continues to maintain the default signature settings through automatic updates. You are free to modify and add signatures, however it is not required."), rpc.companyName)
            },{
                title: this.i18n._("Updates"),
                cls: 'description',
                items: [{
                    xtype: 'displayfield',
                    fieldLabel: this.i18n._("Last check for updates"),
                    name: 'lastUpdateCheck',
                    labelWidth:200,
                    value: ( this.lastUpdateCheck !== null && this.lastUpdateCheck.time !== 0 ) ? i18n.timestampFormat(this.lastUpdateCheck) : i18n._("Never")
                },{
                    xtype: 'displayfield',
                    fieldLabel: this.i18n._("Last update"),
                    name: 'lastUpdateCheck',
                    labelWidth:200,
                    value: ( this.lastUpdate !== null && this.lastUpdate.time !== 0 && this.lastUpdateCheck !== null && this.lastUpdateCheck.time !== 0 ) ? i18n.timestampFormat(this.lastUpdate) : i18n._("Never")
                }]
            }]
        });
    },

    // Rules Panel
    buildRules: function() {
        this.gridRules = Ext.create('Ung.grid.Panel', {
            // helpSource: 'intrusion_detection_prevention_rules', //FIXME disabled for now so it doesnt break test - uncomment me when docs exist
            name: 'Rules',
            groupField: 'classtype',
            settingsCmp: this,
            title: this.i18n._("Rules"),
            dataProperty: 'rules',
            formatData: function(data) {
                return this.callSuper(data);
            },
            features: [{
                ftype: 'grouping',
                groupHeaderTpl: '{columnName}: {name} ({rows.length} rule{[values.rows.length > 1 ? "s" : ""]})',
                startCollapsed: true
            }],
            recordJavaClass: "com.untangle.node.idps.IpsRule",
            emptyRow: {
                "classtype": "unknown",
                "category": "app-detect",
                "msg" : "new rule",
                "sid": "1999999",
                "log": true,
                "block": false,
                "rule": "alert tcp any any -> any any ( msg:\"new rule\"; classtype:unknown; sid:1999999; content:\"matchme\"; nocase;)"
            },
            sortField: 'category',
            fields: [{
                name: 'sid',
                sortType: 'asInt'
            },{
                name: 'category'
            },{
                name: 'classtype'
            },{
                name: 'msg'
            },{
                name: 'rule'
            },{
                name: 'log'
            },{
                name: 'block'
            }],
            columns: [{
                header: this.i18n._("Sid"),
                dataIndex: 'sid',
                sortable: true,
                width: 70,
                editor: null,
                menuDisabled: false/*,
                renderer: function( value, metaData, record, rowIdx, colIdx, store ){
                    var id = record.get("originalId").split("_");
                    metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( i18n._("Sid:") + id[0] + ", " + i18n._("Gid:") +id[1]) + '"';
                    return value;
                }*/
            },{
                header: this.i18n._("Classtype"),
                dataIndex: 'classtype',
                sortable: true,
                width: 100,
                flex:1,
                editor: null,
                menuDisabled: false/*,
                renderer: function( value, metaData, record, rowIdx, colIdx, store ){
                    var dr = this.settingsCmp.classtypesStore.findRecord( "name", value );
                    metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode(dr != null ? dr.get("description") : value ) + '"';
                    return value;
                }*/
            },{
                header: this.i18n._("Category"),
                dataIndex: 'category',
                sortable: true,
                width: 100,
                flex:1,
                menuDisabled: false/*,
                renderer: function( value, metaData, record, rowIdx, colIdx, store ){
                    var dr = this.settingsCmp.categoriesStore.findRecord( "name", value );
                    metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode(dr != null ? dr.get("description") : value ) + '"';
                    return value;
                }*/
            },{
                header: this.i18n._("Msg"),
                dataIndex: 'msg',
                sortable: true,
                width: 200,
                flex:3,
                editor: null,
                menuDisabled: false
            },{
                header: this.i18n._("Reference"),
                dataIndex: 'rule',
                sortable: true,
                width: 100,
                flex:1,
                menuDisabled: false/*,
                renderer: function( value, metaData, record, rowIdx, colIdx, store ){
                    var matches = value.match(this.regexRuleReference);
                    if( matches == null ){
                        return "";
                    }
                    var references = [];
                    for( var i = 0; i < matches.length; i++ ){
                        // Apparently need to rebuild regexp each time to correctly extract in a loop.
                        var re = /\s+reference:\s*([^\;]+)\;/g;
                        var rmatches = re.exec( matches[i] );

                        var url = "";
                        var referenceFields = rmatches[1].split(",");
                        var drr = this.referencesStore.findRecord( "system", referenceFields[0] );
                        if( drr != null ){
                            url = drr.get("prefix") + referenceFields[1];
                            references.push('<a href="'+ url + '" class="icon-detail-row" style="text-decoration:none !important;" target="_reference">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</a>');
                        }

                    }
                    return references.join("&nbsp;");
                }*/
            },{
                xtype:'checkcolumn',
                header: this.i18n._("Log"),
                dataIndex: 'log',
                sortable: true,
                resizable: false,
                width:55,
                menuDisabled: false,
                listeners: {
                    checkchange: Ext.bind(function ( column, recordIndex, checked ){
                        var record = this.gridRules.getStore().getAt(recordIndex);
                        if( checked === false ){
                            record.set('block', false );
                        }
                        this.gridRules.updateRule(record, null );
                    }, this)
                }
            },{
                xtype:'checkcolumn',
                header: this.i18n._("Block"),
                dataIndex: 'block',
                sortable: true,
                resizable: false,
                width:55,
                menuDisabled: false,
                listeners: {
                    checkchange: Ext.bind(function ( column, recordIndex, checked ){
                        var record = this.gridRules.getStore().getAt(recordIndex);
                        if( checked === true ){
                            record.set('log', true );
                        }
                        this.gridRules.updateRule(record, null );
                    }, this)
                }
            }],
            rowEditorInputLines: [{
                name: "Classtype",
                dataIndex: "classtype",
                fieldLabel: this.i18n._("Classtype"),
                emptyText: this.i18n._("[enter class]"),
                allowBlank: false,
                width: 400,
                xtype: 'combo',
                queryMode: 'local',
                valueField: 'id',
                displayField: 'value',
                regexMatch: /\s+classtype:([^;]+);/,
                validator: function( value ){
                    if( this.store.find( "value", value ) == -1 ){
                        return this.i18n._("Invalid Classtype");
                    }
                    return true;
                },
                listeners: {
                    select: function( combo, record, eOpts ){
                        var editorWindow = this.up("[$className=Ung.RowEditorWindow]");
                        var rule = this.up("[$className=Ung.RowEditorWindow]").down("[name=Rule]");

                        var newField = " classtype:" + combo.getValue() + ";";
                        var ruleValue = rule.getValue();
                        if( this.regexMatch.test( ruleValue ) === true ){
                            ruleValue = ruleValue.replace( this.regexMatch, newField );
                        }else{
                            ruleValue += newField;
                        }
                        rule.setRawValue(ruleValue);
                    }
                }
            },{
                name: "Category",
                fieldLabel: this.i18n._("Category"),
                dataIndex: "category",
                emptyText: this.i18n._("[enter category]"),
                allowBlank: false,
                width: 400,
                xtype: 'combo',
                queryMode: 'local',
                valueField: 'id',
                displayField: 'value'
            },{
                xtype:'textfield',
                name: "Msg",
                dataIndex: "msg",
                fieldLabel: this.i18n._("Msg"),
                emptyText: this.i18n._("[enter name]"),
                allowBlank: false,
                width: 400,
                regexMatch: /\s+msg:"([^;]+)";/,
                validator: function( value ){
                    invalidChars = new RegExp(/[";]/);
                    if( invalidChars.test(value) ){
                        return i18n._("Msg contains invalid characters.");
                    }
                    return true;
                },
                listeners: {
                    change: function( me, newValue, oldValue, eOpts ){
                        var editorWindow = this.up("[$className=Ung.RowEditorWindow]");
                        var rule = this.up("[$className=Ung.RowEditorWindow]").down("[name=Rule]");

                        var newField = " msg:\"" + newValue + "\";";
                        var ruleValue = rule.getValue();
                        if( this.regexMatch.test( ruleValue ) === true ){
                            ruleValue = ruleValue.replace( this.regexMatch, newField );
                        }else{
                            ruleValue += newField;
                        }
                        rule.setRawValue(ruleValue);
                    }
                }
            },{
                xtype:'textfield',
                name: "Sid",
                dataIndex: "sid",
                fieldLabel: this.i18n._("Sid"),
                emptyText: this.i18n._("[enter sid]"),
                allowBlank: false,
                width: 400,
                regexMatch: /\s+sid:([^;]+);/,
                gidRegex: /\s+gid:\s*([^;]+);/,
                validator: function( ourValue ){
                    validChars = new RegExp(/[0-9]+/);
                    if( validChars.test( ourValue ) === false ){
                        return i18n._("Sid must be numeric");
                    }
                    var record = this.up("[$className=Ung.RowEditorWindow]").record;

                    var ourRule = this.up("[$className=Ung.RowEditorWindow]").down("[name=Rule]");
                    var ourGid = "1";
                    if( this.gidRegex.test( ourRule.getValue() ) === true ){
                        ourGid = this.gidRegex.exec( ourRule.getValue() )[1];
                    }

                    var match = false;
                    this.up("[$className=Ung.RowEditorWindow]").grid.store.each(
                        function( storeRecord ){
                            var ruleGid;
                            if( ( storeRecord != record ) &&
                                (  storeRecord.get("sid") == ourValue) ){

                                ruleGid = "1";
                                if( this.gidRegex.test( storeRecord.get("rule") ) === true ){
                                    ruleGid = this.gidRegex.exec( storeRecord.get("rule") );
                                }

                                if( ourGid == ruleGid ){
                                    match = true;
                                }
                            }
                        },
                        this
                    );
                    if( match === true ){
                        return i18n._("Sid already in use.");
                    }
                    return true;
                },
                listeners: {
                    change: function( me, newValue, oldValue, eOpts ){
                        var editorWindow = this.up("[$className=Ung.RowEditorWindow]");
                        var rule = this.up("[$className=Ung.RowEditorWindow]").down("[name=Rule]");

                        var newField = " sid:" + newValue + ";";
                        var ruleValue = rule.getValue();
                        if( this.regexMatch.test( ruleValue ) === true ){
                            ruleValue = ruleValue.replace( this.regexMatch, newField );
                        }else{
                            ruleValue += newField;
                        }
                        rule.setRawValue(ruleValue);
                    }
                }
            },{
                xtype:'checkbox',
                name: "Log",
                dataIndex: "log",
                fieldLabel: this.i18n._("Log"),
                listeners: {
                    change: function( me, newValue, oldValue, eOpts ){
                        if( newValue === false ){
                            this.up("[$className=Ung.RowEditorWindow]").down("[dataIndex=block]").setValue(false);
                        }
                        this.up("[$className=Ung.RowEditorWindow]").grid.updateRule(this.up("[$className=Ung.RowEditorWindow]").record, this.up("[$className=Ung.RowEditorWindow]") );
                    }
                }
            },{
                xtype:'checkbox',
                name: "Block",
                dataIndex: "block",
                fieldLabel: this.i18n._("Block"),
                listeners: {
                    change: function( me, newValue, oldValue, eOpts ){
                        if( newValue === true ){
                            this.up("[$className=Ung.RowEditorWindow]").down("[dataIndex=log]").setValue(true);
                        }  
                        this.up("[$className=Ung.RowEditorWindow]").grid.updateRule(this.up("[$className=Ung.RowEditorWindow]").record, this.up("[$className=Ung.RowEditorWindow]") );
                    }
                }
            },{
                xtype:'textareafield',
                name: "Rule",
                dataIndex: "rule",
                fieldLabel: this.i18n._("Rule"),
                emptyText: this.i18n._("[enter rule]"),
                allowBlank: false,
                width: 500,
                height: 150,
                actionRegexMatch: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)/,
                regexMatch: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)\s+(tcp|udp|icmp|ip)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+\((.+)\)$/,
                validator: function( value ){
                    if( this.regexMatch.test(value) === false ){
                        return i18n._("Rule formatted wrong.");
                    }
                    return true;
                },
                listeners: {
                    change: function( me, newValue, oldValue, eOpts ){
                        var editorWindow = this.up("[$className=Ung.RowEditorWindow]");
                        var value = this.getValue();

                        value = value.replace( /(\r\n|\n|\r)/gm, "" );

                        var updateFields = [ "Classtype", "Msg", "Sid" ]; 
                        var match;
                        for( var i = 0; i < updateFields.length; i++ ){
                            var field = this.up("[$className=Ung.RowEditorWindow]").down("[name=" + updateFields[i] + "]");
                            if( field.regexMatch.test( value ) ){
                                match = field.regexMatch.exec( value );
                                field.setRawValue( match[1] );
                                field.validate();
                            }
                        }

                        // Action
                        var log = this.up("[$className=Ung.RowEditorWindow]").down("[name=Log]");
                        var block = this.up("[$className=Ung.RowEditorWindow]").down("[name=Block]");

                        var logValue = false;
                        var blockValue = false;
                        if( this.actionRegexMatch.test( value ) === true ){
                            match = this.actionRegexMatch.exec( value );
                            if( match[2] == "alert" ){
                                logValue = true;
                            }else if( ( match[2] == "drop" ) || ( match[2] == "sdrop" ) ){
                                logValue = true;
                                blockValue = true;
                            }
                            if( match[1] == "#" ){
                                logValue = false;
                                blockValue = false;
                            }
                        }
                        log.setRawValue(logValue);
                        block.setRawValue(blockValue);

                        this.setRawValue( value );
                    }
                }
            }],
            actionRegexMatch: /^([#]+|)(alert|log|pass|activate|dynamic|drop|sdrop|reject)/,
            updateRule: function( record, source ){
                // Rebuild rule according to record or form values
                var logValue = false;
                var blockValue = false;
                var ruleValue = "";
                if( source === null ) {
                    // Pull values from record
                    logValue = record.get("log");
                    blockValue = record.get("block");
                    ruleValue = record.get("rule");
                } else {
                    // Pull values from form
                    logValue = source.down("[dataIndex=log]").getValue();
                    blockValue = source.down("[dataIndex=block]").getValue();
                    ruleValue = source.down("[dataIndex=rule]").getValue();
                }

                var newField = "alert";
                if( logValue === true && blockValue === true ) {
                    newField = "drop";
                } else if( logValue === false && blockValue === true ) {
                    newField = "sdrop";
                } else if( logValue === false && blockValue === false ) {
                    newField = "#" + newField;
                }

                if( this.actionRegexMatch.test( ruleValue ) === true ) {
                    ruleValue = ruleValue.replace( this.actionRegexMatch, newField );
                } else {
                    ruleValue = ruleValue + newField;
                }

                if( source === null ) {
                    record.data.rule = ruleValue;
                } else {
                    source.down("[dataIndex=rule]").setRawValue(ruleValue);    
                }
            }
        });
    },

    // Variables Panel
    buildVariables: function() {
        this.gridVariables = Ext.create('Ung.grid.Panel', {
            //helpSource: 'intrusion_detection_prevention_variables', //FIXME disabled for now so it doesnt break test - uncomment me when docs exist            
            title: this.i18n._("Variables"),
            name: 'Variables',
            settingsCmp: this,
            dataProperty: 'variables',
            recordJavaClass: "com.untangle.node.idps.IpsVariable",
            emptyRow: {
                "variable": "",
                "definition": "",
                "description": ""
            },
            sortField: 'variable',
            fields: [{
                name: 'id'
            },{
                name: 'variable',
                type: 'string'
            },{
                name: 'definition',
                type: 'string'
            },{
                name: 'description',
                type: 'string'
            }, {
                name: 'originalId',
                mapping: 'variable'
            }],
            columns: [{
                header: this.i18n._("Name"),
                width: 170,
                dataIndex: 'variable',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter name]"),
                    allowBlank: false
                }
            },{
                id: 'definition',
                header: this.i18n._("Definition"),
                width: 300,
                dataIndex: 'definition',
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter definition]"),
                    allowBlank: false
                }
            },{
                header: this.i18n._("Description"),
                width: 300,
                dataIndex: 'description',
                flex:1,
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter description]"),
                    allowBlank: false
                }
            }],
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "Name",
                dataIndex: "variable",
                fieldLabel: this.i18n._("Name"),
                emptyText: this.i18n._("[enter name]"),
                allowBlank: false,
                width: 300
            },{
                xtype:'textfield',
                name: "Pass",
                dataIndex: "definition",
                fieldLabel: this.i18n._("Pass"),
                emptyText: this.i18n._("[enter definition]"),
                allowBlank: false,
                width: 400
            },{
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: this.i18n._("Description"),
                emptyText: this.i18n._("[enter description]"),
                allowBlank: false,
                width: 400
            }],
            deleteHandler: function( record ){
                var ruleEditorGrid = this.up("panel").down("grid");
                if( ruleEditorGrid.storeActiveVariables.getCount() == 0 ){
                    Ext.MessageBox.wait(i18n._("Validating..."), i18n._("Please wait") );
                    var buildStore = Ext.create('Ext.util.DelayedTask', 
                        function(){
                            ruleEditorGrid.buildActiveVariableStore();
                            Ext.MessageBox.hide();
                            this.deleteCheck(record);
                        },
                        this
                        );
                    buildStore.delay(100);
                }else{
                    this.deleteCheck(record);
                }
            },
            deleteCheck: function( record ){
                var activeVariableRecord = this.up("panel").down("grid").storeActiveVariables.findRecord("id", record.get('variable'), 0, false, false, true );
                if( ( activeVariableRecord != null ) &&
                    ( activeVariableRecord.get("ruleIds").length > 0 ) ){
                    Ext.MessageBox.alert(
                        i18n._("Cannot Delete Variable"), 
                        i18n._("Variable is in use by one or more rules.")
                    );
                } else {
                    this.stopEditing();
                    this.updateChangedData(record, "deleted");
                }
            }
        });
    },
    // Event Log
    buildEventLog: function() {
        var visibleColumns = ['time_stamp','sig_id', 'source_addr', 'source_port', 'dest_addr', 'dest_port', 'protocol', 'blocked', 'category', 'classtype', 'msg' ];
        this.gridEventLog = Ext.create('Ung.GridEventLog',{
            settingsCmp: this,
            helpSource: 'intrusion_detection_prevention_event_log',
            eventQueriesFn: this.getRpcNode().getEventQueries,
            fields: [{
                name: 'time_stamp',
                sortType: Ung.SortTypes.asTimestamp
            }, {
                name: 'sig_id',
                sortType: 'asInt'
            }, {
                name: 'gen_id',
                sortType: 'asInt'
            }, {
                name: 'class_id',
                sortType: 'asInt'
            }, {
                name: 'source_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'source_port',
                sortType: 'asInt'
            }, {
                name: 'dest_addr',
                sortType: Ung.SortTypes.asIp
            }, {
                name: 'dest_port',
                sortType: 'asInt'
            }, {
                name: 'protocol',
                sortType: 'asInt'
            }, {
                name: 'blocked',
                type: 'boolean'
            }, {
                name: 'category',
                type: 'string'
            }, {
                name: 'classtype',
                type: 'string'
            }, {
                name: 'msg',
                type: 'string'
            }],
            columns: [{
                hidden: visibleColumns.indexOf('time_stamp') < 0,
                header: i18n._("Timestamp"),
                width: Ung.Util.timestampFieldWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                renderer: function(value) {
                    return i18n.timestampFormat(value);
                }
            }, {
                hidden: visibleColumns.indexOf('sig_id') < 0,
                header: i18n._("Sid"),
                width: 70,
                sortable: true,
                dataIndex: 'sig_id',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumns.indexOf('gen_id') < 0,
                header: i18n._("Gid"),
                width: 70,
                sortable: true,
                dataIndex: 'gen_id',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumns.indexOf('class_id') < 0,
                header: i18n._("Cid"),
                width: 70,
                sortable: true,
                dataIndex: 'class_id',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumns.indexOf('source_addr') < 0,
                header: i18n._("Source Address"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'source_addr'
            }, {
                hidden: visibleColumns.indexOf('source_port') < 0,
                header: i18n._("Source port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 'source_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumns.indexOf('dest_addr') < 0,
                header: i18n._("Destination Address"),
                width: Ung.Util.ipFieldWidth,
                sortable: true,
                dataIndex: 'dest_addr'
            }, {
                hidden: visibleColumns.indexOf('dest_port') < 0,
                header: i18n._("Destination port"),
                width: Ung.Util.portFieldWidth,
                sortable: true,
                dataIndex: 'dest_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumns.indexOf('protocol') < 0,
                header: i18n._("Protocol"),
                width: 70,
                sortable: true,
                dataIndex: 'protocol',
                filter: {
                    type: 'numeric'
                }
            }, {
                hidden: visibleColumns.indexOf('blocked') < 0,
                header: i18n._("Blocked"),
                width: Ung.Util.booleanFieldWidth,
                sortable: true,
                dataIndex: 'blocked',
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }, {
                hidden: visibleColumns.indexOf('category') < 0,
                header: i18n._("Category"),
                width: 200,
                sortable: true,
                dataIndex: 'category'
            }, {
                hidden: visibleColumns.indexOf('classtype') < 0,
                header: i18n._("Classtype"),
                width: 200,
                sortable: true,
                dataIndex: 'classtype'
            }, {
                hidden: visibleColumns.indexOf('msg') < 0,
                header: i18n._("Msg"),
                width: 200,
                sortable: true,
                dataIndex: 'msg'
            }]
        });
    },
    setupWizard: function() {
        var welcomeCard = Ext.create('Webui.untangle-node-idps.Wizard.Welcome', {
            i18n: this.i18n,
            node: this.getRpcNode(),
            gui: this
        });
        var classtypesCard = Ext.create('Webui.untangle-node-idps.Wizard.Classtypes', {
            i18n: this.i18n,
            node: this.getRpcNode(),
            gui: this
        });
        var categoriesCard = Ext.create('Webui.untangle-node-idps.Wizard.Categories', {
            i18n: this.i18n,
            node: this.getRpcNode(),
            gui: this
        });
        var congratulationsCard = Ext.create('Webui.untangle-node-idps.Wizard.Congratulations', {
            i18n: this.i18n,
            node: this.getRpcNode(),
            nodeWidget: this.node,
            gui: this
        });
        var setupWizard = Ext.create('Ung.Wizard',{
            modalFinish: true,
            hasCancel: true,
            cardDefaults: {
                labelWidth: 200,
                cls: 'untangle-form-panel'
            },
            cards: [welcomeCard, classtypesCard, categoriesCard, congratulationsCard],
            cancelAction: function() {
                this.up("window").close();
            }
        });
        this.wizardWindow = Ext.create('Ung.Window',{
            title: this.i18n._("Intrusion Prevention Setup Wizard"),
            items: setupWizard,
            closeWindow: Ext.bind(function() {
                this.wizardWindow.hide();
                Ext.destroy(this.wizardWindow);
                this.enable();
            }, this),
            listeners: {
                beforeclose: Ext.bind(function() {
                    var wizard = this.wizardWindow.down('panel[name="wizard"]');
                    if(!wizard.finished) {
                        wizard.finished=true;
                        Ext.MessageBox.alert(this.i18n._("Setup Wizard Warning"), this.i18n._("You have not finished configuring Intrusion Prevention. Please run the Setup Wizard again."), Ext.bind(function () {
                            this.wizardWindow.close();
                        }, this));
                        return false;
                    }
                    return true;
                }, this)
            }
        });

        this.wizardWindow.show();
        setupWizard.loadPage(0);
    },
    beforeSave: function(isApply, handler) {
        if( this.getRpcNode().getUpdatedSettingsFlag() === true ){
            Ext.MessageBox.alert(this.i18n._("Intrusion Prevention Warning"), this.i18n._("Settings have been changed by rule updater.  Current changes must be discarded."), Ext.bind(function () {
                this.reload();
            }, this));
            return;
        }

        if( this.wizard ) {
            this.wizard = false;
        } else {
            this.settings.rules.list = null;
            this.settings.variables.list = null;
            this.settings.rules.list = this.gridRules.getList();
            this.settings.variables.list = this.gridVariables.getList();
        }
        handler.call(this, isApply);
    },
    save: function(isApply) {
        // Due to the number of Snort rules, it takes too long to send everything back.
        // Instead, we send all settings except rules and variables.  Rules and variables
        // are sent as changedData sets from their grids.
        var changedDataSet = {};
        var keys = Object.keys(this.settings);
        for( var i = 0; i < keys.length; i++){
            if( ( keys[i] == "rules" ) || 
                ( keys[i] == "variables" ) ||
                ( keys[i] == "activeGroups" && !this.wizardSettings ) ){
                continue;
            }
            changedDataSet[keys[i]] = this.settings[keys[i]];
        }

        // This will always set rules/variables to minimally empty "diff"  objects if nothing  has changed
        Ext.Array.each( 
            this.query("*[changedData]"),
            function( c ){
                if( c.changedData ){ 
                    changedDataSet[c.dataProperty] = c.changedData;
                }
            }
        );

        Ext.Ajax.request({
            url: "/webui/download",
            jsonData: changedDataSet,
            method: 'POST',
            params: {
                type: "IdpsSettings",
                arg1: "save",
                arg2: this.tid
            },
            scope: this,
            timeout: 600000,
            success: function(response){
                var r = Ext.decode( response.responseText );
                if( r.success === false ) {
                    Ext.MessageBox.hide();
                    Ext.MessageBox.alert(i18n._("Error"), i18n._("Unable to save settings"));
                } else {
                    this.getRpcNode().reconfigure();
                    Ext.MessageBox.hide();

                    if (!isApply) {
                        this.closeWindow();
                        return;
                    } else {
                        this.clearDirty();
                        if( this.forceReload === true ) {
                            this.reload();
                        }
                    }
                }
            },
            failure: function(response){
                Ext.MessageBox.hide();
                Ext.MessageBox.alert(i18n._("Error"), i18n._("Unable to save settings"));
            }
        });
    }
});

// IDPS wizard configuration cards.
Ext.define('Webui.untangle-node-idps.Wizard.Welcome',{
    constructor: function( config ) {
        Ext.apply(this, config);

        var items = [{
            xtype: 'component',
            html: '<h2 class="wizard-title">'+this.i18n._("Welcome to the Intrusion Prevention Setup Wizard!")+'</h2>'
        },{
            xtype: 'component',
            html: this.i18n._("Intrusion Prevention operates using rules to identify possible threats.  An enabled ruled performs an action, either logging or blocking traffic.  Not all rules are necessary for a given network environment and enabling all of them may negatively impact your network."),
            margin: '0 0 10 10'
        },{
            xtype: 'component',
            html: this.i18n._("This wizard is designed to help you correctly configure the appropriate amount of rules for your network by selecting rule identifiers: classtypes and categories.  The more that you select, the more rules will be enabled.  Again, too many enabled rules may negatively impact your network."),
            margin: '0 0 10 10'
        },{
            xtype: 'component',
            html: this.i18n._("It is highly suggested that you use Recommended values."),
            margin: '0 0 10 10'
        }];

        if( this.gui.getSettings().configured === true ){
            items.push({
                xtype: 'component',
                html: this.i18n._('WARNING: Completing this setup wizard will overwrite the previous settings with new settings. All previous settings will be lost!'),
                cls: 'warning',
                margin: '0 0 10 10'
            });
        }

        this.title = this.i18n._("Welcome");
        this.panel = Ext.create('Ext.container.Container',{
            items: items
        });

        this.onNext = Ext.bind( this.loadDefaultSettings, this );
        this.initialLoad = true;
    },

    loadDefaultSettings: function(handler){
        if( this.initialLoad === true || this.gui.wizardSettings ) {
            this.initialLoad = false;
            handler();
        } else {
            Ext.MessageBox.wait(this.i18n._("Determining recommended settings..."), this.i18n._("Please wait"));
            Ext.Ajax.request({
                url: "/webui/download",
                method: 'POST',
                params: {
                    type: "IdpsSettings",
                    arg1: "wizard",
                    arg2: this.gui.node.nodeId
                },
                scope: this,
                timeout: 600000,
                success: function(response){
                    var wizardDefaults = Ext.decode( response.responseText );
                    // Determine profile to use based on system stats.
                    var stats = rpc.metricManager.getMetricsAndStats();
                    var profile;
                    var match;
                    for( var p = 0; p < wizardDefaults.profiles.length; p++ ){
                        match = false;
                        for( var statKey in wizardDefaults.profiles[p].systemStats ){
                            if( statKey == "MemTotal"){
                                var memoryTotal = stats.systemStats[statKey];
                                match = ( memoryTotal < ( wizardDefaults.profiles[p].systemStats[statKey] + ( wizardDefaults.profiles[p].systemStats[statKey] * 0.10 ) ) );
                            }else if( statKey == "architecture"){
                                var architecture = stats.systemStats[statKey];
                                if( architecture == "i386"){
                                    architecture = "32";
                                }else if( architecture == "amd64"){
                                    architecture = "64";
                                }else{
                                    architecture = "unknown";
                                }
                                match = ( architecture == wizardDefaults.profiles[p].systemStats[statKey] );
                            }else{
                                match = ( stats.systemStats[statKey] == wizardDefaults.profiles[p].systemStats[statKey] );
                            }
                            if( match == false ){
                                break;
                            }
                        }
                        if( match == true ){
                            profile = wizardDefaults.profiles[p];
                            profile.profileVersion = wizardDefaults.version;
                            break;
                        }
                    }

                    // Preserve recommended values
                    this.gui.wizardRecommendedSettings = Ext.clone(profile);

                    this.gui.wizardSettings = Ext.clone(profile);
                    var i;
                    if( this.gui.settings.configured === true ){
                        // Setup wizard already configured.  Pull current settings.
                        if( this.gui.settings.activeGroups ){
                            keys = Object.keys(this.gui.settings.activeGroups);
                            for( i = 0; i < keys.length; i++){
                                this.gui.wizardSettings.activeGroups[keys[i]] = this.gui.settings.activeGroups[keys[i]];
                            }
                        }
                    }

                    Ext.MessageBox.hide();
                    handler();
                },
                failure: function(response){
                    Ext.MessageBox.hide();
                    Ext.MessageBox.alert(
                        this.i18n._("Setup Wizard Error"), 
                        this.i18n._("Unable to obtain default settings.  Please run the Setup Wizard again."), 
                        Ext.bind(function () {
                        this.gui.wizardWindow.hide();
                    }, this));
                }
            });
        }
    }
});

Ext.define('Webui.untangle-node-idps.Wizard.Classtypes',{
    constructor: function( config ) {
        Ext.apply(this, config);

        this.classtypesCheckboxGroup = {
            xtype: 'checkboxgroup',
            fieldLabel: this.i18n._("Classtypes"),
            columns: 1,
            items: [],
        };

        this.gui.classtypesStore.each( function(record){
            this.classtypesCheckboxGroup.items.push({
                boxLabel: record.get( 'name' ) + ' (' + record.get( 'priority' ) + ')',
                name: 'classtypes_selected',
                inputValue: record.get( 'name' ),
                listeners: {
                    render: function(){
                        var id = Ext.get(Ext.DomQuery.select( 'td#' + this.id + '-bodyEl.x-form-item-body.x-form-cb-wrap' ));
                        Ext.QuickTips.register({
                            target:  id.elements[id.elements.length - 1].id, 
                            text: record.get( 'description' ),
                            dismissDelay: 5000
                        });
                    },
                    destroy: function(){
                        var id = Ext.get(Ext.DomQuery.select( 'td#' + this.id + '-bodyEl.x-form-item-body.x-form-cb-wrap' ));
                        Ext.QuickTips.unregister(id.elements[id.elements.length-1].id);
                    }
                }
            });
        }, this );


        this.title = this.i18n._( "Classtypes" );
        this.panel = Ext.create('Ext.container.Container',{
            items: [{
                xtype: 'container',
                html: '<h2 class="wizard-title">'+this.i18n._("Classtypes")+'</h2>'
            },{
                xtype: 'container',
                html: this.i18n._("Classtypes are a generalized  grouping for rules, such as attempts to gain user access."),
                margin: '0 0 10 0'
            },{
                name: 'classtypes',
                xtype: 'radio',
                inputValue: 'recommended',
                boxLabel: this.i18n._('Recommended (default)'),
                hideLabel: true,
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this)
            },{
                name: 'classtypes_recommended_settings',
                xtype: 'fieldset',
                hidden: true,
                html: "<i>" + this.i18n._("Recommended classtype Settings") + "</i>"
            },{
                name: 'classtypes',
                xtype: 'radio',
                inputValue: 'custom',
                boxLabel: this.i18n._('Custom'),
                hideLabel: true,
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this)
            },{
                name: 'classtypes_custom_settings',
                xtype:'fieldset',
                hidden: true,
                items: [
                    this.classtypesCheckboxGroup
                ]
            }]
        });

        this.onLoad = Ext.bind( this.onLoad, this );
        this.onNext = Ext.bind( this.getValues, this );
    },

    setVisible: function( id, checked ){
        if( checked === false ){
            return;
        }
        Ext.Array.each( 
            this.panel.query(""), 
            function( c ){ 
                if( !c.name || c.name.indexOf('classtypes_') !== 0 ){
                    return true;
                }
                if( c.xtype == "fieldset"){
                    if( c.name.indexOf( id ) != -1 ){
                        c.setVisible(true);
                    }else{
                        c.setVisible(false);
                    }
                }
            }
        );
    },

    onLoad: function( handler ){
        if( this.loaded !== true ){
            if( this.gui.wizardSettings.activeGroups ){

                Ext.Array.each(
                    this.panel.query("radio[name=classtypes]"),
                    function(c){
                        if( c.inputValue == this.gui.wizardSettings.activeGroups.classtypes ){
                            c.setValue(true);
                        }
                    },
                    this
                );

                var i, value;
                var checkboxes = this.panel.query("checkbox[name=classtypes_selected]");
                for( i = 0; i < this.gui.wizardSettings.activeGroups.classtypesSelected.length; i++ ){
                    value = this.gui.wizardSettings.activeGroups.classtypesSelected[i];
                    if( value.indexOf("+") == 0 ){
                        value = value.substr(1);
                    }else if( value.indexOf("-") == 0 ){
                        value = value.substr(1);
                    }
                    for( var j = 0; j < checkboxes.length; j++ ){
                        if( checkboxes[j].inputValue == value ){
                            checkboxes[j].setValue(true);
                        }
                    }
                }

                if( this.gui.wizardRecommendedSettings.activeGroups.classtypesSelected.length === 0 ){
                    this.panel.down( "[name=classtypes_recommended_settings]" ).update( this.i18n._("None.  Classtypes within selected categories will be used.") );
                }else{
                    var recommendedValues = [];
                    for( i = 0 ; i < this.gui.wizardRecommendedSettings.activeGroups.classtypesSelected.length; i++ ){
                        value = this.gui.wizardRecommendedSettings.activeGroups.classtypesSelected[i];
                        if( value.indexOf("+") == 0 ){
                            value = value.substr(1);
                        }else if( value.indexOf("-") == 0 ){
                            value = value.substr(1);
                        }
                        recommendedValues.push(value);
                    }
                    this.panel.down( "[name=classtypes_recommended_settings]" ).update(recommendedValues.join( ", "));
                }
            }
            this.loaded = true;
        }
        handler();
    },

    getValues: function( handler ){
        this.gui.wizardSettings.activeGroups.classtypes = this.panel.down("radio[name=classtypes]").getGroupValue();
        if( this.panel.down("radio[name=classtypes]").getGroupValue() == "recommended") {
            this.gui.wizardSettings.activeGroups.classtypesSelected = this.gui.wizardRecommendedSettings.activeGroups.classtypesSelected;
        }else{
            this.gui.wizardSettings.activeGroups.classtypesSelected = [];
            Ext.Array.each( 
                this.panel.query("checkbox[name=classtypes_selected][checked=true]"), 
                function( c ){ 
                    this.gui.wizardSettings.activeGroups.classtypesSelected.push( "+" + c.inputValue );
                },
                this
            );
        }
        handler();
    }

});

Ext.define('Webui.untangle-node-idps.Wizard.Categories',{
    constructor: function( config ) {
        Ext.apply(this, config);

        var categoriesCheckboxGroup = {
            xtype: 'checkboxgroup',
            fieldLabel: this.i18n._("Categories"),
            columns: 1,
            items: []
        };

        this.gui.categoriesStore.each( function(record){
            categoriesCheckboxGroup.items.push({
                boxLabel: record.get( 'name' ),
                name: 'categories_selected',
                inputValue: record.get( 'name' ),
                listeners: {
                    render: function(){
                        var id = Ext.get(Ext.DomQuery.select( 'td#' + this.id + '-bodyEl.x-form-item-body.x-form-cb-wrap' ));
                        Ext.QuickTips.register({
                            target:  id.elements[id.elements.length - 1].id, 
                            text: record.get( 'description' ),
                            dismissDelay: 5000
                        });
                    },
                    destroy: function(){
                        var id = Ext.get(Ext.DomQuery.select( 'td#' + this.id + '-bodyEl.x-form-item-body.x-form-cb-wrap' ));
                        Ext.QuickTips.unregister(id.elements[id.elements.length-1].id);
                    }
                }
            });
        } );

        this.title = this.i18n._( "Categories" );
        this.panel = Ext.create('Ext.container.Container',{
            items: [{
                xtype: 'container',
                html: '<h2 class="wizard-title">'+this.i18n._("Categories")+'</h2>'
            },{
                xtype: 'container',
                html: this.i18n._("Categories are a different rule grouping that can span multiple classtypes, such as VOIP access."),
                margin: '0 0 10 0'
            },{
                name: 'categories',
                xtype: 'radio',
                inputValue: 'recommended',
                boxLabel: this.i18n._('Recommended (default)'),
                hideLabel: true,
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this)
            },{
                name: 'categories_recommended_settings',
                xtype:'fieldset',
                hidden:true
            },{
                name: 'categories',
                xtype: 'radio',
                inputValue: 'custom',
                boxLabel: this.i18n._('Select by name'),
                hideLabel: true,
                checked: false,
                handler: Ext.bind(function(elem, checked) {
                    this.setVisible( elem.inputValue, checked );
                }, this)                
            },{
                name: 'categories_custom_settings',
                xtype:'fieldset',
                hidden:true,
                items: [
                    categoriesCheckboxGroup
                ]
            }]
        });

        this.onLoad = Ext.bind( this.onLoad, this );
        this.onNext = Ext.bind( this.getValues, this );
    },

    setVisible: function( id, checked ){
        if( checked === false ){
            return;
        }
        Ext.Array.each( this.panel.query(""), 
            function( c ) { 
                if( !c.name || c.name.indexOf('categories_') !== 0 ) {
                    return true;
                }
                if( c.xtype == "fieldset"){
                    if( c.name.indexOf( id ) != -1 ) {
                        c.setVisible(true);
                    } else {
                        c.setVisible(false);
                    }
                }
            }
        );
    },

    onLoad: function( handler ){
        if( this.loaded !== true ){
            if( this.gui.wizardSettings.activeGroups ){
                Ext.Array.each( this.panel.query("radio[name=categories]"),
                    function(c){
                        if( c.inputValue == this.gui.wizardSettings.activeGroups.categories ){
                            c.setValue(true);
                        }
                    },
                    this
                );

                var i, value;
                var checkboxes = this.panel.query("checkbox[name=categories_selected]");
                for( i = 0; i < this.gui.wizardSettings.activeGroups.categoriesSelected.length; i++ ){
                    value = this.gui.wizardSettings.activeGroups.categoriesSelected[i];
                    if( value.indexOf("+") == 0 ){
                        value = value.substr(1);
                    }else if( value.indexOf("-") == 0 ){
                        value = value.substr(1);
                    }
                    for( var j = 0; j < checkboxes.length; j++ ){
                        if( checkboxes[j].inputValue == value ){
                            checkboxes[j].setValue(true);
                        }
                    }
                }

                if( this.gui.wizardRecommendedSettings.activeGroups.categoriesSelected.length === 0 ) {
                    this.panel.down( "[name=categories_recommended_settings]" ).update( this.i18n._("None.  Categories within selected classtypes will be used.") );
                } else {
                    var recommendedValues = [];
                    for( i = 0 ; i < this.gui.wizardRecommendedSettings.activeGroups.categoriesSelected.length; i++ ){
                        value = this.gui.wizardRecommendedSettings.activeGroups.categoriesSelected[i];
                        if( value.indexOf("+") == 0 ){
                            value = value.substr(1);
                        }else if( value.indexOf("-") == 0 ){
                            value = value.substr(1);
                        }
                        recommendedValues.push(value);
                    }
                    this.panel.down( "[name=categories_recommended_settings]" ).update(recommendedValues.join( ", "));
                }
            }
            this.loaded = true;
        }
        handler();
    },
    
    getValues: function( handler ){
        this.gui.wizardSettings.activeGroups.categories = this.panel.down("radio[name=categories]").getGroupValue();
        
        if( this.panel.down("radio[name=categories]").getGroupValue() == "recommended") {
            this.gui.wizardSettings.activeGroups.categoriesSelected = this.gui.wizardRecommendedSettings.categoriesSelected;
        }else{
            this.gui.wizardSettings.activeGroups.categoriesSelected = [];
            Ext.Array.each( 
                this.panel.query("checkbox[name=categories_selected][checked=true]"), 
                function( c ){ 
                    this.gui.wizardSettings.activeGroups.categoriesSelected.push( "+" + c.inputValue );
                },
                this
            );
        }
        handler();
    }
});


Ext.define('Webui.untangle-node-idps.Wizard.Congratulations',{
    constructor: function( config ) {
        Ext.apply(this, config);

        this.title = this.i18n._( "Finish" );
        this.panel = Ext.create('Ext.container.Container',{
            items: [{
                xtype: 'container',
                html: '<h2 class="wizard-title">'+this.i18n._("Congratulations!")+'</h2>'
            }, {
                xtype: 'container',
                html: this.i18n._('Intrusion Prevention is now configured and enabled.'),
                margin: '10 0 0 10'
            }]
        });

        this.onNext = Ext.bind(this.completeWizard, this );
    },

    completeWizard: function( handler ) {
        // Copy values into form settings.
        var keys = Object.keys(this.gui.wizardSettings);
        for( var i = 0; i < keys.length; i++){
            if( keys[i] == "systemStats" ){
                continue;
            }
            this.gui.settings[keys[i]] = this.gui.wizardSettings[keys[i]];
        }
        this.gui.settings.configured = true;

        // Save, enable, teardown wizard
        this.gui.dirtyFlag = true;
        this.gui.wizard = true;

        this.gui.applyAction();

        this.nodeWidget.setPowerOn(true);
        this.nodeWidget.setState("attention");
        this.gui.getRpcNode().start(Ext.bind(function(result, exception) {
            this.gui.wizardWindow.endAction();
            this.gui.getRpcNode().getRunState(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.nodeWidget.updateRunState(result);
            }, this));
        }, this));
        this.gui.wizardWindow.hide();
        this.gui.forceReload = true;
    }
});