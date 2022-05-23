import java.util.List;
import java.util.LinkedList;
class RNode{
	int key;
	String nodeIp;
	MBR mbr;
	String fatherIp;//talvez não seja necessario o mbr do pai (seria até dificil manter a atualização dele)
	int fatherKey;

	List<Child> children;//provavel que mude para um array de tamanho fixo
	List<Point> points;//talvez seja o caso de usar a referencia isEmpty pra verificar se é um leaf ou non leaf
	//melhor usar o points/child == null pra ver se é leaf ou não
	//quando um Rnode for deixar de ser leaf fazer uma iteração para transferir os points e depois usar método de clear da lista
	RNode(int key,String nodeIp,int x1,int y1,int t1,int x2,int y2,int t2,int fatherKey,String fatherIp){
		this.key = key;
		this.nodeIp = nodeIp;
		this.mbr = new MBR(x1,y1,t1,x2,y2,t2);
		this.fatherKey = fatherKey;
		this.fatherIp = fatherIp;
	}
	//quando um child será adicionado? quando ocorrer um overflow de um de seus filhos
	void addChild(Child child){
		if(children == null){
			children = new LinkedList<Child>();
		}
		this.children.add(child);
	}
	void addChild(RNode rnode){
		Child child = new Child(rnode.key,rnode.nodeIp,rnode.mbr.x1,rnode.mbr.y1,rnode.mbr.t1,rnode.mbr.x2,rnode.mbr.y2,rnode.mbr.t2);
		addChild(child);
	}
	void addPoint(Point point){//quando um point será adicionado? quando este fdor um leaf e ele foi escolhido para armazenar um point
		if(points == null){
			points = new LinkedList<Point>();
		}
		this.points.add(point);

	}
	void adjustMBR(){
		if(this.points == null){//non leaf
			for(int i = 0; i < children.size();i++){
				this.mbr.expand(children.get(i).mbr.x1,children.get(i).mbr.y1,children.get(i).mbr.t1);
				this.mbr.expand(children.get(i).mbr.x2,children.get(i).mbr.y2,children.get(i).mbr.t2);
			}
		}
		else{//leaf
			for(int i =0; i< points.size();i++){
				this.mbr.expand(this.points.get(i).x,this.points.get(i).y,this.points.get(i).t);
			}
		}
	}

}