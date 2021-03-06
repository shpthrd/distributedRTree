import java.util.List;
import java.util.LinkedList;
import java.lang.Math;

class RNode{
	int key;
	String nodeIp;
	MBR mbr;
	String fatherIp;//talvez não seja necessario o mbr do pai (seria até dificil manter a atualização dele)
	int fatherKey;

	List<Child> children = new LinkedList<Child>();//provavel que mude para um array de tamanho fixo
	List<Point> points = new LinkedList<Point>();//talvez seja o caso de usar a referencia isEmpty pra verificar se é um leaf ou non leaf
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

	int findIndex(int x, int y, int t){//@@@@@@@@@@@@@@@@ iMPLEMENTAR ok -> testar
		int volume = this.volume(x, y, t, 0);
		int index = 0;
		for(int i = 1; i < this.children.size();i++){
			int tempVolume = this.volume(x, y, t, i);
			if(tempVolume < volume){
				volume = tempVolume;
				index = i;
			}
		}
		return index;
	}
	int[] splitIndexes(){//tanto para leaf quanto para non leaf //@@@@@@@@@@@@@@@@@@@@@@TESTAR
		int[] arr = {0,1};
		int volume = -1;
		int i,j;
		if(this.points.isEmpty()){//non leaf
			for(i = 0;i < this.children.size()-1; i++){//aqui está quadratica, pode ser linear
				for(j = i; j < this.children.size();j++){
					int tempVolume,x1,y1,t1,x2,y2,t2;
					if(this.children.get(i).mbr.x1 < this.children.get(j).mbr.x1){
						x1 = this.children.get(i).mbr.x1;
					}
					else{
						x1 = this.children.get(j).mbr.x1;
					}
					if(this.children.get(i).mbr.y1 < this.children.get(j).mbr.y1){
						y1 = this.children.get(i).mbr.y1;
					}
					else{
						y1 = this.children.get(j).mbr.y1;
					}
					if(this.children.get(i).mbr.t1 < this.children.get(j).mbr.t1){
						t1 = this.children.get(i).mbr.t1;
					}
					else{
						t1 = this.children.get(j).mbr.t1;
					}
					if(this.children.get(i).mbr.x2 < this.children.get(j).mbr.x2){
						x2 = this.children.get(i).mbr.x2;
					}
					else{
						x2 = this.children.get(j).mbr.x2;
					}
					if(this.children.get(i).mbr.y2 < this.children.get(j).mbr.y2){
						y2 = this.children.get(i).mbr.y2;
					}
					else{
						y2 = this.children.get(j).mbr.y2;
					}
					if(this.children.get(i).mbr.t2 < this.children.get(j).mbr.t2){
						t2 = this.children.get(i).mbr.t2;
					}
					else{
						t2 = this.children.get(j).mbr.t2;
					}
					tempVolume = (x2 - x1) * (y2 - y1) * (t2 - t1);
					if(tempVolume > volume){
						volume = tempVolume;
						arr[0] = i;
						arr[1] = j;
					}
				}
			}
		}
		else{//leaf
			for(i = 0;i < this.points.size()-1; i++){//aqui está quadratica, pode ser linear
				for(j = i; j < this.points.size();j++){
					int tempVolume = Math.abs( (this.points.get(i).x - this.points.get(j).x) * (this.points.get(i).y - this.points.get(j).y) * (this.points.get(i).t - this.points.get(j).t) );
					if(tempVolume > volume){
						volume = tempVolume;
						arr[0] = i;
						arr[1] = j;
					}
				}
			}
		}
		return arr;
	}

	int areaDiff(Point p){//diferença da area expandida com o ponto e da area original
		int x1,y1,t1,x2,y2,t2;
		if(p.x < this.mbr.x1){
			x1 = p.x;
			x2 = this.mbr.x2;
		}
		else{
			if(p.x > this.mbr.x2){
				x1 = this.mbr.x1;
				x2 = p.x;
			}
			else{
				x1 = this.mbr.x1;
				x2 = this.mbr.x2;
			}
		}
		if(p.y < this.mbr.y1){
			y1 = p.y;
			y2 = this.mbr.y2;
		}
		else{
			if(p.y > this.mbr.y2){
				y1 = this.mbr.y1;
				y2 = p.y;
			}
			else{
				y1 = this.mbr.y1;
				y2 = this.mbr.y2;
			}
		}
		if(p.t < this.mbr.t1){
			t1 = p.t;
			t2 = this.mbr.t2;
		}
		else{
			if(p.t > this.mbr.t2){
				t1 = this.mbr.t1;
				t2 = p.t;
			}
			else{
				t1 = this.mbr.t1;
				t2 = this.mbr.t2;
			}
		}
		return Math.abs(((x2-x1) * (y2-y1) * (t2-t1))) - this.volume();
	}

	int areaDiff(Child ch){//@@@@@@@@@@@@@@ IMPLEMENTAR
		int x1,y1,t1,x2,y2,t2;
		if(ch.mbr.x1 < this.mbr.x1){
			x1 = ch.mbr.x1;
		}
		return 0;
	}

	int volume(){
		return Math.abs((this.mbr.x2-this.mbr.x1) * (this.mbr.y2-this.mbr.y1) * (this.mbr.t2-this.mbr.t1)); 
	}

	int volume(int x,int y,int t, int i){
		if(x > this.children.get(i).mbr.x2){
			x = x - this.children.get(i).mbr.x1;
		}
		else{
			if(x < this.children.get(i).mbr.x1){
				x = this.children.get(i).mbr.x2 - x;
			}
			else{
				x = this.children.get(i).mbr.x2- this.children.get(i).mbr.x1;
			}
		}
		if(y > this.children.get(i).mbr.y2){
			y = y - this.children.get(i).mbr.y1;
		}
		else{
			if(y < this.children.get(i).mbr.y1){
				y = this.children.get(i).mbr.y2 - y;
			}
			else{
				y = this.children.get(i).mbr.y2- this.children.get(i).mbr.y1;
			}
		}
		if(t > this.children.get(i).mbr.t2){
			t = t - this.children.get(i).mbr.t1;
		}
		else{
			if(t < this.children.get(i).mbr.t1){
				t = this.children.get(i).mbr.t2 - t;
			}
			else{
				t = this.children.get(i).mbr.t2- this.children.get(i).mbr.t1;
			}
		}
		return x * y * t;
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

	void print(){
		System.out.println("RNode key@ip " + this.key + "@" + this.nodeIp + "| my father key@ip " +  this.fatherKey + "@" +this.fatherIp);
		System.out.println("MBR(" + this.mbr.x1 + "," + this.mbr.y1 + "," + this.mbr.t1 + ")->(" + this.mbr.x2 + "," + this.mbr.y2 + "," + this.mbr.t2 + ")");
		if(children.size() != 0){//non leaf
			System.out.println("Children:");
			for(int i = 0; i < this.children.size(); i++){
				System.out.println("Child key@ip " + this.children.get(i).key + "@" + this.children.get(i).nodeIp);
				System.out.println("MBR(" + this.children.get(i).mbr.x1 + "," + this.children.get(i).mbr.y1 + "," + this.children.get(i).mbr.t1 + ")->(" + this.children.get(i).mbr.x2 + "," + this.children.get(i).mbr.y2 + "," + this.children.get(i).mbr.t2 + ")");
			}
		}
		if(points.size() != 0){
			System.out.println("Points");
			for(int i = 0; i < this.points.size(); i++){
				System.out.println("Point key@trajKey@ip " + this.points.get(i).key + "@" + this.points.get(i).trajKey + "@" + this.points.get(i).nodeIp);
				System.out.println("Coord(" + this.points.get(i).x + "," + this.points.get(i).y + "," + this.points.get(i).t + ")");
			}
		}
	}

}