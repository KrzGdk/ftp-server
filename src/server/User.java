/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import java.io.File;

/**
 *
 * @author root
 */
public class User {
    protected String name, group;
    protected int id, groupId;
    
    public User(String name, String group, int id, int groupId){
        this.name = name;
        this.group = group; 
        this.id = id;
        this.groupId = groupId;
    }
    
    public boolean canRead(File f){
        Database db = new Database();
        return db.canRead(this, f);
    }
    
    public boolean canWrite(File f){
        Database db = new Database();
        return db.canWrite(this, f);
    }

    /**
     * @return the username
     */
    public String getName() {
        return name;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the groupId
     */
    public int getGroupId() {
        return groupId;
    }
    
    @Override
    public String toString(){
        return name + ", id: " + id + ", group: " + group + ", group id: " + groupId;
    }
}
