import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class ClientSomthing {
    public boolean isRun = true;
    private Socket socket;
    private DataInputStream in; // поток чтения из сокета
    Scanner inputUser1;
    private BufferedWriter out; // поток чтения в сокет
    private BufferedReader inputUser; // поток чтения с консоли
    private String addr; // ip адрес клиента
    private int port; // порт соединения
    private String nickname; // имя клиента
    public static ReentrantLock locker = new ReentrantLock();
    public static Condition condition = locker.newCondition();
    public ClientSomthing(String addr, int port) {
        this.addr = addr;
        this.port = port;
        try {
            this.socket = new Socket(addr, port);
        } catch (IOException e) {
            System.err.println("Socket failed");
        }
        try {
            // потоки чтения из сокета / записи в сокет, и чтения с консоли
            //inputUser = new BufferedReader(new InputStreamReader(System.in));
            inputUser1 =  new Scanner(System.in);
            in = new DataInputStream(socket.getInputStream());
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            ReadMsg r = new ReadMsg();
            WriteMsg w = new WriteMsg();
            w.start();
            r.start();
            try{
                w.join(); // нить пишущая сообщения в сокет приходящие с консоли в бесконечном цикле
                r.join(); // нить читающая сообщения из сокета в бесконечном цикле
            }catch (InterruptedException e){
                System.out.println(e);
            }
        } catch (IOException e) {
            // Сокет должен быть закрыт при любой
            // ошибке, кроме ошибки конструктора сокета:
            ClientSomthing.this.downService();
        }
        // В противном случае сокет будет закрыт
        // в методе run() нити.
    }
    /**
     * закрытие сокета
     */
    private void downService() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException ignored) {}
    }

    // нить чтения сообщений с сервера
    private class ReadMsg extends Thread {
        @Override
        public void run() {
            String str;
            try {
                while (true) {
                    str = in.readUTF(); // ждем сообщения с сервера
                    System.out.println(str); // пишем сообщение с сервера на консоль
                }
            } catch (IOException e) {
                ClientSomthing.this.downService();
            }
        }
    }

    // нить отправляющая сообщения приходящие с консоли на сервер
    public class WriteMsg extends Thread {
        @Override
        public void run() {
            while (!socket.isClosed()) {
                String userWord;
                try {
                    userWord = inputUser1.nextLine(); // сообщения с консоли
                    String[] comand =  userWord.split(" ");
                    switch(comand[0]){ //userWord.substring(4)
                        case "put":
                            out.write(userWord + "\n");
                            out.flush();
                            break;
                        case "get":
                            out.write(userWord + "\n");
                            out.flush();
                            break;
                        case "cache":
                            out.write(comand[0] + "\n");
                            out.flush();
                            break;
                        case "logLevel":
                            if(comand.length>=2){
                                switch (comand[1]){
                                    case "off":
                                        out.write(comand[0]+" " +comand[1] + "\n"); // отправляем на сервер
                                        out.flush();
                                        break;
                                    case "fatal":
                                        out.write(comand[0]+" " +comand[1] + "\n"); // отправляем на сервер
                                        out.flush();
                                        break;
                                    case "error":
                                        out.write(comand[0]+" " +comand[1] + "\n"); // отправляем на сервер
                                        out.flush();
                                        break;
                                    case "warn":
                                        out.write(comand[0]+" " +comand[1] + "\n"); // отправляем на сервер
                                        out.flush();
                                        break;
                                    case "info":
                                        out.write(comand[0]+" " +comand[1] + "\n"); // отправляем на сервер
                                        out.flush();
                                        break;
                                    case "debug":
                                        out.write(comand[0]+" " +comand[1] + "\n"); // отправляем на сервер
                                        out.flush();
                                        break;
                                    case "trace":
                                        out.write(comand[0]+" " +comand[1] + "\n"); // отправляем на сервер
                                        out.flush();
                                        break;
                                    case "all":
                                        out.write(comand[0]+" " +comand[1] + "\n"); // отправляем на сервер
                                        out.flush();
                                        break;
                                    default:
                                        help();
                                }

                                break;
                            }
                        case "help":
                            if(userWord.length()!=4){
                                help();
                            }
                            help();
                            break;
                        case "quit":
                            if(userWord.length()!=4){
                                help();
                                out.write("help" + "\n");
                                out.flush();
                            }
                            else{
                                out.write("disconnect" + "\n"); // отправляем на сервер
                                out.flush();
                                downService();
                                System.exit(0);
                            }
                            break;
                        case "disconnect":
                            if(userWord.length()!=10){
                                help();
                                out.write("help" + "\n");
                                out.flush();
                            }
                            else{
                                out.write("disconnect" + "\n"); // отправляем на сервер
                                out.flush();
                                downService();
                            }
                            break;
                        default:
                            help();
                    }
                    out.flush(); // чистим
                } catch (IOException e) {
                    ClientSomthing.this.downService(); // в случае исключения тоже харакири
                }
            }
        }
        void help(){
            System.out.println("[connect] [ip] [port] to connect to the server");
            System.out.println("[disconnect] to disconnect from the server");
            System.out.println("[put] [key] [value] send key with value");
            System.out.println("[get] [key]  get value by key");
            System.out.println("[cache] get key cache");
            System.out.println("[logLevel] [ALl|DEBUG|INFO|WARN|ERROR|FATAL|OFF] to set the level of logging");
            System.out.println("[quit] to exit the program");
        }
    }
}

public class Client {


    public static String addres;
    public static BufferedReader inputUser; // поток чтения с консоли
    public static ReentrantLock locker = new ReentrantLock();
    public static Utilities utilities = new Utilities();
    public static String ipAddr = "localhost";
    public static int port = 8080;
    public static void main(String[] args) throws InterruptedException {
        inputUser = new BufferedReader(new InputStreamReader(System.in));
        while(true){
            input();
            System.out.println("3");
        }
    }
    public static void input() {
        try{
            System.out.println("input connection");
            addres = inputUser.readLine();
            System.out.println("input connection1");
            System.out.println("c0 comand read");
            String[] conect =  addres.split(" ");
            System.out.println(conect[0]);
            if(conect[0].equals("connect")) {
                System.out.println("c1 conect corect");
                if(utilities.isIpAddress(conect[1])){
                    System.out.println("c2 ip corect");
                    ipAddr=conect[1];
                    if(Integer.parseInt(conect[2])>=1024 && Integer.parseInt(conect[2])<=65535){
                        System.out.println("c3 port conect");
                        port = Integer.parseInt(conect[2]);
                    }
                }
            }
            new ClientSomthing(ipAddr, port);
        }
        catch (IOException ignored) {
        }
    }
}

class Utilities {
    private static Pattern VALID_IPV4_PATTERN = null;
    private static Pattern VALID_IPV6_PATTERN = null;
    private static final String ipv4Pattern = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    private static final String ipv6Pattern = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

    static {
        try {
            VALID_IPV4_PATTERN = Pattern.compile(ipv4Pattern, Pattern.CASE_INSENSITIVE);
            VALID_IPV6_PATTERN = Pattern.compile(ipv6Pattern, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            //logger.severe("Unable to compile pattern", e);
        }
    }

    public static boolean isIpAddress(String ipAddress) {
        Matcher m1 = Utilities.VALID_IPV4_PATTERN.matcher(ipAddress);
        if (m1.matches()) {
            return true;
        }
        Matcher m2 = Utilities.VALID_IPV6_PATTERN.matcher(ipAddress);
        return m2.matches();
    }
}