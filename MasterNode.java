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

	Thread tConsumer;
	static Queue<String> queue = new LinkedList<String>();//queue para produtor e consumidor das requisicoes trabalharem
	HashMap<Integer, Point> pointMap = new HashMap<Integer, Point>();//KV para os pontos de trajetorias armazenadas no Node, mapeados pelo
	HashMap<Integer, RNode> rnodeMap = new HashMap<Integer, RNode>();//KV para os RNodes da RTREE armazenadas no Node
	List<String> nodeList = new LinkedList<String>();//exclusivo do Master, aponta para todos os nodes que sao incluidos no sistema (no caso armazena o ip de cada node)
	static Semaphore mutex1 = new Semaphore(0);//semaforos para a concorrencia entre produtor e consumidor
	static Semaphore mutex = new Semaphore(1);//^
	ServerSocket ss;


	MasterNode(){
		this.ip = getHostIp();
		rootKey = RandomAux.getKey();
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
            //String[] cmd = code.split("##");
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
					String[] cmd = code.split("##");
					cmd[0] = cmd[0].toUpperCase();
					try{
						switch (cmd[0]) {
							case "ADI": //MASTER| -> ADI##p.x##p.y##p.t##p.trajKey##p.backwardKey@Ip##user.IP
								System.out.println("add");
								addInit(Integer.parseInt(cmd[1]),Integer.parseInt(cmd[2]),Integer.parseInt(cmd[3]),Integer.parseInt(cmd[4]),cmd[5],cmd[6]);
								break;
							
							case "RTI": //MASTER| INIT OF A RETRIEVE ->
								retrieveInit();
								break;

							case "SRI": //MASTER| search init ->
								searchInit("sri placeholder");
								break;

							case "ADN": //MASTER| add node -> ADN## NODE IP
								addNode(cmd[1]);
								break;

							case "ADD": //segue a lógica de inserção -> ADD##RNODE KEY##p.x##p.y##p.t##p.trajKey##p.backwardKey@Ip##user.IP
								System.out.println("add");
								addPoint("add placeholder");
								break;

							case "RET": //retrieve item and call the next itens (RNodes or Points) Node
								retrieveTree("ret placeholder");
								break;
							
							case "SER": //keep search received to a especific node -> SER##
								searchArea("ser placeholder");
								break;

							case "BWU": //BACKWARD UPDATE -> BWU##KEY DO POINT(LÁ)##KEY@IP PARA POR NO FOWARD DO POINT A SER ATUALIZADO (KEY)
								
								break;
							
							case "TRN": //TRANSFER RNODE -> TRN##KEY RNODE##KEY FATHER##IP FATHER##X1##Y1##T1##X2##Y2##T2
								
								break;

							case "TCH": //TRANSFER CHILD -> TCH##KEY CHILD##X1##Y1##T1##X2##Y2##T2

								break;

							case "TPO": //TRANSFER POINT -> TPO##KEY POINT##X##Y##T 

								break;

							case "UFA": //UPDATE FATHER -> UFA##KEY DO FATHER##KEY DO RNODE QUE MUDOU##NOVO IP

								break;

							case "UCH": //UPDATE CHILD -> UCH##KEY DO CHILD##NOVO IP DO FATHER DESTE CHILD

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

	void addInit(int x, int y, int t,int trajKey, String backward, String userIP){//MASTER
		//ADI##p.x##p.y##p.t##p.trajKey##p.backwardKey@Ip##user.IP
		// ->
		System.out.println("inicio do addInit");
		RNode root = rnodeMap.get(rootKey);
		System.out.println("if(root.points.size() && root.children.size())");
		System.out.println(root.points.size() + " " + root.children.size());
		if(root.mbr.volume() == 0){//nada foi inserido ainda
		//if(root.points.isEmpty() && root.children.isEmpty()){//
			System.out.println("if(root.points.isEmpty() && root.children.isEmpty())");
			Point point = new Point(RandomAux.getKey(),trajKey,this.ip,x,y,t,backward);
			root.addPoint(point);//como está vazio não precisa de verificação
			this.pointMap.put(point.key,point);
			//como foi o primeiro point de todos não precisa avisar o backward
			try{
				System.out.println("sM bck 1");
				sendMsg("BCK##" + point.key + "@" + ip,userIP,5000);//PORTA PADRAO PARA RESPONDER APENAS O ADD
			}catch(Exception e){
				
			}
		}
		else{//já há algo inserido
			if(root.points.size()>0){//root é leaf e o point é inserido no root
				Point point = new Point(RandomAux.getKey(),trajKey,this.ip,x,y,t,backward);
				root.addPoint(point);
				this.pointMap.put(point.key,point);
				if(root.points.size() > Parameters.M){//overflow
					//criar dois RNodes, um mantem no Master (rnodes.put?)
					//e o outro verifica-se caso tenha outros nodes no sitema
					//caso tenha transfere um node
					//depois da clear na lista points 
					//e adiciona os dois child na lista children do root
					int volume = -1;
					int p1Index=0;
					int p2Index=1;
					for(int i=0;i < root.points.size()-1;i++){//calcula o volume ponto a ponto para achar a combinação dos dois pontos com maior volume
						for(int j=i+1;j < root.points.size();j++){
							int volumeTemp = root.points.get(i).volume(root.points.get(j));
							if(volumeTemp > volume){
								volume = volumeTemp;
								p1Index = i;
								p2Index = j;
							}
						}
					}
					RNode r1 = new RNode(RandomAux.getKey(),this.ip,0,0,0,0,0,0,rootKey,this.ip);
					RNode r2 = new RNode(RandomAux.getKey(),this.ip,0,0,0,0,0,0,rootKey,this.ip);
					r1.addPoint(root.points.get(p1Index));
					r2.addPoint(root.points.get(p2Index));
					for(int i = 0; i<root.points.size();i++){
						if(i != p1Index && i !=p2Index){
							int volume1 = r1.points.get(0).volume(root.points.get(i));
							int volume2 = r2.points.get(0).volume(root.points.get(i));
							if((volume1 > volume2 && r2.points.size() <= Parameters.M/2) || r1.points.size() > Parameters.M/2){//verificar isso
								r2.addPoint(root.points.get(i));
							}
							else{
								r1.addPoint(root.points.get(i));
							}
						}
					}
					r1.adjustMBR();
					r2.adjustMBR();
					root.points.clear();
					root.addChild(r1);
					this.rnodeMap.put(r1.key,r1);
					if(nodeList.isEmpty()){//só tem o master
						root.addChild(r2);
						this.rnodeMap.put(r2.key,r2);
					}
					else{//tem mais algum node, entao tera que decidir para onde vai
						int choosen = countPoints % nodeList.size() + 1;
						if(choosen == 0){//o escolhido foi o proprio master
							root.addChild(r2);
							this.rnodeMap.put(r2.key,r2);
						}
						else{//O ESCOLHIDO EH ALGUM OUTRO NODE
							r2.nodeIp = nodeList.get(choosen-1);//JÁ ATUALIZANDO O IP DO RNODE PARA A FUNCAO QUE TRANSFERE JA ENVIAR A MSG PARA O IP
							root.addChild(r2);
							try{
								transferRNode(r2);//iMPLEMENTACAO SEM TESTE
							}catch(Exception e){
								
							}
							
							if(r2.points.contains(point)){//significa que o ponto inserido foi embora no overflow para outro node
								if(backward != "null" && backward != "NULL"){
									String[] backwardRef = backward.split("@");//[0] = key, [1] = IP
									if(backwardRef[1].compareTo(this.ip)==0){//significa que o point anterior está armazenado neste node
										Point backPoint = this.pointMap.get(Integer.parseInt(backwardRef[0]));
										backPoint.forwardAddr = point.key + "@" + nodeList.get(choosen-1);//padrão KEY@IP
									}
									else{
										try{
											System.out.println("sM bwu 1");
											sendMsg("BWU##" + backwardRef[0] + "##"+ point.key + "@" + nodeList.get(choosen-1),backwardRef[1],8000);//BACKWARD UPDATE -> BWU##KEY DO POINT(LÁ)##KEY@IP PARA POR NO FOWARD DO POINT A SER ATUALIZADO (KEY)
										}
										catch(Exception e){
											
										}
									}
								}
							}
							else{
								if(backward != "null" && backward != "NULL"){
									String[] backwardRef = backward.split("@");//[0] = key, [1] = IP
									if(backwardRef[1].compareTo(this.ip)==0){//significa que o point anterior está armazenado neste node
										Point backPoint = this.pointMap.get(Integer.parseInt(backwardRef[0]));
										backPoint.forwardAddr = point.key + "@" + this.ip;//padrão KEY@IP
									}
									else{
										try{
											System.out.println("sM bwu 2");
											sendMsg("BWU##" + backwardRef[0] + "##"+ point.key + "@" + this.ip,backwardRef[1],8000);//BACKWARD UPDATE -> BWU##KEY DO POINT(LÁ)##KEY@IP PARA POR NO FOWARD DO POINT A SER ATUALIZADO (KEY)
										}catch(Exception e){

										}
									}
								}
							}
						}
					}
				}
				else{//sem overflow
					if(backward != "null" && backward != "NULL"){
						String[] backwardRef = backward.split("@");//[0] = key, [1] = IP
						System.out.println("backwardref[1]: "+ backwardRef[1]+" this.ip: "+this.ip);
						System.out.println("backwardref[1].length(): "+backwardRef[1].length()+" this.ip.length(): "+this.ip.length());
						//if(backwardRef[1] == this.ip){//significa que o point anterior está armazenado neste node
						if(backwardRef[1].compareTo(this.ip)==0){
							System.out.println("entrou aqui no if");
							Point backPoint = this.pointMap.get(Integer.parseInt(backwardRef[0]));
							backPoint.forwardAddr = point.key + "@" + this.ip;//padrão KEY@IP
						}
						else{
							try{
								System.out.println("sM bwu 3");
								sendMsg("BWU##" + backwardRef[0] + "##"+ point.key + "@" + this.ip,backwardRef[1],8000);//BACKWARD UPDATE -> BWU##KEY DO POINT(LÁ)##KEY@IP PARA POR NO FOWARD DO POINT A SER ATUALIZADO (KEY)
							}catch(Exception e){

							}
						}
						
						System.out.println("saiu do if else do bwu 3");
					}
					try{
						System.out.println("sM bck ROOT == LEAF SEM OVERFLOW");
						sendMsg("BCK##" + point.key + "@" + ip,userIP,5000);//PORTA PADRAO PARA RESPONDER APENAS O ADD
					}catch(Exception e){
						
					}
				}
			}
			else{//AQUI É CASO O ROOT NAO É LEAF
				System.out.println("caso o root nao eh leaf");
				boolean end = false;
				RNode choosenRNode = root;
				while(!end){
					int volumeExp = choosenRNode.children.get(0).mbr.expandVolValue(x,y,t);
					int index = 0;
					for(int i = 1; i < root.children.size();i++){
						int volumeTemp = choosenRNode.children.get(i).mbr.expandVolValue(x,y,t);
						if(volumeTemp < volumeExp){
							volumeExp = volumeTemp;
							index = i;
						}
					}//ao final deste for será escolhido em qual dos childs sera inserido o point
					choosenRNode.children.get(index).mbr.expand(x,y,t);
					if(choosenRNode.children.get(index).nodeIp == this.ip){//significa que este child esta neste mesmo node
						RNode childRNode = rnodeMap.get(choosenRNode.children.get(index).key);
						if(childRNode.points.size() == 0){//significa que este child tambem eh non leaf
							choosenRNode = childRNode;
						}
						else{//significa que este child é leaf e será inserido nele o point
							end = true;
							Point point = new Point(RandomAux.getKey(),trajKey,this.ip,x,y,t,backward);
							childRNode.addPoint(point);
							this.pointMap.put(point.key,point);
							//fazer a verificacao de OVERFLOW AQUI
							//fazer update do backward BWU## e responder para o client o BCK## (ACHO)
						}
					}
					else{//significa que o child escolhido está em outro node e precisa então passar adiante a requisição
						end = true;
						try{
							System.out.println("sM add 1");
							sendMsg("ADD##",choosenRNode.children.get(index).nodeIp,8000);//ADD##RNODE KEY##p.x##p.y##p.t##p.trajKey##p.backwardKey@Ip##user.IP
							//COMPLETAR AQUI
						}catch(Exception e){

						}
						
					}
					
				}
			}

		}
		System.out.println("antes do root.mbr.expand");
		root.mbr.expand(x,y,t);
		countPoints++;
		System.out.println("fim do addinit");
	}


	void retrieveInit(){//MASTER
		//inicia a devolução de todos os nodes começando pelo root e spawnando todos os filhos do root e assim por diante
	}
	
	void searchInit(String areaStr){//MASTER
		//encontra a area/volume pesquisado e começa a pesquisa pelo root
	}

	void addNode(String ipStr){
		nodeList.add(ipStr);
		System.out.println("node added");
	}

	void addPoint(String pointStr){
		//quebrar a str nos numeros e adicionar
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
		sendMsg("TRN##" + r.key + "##" + r.fatherKey + "##" + r.fatherIp + "##" + r.mbr.x1 + "##" + r.mbr.y1 + "##" + r.mbr.t1 + "##" + r.mbr.x2 + "##" + r.mbr.y2 + "##" + r.mbr.t2,r.nodeIp,8000);//TRANSFER RNODE -> TRN##KEY RNODE##KEY FATHER##IP FATHER##X1##Y1##T1##X2##Y2##T2
		//PRECISA AVISAR O FATHER QUE O NODE FOI TRANSFERIDO ok(falta implementar a function em UFA## nao ta chamando ninguem)
		if(r.fatherIp == this.ip){//quer dizer que o pai eh daqui
			for(int i=0;i < rnodeMap.get(r.fatherKey).children.size();i++){
				if(rnodeMap.get(r.fatherKey).children.get(i).key == r.key){
					rnodeMap.get(r.fatherKey).children.get(i).nodeIp = r.nodeIp;
					i = rnodeMap.get(r.fatherKey).children.size();
				}
			}//provavel que vai ter que fazer isso na function UFA##
			
		}
		else{//o pai eh de outro node
			sendMsg("UFA##" + r.fatherKey + "##" + r.key + "##" + r.nodeIp,r.fatherIp,8000);//UPDATE FATHER -> UFA##KEY DO FATHER##KEY DO RNODE QUE MUDOU##NOVO IP
		}
		
		if(r.points.size()==0){//non leaf
			for(int i = 0;i<r.children.size();i++){
				Child ch = r.children.get(i);
				sendMsg("TCH##" + ch.key + "##" + ch.mbr.x1 + "##" + ch.mbr.y1 + "##" + ch.mbr.t1 + "##" + ch.mbr.x2 + "##" + ch.mbr.y2 + "##" + ch.mbr.t2,r.nodeIp,8000);//TRANSFER CHILD -> TCH##KEY CHILD##X1##Y1##T1##X2##Y2##T2
				//AQUI PRECISA ATUALIZAR O RNODE QUE CADA CHILD PORQUE O FATHER DELES APONTAM PARA UM LUGAR ERRADO
				if(ch.nodeIp == this.ip){//quer dizer que o child esta aqui neste node
					rnodeMap.get(ch.key).fatherIp = r.nodeIp;
				}
				else{//o child esta em outro node
					sendMsg("UCH##" + ch.key + "##" + r.nodeIp,ch.nodeIp,8000);//UPDATE CHILD -> UCH##KEY DO CHILD##NOVO IP DO FATHER DESTE CHILD
				}
			}
		}
		else{
			for(int i = 0;i<r.points.size();i++){
				Point p = r.points.get(i);
				sendMsg("TPO##" + p.key + "##" + p.x + "##" + p.y + "##" + p.t,r.nodeIp,8000);//TRANSFER POINT -> TPO##KEY POINT##X##Y##T
				//*********************************TEM MAIS INFO PRA POR AQUI
			}
		}
		this.rnodeMap.remove(r.key);

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