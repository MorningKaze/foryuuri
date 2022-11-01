package mgKze;

import java.sql.*;
import java.util.HashMap;

public class SqlOps {
    String driverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    String dbURL = "jdbc:sqlserver://localhost:1433;DatabaseName=forYuuri";
    String userName = "mgKze";
    String password = "yuuri";
    Connection dbConn;
    Statement stmt;

    public boolean isValid(String s){
        return s.matches("[a-zA-Z0-9._]+");
    }

    public void createTables() throws SQLException{
        if(!checkVtbTable())
            createVtb();
        if(!checkOsuTable())
            createOsu();
        if(!checkServerTable())
            createServer();
    }

    public void connect() throws ClassNotFoundException, SQLException{
        Class.forName(driverName);
        dbConn = DriverManager.getConnection(dbURL, userName, password);
        stmt = dbConn.createStatement();
    }

    //Vtb
    public boolean checkVtbTable() throws SQLException {
        String payload = "select name from sys.tables";
        ResultSet rs = stmt.executeQuery(payload);
        while(rs.next()){
            if(rs.getString("name").equals("vtbTable"))
                return true;
        }
        return false;
    }

    public void createVtb() throws SQLException {
        String payload = "create table vtbTable(\n" +
                "\tuid int, --用户uid\n" +
                "\tislive bit, --直播状态\n" +
                "\tgroupid int , --群号\n" +
                ");\n" +
                "insert into vtbTable values(12134211, 0, 963712049);";
        stmt.executeUpdate(payload);
    }

    public boolean addVtb(String uid, String groupid){
        try{
            String payload = "insert into vtbTable values(" + uid + ", 0, " + groupid + ")";
            stmt.executeUpdate(payload);
            System.out.println("[+]Vtb Insert Success!");
            return true;
        }
        catch (Exception e){
            System.out.println("[-]Vtb Insert Failed!");
            e.printStackTrace();
            return false;
        }
    }

    public boolean delVtb(String uid, String groupid){
        try{
            String payload = "delete from vtbTable where uid = " + uid + " and groupid = " + groupid;
            stmt.executeUpdate(payload);
            System.out.println("[+]Vtb Delete Success!");
            return true;
        }
        catch (Exception e){
            System.out.println("[-]Vtb Delete Failed!");
            e.printStackTrace();
            return false;
        }
    }

    public long[][] listVtb(){
        try {
            String payloadLen = "select count(*) as len from vtbTable";
            ResultSet rsLen = stmt.executeQuery(payloadLen);
            int length = 0;
            while (rsLen.next())
                length = rsLen.getInt("len");

            String payload = "select * from vtbTable";
            ResultSet rs = stmt.executeQuery(payload);
            long[][] result = new long[length][2];
            int i = 0;
            while (rs.next()){
                long uid = rs.getInt("uid");
                long groupid = rs.getLong("groupid");
                result[i][0] = uid;
                result[i++][1] = groupid;
            }
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public boolean isLive(String uid){
        try {
            String payload = "select * from vtbTable where uid = " + uid;
            ResultSet rs = stmt.executeQuery(payload);
            while (rs.next()){
                if (rs.getInt("islive") == 1)
                    return true;
            }
            return false;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public void setLive(String uid, int newLive){
        try {
            String payload = "update vtbTable set islive = " + newLive + " where uid = " + uid;
            stmt.executeUpdate(payload);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    //Osu
    public boolean checkOsuTable() throws SQLException {
        String payload = "select name from sys.tables";
        ResultSet rs = stmt.executeQuery(payload);
        while(rs.next()){
            if(rs.getString("name").equals("osuTable"))
                return true;
        }
        return false;
    }

    public void createOsu() throws SQLException {
        String payload = "create table osuTable(\n" +
                "\tid varchar(32) primary key, --用户id\n" +
                "\tqq varchar(32), --用户QQ\n" +
                ")\n";
        stmt.executeUpdate(payload);
    }

    public boolean addOsu(String id, String qq){
        try{
            if(isValid(id)){
                String payload = "insert into osuTable values('" + id + "', '" + qq + "')";
                stmt.executeUpdate(payload);
                System.out.println("[+]Osu Insert Success!");
                return true;
            }
            else
                return false;
        }
        catch (Exception e){
            System.out.println("[-]Osu Insert Failed!");
            e.printStackTrace();
            return false;
        }
    }

    public boolean delOsu(String id){
        try{
            String payload = "delete from osuTable where id = '" + id + "'";
            stmt.executeUpdate(payload);
            System.out.println("[+]Osu Delete Success!");
            return true;
        }
        catch (Exception e){
            System.out.println("[-]Osu Delete Failed!");
            e.printStackTrace();
            return false;
        }
    }

    public boolean isPlayerExist(String id){
        try {
            String payload = "select * from osuTable where id = '" + id + "'";
            ResultSet rs = stmt.executeQuery(payload);
            String result = "null";
            while (rs.next()){
                result = rs.getString("id");
            }
            if(result.equals("null"))
                return false;
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public String findPlayerByQQ(String qq){
        try {
            String payload = "select * from osuTable where qq = '" + qq + "'";
            ResultSet rs = stmt.executeQuery(payload);
            String id = "null";
            while (rs.next()){
                id = rs.getString("id");
            }
            return id;
        }
        catch (Exception e){
            e.printStackTrace();
            return "null";
        }
    }

    //Server
    public boolean checkServerTable() throws SQLException {
        String payload = "select name from sys.tables";
        ResultSet rs = stmt.executeQuery(payload);
        while(rs.next()){
            if(rs.getString("name").equals("serverTable"))
                return true;
        }
        return false;
    }

    public void createServer() throws SQLException {
        String payload = "create table serverTable(\n" +
                "\tname varchar(32) primary key, --服务器名称\n" +
                "\tip varchar(32), --服务器ip\n" +
                ")\n" +
                "insert into serverTable values('GSE', 'ftbi.nowamuse.com:25701')";
        stmt.executeUpdate(payload);
    }

    public boolean addServer(String name, String ip){
        try{
            String payload = "insert into serverTable values('" + name + "', '" + ip + "')";
            stmt.executeUpdate(payload);
            System.out.println("[+]Server Insert Success!");
            return true;
        }
        catch (Exception e){
            System.out.println("[-]Server Insert Failed!");
            e.printStackTrace();
            return false;
        }
    }

    public boolean delServer(String name){
        try{
            String payload = "delete from serverTable where name = '" + name + "'";
            stmt.executeUpdate(payload);
            System.out.println("[+]Server Delete Success!");
            return true;
        }
        catch (Exception e){
            System.out.println("[-]Server Delete Failed!");
            e.printStackTrace();
            return false;
        }
    }

    public String[] listServer(){
        try {
            String payloadLen = "select count(*) as len from serverTable";
            ResultSet rsLen = stmt.executeQuery(payloadLen);
            int length = 0;
            while (rsLen.next())
                length = rsLen.getInt("len");

            String payload = "select * from serverTable";
            ResultSet rs = stmt.executeQuery(payload);
            String[] result = new String[length];
            int i = 0;
            while (rs.next()){
                result[i] = rs.getString("name");
                i++;
            }
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public String getIP(String name){
        try {
            if(isValid(name)){
                String payload = "select * from serverTable where name = '" + name + "'";
                ResultSet rs = stmt.executeQuery(payload);
                String ip = "null";
                while (rs.next()){
                    ip = rs.getString("ip");
                }
                return ip;
            }
            else return "null";
        }
        catch (Exception e){
            e.printStackTrace();
            return "null";
        }
    }
}
