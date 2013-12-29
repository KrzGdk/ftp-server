package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;


public class SessionThread implements Runnable{

    protected Socket clientSocket = null;
    protected String serverText = null;
    protected boolean logged = false;
    protected long timeOfLastRequest;

    public SessionThread(Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        Calendar cal = Calendar.getInstance();
    	timeOfLastRequest = cal.getTimeInMillis();
    }

    @Override
    public void run() {
        long now;
        String request, prevRequest = "", response;
        try(BufferedReader fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));PrintWriter toClient = new PrintWriter(clientSocket.getOutputStream(), true)) {
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
                    if(request.startsWith("USER") && request.length() > 5){
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
                    else if(request.startsWith("PASS") && request.length() > 5){
                        if(prevRequest.startsWith("USER") && !logged){
                            System.out.println("user " + prevRequest.substring(5) + " pass " + request.substring(5));
                            Database db = new Database();
                            if(db.checkUser(prevRequest.substring(5), request.substring(5))){
                                response = "230 User logged in";
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