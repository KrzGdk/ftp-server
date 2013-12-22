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
            try (ServerSocket server = new ServerSocket(7777); Socket client = server.accept()) {
                BufferedReader from_client = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter to_client = new PrintWriter(client.getOutputStream(), true);
                String s;
                while((s = from_client.readLine()) != null){
                    System.out.println(s);
                    to_client.println("echo dupa");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            
        }
    }
    
}
