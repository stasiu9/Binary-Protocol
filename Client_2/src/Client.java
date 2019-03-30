import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable{

    private Socket clientSocket;
    private DataOutputStream out;
    private DataInputStream in;
    private int sessionID;
    private static boolean condition = true;
    private static boolean begin = true;
    private boolean ingame = false;

    Client(String IP, int port){

        try {
            System.out.println("Waiting for connection...");

            clientSocket = new Socket(IP, port); // Laczenie z serwerem (o numerze ip ... i porcie ...)
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            // linia 23-25: do sprawdzenia tylko i wylacznie
            //byte[] byteArray = new byte[4];
            //in.read(byteArray);
            //decodePacket(byteArray);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] generatePacket(int operation, int answer, int id, int number){

        byte[] byteArray = new byte[4];

        byteArray[0] = (byte) ((operation & 0b00011111) << 3);
        byteArray[0] = (byte) (byteArray[0] | (byte) ((answer & 0b00001110) >> 1));

        byteArray[1] = (byte) ((answer & 0b00000001) << 7);
        byteArray[1] = (byte) (byteArray[1] | (byte) ((id & 0b00000111) << 4));
        byteArray[1] = (byte) (byteArray[1] | (byte) ((number & 0b11110000) >> 4));

        byteArray[2] = (byte) ((number & 0b00001111) << 4);

        return byteArray;
    }

    void sendPacket(int operation, int answer, int id, int number){
        try {
            out.write(generatePacket(operation, answer, id, number),0,4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void decodePacket(byte[] byteArray){

        int operation, answer, id, number, attempt;

        operation = (byteArray[0] & 0b11111000) >> 3;
        answer = ((byteArray[0] & 0b00000111) << 1) | ((byteArray[1] & 0b10000000) >> 7);
        id = (byteArray[1] & 0b01110000) >> 4;
        number = ((byteArray[1] & 0b00001111) << 4) | ((byteArray[2] & 0b11110000) >> 4);
        attempt = ((byteArray[2] & 0b00001111) << 4) | ((byteArray[3] & 0b11110000) >> 4);

        if(id == sessionID){
            execute(operation, answer, number, attempt);
        } else{
            if(operation == 0 && answer == 0){
                sessionID = id;
            }else{
                System.out.println("Odebrano niepoprawny komunikat od serwera.");
            }
        }



        // linia 43-47: do sprawdzenia tylko i wylacznie
        //System.out.println(operation);
        //System.out.println(answer);
        //System.out.println(id);
        //System.out.println(number);
        //System.out.println(attempt);
    }

    private void execute(int operation, int answer, int number, int attempt){
        // switch - reakcja na operacje
        // if - reakcja na answer

        switch (operation){
            case 2:
                if(answer == 0){
                    System.out.println("Start gry!");
                    System.out.println("Podaj liczbe nieparzysta: ");
                    ingame = true;
                }
                if(answer == 1){
                    System.out.println("Pozostalo " + attempt + " prob");
                }
                break;
            case 3:
                if(ingame){
                    if(answer == 1){
                        System.out.println("Liczba jest za mala");
                        System.out.println("Pozostalo " + attempt + " prob.");
                    }
                    if(answer == 2){
                        System.out.println("Pozostalo " + attempt + " prob.");
                    }
                    if(answer == 4){
                        System.out.println("Liczba jest za duza.");
                        System.out.println("Pozostalo " + attempt + " prob.");
                    }
                }
                break;
            case 7:
                if(answer == 1){
                    System.out.println("Wygrales!");
                }

                if(answer == 2){
                    System.out.println("Liczba prob sie skonczyla.");
                }
                sendPacket(7,7,sessionID,0);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ingame = condition = false;
                break;
            default:
                break;
        }
    }

    public static void main(String[] args){

        // tutaj startujemy watek kliencki (uruchamiamy metoda run)
        Client client = new Client("127.0.0.1", 1234);
        if(client.clientSocket != null){
            System.out.println("Polaczono z serwerem");
            new Thread(client).start();
        } else{
            System.out.println("Nie mozna bylo polaczyc sie z serwerem.");
            condition = false;
        }
    }

    public void run(){

        int number, length;
        int attempt;
        byte[] byteArray = new byte[4];
        Scanner scanner = new Scanner(System.in);

        // pÄ™tla do zczytania liczby nieparzystej do okreslenia liczby prob
        while(begin)
        {
            try {
                if (in.available() > 0){
                    length = in.read(byteArray);
                    if(length == -1){
                        condition = false;
                    } else{
                        decodePacket(byteArray);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (System.in.available() > 0){
                    attempt = scanner.nextInt();
                    sendPacket(2,2, sessionID, attempt);
                    condition = true;
                    begin = false;

                }
            } catch (Throwable e) {
                System.out.println(e.getMessage());
            }
        }


        while(condition){

            // tutaj wpisujemy liczbe do sprawdzenia
            try {
                if (System.in.available() > 0){
                    System.out.println("Podaj liczbe naturalna: ");
                    number = scanner.nextInt();
                    sendPacket(3,0, sessionID, number);
                }
            } catch (Throwable e) {
                scanner.next();
            }

            // tutaj otrzymujemy i dekodujemy pakiety w kliencie
            try {
                if (in.available() > 0){
                    length = in.read(byteArray);
                    if(length == -1){
                        condition = false;
                    } else{
                        decodePacket(byteArray);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}