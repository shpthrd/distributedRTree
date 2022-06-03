class MBR{
	int x1,y1,t1,x2,y2,t2;

	MBR(int x1,int y1, int t1, int x2, int y2, int t2){
		this.x1 = x1;
		this.y1 = y1;
		this.t1 = t1;
		this.x2 = x2;
		this.y2 = y2;
		this.t2 = t2;
	}

	int area(){
		return (x2-x1)*(y2-y1);//verificar modulo
	}

	int volume(){
		return (x2-x1)*(y2-y1)*(t2-t1);//verificar modulo
	}

	int expandVolValue(int x, int y, int t){
		int dx = this.x2 - this.x1, dy = this.y2 - this.y1, dt = this.t2 - this.t1;
		if(x > this.x2){
			dx = x - this.x1;
		}
		if(x < this.x1){
			dx = this.x2 - x;
		}
		if(y > this.y2){
			dy = y - this.y1;
		}
		if(y < this.y1){
			dy = this.y2 - y;
		}
		if(t > this.t2){
			dt = t - this.t1;
		}
		if(t < this.t1){
			dt = this.t2 - t;
		}
		return dx*dy*dt - this.volume();
	}

	void expand(Point p){
		this.expand(p.x,p.y,p.t);
	}

	void expand(Child ch){
		this.expand(ch.mbr.x1,ch.mbr.y1,ch.mbr.t1);
		this.expand(ch.mbr.x2,ch.mbr.y2,ch.mbr.t2);
	}
	
	void expand(int x, int y, int t){
		if(x >this.x2){
			this.x2 = x;
		}
		if(x < this.x1){
			this.x1 = x;
		}
		if(y > this.y2){
			this.y2 = y;
		}
		if(y < this.y1){
			this.y1 = y;
		}
		if(t > this.t2){
			this.t2 = t;
		}
		if(t < this.t1){
			this.t1 = t;
		}
	}
}