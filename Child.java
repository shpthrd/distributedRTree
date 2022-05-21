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
}