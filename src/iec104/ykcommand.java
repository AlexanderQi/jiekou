/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iec104;

import ClrIFace.ykobj;
import ClrIFace.ykExeObj;
import java.sql.*;
import java.util.LinkedList;
//import java.util.List;
import java.util.Queue;
import ClrIFace.ClrIFace_YK;
//import javax.print.attribute.standard.DateTimeAtCompleted;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 *
 * @author ibm
 */
public class ykcommand implements ClrIFace_YK {

    public Queue<ykobj> YkQueue = new LinkedList<ykobj>();
    public Queue<ykExeObj> ExeQueue = new LinkedList<ykExeObj>();
    public boolean CanLoad = true;
    public ykExeObj CurExeObj = null;
    public Date LastCtrlCmdTime;
    public cfgParam.JkParam jkp;
    public JkOut out = JkOut.Instance();
    public H2Helper helper = H2Helper.Instance();

    public ykcommand() {
        jkp = Jk104.Instance.jkp;
        ini();
    }

    void ini() {

        ClearYK();
    }

    public long GetBetween2Cur(Date t) {
        //SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long between = -1;
        try {
            Date begin = t;    //"2004-01-02 11:30:24"
            Date end = new Date();
            between = (end.getTime() - begin.getTime()) / 1000;//除以1000是为了转换成秒
        } catch (Exception e) {
            out.AppendError("GetBetween2Cur()->" + e.toString());
        }
        return between;
    }

    Statement ykstatement = null;
    public synchronized  void LoadYK() {
        if (YkQueue.size() != 0) {
            return;
        }      
        try{
            if(helper.conn == null || helper.conn.isClosed()){
                helper.dbDir = "tcp://" + jkp.cfg_host + ":" + jkp.cfg_insidePort;
                helper.Connect("sa","sa");
            }
            if(ykstatement == null||ykstatement.isClosed()){
                ykstatement = helper.conn.createStatement();
            }
        }catch(Exception e){
            out.AppendError("LoadYK()->连接实时库错误 "+e.toString());
        }
        
        String sql = "select t.id,t.CONTROLAREA,t.schemeid,t.schemeindex,t.cmddatetime,t.czh,t.ykyth,t.ykytvalue,t.ytvalue,t.ykyttype from tblcommand t where t.dealtag=0 and t.CONTROLAREA="
                + jkp.cfg_ca + " order by t.id,t.schemeid,t.schemeindex";
        //ResultSet rs = SloaderView.instance.ExeSql(sql);
        ResultSet rs = null;
        try {    
            rs = ykstatement.executeQuery(sql);
            while (rs.next()) {
                java.util.Date CmdTime = rs.getTimestamp("cmddatetime");
                int Id = rs.getInt("ID");
                if (GetBetween2Cur(CmdTime) > 180) //2012-9-27 3分钟以前的命令就放弃处理。
                {
                    out.AppendInfo("发现命令ID:" + Id + " 已过有效期(180s)被丢弃.");
                    ChangeOneCommandState(Id, 4);
                    continue;
                }else{
                    ykobj yk = new ykobj();
                    yk.dealtag = 0;
                    yk.ca_id = -1;
                    yk.Tag = -1;
                    yk.id = Id;
                    yk.sid = rs.getInt("schemeid");//2014-4-14 当sid=-1 仅预置  sid=-2 仅遥控； 
                    yk.yktime = (java.util.Date) CmdTime.clone();
                    yk.czh = rs.getInt("czh");
                    yk.ykh = rs.getInt("ykyth");
                    yk.ykyttype = rs.getInt("ykyttype");
                    if (rs.wasNull()) {
                        yk.ykyttype = -1;
                    }
                    if(yk.ykyttype == 0)
                        yk.ykh += jkp.ykh_offset;
                    else if(yk.ykyttype == 1)
                        yk.ykh += jkp.yth_offset;

                    yk.ytvalue = rs.getFloat("ytvalue");
                    if (rs.wasNull()) {
                        yk.ytvalue = Float.NaN;
                    }
                    yk.ykvalue = rs.getInt("ykytvalue");
                    if (rs.wasNull()) {
                        yk.ykvalue = -1;
                    }
                    if ((yk.ykyttype != -1) && (yk.ytvalue != Float.NaN || yk.ykvalue != -1)) {
                        YkQueue.offer(yk);
                        ChangeOneCommandState(yk.id, 1);
                    } else {
                        out.AppendInfo("载入命令失败，遥控遥调参数不完整,遥控类型，遥调值或遥控值...");
                    }
                }
            }
            int ct = YkQueue.size();
            out.AppendInfo("载入命令: " + ct);
            CanLoad = (ct == 0);
        } catch (Exception e) {
            out.AppendError(" LoadYK()->"+e.toString());
        }
    }
    public SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public String GetCurTimeStr() {
        Date now = new Date();
        return df.format(now);
    }

    Statement CommandState = null;
    public void ChangeOneCommandState(int ID, int NewState) {
        StringBuilder sb = new StringBuilder();
        String rt = GetCurTimeStr();
        //to_date('时间','yyyy-mm-dd hh24:mi:ss')   for oracle
//        sb.append("update tblcommand set dealtag=").
//                append(NewState).append(", dealdatetime=to_date('").append(rt).append("','yyyy-mm-dd hh24:mi:ss') where id=").append(ID);

        sb.append("update tblcommand set dealtag=").
                append(NewState).append(", dealdatetime='").append(rt).append("' where id=").append(ID);

        String sql = sb.toString();
        try {
            if(CommandState == null || CommandState.isClosed())
                CommandState = helper.conn.createStatement();
            CommandState.execute(sql);     
        } catch (Exception e) {
            out.AppendError("ykcommand->ChangeOneCommandState()"+e.toString());
        }

    }

    void CreateExeList() {
        ykobj yk = YkQueue.poll();
        if (yk == null) {
            out.AppendInfo("本批次命令完成");
            CanLoad = true;
            return;
        }
        String str;
//        if (yk.sid == 0) //单个命令
//        {
        //str = "准备发送命令 YKH="+(yk.ykh-0x6001)+" Act="+yk.ykvalue;
        //SloaderView.instance.AppendInfo(str);
       if (yk.ykyttype == 0) {//遥控类型
            if(yk.sid == -1){    //仅预置
                Offer(yk, ykExeObj.ExeType.Prepare);
            }else if(yk.sid == -2){  //仅控制
                Offer(yk, ykExeObj.ExeType.Contrl);
            }else{            //自动预置控制
                Offer(yk, ykExeObj.ExeType.Prepare);
                Offer(yk, ykExeObj.ExeType.Contrl);
            }
        } else if (yk.ykyttype == 1) {//遥调类型
            Offer(yk, ykExeObj.ExeType.Contrl);
        }
//        } 
//        else //组合命令
//        {
//            ykobj yk2 = YkQueue.poll();
//            if (yk2.sid == yk.sid) {
//                //str = "准备发送组合命令 YK1.YKH="+(yk.ykh-0x6001)+" YK1.Act="+yk.ykvalue + " YK2.YKH=" + (yk2.ykh-0x6001) + " YK2.Act=" + yk2.ykvalue;
//                //SloaderView.instance.AppendInfo(str);
//
//                Offer(yk, ykExeObj.ExeType.Prepare);
//                Offer(yk, ykExeObj.ExeType.Cancel);
//                Offer(yk2, ykExeObj.ExeType.Prepare);
//                Offer(yk2, ykExeObj.ExeType.Cancel);
//
//                Offer(yk, ykExeObj.ExeType.Prepare);
//                Offer(yk, ykExeObj.ExeType.Contrl);
//                Offer(yk2, ykExeObj.ExeType.Prepare);
//                Offer(yk2, ykExeObj.ExeType.Contrl);
//            }
//        }
    }

    void Offer(ykobj yk, ykExeObj.ExeType type) {
        ykExeObj exe_ = new ykExeObj();
        exe_.id = yk.id;
        exe_.czh = yk.czh;
        exe_.ykh = yk.ykh;
        exe_.ykyttype = yk.ykyttype;
        exe_.ykType = type;
        if (yk.ykyttype == 0) {
            exe_.ykvalue = yk.ykvalue;
            exe_.ykType = type;
            if (type == ykExeObj.ExeType.Prepare) {
                exe_.ykvalue += 0x80;
            }
        } else {
            exe_.ytvalue = yk.ytvalue;
        }
        ExeQueue.offer(exe_);
    }

    public ykExeObj PollExeObj() {
        CurExeObj = ExeQueue.poll();
        if (CurExeObj == null) {
            CreateExeList();
            CurExeObj = ExeQueue.poll();
        }
        return CurExeObj;
    }

    public ykExeObj GetYkExeObj() {
        return PollExeObj();
    }

    public void YkExeResult(ykExeObj obj) {
        if (obj.IsReturn) {
            if (obj.IsSuccess) {
                if (obj.ykType == ykExeObj.ExeType.Contrl) {
                    ChangeOneCommandState(obj.id, 2);
                }
            } else {
                ExeQueue.clear();
                out.AppendInfo("当前命令失败，撤销相关命令");
                ChangeOneCommandState(obj.id, 3);
                if (obj.ykType == ykExeObj.ExeType.Prepare) //如果是预置失败，则准备发送该命令的预置撤销。
                {
                    obj.ykType = ykExeObj.ExeType.Cancel;
                    ExeQueue.offer(obj);
                }
            }
        }
    }

    public void ClearYK() {
        ExeQueue.clear();
        YkQueue.clear();
    }

    public void CancelCurrentExeQueue() {
        ExeQueue.clear();
    }
}
