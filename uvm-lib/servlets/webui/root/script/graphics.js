function Graphics() {
}
Graphics.setPixel = function(x, y, w, h,color) {
	return '<div style="position:absolute;left:'+x+'px;top:'+y+'px;width:'+w+'px;height:'+h+'px;background-color:'+color+';overflow:hidden;"></div>';
}
Graphics.drawLine = function(x1, y1, x2, y2,color) {
	var out=[];
	//Always draw from left to right. This makes the algorithm much easier...
	if (x1 > x2) {
		var tmpx = x1; var tmpy = y1;
		x1 = x2; y1 = y2;
		x2 = tmpx; y2 = tmpy;
	}
	
	var dx = x2 - x1;	
	var dy = y2 - y1; var sy = 1;	
	if (dy < 0)	{
		sy = -1;
		dy = -dy;
	}
	
	dx = dx << 1; dy = dy << 1;
	if (dy <= dx) {	
		var fraction = dy - (dx>>1);
		var mx = x1;
		while (x1 != x2)
		{
			x1++;
			if (fraction >= 0) {
				out.push(Graphics.setPixel(mx, y1,x1-mx,1,color));
				y1 += sy;
				mx = x1;
				fraction -= dx;
			}
			fraction += dy;
		}
		out.push(Graphics.setPixel(mx,y1,x1-mx,1,color));
	} else {
		var fraction = dx - (dy>>1);
		var my = y1;
		if (sy > 0)
		{		
			while (y1 != y2)
			{
				y1++;
				if (fraction >= 0)
				{
					out.push(Graphics.setPixel(x1++, my,1,y1-my,color));
					my = y1;
					fraction -= dy;
				}
				fraction += dx;
			}	
			out.push(Graphics.setPixel(x1,my,1,y1-my,color));
		}
		else {
			while (y1 != y2) {
				y1--;
				if (fraction >= 0) {
					out.push(Graphics.setPixel(x1++, y1,1,my-y1,color));
					my = y1;
					fraction -= dy;
				}
				fraction += dx;
			}	
			out.push(Graphics.setPixel(x1,y1,1,my-y1,color));		
		}
	}
	return out.join('');
}
