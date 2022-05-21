import java.util.Random;

class RandomAux{
    static Random random = new Random();

    RandomAux(){

    }
	
    static int getKey(){
        return random.nextInt(100000);
    }
}