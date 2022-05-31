import java.lang.Math;
class Point{
	int key;
	int trajKey;
	String nodeIp;
	int x,y,t;//t = timestamp?

	Point(int key,int trajKey,String nodeIp,int x, int y, int t){
		this.key = key;
		this.trajKey = trajKey;
		this.nodeIp = nodeIp;
		this.x = x;
		this.y = y;
		this.t = t;
	}

	int volume(Point p){
		return Math.abs((p.x-this.x)*(p.y-this.y)*(p.t-this.t));
	}
	
}