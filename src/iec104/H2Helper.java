/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iec104;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.tools.Server;

/**
 *
 * @author liqi
 */
public class H2Helper {

    public Server server;
    public String port = "31833";
    public String dbDir;
    public String user, password;
    public ResultSet result;
    public Connection conn;
    public Statement stat;
    private static H2Helper instance = null;
    public static H2Helper Instance(){
        if(instance == null)
            instance = new H2Helper();
        return instance;
    }
    
    public H2Helper(){
        instance = this;
    }

    public int GetSqlReturn() throws Exception{
       return stat.getUpdateCount();
    }
    public boolean ExecuteSql(String sql) throws Exception {

        if (stat == null) {
            return false;
        }
        if (result != null) {
            result.close();
        }
        stat.execute(sql); //stat.executeQuery("select name from test ");
        
        return true;
    }

    public boolean Query(String sql) throws Exception {

        if (stat == null) {
            return false;
        }
        if (result != null && (!result.isClosed())) {
            result.close();
        }
        result = stat.executeQuery(sql); //stat.executeQuery("select name from test ");
        return true;
    }

    public boolean Connect(String _user, String _password) throws Exception {
        if (conn != null) {
            conn.close();
        }
        if (stat != null) {
            stat.close();
        }
        //dbDir = _dbDir;
        user = _user;
        password = _password;
        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:" + dbDir + "/mem:scdb",user,password);//;Key=sa"
        //user, password);

        stat = conn.createStatement();
        return true;
    }

    public boolean startServer() throws Exception {
        server = Server.createTcpServer(new String[]{"-tcpPort", port}).start();
        dbDir = server.getURL();
        return true;
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
}
