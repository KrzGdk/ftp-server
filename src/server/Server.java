/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Krzysiek
 */
public class Server{
    public static void main(String[] args){
        try {
            ServerSocket server = new ServerSocket(20);
            Socket client = server.accept();
            BufferedReader client_in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter client_out = new PrintWriter(client.getOutputStream());
            while(client_in.readLine() != null){
                client_out.println("echo dupa");
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
