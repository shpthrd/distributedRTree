class nodeRun{
	public static void main(String[] args) throws Exception{
		Node node;
		node = new Node();
		System.out.println("ip: " + node.ip);
		node.Consumer();
		node.Producer();

	}
}