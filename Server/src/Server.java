import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

public class Server {

    private ServerSocket serverSocket;
    private Client client1;
    private Client client2;
    private int sessionID_1;
    private int sessionID_2;
    private int random_number;
    private int attempt, attempt_1 = 0, attempt_2 = 0;


    // w konstruktorze tworzymy serwer socket
    Server(int port){
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // funkcja ktora generuje identyfikatory sesji (sessionID_1 i sessionID_2) w serwerze (zakres od 1 do 7)
    private void generate_sessionID(){

        Random random = new Random();
        sessionID_1 = random.nextInt(7) + 1;
        sessionID_2 = random.nextInt(7) + 1;
        if(sessionID_1==sessionID_2)
        {
            sessionID_1=random.nextInt(7)+1;
        }
    }

    // funkcja ktora generuje liczbe ktora bedziemy losowali (zakres od 0 do 255)
    private void generate_random_number(){

        Random random = new Random();
        random_number = random.nextInt(256);
    }

    void numberOf_attempts(int id, int number, Client client){

        if(id == sessionID_1){
            attempt_1 = number;
        }
        if(id == sessionID_2){
            attempt_2 = number;
        }
        if(attempt_1 != 0 && attempt_2 != 0){
            attempt = (attempt_1 + attempt_2) / 2;
            attempt_1 = attempt;
            attempt_2 = attempt;
        }
    }

    void check(int id, int tip, Client client)
    {
        if(tip == random_number)
        {
            if(id==sessionID_1)
            {
                client.sendPacket(7, 1, 0, 0);


            }
            if(id==sessionID_2)
            {
                client.sendPacket(7,1,0,0);
            }


        }
        else if(tip < random_number)
        {
            if(id == sessionID_1){
                attempt_1--;
                client.sendPacket(3,1,0,attempt_1);
                if(attempt_1==0)
                {
                    client.sendPacket(7,2,0,0);
                }
            }
            if(id == sessionID_2){
                attempt_2--;
                client.sendPacket(3,1,0,attempt_2);
                if(attempt_2==0)
                {
                    client.sendPacket(7,2,0,0);
                }
            }
        }
        else if(tip > random_number)
        {
            if(id == sessionID_1){
                attempt_1--;
                client.sendPacket(3,4,0,attempt_1);
                if(attempt_1==0)
                {
                    client.sendPacket(7,2,0,0);
                }
            }
            if(id == sessionID_2){
                attempt_2--;
                client.sendPacket(3,4,0,attempt_2);
                if(attempt_2==0)
                {
                    client.sendPacket(7,2,0,0);
                }
            }
        }
    }

    void start(){

        generate_sessionID();
        generate_random_number();

        Client client1 = new Client(serverSocket, sessionID_1,this);
        Client client2 = new Client(serverSocket, sessionID_2,this);

        Thread t1 = new Thread(client1);
        Thread t2 = new Thread(client2);

        client1.sendPacket(2,0,0, 0);
        client2.sendPacket(2,0,0, 0);

        t1.start();
        t2.start();

        t1.interrupt();
        t2.interrupt();

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {

        Server server = new Server(1234);
        server.start();
    }
}
