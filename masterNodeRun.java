import java.util.ArrayList;
class masterNodeRun{
	public static void main(String[] args) throws Exception{
		MasterNode masterNode;
		masterNode = new MasterNode();
		System.out.println("ip: " + masterNode.ip);
		masterNode.MasterConsumer();
		masterNode.MasterProducer();
		masterNode.tConsumer.join();
		ArrayList<RNode> rnodeList = new ArrayList<RNode>(masterNode.rnodeMap.values());
		for(int i = 0; i < masterNode.rnodeMap.size();i++){
			rnodeList.get(i).print();
		}
	}
}