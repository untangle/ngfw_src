
var PageName = 'Protocol Control - User Summary';
var PageId = 'p08c6454b7e5440c98521c8a1ea9ce5ff'
document.title = 'Protocol Control - User Summary';

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

var u10 = document.getElementById('u10');
gv_vAlignTable['u10'] = 'top';
var u5 = document.getElementById('u5');
gv_vAlignTable['u5'] = 'top';
var u0 = document.getElementById('u0');
gv_vAlignTable['u0'] = 'top';
var u3 = document.getElementById('u3');

var u9 = document.getElementById('u9');
gv_vAlignTable['u9'] = 'top';
var u6 = document.getElementById('u6');
gv_vAlignTable['u6'] = 'top';
var u1 = document.getElementById('u1');

u1.style.cursor = 'pointer';
if (bIE) u1.attachEvent("onclick", Clicku1);
else u1.addEventListener("click", Clicku1, true);
function Clicku1(e)
{

if (true) {

	self.location.href="Protocol Control - User Incident.html" + GetQuerystring();

}

}
gv_vAlignTable['u1'] = 'top';
var u4 = document.getElementById('u4');

var u7 = document.getElementById('u7');
gv_vAlignTable['u7'] = 'top';
var u2 = document.getElementById('u2');

var u8 = document.getElementById('u8');
gv_vAlignTable['u8'] = 'top';
if (window.OnLoad) OnLoad();
