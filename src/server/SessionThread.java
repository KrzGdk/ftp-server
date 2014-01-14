package server;

import com.mysql.jdbc.StringUtils;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;


public class SessionThread implements Runnable{

    protected Socket clientSocket = null, clientPassiveSocket = null;
    protected String serverText = null;
    protected ServerSocket passiveSocket = null;
    protected boolean logged = false;
    protected long timeOfLastRequest;
    protected String passiveIP;
    protected int passivePort;
    protected String currentDir;
    protected User user;

    public SessionThread(Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        Calendar cal = Calendar.getInstance();
    	timeOfLastRequest = cal.getTimeInMillis();
        currentDir = ".";
    }

    @Override
    public void run() {
        long now;
        String request, prevRequest = "", response;
        try(BufferedReader fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));PrintWriter toClient = new PrintWriter(clientSocket.getOutputStream(), true)) {
            toClient.println("220-Welcome to ftp server");
            toClient.println("220 You will be diconnected after 1 minute of inactivity");
            while(true){
                request = fromClient.readLine();
                System.out.println("Request: " + request);
                now = Calendar.getInstance().getTimeInMillis();
                if((now - timeOfLastRequest) > 60 * 1000){
                    logged = false;
                    response = "221 Connection timed out";
                    toClient.println(response);
                    System.out.println(response);
                    timeOfLastRequest = now;
                }
                else{
                    timeOfLastRequest = now;
                    if(request.startsWith("USER ") && request.length() > 3){
                        if(logged){
                            response = "230 Already logged in";
                            toClient.println(response);
                            System.out.println(response);
                        }
                        else{
                            System.out.println("Logging user " + request.substring(5));
                            response = "331 Password required";
                            toClient.println(response);
                            System.out.println(response);
                        }
                            prevRequest = request;
                    }
                    else if(request.startsWith("PASS ") && request.length() > 3){
                        if(prevRequest.startsWith("USER ") && !logged){
                            System.out.println("user " + prevRequest.substring(5) + " pass " + request.substring(5));
                            Database db = new Database();
                            if(db.checkUser(prevRequest.substring(5), request.substring(5))){
                                response = "230 User logged in";
                                user = db.getUser(prevRequest.substring(5));
                                logged = true;
                            }
                            else{
                                response = "430 Invalid username or password";
                            }
                            toClient.println(response);
                        }
                        else{
                            response = "503 Bad sequence of commands";
                            toClient.println(response);
                        }
                        prevRequest = request;
                    }
                    else if(request.toUpperCase().equals("LIST")){
                        File dir = new File(currentDir);
                        File[] files = dir.listFiles();
                        clientPassiveSocket = passiveSocket.accept();
                        toClient.println("150 Opening connection");
                        try (DataOutputStream dataOut = new DataOutputStream(clientPassiveSocket.getOutputStream())) {
                            String list = "";
                            Database db = new Database();
                            for(File l : files){
                                String type, perm = "", owner = "", group = "";
                                if(l.isDirectory()) type="d ";
                                else {
                                    type="- ";
                                    perm = db.checkPermissions(l) + " ";
                                    owner = db.getOwner(l) + " ";
                                    group = db.getGroup(l) + " ";
                                }
                                
                                list = list + type + perm + owner + group + l.getName() +"\n";
                            }
                            dataOut.writeUTF(list);
                            toClient.println("226 Transfer complete");
                        }
                    }
                    else if(request.toUpperCase().equals("PASV")){
                        if(logged){
                            int serverPort = 0;
                            passiveSocket = new ServerSocket(serverPort);
                            passiveIP = passiveSocket.getInetAddress().getHostAddress();
                            passiveIP = "127.0.0.1"; // local loopback
                            String[] passiveSplitIP = passiveIP.split("\\.");
                            passivePort = passiveSocket.getLocalPort();
                            System.out.println(passivePort);
                            int p1 = passivePort >> 8;
                            int p2 = passivePort % 256;
                            response = "227 Entering Passive Mode ("+passiveSplitIP[0]+","+passiveSplitIP[1]+","
                                    +passiveSplitIP[2]+","+passiveSplitIP[3]+","+p1+","+p2+")";
                            toClient.println(response);
                            prevRequest = request;
                        }
                        else{
                            response = "530 Not logged in";
                            toClient.println(response);
                            prevRequest = request;
                        }
                    }
                    else if(request.toUpperCase().startsWith("STOR ")){
                        if(prevRequest.toUpperCase().equals("PASV")){
                            String filename = request.substring(5);
                            clientPassiveSocket = passiveSocket.accept();
                            toClient.println("150 Opening connection");
                            File f = new File(filename);
                            if(!f.exists() || (f.exists() && user.canWrite(f))){
                                try (RandomAccessFile file = new RandomAccessFile((filename),"rw"); 
                                    DataInputStream dataIn = new DataInputStream(clientPassiveSocket.getInputStream())) {

                                    int offset;
                                    byte[] data = new byte[1024];
                                    while( (offset = dataIn.read(data)) != -1){
                                        file.write(data, 0, offset);
                                    }
                                    Database db = new Database();
                                    db.addFile(filename, user.getId(), user.getGroupId());
                                    toClient.println("226 Transfer complete");
                                } //todo thread
                                clientPassiveSocket.close();
                            }
                            else{
                                toClient.println("550 File exists, no permission to overwrite");
                            }
                        }
                    }
                    else if(request.equals("NOOP")){
                        response = "200 Command successful";
                        toClient.println(response);
                        prevRequest = request;
                    }
                    else if(request.equals("QUIT")){
                        logged = false;
                        response = "221 Bye";
                        toClient.println(response);
                        clientSocket.close();
                        return;
                    }
                    else{
                        response = "502 Command not implemented";
                        toClient.println(response);
                        prevRequest = request;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
}