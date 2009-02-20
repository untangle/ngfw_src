
var PageName = 'Protocol Control - Host Incident';
var PageId = 'pf4ee85c6c37a41039112d0fe926cf4bd'
document.title = 'Protocol Control - Host Incident';

if (top.location != self.location)
{
	if (parent.HandleMainFrameChanged) {
		parent.HandleMainFrameChanged();
	}
}

var $OnLoadVariable = '';

var $CSUM;

var hasQuery = false;
var query = window.location.hash.substring(1);
if (query.length > 0) hasQuery = true;
var vars = query.split("&");
for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split("=");
    if (pair[0].length > 0) eval("$" + pair[0] + " = decodeURI(pair[1]);");
} 

if (hasQuery && $CSUM != 1) {
alert('Prototype Warning: Variable values were truncated.');
}

function GetQuerystring() {
    return encodeURI('#OnLoadVariable=' + $OnLoadVariable + '&CSUM=1');
}

function PopulateVariables(value) {
  value = value.replace(/\[\[OnLoadVariable\]\]/g, $OnLoadVariable);
  value = value.replace(/\[\[PageName\]\]/g, PageName);
  return value;
}

function OnLoad() {

}

var u2 = document.getElementById('u2');
gv_vAlignTable['u2'] = 'center';
var u1 = document.getElementById('u1');

var u0 = document.getElementById('u0');
gv_vAlignTable['u0'] = 'top';
var u3 = document.getElementById('u3');

if (window.OnLoad) OnLoad();
