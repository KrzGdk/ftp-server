/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.File;
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
        return count == 1;
    }
    
    public User getUser(String name){
        connect();
        int userId = 0, groupId = 0;
        String group = "";
        String getIdQuery = "SELECT `id` FROM `users` WHERE username = '"+name+"';";
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(getIdQuery);
            resultSet.next();
            userId = Integer.parseInt(resultSet.getString("id"));
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
        
        String getGroupIdQuery = "SELECT `group_id` FROM `usergroup` WHERE user_id = '"+userId+"';";
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(getGroupIdQuery);
            resultSet.next();
            groupId = Integer.parseInt(resultSet.getString("group_id"));
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
        
        String getGroupQuery = "SELECT `group` FROM `groups` WHERE `id` = '"+groupId+"';";
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(getGroupQuery);
            resultSet.next();
            group = resultSet.getString("group");
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
        return new User(name,group,userId,groupId);
    }
    
    public void addFile(String name, int ownerId, int groupId){
        connect();
        String addFileQuery;
        addFileQuery = "INSERT INTO `files`(`filename`, `owner_id`, `group_id`, `user_read`, `user_write`, `group_read`, `group_write`)"
                + " VALUES ('"+ name +"','"+ ownerId +"','"+ groupId +"',TRUE,TRUE,TRUE,FALSE)";
        try{
            statement = connection.createStatement();
            statement.executeUpdate(addFileQuery);
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }
    
    public String checkPermissions(File f){
        connect();
        String checkFileQuery;
        checkFileQuery = "SELECT * FROM files WHERE filename = '"+ f.toString().substring(1) +"';";
        int userRead, userWrite, groupRead, groupWrite;
        int userPerm = 0, groupPerm = 0;
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(checkFileQuery);
            if(resultSet.next()){
                userRead = resultSet.getInt("user_read");
                userWrite = resultSet.getInt("user_write");
                groupRead = resultSet.getInt("group_read");
                groupWrite = resultSet.getInt("group_write");
                userPerm = userRead + 2 * userWrite;
                groupPerm = groupRead + 2 * groupWrite;
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
        return userPerm + "" + groupPerm;
    }
    
    public boolean canRead(User u, File f){
        connect();
        String canReadQuery;
        canReadQuery = "SELECT `group_read` OR (`user_read` AND `owner_id`='"+u.getId()+"') FROM `files` AS `f` WHERE `f`.`filename`='"+f.toString().substring(1)+"' AND `f`.`group_id` IN (SELECT `group_id` FROM `usergroup` WHERE `user_id`='"+u.getId()+"')";
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(canReadQuery);
            if(resultSet.next()){
                return resultSet.getBoolean(1);
            }
            else return false;
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
        return false;
    }
    
    public boolean canWrite(User u, File f){
        connect();
        String canWriteQuery;
        canWriteQuery = "SELECT `group_write` OR (`user_write` AND `owner_id`='"+u.getId()+"') FROM `files` AS `f` WHERE `f`.`filename`='"+f.toString().substring(1)+"' AND `f`.`group_id` IN (SELECT `group_id` FROM `usergroup` WHERE `user_id`='"+u.getId()+"')";
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(canWriteQuery);
            if(resultSet.next()){
                return resultSet.getBoolean(1);
            }
            else return false;
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
        return false;
    }
    
    public String getOwner(File f){
        connect();
        String getOwnerQuery, owner = "";
        getOwnerQuery = "SELECT `username` FROM `users` WHERE `id`= (SELECT `owner_id` FROM `files` WHERE `filename` = '"+f.toString().substring(1)+"');";
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(getOwnerQuery);
            if(resultSet.next()){
                owner = resultSet.getString(1);
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
        return owner;
    }
    
    public String getGroup(File f){
        connect();
        String getGroupQuery, group = "";
        getGroupQuery = "SELECT `group` FROM `groups` WHERE `id`= (SELECT `group_id` FROM `files` WHERE `filename` = '"+f.toString().substring(1)+"');";
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(getGroupQuery);
            if(resultSet.next()){
                group = resultSet.getString(1);
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
        return group;
    }
}