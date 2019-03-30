import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.io.IOException;

public class Client implements Runnable{

    private Socket clientsocket;
    private DataOutputStream out;
    private DataInputStream in;
    private Server server;
    private int sessionID;
    private byte[] packet = new byte[4];
    private boolean condition = true;

    Client(ServerSocket serverSocket, int clientID, Server server){
        try {
            clientsocket = serverSocket.accept();

            in = new DataInputStream(clientsocket.getInputStream());
            out = new DataOutputStream(clientsocket.getOutputStream());

            this.server = server;
            sessionID = clientID;

            sendPacket(0,0,0,0);
            System.out.println("Polaczono z klientem " + sessionID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // funkcja ktora generuje pakiet w serwerze
    private byte[] generatePacket(int operation, int answer, int number, int attempt){

        byte[] byteArray = new byte[4];

        byteArray[0] = (byte) ((operation & 0b00011111) << 3);
        byteArray[0] = (byte) (byteArray[0] | (byte) ((answer & 0b00001110) >> 1));

        byteArray[1] = (byte) ((answer & 0b00000001) << 7);
        byteArray[1] = (byte) (byteArray[1] | (byte) ((sessionID & 0b00000111) << 4));
        byteArray[1] = (byte) (byteArray[1] | (byte) ((number & 0b11110000) >> 4));

        byteArray[2] = (byte) ((number & 0b00001111) << 4);
        byteArray[2] = (byte) (byteArray[2] | (byte) ((attempt & 0b11110000) >> 4));

        byteArray[3] = (byte) ((attempt & 0b00001111) << 4);

        return byteArray;
    }

    void sendPacket(int operation, int answer, int number, int attempt){
        try {
            out.write(generatePacket(operation, answer, number, attempt),0,4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void end(){
        try {
            clientsocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            condition = false;
        }
    }

    private void execute(int operation, int answer, int id, int number)
    {
        if(operation == 2 && answer == 2){
            server.numberOf_attempts(id, number,this);
        }
        if(operation == 3 && answer == 0)
        {
            server.check(id, number,this);
        }
        if(operation == 7 && answer==7)
        {
            System.out.println("Klient "+ sessionID + " rozlaczyl sie.");
            end();
        }
    }

    private void decodePacket(byte[] byteArray){

        int operation, answer, id, number;

        operation = (byteArray[0] & 0b11111000) >> 3;
        answer = ((byteArray[0] & 0b00000111) << 1) | ((byteArray[1] & 0b10000000) >> 7);
        id = (byteArray[1] & 0b01110000) >> 4;
        number = ((byteArray[1] & 0b00001111) << 4) | ((byteArray[2] & 0b11110000) >> 4);

        if(id == sessionID){
            execute(operation, answer, id, number);
        }
        else{
            System.out.println("Odebrano niepoprawny komunikat od klienta " + id);
        }

        //System.out.println(operation);
        //System.out.println(answer);
        //System.out.println(id);
        //System.out.println(number);
    }

    public void run(){

        int length;

        while(condition){
            try {
                length = in.read(packet);
                if (length == -1){
                    System.out.println("Klient " + sessionID + " rozlaczyl sie.");
                    condition = false;
                    break;
                } else {
                    decodePacket(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
