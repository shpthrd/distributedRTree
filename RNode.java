import java.util.List;
import java.util.LinkedList;
class RNode{
	int key;
	String nodeIp;
	MBR mbr;
	List<Child> children;//provavel que mude para um array de tamanho fixo
	String fatherIp;//talvez não seja necessario o mbr do pai (seria até dificil manter a atualização dele)
	int fatherKey;
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
	void addPoint(Point point){//quando um point será adicionado? quando este fdor um leaf e ele foi escolhido para armazenar um point
		if(points == null){
			points = new LinkedList<Point>();
		}
		this.points.add(point);

	}

}