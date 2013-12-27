/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.security.SecureRandom;
import java.sql.*;
import java.util.Random;
 
public class Database{
    private Connection connection = null;
    private Statement statement = null;
    private ResultSet resultSet = null;
    
    public void connect(){
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/ftp?characterEncoding=latin2","root","");
        }
        catch(ClassNotFoundException e){
            System.out.println("ClassNorFound");
        }
        catch(InstantiationException e){
            System.out.println("Instatiation");
        }
        catch(IllegalAccessException e){
            System.out.println("IllegalAccess");
        }
        catch(SQLException e){
            System.out.println("SQLExc");
            e.printStackTrace(System.out);
        }
    }
    public boolean addUser(String username, String password){
        connect();
        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        String salt = sb.toString();
        String query = "INSERT INTO `users`(`username`, `password`, `salt`) VALUES ('"+username+"',PASSWORD(CONCAT(PASSWORD('"+password+"'),'"+salt+"')),'"+salt+"')";
        
        try{
            statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch(SQLException e){
            e.printStackTrace(System.out);
            return false;
        }
        return true;
        
    }
    public boolean checkUser(String username, String password){
        connect();
        int count = 0;
        String salt = null;
        String getSaltQuery = "SELECT `salt` FROM `users` WHERE username = '"+username+"';";
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(getSaltQuery);
            resultSet.next();
            salt = resultSet.getString("salt");
        } catch(SQLException e){
            e.printStackTrace(System.out);
            return false;
        }
        
        String query = "SELECT count(*) FROM `users` WHERE username = '"+username+"' and password = PASSWORD(CONCAT(PASSWORD('"+password+"'),'"+salt+"'));";
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            resultSet.next();
            count = resultSet.getInt(1);
        } catch(SQLException e){
            e.printStackTrace(System.out);
            return false;
        }
        if(count == 1){
            return true;
        }
        else{
            return false;
        }
    }
}