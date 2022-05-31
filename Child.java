import java.lang.Math;


class Child{
	int key;
	String nodeIp;
	MBR mbr;

	Child(int key, String nodeIp, int x1, int y1, int t1, int x2, int y2, int t2){
		this.key = key;
		this.nodeIp = nodeIp;
		this.mbr = new MBR(x1,y1,t1,x2,y2,t2);
	}
	Child(int key, String nodeIp){//usado para o father
		this.key = key;
		this.nodeIp = nodeIp;
	}
	int volume(Child rn){
		int x1 = rn.mbr.x1,y1 = rn.mbr.y1,t1 = rn.mbr.t1,x2 = rn.mbr.x2,y2 = rn.mbr.y2,t2 = rn.mbr.t2;
		if(this.mbr.x1 < x1){
			x1 = this.mbr.x1;
		}
		if(this.mbr.y1 < y1){
			y1 = this.mbr.y1;
		}
		if(this.mbr.t1 < t1){
			t1 = this.mbr.t1;
		}
		if(this.mbr.x2 > x2){
			x2 = this.mbr.x2;
		}
		if(this.mbr.y2 > y2){
			y2 = this.mbr.y2;
		}
		if(this.mbr.t2 > t2){
			t2 = this.mbr.t2;
		}
		return Math.abs((x2-x1)*(y2-y1)*(t2-t1)); 
	}
}