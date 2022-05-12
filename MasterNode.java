import java.util.HashMap;
import java.util.Scanner;
import java.util.List;
import java.util.LinkedList;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Queue;
import java.util.concurrent.Semaphore;

class MasterNode{
    String ip;
    
    
    static Queue<String> queue = new LinkedList<String>();//queue para produtor e consumidor das requisicoes trabalharem
    HashMap<Integer, Point> pointMap = new HashMap<Integer, Point>();//KV para os pontos de trajetorias armazenadas no Node, mapeados pelo
    HashMap<Integer, RNode> rnodeMap = new HashMap<Integer, RNode>();//KV para os RNodes da RTREE armazenadas no Node
    List<String> nodeList = new LinkedList<String>();//exclusivo do Master, aponta para todos os nodes que sao incluidos no sistema (no caso armazena o ip de cada node)
    static Semaphore mutex1 = new Semaphore(0);//semaforos para a concorrencia entre produtor e consumidor
    static Semaphore mutex = new Semaphore(1);//^

    MasterNode(){
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            //TODO: handle exception
        }
        System.out.println("MasterNode criado");
    }

}