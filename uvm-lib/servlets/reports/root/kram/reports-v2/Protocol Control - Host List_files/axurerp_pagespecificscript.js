
var PageName = 'Protocol Control - Host List';
var PageId = 'pffde7b634ff64f2caaf7ee60699c2577'
document.title = 'Protocol Control - Host List';

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

var u5 = document.getElementById('u5');
gv_vAlignTable['u5'] = 'top';
var u0 = document.getElementById('u0');

u0.style.cursor = 'pointer';
if (bIE) u0.attachEvent("onclick", Clicku0);
else u0.addEventListener("click", Clicku0, true);
function Clicku0(e)
{

if (true) {

	self.location.href="Protocol Control - Host Summary.html" + GetQuerystring();

}

}
gv_vAlignTable['u0'] = 'top';
var u3 = document.getElementById('u3');
gv_vAlignTable['u3'] = 'top';
var u6 = document.getElementById('u6');
gv_vAlignTable['u6'] = 'top';
var u1 = document.getElementById('u1');
gv_vAlignTable['u1'] = 'top';
var u4 = document.getElementById('u4');
gv_vAlignTable['u4'] = 'top';
var u7 = document.getElementById('u7');
gv_vAlignTable['u7'] = 'top';
var u2 = document.getElementById('u2');
gv_vAlignTable['u2'] = 'top';
var u8 = document.getElementById('u8');
gv_vAlignTable['u8'] = 'top';
if (window.OnLoad) OnLoad();
