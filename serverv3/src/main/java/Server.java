import org.apache.log4j.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;

class ServerSomthing extends Thread {

    private Socket socket; // сокет, через который сервер общается с клиентом,
    private BufferedReader in; // поток чтения из сокета
    private DataOutputStream out; // поток завписи в сокет
    private Logger log = Logger.getRootLogger();
    private  FileWriter writer;
    FileReader reader;
    File filePath = new File("DATA");
    ArrayList<String> kyes = new ArrayList<String>();
    Hashtable hashtable = new Hashtable();
    int size;


    DataInputStream dataInput;
    public ServerSomthing(Socket socket, int size1) throws IOException {

        filePath.mkdir();
        size=size1;
        this.socket = socket;
        log.debug("socket create");
        // если потоку ввода/вывода приведут к генерированию искдючения, оно проброситься дальше
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        log.debug("reader create");
        out = new DataOutputStream(socket.getOutputStream());
        log.debug("writer create");
        System.out.println("new client");
        log.debug("threade start");
        start(); // вызываем run()
    }
    @Override
    public void run() {
        boolean isRun = true;
        String word;
        try {
            try {
                while (isRun) {
                    word = in.readLine();
                    String[] comand =  word.split(" ");
                    switch (comand[0]){
                        case "disconnect":
                            log.debug("client diconnect" + socket.toString());
                            isRun = false;
                            downService();
                            break;
                        case "put":
                            System.out.println("client send "+word.substring(4));
                            log.debug("client send "+word.substring(4));
                            writer = new FileWriter(filePath +"\\" +comand[1]+".txt", false);
                            writer.write(word.substring(4).substring(comand[1].length()+1));
                            writer.flush();
                            writer.close();
                            out.flush();
                            break;
                        case "get":
                            System.out.println("client send get "+word.substring(3));
                            log.debug("client send get "+word.substring(3));
                                out.writeUTF( Search(comand[1],hashtable,kyes));
                                out.flush();
                            break;
                        case "logLevel":
                            log.info("logLevel set" + comand[1]);
                            switch (comand[1]){
                                case "off":     log.setLevel(Level.OFF); break;
                                case "fatal":   log.setLevel(Level.FATAL); break;
                                case "error":   log.setLevel(Level.ERROR); break;
                                case "warn":    log.setLevel(Level.WARN); break;
                                case "info":    log.setLevel(Level.INFO); break;
                                case "debug":   log.setLevel(Level.DEBUG); break;
                                case "trace":   log.setLevel(Level.TRACE); break;
                                case "all":     log.setLevel(Level.ALL); break;
                                default:    break;
                            }
                            break;
                        case "cache":
                            for(String k : kyes){
                                out.writeUTF(k);
                            }
                    }
                }

            } catch (NullPointerException ignored) {}

        } catch (IOException e) {
            this.downService();
        }

    }
    public String Search(String key, Hashtable table, ArrayList keys){
        if(keys.contains(key)){                 //если ключ существует в кеше
            keys.remove(key);                   //переписываем его
            keys.add(key);                      //в конец списка
            return (table.get(key)).toString(); //возвращяем значение по ключу
        }
        else {                                  //если ключа в кеше нет
            if(!find(key).contains("-1")){      //иишим по файлам если нашли
                if(keys.size()<size){           //проверяем размер кеша если еще не заполнен
                    keys.add(key);              //добавляем ключ в список
                    table.put(key, find(key));  //значение в таблицу
                }
                else {                          //если кеш заполнен
                    table.remove(keys.get(0));  //удаляем ключ который
                    keys.remove(0);        //из начала списка
                    keys.add(key);              //добавляем новый ключ
                    table.put(key, find(key));  //и значения в конец списка

                }
                return find(key);               //возвращаем искомое значение
            }
            return "eror 404";                  //если и по файлам не нашли возвращаем ошибку
        }
    }
    public  String find(String key){
        try (FileReader reader = new FileReader(filePath +"\\" + key+".txt")){
            String f = "";
            int c;
            while ((c = reader.read()) != -1) {
                f += (char) c;
            }
            reader.close();
            return f;

        }
        catch (IOException ex){
            return "-1";
        }
    }
    /**
     * закрытие сервера
     * прерывание себя как нити и удаление из списка нитей
     */
    private void downService() {
        try {
            if(!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException ignored) {}
    }
}

public class Server {

    public static  int PORT = 8080;
    public static  int SIZE = 10;
    public static LinkedList<ServerSomthing> serverList = new LinkedList<>(); // список всех нитей - экземпляров
    // сервера, слушающих каждый своего клиента
    private static  Logger log = Logger.getRootLogger();
    public static void main(String[] args) throws IOException {
      if (args.length>0){
            PORT = Integer.parseInt(args[0]);
          System.out.println(PORT);
            if(args.length>1){
                SIZE= Integer.parseInt(args[1]);
            }
        }
        new LogSetup("log/server.log", Level.ALL);
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("Server Started");
        log.info("start server");
        try {
            while (true) {
                // Блокируется до возникновения нового соединения:
                Socket socket = server.accept();
                log.debug("server accept client");
                try {
                    serverList.add(new ServerSomthing(socket,SIZE)); // добавить новое соединенние в список
                } catch (IOException e) {
                    // Если завершится неудачей, закрывается сокет,
                    // в противном случае, нить закроет его:
                    socket.close();
                }
            }
        } finally {
            server.close();
        }
    }
}