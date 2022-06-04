import java.util.HashMap;
import java.util.Scanner;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.util.Queue;
import java.util.concurrent.Semaphore;

class MasterNode{
	String ip;
	int rootKey;//o id(key) do RNode raiz
	int countPoints = 0;//contador de quantos ADDs foram feitos (quantos points tem no sistema)
	int countOverflow = 0;

	Thread tConsumer;
	static Queue<String> queue = new LinkedList<String>();//queue para produtor e consumidor das requisicoes trabalharem
	HashMap<Integer, Point> pointMap = new HashMap<Integer, Point>();//KV para os pontos de trajetorias armazenadas no Node, mapeados pelo
	HashMap<Integer, RNode> rnodeMap = new HashMap<Integer, RNode>();//KV para os RNodes da RTREE armazenadas no Node
	List<String> nodeList = new LinkedList<String>();//exclusivo do Master, aponta para todos os nodes que sao incluidos no sistema (no caso armazena o ip de cada node)
	static Semaphore mutex1 = new Semaphore(0);//semaforos para a concorrencia entre produtor e consumidor
	static Semaphore mutex = new Semaphore(1);//^
	ServerSocket ss;


	MasterNode(){
		this.ip = Config.getHostIp();
		rootKey = Config.getKey();
		rnodeMap.put(rootKey,new RNode(rootKey,this.ip,0,0,0,0,0,0,0,null));
	}

//===============================================================
//MasterProducer:
//O producer no Node Master é quem efetivamente recebe as requisições do processo usuário, 
//além de receber requisições de outros Nodes
//===============================================================

	void MasterProducer() throws Exception{
        System.out.println("Start Producer");
        String code = "";
        while(code != "exit"){
            //System.out.println("Producer: Before ServerSocket");
            ss = new ServerSocket(8000);//padrao a porta 8000 //será que dá ruim?
            Socket s = ss.accept();
            DataInputStream din=new DataInputStream(s.getInputStream());
            code = din.readUTF();
            din.close(); 
            s.close(); 
            ss.close();
            //System.out.println("Producer: After Sockets Close");
            //System.out.println("Producer: before mutex");
            mutex.acquire();
            queue.offer(code);
            mutex.release();  //releasing After Production ;
            mutex1.release();
            System.out.println("Producer: after mutex com o code: "+ code);
            //String[] cmd = code.split("#");
            if(code.startsWith("exit") || code.startsWith("EXIT")){
				System.out.println("producer| entrou no startswith do exit");
                code = "exit";
                //System.out.println("Producer: entrou no if do exit");
            }
            
        }
        System.out.println("Producer: fim do while");
    }

//===============================================================
//MasterConsumer:
//O consumer fica aguardando ter algo na fila de trabalho para consumir 
//quando há um comando ele desenfileira e procura a função para atender a requisição
//===============================================================

	void MasterConsumer() throws Exception{
		System.out.println("Start Consumer");
		//Thread t = new Thread(new Runnable() {
		tConsumer = new Thread(new Runnable() {
			@Override
			public void run(){
				//System.out.println("starting new thread");
				String code = "";
				while(code != "exit"){
					//System.out.println("Consumer: after while code: "+code);
					Boolean noCommand = true;
					while(noCommand){
						try {
							//System.out.println("Consumer: before mutex");
							mutex1.acquire();     /// Again Acquiring So no production while consuming
							mutex.acquire();
							if(queue.size() < 1){//lista vazia
								//System.out.println("lista vazia");
								mutex.release();
								//System.out.println("Consumer: after mutex");
								Thread.sleep(500);
							}
							else{//lista nao vazia
								//System.out.println("peguei um comando");
								code = queue.poll();
								mutex.release();
								//System.out.println("Consumer: after mutex");
								noCommand = false;
							}
						} catch (Exception e) {
						}
						
					}
					System.out.println(code);
					String[] cmd = code.split("#");
					cmd[0] = cmd[0].toUpperCase();
					try{
						switch (cmd[0]) {
							case "ADI": //MASTER| -> ADI#p.x#p.y#p.t#p.trajKey
								System.out.println("add init");
								addInit(Integer.parseInt(cmd[1]),Integer.parseInt(cmd[2]),Integer.parseInt(cmd[3]),Integer.parseInt(cmd[4]));
								break;
							
							case "RTI": //MASTER| INIT OF A RETRIEVE ->
								retrieveInit();
								break;

							case "SRI": //MASTER| search init ->
								searchInit("sri placeholder");
								break;

							case "ADN": //MASTER| add node -> ADN# NODE IP
								addNode(cmd[1]);
								break;

							case "ADD": //segue a lógica de inserção -> ADD#RNODE KEY#P.KEY#p.trajKey#p.x#p.y#p.t
								System.out.println("add");
								addPoint(Integer.parseInt(cmd[1]),Integer.parseInt(cmd[2]),Integer.parseInt(cmd[3]),Integer.parseInt(cmd[4]),Integer.parseInt(cmd[5]),Integer.parseInt(cmd[6]));
								break;
							case "OVF"://update de quando há um overflow que vem de outro node cujo pai está aqui 
										//-> OVF#1-FATHER.KEY#2-RNODE.KEY
										//#3-N1.KEY#4-N1.IP#5-N1.X1#6-N1.Y1#7-N1.T1#8-N1.X2#9-N1.Y2#10-N1.T2
										//#11-N2.KEY#12-N2.IP#13-N2.X1#14-N2.Y1#15-N2.T1#16-NR.X2#17-N2.Y2#18-N2.T2
									Child n1 = new Child(Integer.parseInt(cmd[3]), cmd[4], Integer.parseInt(cmd[5]), Integer.parseInt(cmd[6]), Integer.parseInt(cmd[7]), Integer.parseInt(cmd[8]), Integer.parseInt(cmd[9]), Integer.parseInt(cmd[10]));
									Child n2 = new Child(Integer.parseInt(cmd[11]), cmd[12], Integer.parseInt(cmd[13]), Integer.parseInt(cmd[14]), Integer.parseInt(cmd[15]), Integer.parseInt(cmd[16]), Integer.parseInt(cmd[17]), Integer.parseInt(cmd[118]));
									oveflowUpdate(Integer.parseInt(cmd[1]),Integer.parseInt(cmd[2]),n1,n2);
								break;

							case "RET": //retrieve item and call the next itens (RNodes or Points) Node
								retrieveTree("ret placeholder");
								break;
							
							case "SER": //keep search received to a especific node -> SER#
								searchArea("ser placeholder");
								break;
							
							case "TRN": //TRANSFER RNODE -> TRN#KEY RNODE#KEY FATHER#IP FATHER#X1#Y1#T1#X2#Y2#T2
								
								break;

							case "TCH": //TRANSFER CHILD -> TCH#KEY CHILD#X1#Y1#T1#X2#Y2#T2

								break;

							case "TPO": //TRANSFER POINT -> TPO#KEY POINT#KEY TRAJ#X#Y#T 

								break;

							case "UFA": //UPDATE FATHER -> UFA#KEY DO FATHER#KEY DO RNODE QUE MUDOU#NOVO IP

								break;

							case "UCH": //UPDATE CHILD -> UCH#KEY DO CHILD#NOVO IP DO FATHER DESTE CHILD

								break;

							case "EXIT":
								System.out.println("Consumer: entrou no case EXIT");
								code = "exit";
								spawnExit();
								break;
							
							default:
								break;
						}
					} catch (Exception e) {
						//TODO: handle exception
					}
				}
				System.out.println("Consumer: fim do while");
			}
		});
		tConsumer.start();//t.start();
	}

//===============================================================
//CONSUMER FUNCTIONS
//===============================================================

	void addInit(int x, int y, int t,int trajKey){//MASTER
		//ADI#p.x#p.y#p.t#p.trajKey
		System.out.println("inicio do addInit");
		addPoint(rootKey, Config.getKey(), trajKey,x,y,t);
		
	}
	void addPoint(int rnodeKey,int pointKey,int trajKey,int x,int y,int t){
		//segue a lógica de inserção -> ADD#RNODE KEY#P.KEY#p.trajKey#p.x#p.y#p.t
		System.out.println("inicio do addPoint");
		Point p = new Point(pointKey,trajKey,this.ip,x,y,t);
		boolean end = false;
		RNode rnode = rnodeMap.get(rnodeKey);
		while(!end){
			rnode.mbr.expand(x,y,t);
			if(rnode.children.size() < 1){//node == leaf
				rnode.addPoint(p);
				end = true;
				overflow(rnode);//@@@@@@@@@@@@@IMPLEMENTAR
			}
			else{//non leaf
				int i = rnode.findIndex(x,y,t);//@@@@@@@@@@@implementar
				rnode = rnodeMap.get(rnode.children.get(i).key);
				if(rnode.nodeIp != this.ip){//this rnode is stored in another Node
					end = true;
					try{
						sendMsg("ADD#",rnode.nodeIp,8000);//@@@@@@@@@@@@@@@@@@@@COMPLETAR
					}catch(Exception e){
						
					}
				}
			}

		}
	}

	void overflow(RNode rnode){
		//implementar
		if(rnode.points != null && rnode.points.size() > Config.M){//leaf overflow
			RNode n1 = new RNode(Config.getKey(),rnode.nodeIp,0,0,0,0,0,0,rnode.fatherKey,rnode.fatherIp);
			RNode n2 = new RNode(Config.getKey(),rnode.nodeIp,0,0,0,0,0,0,rnode.fatherKey,rnode.fatherIp);
			int[] indexes = rnode.splitIndexes();//verificar se isso aqui da certo
			n1.addPoint(rnode.points.get(indexes[0]));
			n2.addPoint(rnode.points.get(indexes[1]));
			for(int i = 0 ; i < rnode.points.size(); i++){
				if(i != indexes[0] && i != indexes[1]){
					if((n1.points.size() < Config.M/2 && n2.areaDiff(rnode.points.get(i)) > n1.areaDiff(rnode.points.get(i))) || n2.points.size() >= Config.M/2){
						n1.addPoint(rnode.points.get(i));
						n1.mbr.expand(rnode.points.get(i));
					}
					else{
						n2.addPoint(rnode.points.get(i));
						n2.mbr.expand(rnode.points.get(i));
					}
				}
			}
			this.rnodeMap.put(n1.key,n1);
			if(this.countOverflow++ % 2 == 0){//verificar isso //fica local
				this.rnodeMap.put(n2.key,n2);
			}
			else{
				n2.nodeIp = this.getRandomIp();//implementar
				try{
					transferRNode(n2);
				}catch(Exception e){

				}
			}
			if(rnode.fatherIp != null){//non root
				this.rnodeMap.remove(rnode.key);
				if(rnode.fatherIp == this.ip){//father is here
					RNode father = this.rnodeMap.get(rnode.fatherKey);
					int i;
					for(i = 0 ; i < father.children.size() ; i++){
						if(rnode.key == father.children.get(i).key){
							father.children.remove(i);
							break;
						}
					}
					father.addChild(n1);
					father.addChild(n2);
					overflow(father);
				}
				else{//father not here
					try{
						sendMsg("OVF#",rnode.fatherIp,8000);// @@@@@@@@@@@@ IMPLEMENTAR
					}catch(Exception e){

					}
				}
			}
			else{//node == root
				rnode.points.clear();
				n1.fatherIp = rnode.nodeIp;//é o root, mas vou por assim mesmo
				n2.fatherIp = rnode.nodeIp;
				rnode.addChild(n1);
				rnode.addChild(n2);
			}
		}
	}

	void oveflowUpdate(int fatherKey,int childKey,Child n1,Child n2){
		RNode father = this.rnodeMap.get(fatherKey);
		int i;
		for(i = 0 ; i < father.children.size() ; i++){
			if(rnode.key == father.children.get(i).key){
				father.children.remove(i);
				break;
			}
		}
		father.children.remove(i);
		father.addChild(n1);
		father.addChild(n2);
		overflow(father);
	}

	String getRandomIp(){//@@@@@@@@@@@@@@@@@@@ IMPLEMENTAR
		return "10.0.0.1";
	}


	void retrieveInit(){//MASTER
		//inicia a devolução de todos os nodes começando pelo root e spawnando todos os filhos do root e assim por diante
	}
	
	void searchInit(String areaStr){//MASTER
		//encontra a area/volume pesquisado e começa a pesquisa pelo root
	}

	void addNode(String ipStr){ //ADD#RNODE KEY#P.KEY#p.trajKey#p.x#p.y#p.t
		nodeList.add(ipStr);
		System.out.println("node added");
	}

	

	void retrieveTree(String nodeStr){
		//
	}
	void searchArea(String areaRNodeStr){
		//encontra o RNode que está sendo pesquisado e a area de pesquisa e prossegue com a pesquisa
	}

	void spawnExit(){
		//mandar msg para todos de EXIT
	}

//===============================================================
//AUX FUNCTIONS
//===============================================================
	void sendMsg(String msg,String receiverIp, int port) throws Exception{
		boolean scanning=true;
		int i = 0;
		while(scanning) {
			try {
				//socketChannel.open(hostname, port);
				Socket s = new Socket(receiverIp,port);
				DataOutputStream dout;
				dout=new DataOutputStream(s.getOutputStream());
				dout.writeUTF(msg);
				dout.flush();
				dout.close();
				s.close();
				System.out.println("SUCCESS| reicever: " + receiverIp + "port: "+ port + " msg: " + msg);
				try {
					Thread.sleep(250);// *********** ANALISAR QUANTO TEMPO DEIXAR AQUI DE DELAY
				} catch (Exception e) {
					//TODO: handle exception
				}
				scanning=false;
			} catch(Exception e) {
				System.err.println(e);
				System.out.println("ERROR| reicever: " + receiverIp + "port: "+ port + " msg: " + msg);
				if(i>=3){
					scanning = false;
					System.out.println("terminating");//se falhar 3 vezes (i>=3) a mensagem é dropada
				}
				else{
					try {
						i++;
						Thread.sleep(1000);//ao falahar, espera 1 segundo para tentar novamente
					} catch(InterruptedException ie){
						ie.printStackTrace();
					}
				}
				
			} 
		}
	}



	void transferRNode(RNode r) throws Exception{//este rnode precisa estar com o nodeip já atualizado para onde será transferido
		sendMsg("TRN#" + r.key + "#" + r.fatherKey + "#" + r.fatherIp + "#" + r.mbr.x1 + "#" + r.mbr.y1 + "#" + r.mbr.t1 + "#" + r.mbr.x2 + "#" + r.mbr.y2 + "#" + r.mbr.t2,r.nodeIp,8000);//TRANSFER RNODE -> TRN#KEY RNODE#KEY FATHER#IP FATHER#X1#Y1#T1#X2#Y2#T2
		//PRECISA AVISAR O FATHER QUE O NODE FOI TRANSFERIDO ok(falta implementar a function em UFA# nao ta chamando ninguem)
		if(r.fatherIp == this.ip){//quer dizer que o pai eh daqui
			for(int i=0;i < rnodeMap.get(r.fatherKey).children.size();i++){
				if(rnodeMap.get(r.fatherKey).children.get(i).key == r.key){
					rnodeMap.get(r.fatherKey).children.get(i).nodeIp = r.nodeIp;
					i = rnodeMap.get(r.fatherKey).children.size();
				}
			}//provavel que vai ter que fazer isso na function UFA#
			
		}
		else{//o pai eh de outro node
			sendMsg("UFA#" + r.fatherKey + "#" + r.key + "#" + r.nodeIp,r.fatherIp,8000);//UPDATE FATHER -> UFA#KEY DO FATHER#KEY DO RNODE QUE MUDOU#NOVO IP
		}
		
		if(r.points.size()==0){//non leaf
			for(int i = 0;i<r.children.size();i++){
				Child ch = r.children.get(i);
				sendMsg("TCH#" + ch.key + "#" + ch.mbr.x1 + "#" + ch.mbr.y1 + "#" + ch.mbr.t1 + "#" + ch.mbr.x2 + "#" + ch.mbr.y2 + "#" + ch.mbr.t2,r.nodeIp,8000);//TRANSFER CHILD -> TCH#KEY CHILD#X1#Y1#T1#X2#Y2#T2
				//DONE:AQUI PRECISA ATUALIZAR O RNODE QUE CADA CHILD REPRESENTA PORQUE O FATHER DELES APONTAM PARA UM LUGAR ERRADO
				if(ch.nodeIp == this.ip){//quer dizer que o child esta aqui neste node
					rnodeMap.get(ch.key).fatherIp = r.nodeIp;
				}
				else{//o child esta em outro node
					sendMsg("UCH#" + ch.key + "#" + r.nodeIp,ch.nodeIp,8000);//UPDATE CHILD -> UCH#KEY DO CHILD#NOVO IP DO FATHER DESTE CHILD
				}

			}
		}
		else{//leaf
			for(int i = 0;i<r.points.size();i++){
				Point p = r.points.get(i);
				sendMsg("TPO#" + p.key + "#" + p.trajKey + "#" + p.x + "#" + p.y + "#" + p.t,r.nodeIp,8000);//TRANSFER POINT -> TPO#KEY POINT#KEY TRAJ#X#Y#T
				//DONE: NAO SEI SE TA CERTO
				this.pointMap.remove(p.key);
			}
		}
		this.rnodeMap.remove(r.key);//verificar isso provavel que precise de um if pra ver se está no map

	}

	String getHostIp(){
		final ArrayList<String> commands = new ArrayList<String>();
        commands.add("/bin/bash");
        commands.add("-c");
        commands.add("hostname -I | awk '{print $1}'");
		BufferedReader br = null;
		try {
			final ProcessBuilder p = new ProcessBuilder(commands);
			final Process process = p.start();
			final InputStream is = process.getInputStream();
			final InputStreamReader isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			String line = br.readLine();
			return line;
		} catch (Exception e) {
		}
		return null;
	}
}