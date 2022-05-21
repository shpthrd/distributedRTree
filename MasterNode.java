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
            if(code.startsWith("exit")){
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
		Thread t = new Thread(new Runnable() {
			@Override
			public void run(){
				//System.out.println("starting new thread");//VAI DAR MERDA
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

							case "ADD": //segue a lógica de inserção -> ADD##POINT
								System.out.println("add");
								addPoint("add placeholder");
								break;

							case "RET": //retrieve item and call the next itens (RNodes or Points) Node
								retrieveTree("ret placeholder");
								break;
							
							case "SER": //keep search received to a especific node -> SER##
								searchArea("ser placeholder");
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
		t.start();
	}

//===============================================================
//CONSUMER FUNCTIONS
//===============================================================

	void addInit(int x, int y, int t,int trajKey, String backward, String userIP){//MASTER
		//ADI##p.x##p.y##p.t##p.trajKey##p.backwardKey@Ip##user.IP
		// ->
		RNode root = rnodeMap.get(rootKey);
		if(root.mbr.volume() == 0){//nada foi inserido ainda
			Point point = new Point(RandomAux.getKey(),trajKey,this.ip,x,y,t,backward);
			root.addPoint(point);//como está vazio não precisa de verificação
			this.pointMap.put(point.key,point);
			//como foi o primeiro point de todos não precisa avisar o backward
			try{
				sendMsg("BCK##" + point.key + "@" + ip,userIP,5000);//PORTA PADRAO PARA RESPONDER APENAS O ADD
			}catch(Exception e){
				
			}
		}
		else{//já há algo inserido
			if(!root.points.isEmpty()){//root é leaf
				Point point = new Point(RandomAux.getKey(),trajKey,this.ip,x,y,t,backward);
				root.addPoint(point);
				this.pointMap.put(point.key,point);
				if(root.points.size() > Parameters.M){//overflow
					//criar dois RNodes, um mantem no Master (rnodes.put?)
					//e o outro verifica-se caso tenha outros nodes no sitema
					//caso tenha transfere um node
					//depois da clear na lista points 
					//e adiciona os dois child na lista children do root
				}
				else{//sem overflow
					String[] backwardRef = backward.split("@");//[0] = key, [1] = IP
					if(backwardRef[1] == this.ip){//significa que o point anterior está armazenado neste node
						Point backPoint = this.pointMap.get(backwardRef[0]);
						backPoint.forwardAddr = point.key + this.ip;//padrão KEY@IP
					}
				}
			}
		}
		root.mbr.expand(x,y,t);
		countPoints++;
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
				System.out.println("success");
				try {
					Thread.sleep(250);// *********** ANALISAR QUANTO TEMPO DEIXAR AQUI DE DELAY
				} catch (Exception e) {
					//TODO: handle exception
				}
				scanning=false;
			} catch(Exception e) {
				System.err.println(e);
				System.out.println("Falha na msg para: " + receiverIp + " msg: " + msg);
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