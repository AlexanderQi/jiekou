/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iec104;

//import com.sun.corba.se.spi.servicecontext.SendingContextServiceContext;
import Clr68Hv2.ClrSocket;
import java.util.TimerTask;
import java.util.Date;
import java.text.SimpleDateFormat;
import JkSave.jksave;
public class clrTimer extends TimerTask
{
    //1秒定时触发
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public int Second_GeneralCall = 10;     //60 * 5;
    public int Second_ReConnect = 5;
    public int Second_GetCommand = 1;
    private int S_GeneralCall = 0;
    private int S_ReConnect = 0;
    private int S_GetCommand = 0;
    public int timeout_t1 = 8;
    public int timeout_t2 = 6;           //t2 < t1
    public int timeout_t3 = 30;
    public Date LastGCallTime = null;
//    private int t1 = 0;
    private int t2 = 0;
//    private int t3 = 0;
    private boolean Cango = false;
   // private char st[] = {'|', '/', '-', '\\'}; //意义不大
    private char st_i = 0;
    private cfgParam.JkParam jkp = null;
    private JkOut out = null;
    private jksave jks = null;
    public clrTimer()
    {
        jkp = Jk104.Instance.jkp;
        out = JkOut.Instance();
        jks = jksave.Instance();
        Second_GeneralCall = jkp.cfg_gcall; //SloaderView.cfg_gcall;
        if (Second_GeneralCall > 0 && Second_GeneralCall < 5) {
            Second_GeneralCall = 5;
        }
        if(Second_GeneralCall >= 6)
            S_GeneralCall = Second_GeneralCall - 5;
    }

//    private char getMySt()
//    {
//        if (st_i > 3) {
//            st_i = 0;
//        }
//        return st[st_i++];
//    }

    @Override
    public void run()
    {
        //SloaderView.instance.AppendDebug("Timer action");
        try {
            S_ReConnect++;
            out.AppendDebug(">>>TDeal");
            if(jkp.cfg_HotBackup == 1)
            {              
               //
            }
            if(Jk104.Instance.clrThread == null)
            {
                Jk104.Instance.ConnectServer();
                return;
            }
            
            if (!Jk104.Instance.clrThread.isAlive()) {
                Cango = false;
                if (S_ReConnect >= Second_ReConnect) {
                    S_ReConnect = 0;
                    Jk104.Instance.ConnectServer();
                }
                S_GeneralCall = S_GetCommand = 0;
                return;
            } else {
                Cango = true;
            }

            S_GeneralCall++;
            S_GetCommand++;
            t2++;
            //SloaderView.instance.AppendInfo("step2");
            boolean onlyone = true;
            if (ClrSocket.instance != null && Cango) {
                if (Second_GeneralCall > 0 && S_GeneralCall >= Second_GeneralCall) {
                    S_GeneralCall = 0;
                    if(ClrSocket.instance.IsOpenning || true)  //debug "true"
                    {
                        Date now = new Date();
                        if(LastGCallTime != null){
                            long bet = (LastGCallTime.getTime() - now.getTime())/1000;
                            out.AppendInfo("LastGC="+bet + "  Second_GeneralCall="+Second_GeneralCall);
                            if(bet >= Second_GeneralCall*2){
                                Jk104.Instance.clr_instance.OnGeneralCall = true;
                            }
                        }
                        
                        Jk104.Instance.clr_instance.SendGeneralCall();
                    }
                }
                
                Date now = new Date();
                Date begin = ClrSocket.instance.LastRcvTime;
                if (begin != null) {
                    long between = (now.getTime() - begin.getTime()) / 1000;//除以1000是为了转换成秒
                    //SloaderView.instance.StatusBarText("ti=" + between);
                    long tt1 = (between - timeout_t1);
                    if (tt1 == 0) {
                        if(onlyone){
                            onlyone = false;
                            Jk104.Instance.clr_instance.SendUframe(ClrSocket.TESTFR_ACT, "t1 U帧");
                            out.AppendDebug("t1 发送U帧");
                            //ClrSocket.instance.SendGeneralCall();
                        }
                    }
                    
                    if(between == timeout_t3)
                    {
                        ClrSocket.instance.DiscSrv();
                        return;
                    }

                }
                if (S_GetCommand >= Second_GetCommand) {
                    S_GetCommand = 0;
                    //if(SloaderView.instance.cfg_IsYK == 1)
                    out.AppendStr(df.format(now)+">>Pulse");
                   // jks.myudp.SendMsg();   //通知守护程序（心跳信号）

                    if(Jk104.Instance.ykcmd != null){
                        Jk104.Instance.ykcmd.LoadYK();
                        Jk104.Instance.clr_instance.SendYkCommand();
                    }
                }
            }
        } catch (Exception e) {
            out.AppendError("clrTimer->run() " + e.toString());

        }finally
        {
            out.AppendDebug("<<<TDeal");
        }
    }
}
