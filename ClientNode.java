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

class ClientNode{
	String ip;
	String masterIp = "10.0.0.1";//NO GERAL VAI SER O H1 QUE TEM ESTE IP QUE SERA O MASTER

	static Queue<String> queue = new LinkedList<String>();
	static Semaphore mutex1 = new Semaphore(0);
	static Semaphore mutex = new Semaphore(1);
	ServerSocket ss;

	ClientNode(){
		this.ip = getHostIp();
	}

	void ClientProducer() throws Exception{
        System.out.println("Start Producer");
		mutex.acquire();//APENAS PARA TESTAR COM A FUNÇÃO AUXILIAR PARA GERAR A ADICAO DE ALGUNS ITENS E DEPOIS PROCURA
        queue.offer("SPA##0");
        mutex.release();  //releasing After Production ;
        mutex1.release();
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


	void ClientConsumer() throws Exception{
		System.out.println("Start Consumer");
		Thread t = new Thread(new Runnable() {
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
							case "SPA": 
								System.out.println("SPAWNING");
								auxSpawn();
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

	void auxSpawn(){
		for(int i = 0; i < 6; i++){
			//CONTINUAR DAQUI
		}
	}

	void spawnExit(){

	}

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