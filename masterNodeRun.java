class masterNodeRun{
	public static void main(String[] args) throws Exception{
		MasterNode masterNode;
		masterNode = new MasterNode();
		System.out.println("ip: " + masterNode.ip);
		masterNode.MasterConsumer();
		masterNode.MasterProducer();
	}
}