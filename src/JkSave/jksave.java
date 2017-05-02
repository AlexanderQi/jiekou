/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package JkSave;

import ClrIFace.ClrIFace_YCYX;
import ClrIFace.ClrIFace_Out;
import ClrIFace.ycyxinfo;
import java.util.List;
import java.io.FileWriter;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
//import com.mysql.jdbc.*;
import java.sql.DriverManager;

/**
 *
 * @author ibm
 */
public class jksave implements ClrIFace_YCYX {

    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static boolean IsBusy = false;
    //ExecutorService es = Executors.newFixedThreadPool(20);
    //public MyUdp myudp = new MyUdp();
    public String TimeStr = "";
    public ClrIFace_Out out = null;
    public H2Helper helper = new H2Helper();
    private Map<Integer, Float> map_yc = new HashMap<Integer, Float>();
    private Map<Integer, Integer> map_yx = new HashMap<Integer, Integer>();

    ///////////////////////////////////////////////////////////////////////////
    public boolean IsCreateRtdb = false;
    public String cfg_host = "127.0.0.1";
    public int cfg_insidePort = 31833;
    public Integer CHANNEL = 1;
    //////////////////////////////////////////////////////////////////////////

    public void ClrSaveFunc(ClrIFace_Out OutInstance) {
        instance = this;
        out = OutInstance;
    }
    public static jksave instance = null;

    public static jksave Instance() {
        if (instance == null) {
            instance = new jksave();
        }
        return instance;
    }

    private jksave() {
        instance = this;

    }
    Connection mysqlConn = null;
    public Connection ykConn = null;
    //public String myip = "127.0.0.1";
    String driver = "com.mysql.jdbc.Driver";
    String url = "";
    String user = "dba_softcore";
    String password = "softcore";

    public void iniDb() throws Exception {
        if (cfg_host == null) {
            throw new Exception("未指定数据库地址");
        }
        url = "jdbc:mysql://" + cfg_host + ":3306/pwtbl";
        Class.forName(driver);
        mysqlConn = DriverManager.getConnection(url, user, password);
        ykConn = DriverManager.getConnection(url, user, password);
        Statement stat = mysqlConn.createStatement();
        stat.execute("delete from tblycvalue where EQUIPMENTID = '-1';");
        stat.execute("delete from tblyxvalue where EQUIPMENTID = '-1';");
        stat.close();
    }

    public void IniRtdb() {
        //insert into tblycvalue (ych,ycvalue) values (1,1.111);
        //update tblycvalue set ych=2.222 where ych=1
        //update tblycvalue set refreshtime ='2013-5-8 13:16:00' where ych=1
        try {
            if (IsCreateRtdb) {
                helper.stopServer();
                helper.startServer();
            } else {
                //"tcp://" + url + ":" + JkParam.Instance().cfg_insidePort;
                helper.dbDir = "tcp://127.0.0.1:" + cfg_insidePort;
            }

            if (helper.Connect("sa", "sa")) {
                if (IsCreateRtdb) {
                    String sql = "create table tblYcValue (id int, CHANNEL int,CZH int,YCH int,YcValue double,RefreshTime TIMESTAMP,constraint PK_TBLYCVALUE primary key (YCH));";
                    String sql2 = "create table tblYxValue (id int, CHANNEL int,CZH int,YXH int,YxValue int,RefreshTime TIMESTAMP, sb_time varchar,constraint PK_TBLYXVALUE primary key (YXH));";
                    String sql3 = "create table tblcommand (id int, CHANNEL int, schemeid int, schemeindex int, cmddatetime TIMESTAMP,dealdatetime TIMESTAMP, czh int, ykyth int,ykytvalue int, ykyttype int,ytvalue float,DEALTAG int);";
                    helper.ExecuteSql(sql);
                    helper.ExecuteSql(sql2);
                    helper.ExecuteSql(sql3);
                } else {
                    helper.ExecuteSql("delete from tblYcValue where CHANNEL=" + CHANNEL);
                    helper.ExecuteSql("delete from tblYxValue where CHANNEL=" + CHANNEL);
                }
            } else {
                out.AppendInfo("连接实时库失败");
            }
        } catch (Exception e) {
            out.AppendError(e.toString());
        }

    }

    public int SaveYcFile(List<ycyxinfo> YcList, boolean IsChanged) {
        String fn_yc;
        fn_yc = "/Current.YC";
        int saved = 0;
        try {
            FileWriter fw = new FileWriter(fn_yc, IsChanged);
            String stime = "";//SloaderView.instance.GetCurTimeStr();
            if (!IsChanged) {
                fw.append(stime).append('\t').append(CHANNEL.toString()).append('\n');
                fw.append("XH	CZH	DH	Value").append('\n');
            }
            int ct = YcList.size();
            for (int i = 0; i < ct; i++) {
                ycyxinfo info = YcList.get(i);
//                if(info == null)
//                {
//                    SloaderView.instance.AppendError("SaveYcFile()->ycyxinfo info = SwapList_YC.get(i) = null!;");
//                }
//                if (info.IsSaveFile) {
//                    continue;
//                }
                saved++;
                String str = "";
                str = str + i + '\t' + info.iCZH + '\t' + info.iDH + '\t' + info.fValue;
                fw.append(str).append('\n');
                info.IsSaveFile = true;
            }
            fw.close();
            out.AppendInfo("Save file Current.YC " + saved);
            // myudp.SendMsg();  //通知守护进程
        } catch (Exception e) {
            out.AppendError(e.toString());
        }
        return saved;
    }
    public int SaveYxFile(List<ycyxinfo> YxList, boolean IsChanged) {

        String fn_yx;
        fn_yx = "/Current.YX";//SloaderView.UserPath + "/Current.YX";
        int saved = 0;
        try {
            String stime = "";//SloaderView.instance.GetCurTimeStr();
            FileWriter fw = new FileWriter(fn_yx, IsChanged);
            if (!IsChanged) {
                fw.append(stime).append('\t').append(CHANNEL.toString()).append('\n');
                fw.append("XH	CZH	DH	Value").append('\n');
            }
            int ct = YxList.size();

            for (int i = 0; i < ct; i++) {
                ycyxinfo info = YxList.get(i);
//                if(info == null)
//                {
//                    SloaderView.instance.AppendError("SaveYxFile()->ycyxinfo info = SwapList_YX.get(i) = null!;");
//                }
//                if (info.IsSaveFile) {
//                    continue;
//                }
                saved++;
                String str = "";
                str = str + i + '\t' + info.iCZH + '\t' + info.iDH + '\t' + info.iValue;
                fw.append(str).append('\n');
                info.IsSaveFile = true;
            }
            fw.close();
            StringBuilder sb = new StringBuilder();
            sb.append("Save file Current.YX ").append(saved);//.append(" count=").append(ct);
            out.AppendInfo(sb.toString());
            //myudp.SendMsg();  //通知守护进程
        } catch (Exception e) {
            out.AppendError(e.toString());
            //cl.AppendInfo("SaveYxFile()->" + e.toString());
        }

        return saved;
    }
    public int SaveYcToDb(List<ycyxinfo> YcList, String RefreshTime) {
        int saved = 0;
//        int ct = YcList.size();
//        if (ct > 0) {
//            
//            StringBuilder sb = new StringBuilder();
//            MySqlbatch mb = new MySqlbatch();
//            mb.ini(SloaderView.instance.cfg_host);
//            mb.HintInfo = "YC update count:" + ct;
//
//            for (int i = 0; i < ct; i++) {
//                sb.delete(0, sb.length());
//                ycyxinfo yc = YcList.get(i);
//
//                saved++;
//                sb.append("update tblycvalue set ycvalue=").
//                        append(yc.fValue).append(",REFRESHTIME='")
//                        .append(RefreshTime).append("'").append(" where YCH='")
//                        .append(yc.strDH).append("' and valuesource=").append(SloaderView.cfg_ca);
//                mb.sqlList.add(sb.toString());
//                yc.IsSavedDb = true;
//            }
//            //sb.append("END;");
//            if (saved > 0) {
//                es.execute(mb);
//                
//                //String sql = sb.toString();
//                //SloaderView.instance.AppendInfo(sql);
//                //SloaderView.instance.ExeUpdateSql(sql);
////                SloaderView.instance.ExecuteBanch(sqls);
////                SloaderView.instance.AppendInfo("DB update YC " + saved);
//            }
//            YcList.clear();
//        }
        return saved;
    }
    public int SaveYxToDb(List<ycyxinfo> YxList, String RefreshTime) {
        int ct = YxList.size();
        int saved = 0;
//        if (ct > 0) {
//            MySqlbatch mb = new MySqlbatch();
//            mb.ini(SloaderView.instance.cfg_host);
//            mb.HintInfo = "YX update count:" + ct;
//            //List<String> sqls = new LinkedList<String>();
//            StringBuilder sb = new StringBuilder();
//            //sb.append("BEGIN\n");
//            for (int i = 0; i < ct; i++) {
//                sb.delete(0, sb.length());
//                ycyxinfo yx = YxList.get(i);
//
//                saved++;
//                sb.append("update tblyxvalue set yxvalue=").append(yx.iValue).append(",REFRESHTIME='").append(RefreshTime).append("'")
//                        .append(" where YXH='").append(yx.strDH).append("' and valuesource=").append(SloaderView.cfg_ca);
//                mb.sqlList.add(sb.toString());
//                yx.IsSavedDb = true;
//            }
//            //sb.append("END;");
//
//            if (saved > 0) {
//                es.execute(mb);
//
//                 
//            }
//            YxList.clear();
//        }
        return saved;
    }
    //@Override
    public void SaveToRtdb(List<ycyxinfo> YcList, List<ycyxinfo> YxList) {
        try {
            if (helper.conn == null || helper.conn.isClosed()) {
                helper.dbDir = "tcp://127.0.0.1:" + cfg_insidePort;
                helper.Connect("sa", "sa");
            }
            if (helper.stat.isClosed()) {
                helper.stat = helper.conn.createStatement();
            }
        } catch (Exception e) {
            out.AppendError("JkSave->SaveToRtdb()->访问实时库错误 " + e.toString());
        }
        try {
            String isql_yc = "insert into tblycvalue (ControlArea,ych,ycvalue,refreshtime,czh,id) values (?,?,?,?,?,?)";
            String usql_yc = "update tblycvalue set ycvalue=?,REFRESHTIME=? where YCH=? and CZH=? and ControlArea=?";
            PreparedStatement ps_usql_yc = helper.conn.prepareStatement(usql_yc);
            PreparedStatement ps_isql_yc = helper.conn.prepareStatement(isql_yc);

            Timestamp tst = new Timestamp(System.currentTimeMillis());
            Date now = tst;
            String RefreshTime = df.format(tst);
            StringBuilder sb = new StringBuilder();
            int ic = 0;
            int uc = 0;
            int ct = YcList.size();
            String dh_error = "";
            if (ct > 0) {
                for (int i = 0; i < ct; i++) {
                    ycyxinfo yc = YcList.get(i);
                    sb.setLength(0);
                    if (map_yc.containsKey(yc.iDH)) {
                        ps_usql_yc.setInt(3, yc.iDH);
                        ps_usql_yc.setFloat(1, yc.fValue);
                        ps_usql_yc.setTimestamp(2, tst);
                        ps_usql_yc.setInt(4, yc.iCZH);
                        ps_usql_yc.setInt(5, CHANNEL);
                        ps_usql_yc.addBatch();
                        uc++;
//                        sb.append("update tblycvalue set ycvalue=").
//                                append(yc.fValue).
//                                append(",REFRESHTIME='").
//                                append(RefreshTime).append("'").
//                                append(" where YCH=").
//                                append(yc.strDH);
                    } else {
                        ps_isql_yc.setInt(1, CHANNEL);
                        ps_isql_yc.setInt(2, yc.iDH);
                        ps_isql_yc.setFloat(3, yc.fValue);
                        ps_isql_yc.setTimestamp(4, tst);
                        ps_isql_yc.setInt(5, yc.iCZH);
                        ps_isql_yc.setInt(6, CHANNEL * 10000 + yc.iDH);
                        ps_isql_yc.addBatch();
                        ic++;
//                        sb.append("insert into tblycvalue (ych,ycvalue,refreshtime) values (").
//                                append(yc.iDH).append(',').
//                                append(yc.fValue).append(",'").
//                                append(RefreshTime).append("')");
                    }
                    map_yc.put(yc.iDH, yc.fValue);
                }
                if (ic > 0) {
                    ps_isql_yc.executeBatch();
                }
                if (uc > 0) {
                    ps_usql_yc.executeBatch();
                }
                YcList.clear();
            }
            String isql_yx = "insert into tblyxvalue (ControlArea,yxh,yxvalue,refreshtime,sb_time,id,czh) values (?,?,?,?,?,?,?)";
            String usql_yx = "update tblyxvalue set yxvalue=?,REFRESHTIME=?,sb_time=? where YXH=? and CZH=? and ControlArea=?";
            PreparedStatement ps_usql_yx = helper.conn.prepareStatement(usql_yx);
            PreparedStatement ps_isql_yx = helper.conn.prepareStatement(isql_yx);
            ct = YxList.size();
            dh_error = "";
            uc = 0;
            ic = 0;
            if (ct > 0) {
                for (int i = 0; i < ct; i++) {
                    ycyxinfo yx = YxList.get(i);
                    sb.setLength(0);
                    if (map_yx.containsKey(yx.iDH)) {
                        ps_usql_yx.setInt(6, CHANNEL);
                        ps_usql_yx.setInt(1, yx.iValue);
                        ps_usql_yx.setTimestamp(2, tst);
                        if (yx.TimeStr != null) {
                            Timestamp sta = Timestamp.valueOf(yx.TimeStr);
                            ps_usql_yx.setTimestamp(2, sta);
                            ps_usql_yx.setTimestamp(3, sta);
                        } else {
                            ps_usql_yx.setTimestamp(3, tst);
                        }
                        ps_usql_yx.setInt(4, yx.iDH);
                        ps_usql_yx.setInt(5, yx.iCZH);
                        ps_usql_yx.addBatch();
                        uc++;
//                        sb.append("update tblyxvalue set yxvalue=").
//                                append(yx.iValue).
//                                append(",REFRESHTIME='").
//                                append(RefreshTime).append("'").
//                                append(",sb_time=").append((yx.TimeStr==null)?"null":("'"+yx.TimeStr+"'")).
//                                append(" where YXH=").
//                                append(yx.iDH);

                    } else {
                        ps_isql_yx.setInt(1, CHANNEL);
                        ps_isql_yx.setInt(2, yx.iDH);
                        ps_isql_yx.setInt(3, yx.iValue);
                        ps_isql_yx.setTimestamp(4, tst);
                        if (yx.TimeStr != null) {
                            Timestamp sta = Timestamp.valueOf(yx.TimeStr);
                            ps_isql_yx.setTimestamp(4, sta);
                            ps_isql_yx.setTimestamp(5, sta);
                        } else {
                            ps_isql_yx.setTimestamp(5, tst);
                        }
                        ps_isql_yx.setInt(6, CHANNEL * 10000 + yx.iDH);
                        ps_isql_yx.setInt(7, yx.iCZH);
                        ps_isql_yx.addBatch();
                        ic++;
//                        sb.append("insert into tblyxvalue (yxh,yxvalue,refreshtime,sb_time) values (").
//                                append(yx.iDH).append(',').
//                                append(yx.iValue).append(",'").
//                                append(RefreshTime).append("',").append((yx.TimeStr==null)?"null":("'"+yx.TimeStr+"'")).
//                                append(")");
                    }
                    map_yx.put(yx.iDH, yx.iValue);
                }
                if (ic > 0) {
                    ps_isql_yx.executeBatch();
                }
                if (uc > 0) {
                    ps_usql_yx.executeBatch();
                }
                YxList.clear();
            }
        } catch (Exception e) {
            out.AppendInfo("SaveToRtdb()->" + e.toString());

        }
    }
    
    public void SaveToMysql(List<ycyxinfo> YcList, List<ycyxinfo> YxList) {
        PreparedStatement ps_usql_yc = null;
        PreparedStatement ps_isql_yc = null;
        PreparedStatement ps_usql_yx = null;
        PreparedStatement ps_isql_yx = null;
        try {
            if (mysqlConn == null || mysqlConn.isClosed()) {
                mysqlConn = DriverManager.getConnection(url, user, password);
            }
            String isql_yc = "insert into tblycvalue (CHANNEL,ych,ycvalue,refreshtime,czh,id,CONTROLAREA,SUBSTATIONID,EQUIPMENTID,name) values (?,?,?,?,?,?,'-1','-1','-1','未定义点')";
            String usql_yc = "update tblycvalue set ycvalue=?,REFRESHTIME=? where YCH=? and CZH=? and CHANNEL=?";
            ps_usql_yc = mysqlConn.prepareStatement(usql_yc);
            ps_isql_yc = mysqlConn.prepareStatement(isql_yc);

            Timestamp tst = new Timestamp(System.currentTimeMillis());
            Date now = tst;
            String RefreshTime = df.format(tst);
            StringBuilder sb = new StringBuilder();
            int ic = 0;
            int uc = 0;
            int ct = YcList.size();
            String dh_error = "";
            if (ct > 0) {
                for (int i = 0; i < ct; i++) {
                    ycyxinfo yc = YcList.get(i);
                    sb.setLength(0);
                    if (map_yc.containsKey(yc.iDH)) {
                        ps_usql_yc.setInt(3, yc.iDH);
                        ps_usql_yc.setFloat(1, yc.fValue);
                        ps_usql_yc.setTimestamp(2, tst);
                        ps_usql_yc.setInt(4, yc.iCZH);
                        ps_usql_yc.setInt(5, CHANNEL);
                        ps_usql_yc.addBatch();
                        uc++;
//                        sb.append("update tblycvalue set ycvalue=").
//                                append(yc.fValue).
//                                append(",REFRESHTIME='").
//                                append(RefreshTime).append("'").
//                                append(" where YCH=").
//                                append(yc.strDH);
                    } else {
                        ps_isql_yc.setInt(1, CHANNEL);
                        ps_isql_yc.setInt(2, yc.iDH);
                        ps_isql_yc.setFloat(3, yc.fValue);
                        ps_isql_yc.setTimestamp(4, tst);
                        ps_isql_yc.setInt(5, yc.iCZH);
                        ps_isql_yc.setInt(6, CHANNEL * 10000 + yc.iDH);
                        
                        ps_isql_yc.addBatch();
                        ic++;
//                        sb.append("insert into tblycvalue (ych,ycvalue,refreshtime) values (").
//                                append(yc.iDH).append(',').
//                                append(yc.fValue).append(",'").
//                                append(RefreshTime).append("')");
                    }
                    map_yc.put(yc.iDH, yc.fValue);
                }
                if (ic > 0) {
                    ps_isql_yc.executeBatch();
                }
                if (uc > 0) {
                    ps_usql_yc.executeBatch();
                }
                YcList.clear();
            }
            String isql_yx = "insert into tblyxvalue (CHANNEL,yxh,yxvalue,refreshtime,sb_time,id,czh,CONTROLAREA,SUBSTATIONID,EQUIPMENTID,name) values (?,?,?,?,?,?,?,'-1','-1','-1','未定义点')";
            String usql_yx = "update tblyxvalue set yxvalue=?,REFRESHTIME=?,sb_time=? where YXH=? and CZH=? and CHANNEL=?";
            ps_usql_yx = mysqlConn.prepareStatement(usql_yx);
            ps_isql_yx = mysqlConn.prepareStatement(isql_yx);
            ct = YxList.size();
            dh_error = "";
            uc = 0;
            ic = 0;
            if (ct > 0) {
                for (int i = 0; i < ct; i++) {
                    ycyxinfo yx = YxList.get(i);
                    sb.setLength(0);
                    if (map_yx.containsKey(yx.iDH)) {
                        ps_usql_yx.setInt(6, CHANNEL);
                        ps_usql_yx.setInt(1, yx.iValue);
                        ps_usql_yx.setTimestamp(2, tst);
                        if (yx.TimeStr != null) {
                            Timestamp sta = Timestamp.valueOf(yx.TimeStr);
                            ps_usql_yx.setTimestamp(2, sta);
                            ps_usql_yx.setTimestamp(3, sta);
                        } else {
                            ps_usql_yx.setTimestamp(3, tst);
                        }
                        ps_usql_yx.setInt(4, yx.iDH);
                        ps_usql_yx.setInt(5, yx.iCZH);
                        ps_usql_yx.addBatch();
                        uc++;
//                        sb.append("update tblyxvalue set yxvalue=").
//                                append(yx.iValue).
//                                append(",REFRESHTIME='").
//                                append(RefreshTime).append("'").
//                                append(",sb_time=").append((yx.TimeStr==null)?"null":("'"+yx.TimeStr+"'")).
//                                append(" where YXH=").
//                                append(yx.iDH);

                    } else {
                        ps_isql_yx.setInt(1, CHANNEL);
                        ps_isql_yx.setInt(2, yx.iDH);
                        ps_isql_yx.setInt(3, yx.iValue);
                        ps_isql_yx.setTimestamp(4, tst);
                        if (yx.TimeStr != null) {
                            Timestamp sta = Timestamp.valueOf(yx.TimeStr);
                            ps_isql_yx.setTimestamp(4, sta);
                            ps_isql_yx.setTimestamp(5, sta);
                        } else {
                            ps_isql_yx.setTimestamp(5, tst);
                        }
                        ps_isql_yx.setInt(6, CHANNEL * 10000 + yx.iDH);
                        ps_isql_yx.setInt(7, yx.iCZH);
                        ps_isql_yx.addBatch();
                        ic++;
//                        sb.append("insert into tblyxvalue (yxh,yxvalue,refreshtime,sb_time) values (").
//                                append(yx.iDH).append(',').
//                                append(yx.iValue).append(",'").
//                                append(RefreshTime).append("',").append((yx.TimeStr==null)?"null":("'"+yx.TimeStr+"'")).
//                                append(")");
                    }
                    map_yx.put(yx.iDH, yx.iValue);
                }
                if (ic > 0) {
                    ps_isql_yx.executeBatch();
                }
                if (uc > 0) {
                    ps_usql_yx.executeBatch();
                }
                YxList.clear();
            }
            mysqlConn.close();
        } catch (Exception e) {
            out.AppendInfo("savetomysql()->" + e.toString());

        }
    }

    @Override
    public void SaveTodb(List<ycyxinfo> YcList, List<ycyxinfo> YxList) {
        SaveToMysql(YcList, YxList);
    }
}
/*
 //执行多条语句
 public void ExecuteBanch(Vector vector)
 {
 if (vector == null || vector.size() <= 0)
 {
 return;
 }
 try
 {
 Connection connection = GetConnection();
 if (connection != null)
 {
 Statement statement = connection.createStatement();
 for (int i = 0; i < vector.size(); i++)
 {
 statement.addBatch(vector.get(i).toString());
 }
 statement.executeBatch();
 connection.close();
 }
 }
 catch (Exception e)
 {
 System.out.println(e.toString());
 }
 }*/

