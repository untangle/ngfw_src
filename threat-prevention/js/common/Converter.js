Ext.define('Ung.common.Converter.threatprevention', {
    singleton: true,

    reason: function (v) { return Map.webReasons[v] || 'no rule applied'.t(); },

    ruleIdMap: {},
    ruleId: function (value, record) {
        if (value == 0) {
            return '';
        }
        var policyId = record && record.get && record.get('policy_id') ? record.get('policy_id') : 1;
        var reason = record && record.get && record.get('threat_prevention_reason') ? record.get('threat_prevention_reason') : 'N';
        if(reason != 'N'){
            for(var id in Map.webReasons){
                if(Map.webReasons.hasOwnProperty(id)){
                    if(Map.webReasons[id] == reason){
                        reason = id;
                        if(reason == 'default'){
                            reason = 'N';
                        }
                        break;
                    }
                }
            }
        }
        if(!Ung.common.Renderer.threatprevention.ruleIdMap[policyId] ||
            !Ung.common.Renderer.threatprevention.ruleIdMap[policyId][reason] ||
            (value != Renderer.listKey && !(value in Ung.common.Renderer.threatprevention.ruleIdMap[policyId][reason]))){
            if(!Ung.common.Renderer.threatprevention.ruleIdMap[policyId]){
                Ung.common.Renderer.threatprevention.ruleIdMap[policyId] = {};
            }
            if(!Ung.common.Renderer.threatprevention.ruleIdMap[policyId][reason]){
                Ung.common.Renderer.threatprevention.ruleIdMap[policyId][reason] = {};
            }

            var ruleInfo = Renderer.getReportInfo(record, ["threat-prevention"], reason == 'I' ? "passSites" : "rules");
            if(ruleInfo){
                ruleInfo.forEach( function(rule){
                    Ung.common.Renderer.threatprevention.ruleIdMap[policyId][reason][rule["id"] ? rule["id"] : rule["ruleId"]] = rule["description"] ? rule["description"] : rule["string"];
                });
            }
            if(!(value in Ung.common.Renderer.threatprevention.ruleIdMap[policyId][reason]) && (value != Renderer.listKey)){
                // If category cannot be found, don't just keep coming back for more.
                Ung.common.Renderer.threatprevention.ruleIdMap[policyId][reason][value] = 'Unknown'.t();
            }
        }
        if (value == Renderer.listKey) {
            return Ung.common.Renderer.threatprevention.ruleIdMap[policyId][reason] + ' [' + value + ']';
        } else {
            return Ung.common.Renderer.threatprevention.ruleIdMap[policyId][reason][value] + ' [' + value + ']';
        }
    },

    categoryMap:{
        0: 'Spam Sources'.t(),
        1: 'Windows Exploits'.t(),
        2: 'Web Attacks'.t(),
        3: 'Botnets'.t(),
        4: 'Scanners'.t(),
        5: 'Denial of Service'.t(),
        6: 'Reputation'.t(),
        7: 'Phishing'.t(),
        8: 'Proxy'.t(),
        11: 'Mobile Threats'.t(),
        13: 'Tor Proxy'.t(),
        16: 'Keyloggers'.t(),
        17: 'Malware'.t(),
        18: 'Spyware'.t()
    },
    category: function(value){
        if (value == 0){
            return '';
        }
        var descriptions = [];
        var threatBits = Object.keys(Ung.common.Renderer.threatprevention.categoryMap);
        for(var i = 0; i < threatBits.length; i++){
            if(Math.pow(2,threatBits[i]) & value){
                descriptions.push(Ung.common.Renderer.threatprevention.categoryMap[threatBits[i]]);
            }
        }
        return descriptions.join(', ');
    },

    /**
     * converts the reputation value to readable string
     * @param {int} value - the reputation score
     */
    reputation: function(value) {
        return value ? Ung.common.threatprevention.references.getReputationLevel(value).get('description') : '';
    },
});
