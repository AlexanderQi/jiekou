/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Clr68Hv2;

import ClrIFace.ClrIFace_Out;
import ClrIFace.ClrIFace_YCYX;
import ClrIFace.ClrIFace_YK;
import ClrIFace.ycyxinfo;
import ClrIFace.ykExeObj;
import JkSave.jksave;
import iec104.Jk104;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.LinkedList;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

//import sloader.SloaderView;
/**
 *
 * @author liqi
 */
public class ClrSocket implements Runnable {

    static final byte STARTDT_ACT = 0x7;
    static final byte STARTDT_ACK = 0xb;
    static final byte STOPDT_ACT = 0x13;
    static final byte STOPDT_ACK = 0x23;
    public static final byte TESTFR_ACT = 0x43;
    static final byte TESTFR_ACK = (byte) 0x83;
    static final int RcvRufLen = 1024 * 64;
    static final int SendBufLen = 512;
    String ip1, ip2;
    int port;
    List<ycyxinfo> SwapList_YC = new LinkedList<ycyxinfo>();
    List<ycyxinfo> SwapList_YX = new LinkedList<ycyxinfo>();
    int SwapCursor_YC, SwapCursor_YX;
    int RcvTotalLen = 0;  //已接受未处理数据长度，完整性检查通过后清0；
    byte[] tmpRcvBuf = new byte[RcvRufLen * 2];
    byte[] RcvBuf = new byte[RcvRufLen];
    byte[] SendBuf = new byte[SendBufLen];
    int ClientSendNo, //对方发送序号
            ClientRecNo, //对方接收序号
            ClientAck, //对方已确认序号
            RcvNo, //本地接收序号
            SendNo;  	//本地发送序号
    long RcvApdus;
    int SendLength, ReceiveLength;
    short _k, _w;
    public boolean IsOpenning = false;
    private Socket clr = null;
    private InputStream in = null;
    private OutputStream out = null;
    public boolean OnGeneralCall = false; //总召期间
    
    public boolean OnWaitingForReturnOfYKYT = false;  //等待遥控遥调返校期间
    
    public static ClrSocket instance;
    boolean IsSocketError = false;
    public Date LastRcvTime;
    public ClrIFace_YCYX SaveFunc;
    public ClrIFace_Out ClrOut;
    public ClrIFace_YK YkFunc;
    public int ych_offset = 0x4001;
    public int yxh_offset = 0x1;
    public int ykh_offset = 0x6001;
    private ykExeObj CurYkExeObj = null;
    public boolean PrintRcvFrame = false;
    public boolean PrintSendFrame = false;
    public int PrintByteCount = 256;
    public int CopyrightTag = 0;  //版本标志 0：国标，1：南瑞

    void ini() {
        IsSocketError = false;
        OnGeneralCall = false;
        SendLength = ReceiveLength = 0;
        RcvApdus =
                ClientSendNo =
                ClientRecNo =
                ClientAck =
                RcvNo =
                SendNo = 0;
        SwapList_YC.clear();
        SwapList_YX.clear();
        oneoff = true;
        SwapCursor_YC = SwapCursor_YX = 0;
        RcvTotalLen = 0;
        CurYkExeObj = new ykExeObj();
        CurYkExeObj.IsReturn = true;
    }

    public void SetSaveFunc(ClrIFace_YCYX SaveFunc_) {
        SaveFunc = SaveFunc_;
    }

    public void SetInfoOut(ClrIFace_Out Out_) {
        ClrOut = Out_;
    }

    public void SetYkFunc(ClrIFace_YK YkFunc_) {
        YkFunc = YkFunc_;
    }
    
    public void SaveAYX(int czh,int dh,int value,String hint){
        try{
            ycyxinfo yxinfo = new ycyxinfo();
            yxinfo.iDH = dh;
            yxinfo.iCZH = czh;
            yxinfo.strDH = String.valueOf(dh);
            yxinfo.strCZH = String.valueOf(czh);
            yxinfo.iValue = value;
            yxinfo.strValue = String.valueOf(value);
            yxinfo.IsSaveFile = false;
            yxinfo.IsSaveRtdb = false;
            yxinfo.IsSavedDb = false;
            SwapList_YX.add(yxinfo);
            SaveData(false);
            ClrOut.AppendInfo(hint);//"104接口通道状态:"+value + " CA:"+Jk104.Instance.jkp.cfg_ca
        }catch(Exception e){
            ClrOut.AppendError(e.toString());
        }
    }
    
    public boolean IsFatalError = false;

    public String SelfIp = "";
    public int SelfPort = 2404;
    public boolean Connect() {
        boolean isSuccess = false;
        if (IsFatalError) {
            ClrOut.AppendInfo("严重错误，停止连接");
            return false;
        }
        try {
            ini();
            IsSocketError = false;
            ClrOut.AppendInfo("准备连接" + cur_ip + " port:" + port);
            clr = new Socket();
            SocketAddress addr = new InetSocketAddress(cur_ip, port);
//            if(!SelfIp.equals("")){
//                SocketAddress SelfAddr = new InetSocketAddress(SelfIp, SelfPort);   //2014-3-30 绑定本机一个指定IP
//                clr.bind(SelfAddr);
//            }
            //clr.setSoTimeout(1000 * 7);  //7秒无响应超时
            clr.connect(addr, 7000);     //7秒连接超时
            clr.setSoTimeout(1000 * 7);  //7秒无响应超时
            IsSocketError = false;
            //IsFirstGC = true;
            ClrOut.AppendInfo("连接上" + cur_ip);
            isSuccess = true;
            ClrOut.SetConnectInfo(cur_ip);
            in = clr.getInputStream();
            out = clr.getOutputStream();
            Thread.sleep(500);
            SendUframe(STARTDT_ACT, "启动帧");
            //SaveAYX(100,90104,1,"104接口 通道号:"+Jk104.Instance.jkp.cfg_ca+" 已连接");
            
            Thread.sleep(3000);
            SendGeneralCall();
        } catch (Exception e) {
            isSuccess = false;
            IsSocketError = true;
            ClrOut.AppendInfo("连接失败");
            ClrOut.AppendError("Connect()->" + e.toString());
            //SaveAYX(100,90104,0,"104接口 通道号:"+Jk104.Instance.jkp.cfg_ca+" 已断开");
            changeSrvIp();
        } finally {
            return isSuccess;
        }
    }

    public void changeSrvIp() {
        if (cur_ip.equals(ip2)) {
            cur_ip = ip1;
        } else {
            cur_ip = ip2;
        }
        ClrOut.AppendError("服务IP转为 " + cur_ip);
    }
    public String cur_ip;

    public ClrSocket(String ip1, String ip2, int port) {
        instance = this;
        this.ip1 = ip1;
        this.ip2 = ip2;
        cur_ip = ip1;
        this.port = port;
        //CurYkExeObj.IsReturn = true;
    }

    public boolean IsClosed() {
        return clr.isClosed();
    }

    public boolean IsConnected() {
        return clr.isConnected();
    }

    private boolean IsIframe(byte v) {
        int i = v;
        return ((i & 1) == 0);

    }

    private boolean IsSframe(byte v) {
        int i = v;
        boolean b = false;
        if ((i & 1) == 1) {
            if ((i & (1 << 1)) == 0) {
                b = true;
            }
        }
        return b;
    }

    private boolean IsUframe(byte v) {
        int i = v;
        boolean b = false;
        if ((i & 1) == 1) {
            if ((i & (1 << 1)) == 1) {
                b = true;
            }
        }
        return b;
    }

    private int GetRcvNumber(int offset) {
        return GetSendNumber(offset); //算法一样,位置不同而已
    }

    private int GetSendNumber(int offset) {
        int v = 0;
        v = GetUnsignedValue(RcvBuf[offset + 1]);
        v <<= 8;
        v += GetUnsignedValue(RcvBuf[offset]);
        v >>= 1;
        return v;
    }

    private void SetSendNumber(int offset) {
        int v = SendNo << 1;
        SendBuf[offset] = (byte) (v & 0xFF);
        SendBuf[offset + 1] = (byte) ((v & 0xFF00) >> 8);
    }

    private void SetRcvNumber(int offset) {
        SetSendNumber(offset);
    }
    public String CurThreadName;

    @Override
    public void run() {
        try {
            rundeal();
            ClrOut.AppendInfo("结束本次连接");
            ClrOut.AppendError(CurThreadName + " 结束本次连接");
        } catch (Exception e) {
            ClrOut.AppendInfo("clrsocket-run()->" + e.toString());
        }
    }

    public float GetFloatFromGYZ(int lsb, int msb) {  //归一化值转浮点数 lsb模拟低位 msb模拟高位
        int v = (msb << 8) | lsb;
        return (float) v / 32767;
    }
    
    public float GetFloatFromNrGYZ(int lsb,int msb){
        int v = -9999;
        if(msb>0)
            v = (msb << 8)|lsb;
        else
            v = lsb;
        return (float)v;
    }
    
    

    public int GetGYZ(float v) {  //浮点数转归一化值
        if (v <= 0) {
            return 0;
        }
        v = v * 32767;
        return (int) (v + 0.5);
    }

    public byte GetGyzLsb(int v) {
        return (byte) (v & 0xff);
    }

    public byte GetGyzMsb(int v) {
        return (byte) ((v >> 8) & 0xff);
    }

    private String Get7ByteTime(int offset) {
        String value = "";
        int tmp = GetUnsignedValue(RcvBuf[offset]) + (GetUnsignedValue(RcvBuf[offset + 1]) << 8);  //ms
        int ms = tmp % 1000;
        int ss = tmp / 1000;
        String sss = ((ss < 10) ? "0" : "")+ss+"."+ms;
        tmp = GetUnsignedValue(RcvBuf[offset + 2]) & 0x3f;
        int mm = tmp;
        String smm = ((mm < 10) ? "0" : "") + mm;
        tmp = GetUnsignedValue(RcvBuf[offset + 3]) & 0x1f;
        int hh = tmp;
        String shh = ((hh < 10) ? "0" : "") + hh;
        tmp = GetUnsignedValue(RcvBuf[offset + 4]) & 0x1f;
        int dd = tmp;
        String sdd = ((dd < 10) ? "0" : "") + dd;
        tmp = GetUnsignedValue(RcvBuf[offset + 5]) & 0xf;
        int MM = tmp;
        String sMM = ((MM < 10) ? "0" : "") + MM;
        tmp = GetUnsignedValue(RcvBuf[offset + 6]) & 0x7f;
        int yy = tmp;
        String syy = ((yy < 10) ? "0" : "") + yy;

        value = "20" + syy + "-" + sMM + "-" + sdd + " " + shh + ":" + smm + ":" + sss;
        return value;
    }
    
    private Date Set7ByteTime(int offset) {
        SimpleDateFormat sd = new SimpleDateFormat("yy-MM-dd-HH-mm-ss-SSS");
        Date d = new Date();
//        long mss = d.getTime();
//        long seconds = (mss % (1000 * 60)) / 1000;  
//        int ims = (int)((seconds*1000)+(mss % 1000)); //秒用毫秒代替
        String timestr = sd.format(d);
        String[] strs = timestr.split("-");
        int n = Integer.parseInt(strs[5]);
        n = n*1000;
        n += Integer.parseInt(strs[6]);
        SendBuf[offset] = (byte)(n & 0xff);
        SendBuf[offset+1] = (byte)((n & 0xff00)>>8);
        SendBuf[offset+2] = Byte.parseByte(strs[4]);
        SendBuf[offset+3] = Byte.parseByte(strs[3]);
        SendBuf[offset+4] = Byte.parseByte(strs[2]);
        SendBuf[offset+5] = Byte.parseByte(strs[1]);
        SendBuf[offset+6] = Byte.parseByte(strs[0]);
        ClrOut.AppendInfo("组包时间:"+timestr);
        return d;
    }

    private int Get3ByteValue(int offset) {
        int v = GetUnsignedValue(RcvBuf[offset]);
        v += GetUnsignedValue(RcvBuf[offset + 1]) << 8;
        v += GetUnsignedValue(RcvBuf[offset + 2]) << 16;
        return v;
    }

    private float GetFloatFromBytes(int offset) {
        int maskInt = 0xFF;
        int a = ((RcvBuf[offset + 3] << 24)
                | ((RcvBuf[offset + 2] & maskInt) << 16)
                | ((RcvBuf[offset + 1] & maskInt) << 8)
                | (RcvBuf[offset] & maskInt));
        float v = Float.intBitsToFloat(a);
        return v;
    }

    private void CopyToRcv(int len) {
        if (RcvTotalLen + len > RcvRufLen) {
            return;
        }
        for (int i = 0; i < len; i++) {
            RcvBuf[RcvTotalLen + i] = tmpRcvBuf[i];
        }
        RcvTotalLen += len;
    }
    boolean oneoff = true;

    void rundeal() {
        if (!clr.isConnected()) {
            return;
        }
        while (!clr.isClosed()) {
            try {
                if (IsSocketError) {
                    ClrOut.AppendInfo("rundeal()->通信套接字socket错误");
                    break;
                }

                int rc = in.read(tmpRcvBuf);
                if (rc < 0) {
                    ClrOut.AppendInfo("rundeal()->服务断开");
                    break;
                }
                LastRcvTime = new Date();
                CopyToRcv(rc);
                PrintRcvFrame(rc);
                if (!IntegralityCheck()) {
                    continue;
                }

                int BufCursor = 0;
                //int LastFrame;
                /////////////////////////////////////////////////////////////////
                while (BufCursor < RcvTotalLen) {
                    if (RcvApdus > Integer.MAX_VALUE - 2) {
                        RcvApdus = 0;
                    }
                    // if (!OnGeneralCall) {
                    if (RcvApdus % 8 == 0) {
                        SendSFrame("W=8");
                        ClrOut.AppendInfo("W=8 SFrame");
                    }
                    // }
                    RcvApdus++;

                    //APCI * apci = (APCI *) (&  RcvBuf[BufCursor]);
                    if (IsIframe(RcvBuf[BufCursor + 2])) //if (apci ->  Frame.Iframe.CtrlTag == FrameI_flag)
                    {
                        ClientSendNo = GetSendNumber(BufCursor + 2);
                        ClientRecNo = GetRcvNumber(BufCursor + 4);
                        ClientAck = ClientRecNo;
                        RcvNo = ClientSendNo++;
                        if (RcvNo >= 32767) // 帧数将溢出,32767帧应该是极限 15位最大表示32767
                        {
                            //clr.close();
                            //return;
                            RcvNo = 1;
                        }
                        //str = tr("已发送 %1 帧,对端确认 %2 帧").arg(SendNo).arg(ClientAck);

                        AsduDeal(BufCursor);
                        OnGeneralCall = false;
                    } else if (IsSframe(RcvBuf[BufCursor + 2])) {
                        //ClientAck = apci ->  Frame.SUframe.RcvNum;
                        ClientAck = GetRcvNumber(BufCursor + 4);
                        //String str = "S帧: 已发送 " + SendNo + "帧,对端确认 " + ClientAck + " 帧";
                        //ClrOut.AppendInfo(str);
                    } else {
                        ClientSendNo = GetSendNumber(BufCursor + 2);
                        //str = (tr("'U'帧,"));
                        int us_len = 0;
                        switch (RcvBuf[2]) {
                            case STARTDT_ACK:
                                //str.append(tr("启动确认"));
                                ClrOut.AppendInfo("启动确认");
                                IsOpenning = true;
                                //SendUframe(STARTDT_ACK);
//                                if (oneoff) {
//                                    oneoff = false;
//                                    SendGeneralCall();
//                                }
                                us_len = GetUnsignedValue(RcvBuf[BufCursor + 1]);
                                BufCursor += us_len + 2;
                                continue;
                            case STOPDT_ACK:

                                //ClrOut.AppendInfo("");
                                SendUframe(STOPDT_ACK, "停止确认");
                                us_len = GetUnsignedValue(RcvBuf[BufCursor + 1]);
                                BufCursor += us_len + 2;
                                continue;
                            case TESTFR_ACT:
                                //str.append(tr(""));
                                //ClrOut.AppendInfo();
                                SendUframe(TESTFR_ACK, "测试生效");
                                us_len = GetUnsignedValue(RcvBuf[BufCursor + 1]);
                                BufCursor += us_len + 2;
                                continue;

                            case TESTFR_ACK:
                                ClrOut.AppendInfo("收到测试确认");
                                us_len = GetUnsignedValue(RcvBuf[BufCursor + 1]);
                                BufCursor += us_len + 2;
                                continue;
                        }
                    }
                    //BYTE alen = apci ->  ApduLen;
                    int alen = GetUnsignedValue(RcvBuf[BufCursor + 1]);
                    BufCursor += alen + 2;
                }
//                RcvTotalLen = 0;
//                PrintStatus();
                /////////////////////////////////////////////////////////////////
            } catch (SocketTimeoutException e) {
                SendUframe(TESTFR_ACT, "t2 U帧");

            } catch (Exception e) {
                ClrOut.AppendError("rundeal()->" + e.toString());
                changeSrvIp();
                IsSocketError = true;
                break;
            } finally {
                RcvTotalLen = 0;
                PrintStatus();
            }
        }
    }

    void PrintStatus() {
        String str = "已发送 " + SendNo + "帧,对端确认 " + ClientAck + " 帧";
        ClrOut.StatusBarText(str);
    }

    public void DiscSrv() {
        try {
            clr.close();
        } catch (Exception e) {
            ClrOut.AppendInfo("DiscSrv()->" + e.toString());
        }
    }

    void ZeroBuf() {
        for (int i = 0; i < RcvRufLen; i++) {
            RcvBuf[i] = 0;
        }
    }

    void WriteApci(int len) {

        SendBuf[0] = 0x68;
        SendBuf[1] = (byte) (len - 2);
        int sn = SendNo << 1;
        SendBuf[2] = (byte) (sn & 0xFF);
        SendBuf[3] = (byte) ((sn & 0xFF00) >> 8);
        int rn = RcvNo << 1;
        SendBuf[4] = (byte) (rn & 0xFF);
        SendBuf[5] = (byte) ((rn & 0xFF00) >> 8);
        SendLength = len;
        SendNo++;
        if (SendNo >= 32767) {
            SendNo = 0;
        }
    }

    void WriteAsduHead(int TypeId, int VSQ, int Cause, int CommonAddr) {
        int off = 6;
        SendBuf[off] = (byte) TypeId;
        SendBuf[off + 1] = (byte) VSQ;

        SendBuf[off + 2] = (byte) (Cause & 0xFF);
        SendBuf[off + 3] = (byte) ((Cause & 0xFF00) >> 8);

        SendBuf[off + 4] = (byte) (CommonAddr & 0xFF);
        SendBuf[off + 5] = (byte) ((CommonAddr & 0xFF00) >> 8);
    }

    void WriteAsdu_YK(int TypeId, int VSQ, int Cause, int CommonAddr, int ykh, int Value) {
        int off = 6;
        SendBuf[off] = (byte) TypeId;
        SendBuf[off + 1] = (byte) VSQ;

        SendBuf[off + 2] = (byte) (Cause & 0xFF);
        SendBuf[off + 3] = (byte) ((Cause & 0xFF00) >> 8);

        SendBuf[off + 4] = (byte) (CommonAddr & 0xFF);
        SendBuf[off + 5] = (byte) ((CommonAddr & 0xFF00) >> 8);

        SendBuf[off + 6] = (byte) (ykh & 0xFF);
        SendBuf[off + 7] = (byte) ((ykh & 0xFF00) >> 8);
        SendBuf[off + 8] = (byte) ((ykh & 0xFF0000) >> 16);

        SendBuf[off + 9] = (byte) Value;
    }

    void WriteAsdu_YT_BD(int TypeId, int VSQ, int Cause, int CommonAddr, int ykh, int Value) //遥调 标度值或归一化值
    {
        int off = 6;
        SendBuf[off] = (byte) TypeId;
        SendBuf[off + 1] = (byte) VSQ;

        SendBuf[off + 2] = (byte) (Cause & 0xFF);
        SendBuf[off + 3] = (byte) ((Cause & 0xFF00) >> 8);

        SendBuf[off + 4] = (byte) (CommonAddr & 0xFF);
        SendBuf[off + 5] = (byte) ((CommonAddr & 0xFF00) >> 8);

        SendBuf[off + 6] = (byte) (ykh & 0xFF);
        SendBuf[off + 7] = (byte) ((ykh & 0xFF00) >> 8);
        SendBuf[off + 8] = (byte) ((ykh & 0xFF0000) >> 16);

        SendBuf[off + 9] = (byte) (Value & 0xFF);
        SendBuf[off + 10] = (byte) ((Value & 0xFF00) >> 8);
        SendBuf[off + 11] = 0;
    }
    
    void WriteAsdu_YT_FLOAT(int TypeId, int VSQ, int Cause, int CommonAddr, int ykh, float Value) //遥调 标度值或归一化值
    {
        int off = 6;
        SendBuf[off] = (byte) TypeId;
        SendBuf[off + 1] = (byte) VSQ;

        SendBuf[off + 2] = (byte) (Cause & 0xFF);
        SendBuf[off + 3] = (byte) ((Cause & 0xFF00) >> 8);

        SendBuf[off + 4] = (byte) (CommonAddr & 0xFF);
        SendBuf[off + 5] = (byte) ((CommonAddr & 0xFF00) >> 8);

        SendBuf[off + 6] = (byte) (ykh & 0xFF);
        SendBuf[off + 7] = (byte) ((ykh & 0xFF00) >> 8);
        SendBuf[off + 8] = (byte) ((ykh & 0xFF0000) >> 16);

        int iValue = Float.floatToIntBits(Value);
        SendBuf[off + 9] = (byte) (iValue & 0xFF);
        SendBuf[off + 10] = (byte) ((iValue & 0xFF00) >> 8);
        SendBuf[off + 11] = (byte) ((iValue & 0xFF0000) >> 16);
        SendBuf[off + 12] = (byte) ((iValue & 0xFF000000) >> 24);
        
                
        SendBuf[off + 13] = 0;
    }

    synchronized int Send(String hint) {
        try {
            if (clr == null) {
                return -1;
            }
            if (!clr.isConnected()) {
                return -1;
            }
            if (clr.isClosed()) {
                return -1;
            }
            if (clr.isInputShutdown()) {
                return -1;
            }

            if (out != null) {
                out.write(SendBuf, 0, SendLength);
                PrintSendFrame(SendLength, hint);
            } else {
                ClrOut.AppendError("Send()->out==null");
            }

            return 1;
        } catch (Exception e) {
            ClrOut.AppendInfo("Send()->" + e.toString());
            ClrOut.AppendError("Send()->" + e.toString());
            IsSocketError = true;
            return -1;
        }
    }

    public int SendUframe(byte FrameStyle, String hint) {
        /*	memset(SendBuf, 0, 256);
        APCI* ptr = (APCI*)SendBuf;
        ptr->Head = 0x68;
        ptr->ApduLen = 4;
        SendLength = 6;
        ptr->Frame.SUframe.RcvNum = 0;
        ptr->Frame.SUframe.SendNum = 0;
        SendBuf[2] = FrameStyle;
        Send();*/
        SendBuf[0] = 0x68;
        SendBuf[1] = 4;
        SendLength = 6;
        SendBuf[2] = FrameStyle;
        SendBuf[3] = 0;
        SendBuf[4] = 0;
        SendBuf[5] = 0;
        SendLength = 6;
        return Send(hint);
    }

    int SendSFrame(String hint) {
        /*	//memset(SendBuf, 0, SendBufLen);
        ZeroBuf(SendBuf, SendLength);
        APCI* ptr = (APCI*)SendBuf;
        ptr->Head = 0x68;
        ptr->ApduLen = 4;
        SendLength = 6;
        ptr->Frame.SUframe.RcvNum = RcvNo << 1;
        ptr->Frame.SUframe.SendNum = 1;
        Send();*/
        SendLength = 6;
        SendBuf[0] = 0x68;
        SendBuf[1] = 4;
        SendBuf[2] = 1;
        SendBuf[3] = 0;
        int tmp = 0;
        tmp = RcvNo << 1;
        SendBuf[4] = (byte) ((tmp & 0xFF));
        SendBuf[5] = (byte) ((tmp & 0xFF00) >> 8);
        return Send(hint);
    }

    public int SendGeneralCall() {

        if (OnGeneralCall) {
            ClrOut.AppendInfo("STOP GC ->OnGeneralCall:" + OnGeneralCall);
            return -1;
        }
        SendSyncTime();  
        WriteAsduHead(100, 1, 6, 1);
        SendBuf[12] = 0;
        SendBuf[13] = 0;
        SendBuf[14] = 0;
        SendBuf[15] = 0x14;

        WriteApci(16);
        int r = Send("总召");   //调试用
        if (r != -1) {
            OnGeneralCall = true;
        }
        return r;
    }
    
    public int SendSyncTime()
    {
        if (OnGeneralCall) {
            return -1;
        }
        WriteApci(0x16);
        WriteAsduHead(0x67, 1, 6, 1);
        SendBuf[12] = 0;
        SendBuf[13] = 0;
        SendBuf[14] = 0;
        Date d = Set7ByteTime(15);     
        int r = Send("时间同步 "+df.format(d));   //调试用
        return r;
    }
    
//    public void SendTestAPDU()
//    {
//        if (OnGeneralCall) {
//            return;
//        }
//        WriteAsduHead(107, 1, 6, 0);
//        SendBuf[12] = 0;
//        SendBuf[13] = 0;
//        SendBuf[14] = 0;
//        SendBuf[15] = 0;
//        SendBuf[16] = 0;
//        SendBuf[17] = 0;
//        SendBuf[18] = 0;
//        SendBuf[19] = 0;
//        SendBuf[20] = 0;
//        SendBuf[21] = 0;
//        WriteApci(22);
//        Send("<107>APDU测试");   //调试用
//        OnGeneralCall = true;
//    }
    public boolean IsSaveSection = true;
    public boolean IsSaveToRtdb = false;

    void SaveData(boolean Append) {
        if (IsSaveSection) {
            SaveSectionFile(Append);
        }
       SaveFunc.SaveTodb(SwapList_YC, SwapList_YC);
//        if (IsSaveToRtdb) {
//            SaveFunc.SaveToRtdb(SwapList_YC, SwapList_YX);
//        }
    }
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    String GetCurTimeStr() {
        Date now = new Date();
        return df.format(now);
    }

    void AsduDeal(int Start) {
//        ASDU* ptr = (ASDU*)(&RcvBuf[Start + 6]);    //从APCI头偏移6字节到ASDU头部分
//	QString str;  // = tr("ASDU 数据类型:%1").arg(ptr->TypeID);
//	BYTE tid = ptr->TypeID;       
//	BYTE vsq = ptr->VSQ_SQ;
//	BYTE ct = 0;
//	WORD czh = ptr->CommonAddr;
//	SwapData* sd = NULL;
        try {
            int tid = GetUnsignedValue(RcvBuf[Start + 6]);  //ASDU 类型标识
            int vsq = GetUnsignedValue(RcvBuf[Start + 7]);  //ASDU 可变结构限定词
//        int Cause = GetUnsignedValue(RcvBuf[Start + 9]) << 8;
//        Cause += GetUnsignedValue(RcvBuf[Start + 8]);
            int Cause = GetUnsignedValue(RcvBuf[Start + 8]);

            int czh = GetUnsignedValue(RcvBuf[Start + 11]) << 8;
            czh += GetUnsignedValue(RcvBuf[Start + 10]);
            int ct = 0;
            if (vsq > 0x80) {
                ct = vsq - 0x80;
            } else {
                ct = vsq;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Type=").append(tid).append("  Cause=").append(Cause).append("  Total=").append(ct);
            //"TYPE=" + tid + "  VSQ=" + vsq + "  CAUSE=" + Cause + "  COUNT=" + ct + " APDUS=" + RcvApdus
            ClrOut.AppendInfo(sb.toString());
            String TimeStr = GetCurTimeStr();
            sb.setLength(0);
            if (tid == 0x2E || tid == 0x2d || tid == 0x30 || tid == 0x31) {//tid == 46 || tid == 45 || tid == 48 || tid == 49
                int ykh = Get3ByteValue(Start + 12);
                int ykvalue = -1;
                float gyz = 0.0000f;
                if (tid == 0x2e || tid == 0x2d) {
                    ykvalue = GetUnsignedValue(RcvBuf[Start + 15]);
                } else if (tid == 0x30) {
                    gyz = GetFloatFromGYZ(RcvBuf[Start + 15], RcvBuf[Start + 16]);
                } else if (tid == 0x31) {
                    ykvalue = (GetUnsignedValue(RcvBuf[Start + 16]) << 8) + GetUnsignedValue(RcvBuf[Start + 15]);
                }

                boolean Success = Cause == 0x7 ? true : false;
                PrintRcvFrame(RcvTotalLen);
                sb.setLength(0);
                if (tid == 0x2e || tid == 0x2d) {
                    sb.append("遥控遥调返校 ").append("ykh=").append(ykh - ykh_offset).append(" value=").append(Integer.toHexString(ykvalue)).append(" 结果=").append(Success);  //调试用
                    OnWaitingForReturnOfYKYT = false; //返校等待取消
                } else if (tid == 0x30){   //48
                    sb.append("遥调返校 ").append("ykyth=").append(ykh - ykh_offset).append(" (归一化值)value=").append(gyz).append(" 结果=").append(Success);
                    OnWaitingForReturnOfYKYT = false;
                }else if (tid == 0x31){    //49
                    sb.append("遥调返校 ").append("ykyth=").append(ykh - ykh_offset).append(" (标度值)value=").append(ykvalue).append(" 结果=").append(Success);
                    OnWaitingForReturnOfYKYT = false;
                }

                ClrOut.AppendInfo(sb.toString());
                if (YkFunc != null) {
                    if (CurYkExeObj != null && CurYkExeObj.ykh == ykh) // && CurYkExeObj.ykvalue == ykvalue
                    {
                        CurYkExeObj.IsSuccess = Success;
                        CurYkExeObj.IsReturn = true;
                        ykExeObj obj = CurYkExeObj.Clone();
                        YkFunc.YkExeResult(obj);
                    } else {
                        YkFunc.ClearYK();
                        ClrOut.AppendError("遥控返校异常 cur_ykh=" + (CurYkExeObj.ykh - ykh_offset) + " return_ykh=" + (ykh - ykh_offset)
                                + " cur_YkValue=" + CurYkExeObj.ykvalue + " return_YkValue=" + ykvalue);
                        IsSocketError = true;
                        //IsFatalError = true;
                        return;
                    }
                    SendYkCommand();
                } else {
                    ClrOut.AppendInfo("没有设置遥控过程");
                }

            } else if (tid == 0x64 && Cause == 0xa) {//tid==100 && Cause == 10
                //SendSFrame("总召结束");
                SendSFrame("总召结束 S帧确认");
                SaveData(false);

            } //        else if(tid == 0x46){
            //            WriteAsduHead(0x46, 1, 0x7, 1);
            //            SendBuf[12] = 0;
            //            SendBuf[13] = 0;
            //            SendBuf[14] = 0;
            //            SendBuf[15] = 2;
            //            WriteApci(16);
            //            Send();
            //        }
            else if (tid == 1) {
                int off = 0, dh = -1, value = -1;
                for (int i = 0; i < ct; i++) {
                    if (vsq < 80) {
                        off = Start + 12 + i * 4;
                        dh = Get3ByteValue(off);
                        dh -= yxh_offset;//1;   //YX起始地址
                        value = GetUnsignedValue(RcvBuf[off + 3]);
                    } else {
                        if (i == 0) {
                            off = Start + 12;
                            dh = Get3ByteValue(off);
                            dh -= yxh_offset;//1;
                            off += 3;
                            value = GetUnsignedValue(RcvBuf[off]);
                        } else {
                            off++;
                            dh++;
                            value = GetUnsignedValue(RcvBuf[off]);
                        }
                    }
                    try {
                        ycyxinfo yxinfo = new ycyxinfo();
                        //yxinfo.dbID = ClrOut.Map_yxdh.get(dh);
                        yxinfo.iDH = dh;
                        yxinfo.iCZH = czh;
                        yxinfo.strDH = String.valueOf(dh);
                        yxinfo.strCZH = String.valueOf(czh);
                        yxinfo.iValue = value;
                        yxinfo.strValue = String.valueOf(value);
                        yxinfo.IsSaveFile = false;
                        yxinfo.IsSaveRtdb = false;
                        yxinfo.IsSavedDb = false;
                        SwapList_YX.add(yxinfo);
                    } catch (Exception e) {
                        ClrOut.AppendError("AsduDeal()Map_yxdh->" + e.toString());
                    }
                }
                SaveData(false);
                if (Cause == 0xa) {
                    SendSFrame("总召结束 S帧确认");
                } 
            } else if (tid == 0x1e) //tid==30  SOE 带7字节时标  SOE和YX共享一张表，为避免YXH重复，定义SOE的点号偏移100000。
            {
                int off = 0, dh = -1, value = -1;
                String timestr;
                for (int i = 0; i < ct; i++) {
                    if (vsq < 80) {
                        off = Start + 12 + i * 11;
                        dh = Get3ByteValue(off);
                        dh -= yxh_offset;//1;   //YX起始地址
                        value = GetUnsignedValue(RcvBuf[off + 3]);
                        timestr = Get7ByteTime(off + 4);
                    } else {
                        if (i == 0) {
                            off = Start + 12;
                            dh = Get3ByteValue(off);
                            dh -= yxh_offset;//1;
                            off += 3;
                            value = GetUnsignedValue(RcvBuf[off]);
                            off += 1;
                            timestr = Get7ByteTime(RcvBuf[off]);

                        } else {
                            off += 7;
                            dh++;
                            value = GetUnsignedValue(RcvBuf[off]);
                            off += 1;
                            timestr = Get7ByteTime(RcvBuf[off + 1]);
                        }
                    }
                    try {
                        ycyxinfo yxinfo = new ycyxinfo();
                        //yxinfo.dbID = ClrOut.Map_yxdh.get(dh);
                        yxinfo.iDH = 100000+dh; //SOE点号偏移100000；
                        yxinfo.iCZH = czh;
                        yxinfo.strDH = String.valueOf(yxinfo.iDH);
                        yxinfo.strCZH = String.valueOf(czh);
                        yxinfo.iValue = value;
                        yxinfo.strValue = String.valueOf(value);
                        yxinfo.TimeStr = timestr;
                        yxinfo.IsSaveFile = false;
                        yxinfo.IsSaveRtdb = false;
                        yxinfo.IsSavedDb = false;
                        SwapList_YX.add(yxinfo);
                    } catch (Exception e) {
                        ClrOut.AppendError("AsduDeal()Map_yxdh->" + e.toString());
                    }
                }
                SaveData(false);
                if (Cause == 0xa) {
                    SendSFrame("总召结束 S帧确认");
                } 

            } else if (tid == 0xd) {  //短浮点数YC值
                int off = 0, dh = -1;
                float value = -1;
                for (int i = 0; i < ct; i++) {
                    if (vsq < 80) {
                        off = Start + 12 + i * 8;
                        dh = Get3ByteValue(off);
                        dh -= ych_offset;//0x4001;
                        value = GetFloatFromBytes(off + 3);
                    } else {
                        if (i == 0) {
                            off = Start + 12;
                            dh = Get3ByteValue(off);
                            dh -= ych_offset;//0x4001;
                            off += 3;
                            value = GetFloatFromBytes(off);
                        } else {
                            off += 5;
                            dh++;
                            value = GetFloatFromBytes(off);
                        }
                    }

                    try {
                        ycyxinfo ycinfo = new ycyxinfo();
                        //ycinfo.dbID = ClrOut.Map_ycdh.get(dh);
                        ycinfo.iDH = dh;
                        ycinfo.iCZH = czh;
                        ycinfo.strDH = String.valueOf(dh);
                        ycinfo.strCZH = String.valueOf(czh);
                        ycinfo.fValue = value;
                        ycinfo.strValue = String.valueOf(value);
                        ycinfo.IsSaveFile = false;
                        ycinfo.IsSaveRtdb = false;
                        ycinfo.IsSavedDb = false;
                        SwapList_YC.add(ycinfo);
                    } catch (Exception e) {
                        ClrOut.AppendError("AsduDeal()Map_ycdh->" + e.toString());
                    }
                }
                SaveData(false);
                if (Cause == 0xa) {
                    OnGeneralCall = false;
                    SendSFrame("总召结束 S帧确认");
                } 
            } else if (tid == 0x9) {  //归一化YC值
                int off = 0, dh = -1;
                float value = -1;
                for (int i = 0; i < ct; i++) {
                    if (vsq < 80) {
                        off = Start + 12 + i * 6;
                        dh = Get3ByteValue(off);
                        dh -= ych_offset;//0x4001;  
                        int b1 = GetUnsignedValue(RcvBuf[off + 3]);
                        int b2 = GetUnsignedValue(RcvBuf[off + 4]);
                        if(CopyrightTag == 0)
                            value = GetFloatFromGYZ(b1,b2);//GetFloatFromBytes(off + 3);
                        else if(CopyrightTag== 1)
                            value = GetFloatFromNrGYZ(b1,b2);
                            
                    } else {
                        if (i == 0) {
                            off = Start + 12;
                            dh = Get3ByteValue(off);
                            dh -= ych_offset;//0x4001;
                            off += 3;
                            int b1 = GetUnsignedValue(RcvBuf[off]);
                            int b2 = GetUnsignedValue(RcvBuf[off + 1]);
                            if(CopyrightTag == 0)
                                value = GetFloatFromGYZ(b1, b2);
                            else if(CopyrightTag == 1)
                                value = GetFloatFromNrGYZ(b1, b2);
                        } else {
                            off += 3;
                            dh++;
                            //value = GetFloatFromGYZ(RcvBuf[off], RcvBuf[off + 1]);
                            int b1 = GetUnsignedValue(RcvBuf[off]);
                            int b2 = GetUnsignedValue(RcvBuf[off + 1]);
                            if(CopyrightTag == 0)
                                value = GetFloatFromGYZ(b1, b2);
                            else if(CopyrightTag == 1)
                                value = GetFloatFromNrGYZ(b1, b2);
                        }
                    }

                    try {
                        ycyxinfo ycinfo = new ycyxinfo();
                        //ycinfo.dbID = ClrOut.Map_ycdh.get(dh);
                        ycinfo.iDH = dh;
                        ycinfo.iCZH = czh;
                        ycinfo.strDH = String.valueOf(dh);
                        ycinfo.strCZH = String.valueOf(czh);
                        ycinfo.fValue = value;
                        ycinfo.strValue = String.valueOf(value);
                        ycinfo.IsSaveFile = false;
                        ycinfo.IsSaveRtdb = false;
                        ycinfo.IsSavedDb = false;
                        SwapList_YC.add(ycinfo);
                    } catch (Exception e) {
                        ClrOut.AppendError("AsduDeal()Map_ycdh->" + e.toString());
                    }
                }
                SaveData(false);
                if (Cause == 0xa) {
                    SendSFrame("总召结束 S帧确认");
                    
                }
            } else if (tid == 0xb) {  //标度化YC值
                int off = 0, dh = -1;
                int value = -1;
                for (int i = 0; i < ct; i++) {
                    if (vsq < 80) {
                        off = Start + 12 + i * 6;
                        dh = Get3ByteValue(off);
                        dh -= ych_offset;//0x4001;                    
                        value = (GetUnsignedValue(RcvBuf[off + 3]) << 8) + GetUnsignedValue(RcvBuf[off + 4]);
                    } else {
                        if (i == 0) {
                            off = Start + 12;
                            dh = Get3ByteValue(off);
                            dh -= ych_offset;//0x4001;
                            off += 3;
                            value = (GetUnsignedValue(RcvBuf[off]) << 8) + GetUnsignedValue(RcvBuf[off + 1]);
                        } else {
                            off += 3;
                            dh++;
                            value = (GetUnsignedValue(RcvBuf[off]) << 8) + GetUnsignedValue(RcvBuf[off + 1]);
                        }
                    }
                    try {
                        ycyxinfo ycinfo = new ycyxinfo();
                        //ycinfo.dbID = ClrOut.Map_ycdh.get(dh);
                        ycinfo.iDH = dh;
                        ycinfo.iCZH = czh;
                        ycinfo.strDH = String.valueOf(dh);
                        ycinfo.strCZH = String.valueOf(czh);
                        ycinfo.fValue = value;
                        ycinfo.strValue = String.valueOf(value);
                        ycinfo.IsSaveFile = false;
                        ycinfo.IsSaveRtdb = false;
                        ycinfo.IsSavedDb = false;
                        SwapList_YC.add(ycinfo);
                    } catch (Exception e) {
                        ClrOut.AppendError("AsduDeal()Map_ycdh->" + e.toString());
                    }
                }
                SaveData(false);
                if (Cause == 0xa) {
                    SendSFrame("总召结束 S帧确认");
                }
            }else if(tid == 0x67){
                if(Cause == 6){
                    ClrOut.AppendInfo("收到时间同步报文");
                    
                }else if(Cause == 7){
                    ClrOut.AppendInfo("收到时间同步确认报文");
                    String ts = Get7ByteTime(Start + 15);
                    ClrOut.AppendInfo("对方时间: "+ts);
                }
            }
        } catch (Exception e) {
            ClrOut.AppendError("AsduDeal()->" + e.toString());
        }
    }

    public void SetYkTimeOut(int Seconds) {
        YkTimeOut = Seconds;
    }
    public Date LastTimeSendYk = null;
    private int YkTimeOut = 30;         //遥控超时时间 秒

    private long GetBetweenToNow(Date begin) {
//        Date now = new Date();
//        long between = (now.getTime() - begin.getTime()) / 1000;//除以1000是为了转换成秒
//        return between;

        long between = -1;
        try {
            //Date begin = t;    //"2004-01-02 11:30:24"
            Date end = new Date();
            between = (end.getTime() - begin.getTime()) / 1000;//除以1000是为了转换成秒
        } catch (Exception e) {
            ClrOut.AppendError("GetBetween2Cur()->" + e.toString());
        }
        return between;

    }

    public int SendYkCommand() {
        if (OnGeneralCall) {
            return -1;
        }
        if ((LastTimeSendYk != null) && GetBetweenToNow(LastTimeSendYk) < 30) {
            if (CurYkExeObj != null && !CurYkExeObj.IsReturn) {
                return -1;
            }
        }
        if(OnWaitingForReturnOfYKYT){ //如果在等待遥控返校期间，不轮不发命令 
             ClrOut.AppendInfo("等待返校");
             return -1;
        }
     
        //ClrOut.AppendInfo("控制时间判断");
        if (LastTimeSendYk != null) //距离上次发送遥控指令要间隔20s
        {
            if (CurYkExeObj.ykType == ykExeObj.ExeType.Prepare && CurYkExeObj.ykyttype == 0) {
                if (GetBetweenToNow(LastTimeSendYk) < 5) {
                    ClrOut.AppendInfo("距离上次控制时间" + LastTimeSendYk + "还不足5s");
                    return -1;
                }
            } else {
                //ClrOut.AppendInfo("CurYkExeObj.ykType != ykExeObj.ExeType.Prepare"); 调试信息
            }
        } else {
            //ClrOut.AppendInfo("上次控制时间:NULL");  调试信息
        }
        //ClrOut.AppendInfo("控制时间判断结束");
        //ykvalue 1降2升  1开2合
        ykExeObj exe = YkFunc.GetYkExeObj();
        if (exe == null) {
            return -1;
        }
        exe.IsReturn = false;
        exe.IsSuccess = false;

        String ykstr = "";
        int cause = -1;
        if (exe.ykType == ykExeObj.ExeType.Prepare) {
            cause = 6;
            ykstr = "预置";
            OnWaitingForReturnOfYKYT = true;  //准备开始等待返校
        } else if (exe.ykType == ykExeObj.ExeType.Contrl) {
            cause = 6;
            if (exe.ykyttype == 0) {
                ykstr = "控制";
            } else {
                ykstr = "设点";
            }

        } else {
            cause = 0xa;
            exe.IsReturn = true;  //撤销命令无需反馈
            ykstr = "撤销";
            
        }
        StringBuilder sb = new StringBuilder();
        if (exe.ykyttype == 0) {
            sb.append("遥控信息 ").append("ykh=").append(exe.ykh - ykh_offset).append(" value=").
                    append(Integer.toHexString(exe.ykvalue)).append(" 目的=").append(ykstr);
        } else {
            sb.append("遥调信息 ").append("ykyth=").append(exe.ykh - ykh_offset).append(" value=").
                    append((int) exe.ytvalue).append(" 目的=").append(ykstr);
        }

        ClrOut.AppendInfo(sb.toString());
        if (exe.ykyttype == 0) {
            WriteAsdu_YK(0x2d, 1, cause, 1, exe.ykh, exe.ykvalue);  //单点命令
            WriteApci(16);
        } else if (exe.ykyttype == 1) {
            //WriteAsdu_YT_BD(0x31, 1, cause, 1, exe.ykh, (int) exe.ytvalue);  //设点命令 标度化值。
            //WriteApci(18);
            //WriteAsdu_YT_BD(0x30, 1, cause, 1, exe.ykh, (int) exe.ytvalue);  //归一化值  临时 2015-9-11
            //WriteApci(18);
            
            WriteAsdu_YT_FLOAT(0x32, 1, cause, 1, exe.ykh, exe.ytvalue);  //浮点数
            WriteApci(20);
        }

        int i = Send(ykstr);
        //ClrOut.AppendInfo("发令结束");
        if (i > 0) {
            exe.CloneTo(CurYkExeObj);
            if (CurYkExeObj.ykType == ykExeObj.ExeType.Contrl) {
                LastTimeSendYk = new Date();
            }
        }
        //ClrOut.AppendInfo("发令函数结束");
        return i;
    }

    void PrintRcvFrame(int len) {
        StringBuilder sb = new StringBuilder();
        sb.append("Receive[").append(RcvTotalLen).append("]:");
        if (PrintRcvFrame) {
            int ct = RcvTotalLen > PrintByteCount ? PrintByteCount : RcvTotalLen;
            for (int i = 0; i < ct; i++) {
                byte v = tmpRcvBuf[i];
                int value = GetUnsignedValue(v);
                sb.append(Integer.toHexString(value + 0x100).substring(1).toUpperCase());

                sb.append(' ');
            }
            if (RcvTotalLen > PrintByteCount) {
                sb.append("...");
            }
        }
        String str = sb.toString();
        sb = null;
        ClrOut.AppendInfo(str);
    }

     void PrintSendFrame(int len, String hint) {
        try {
            StringBuilder sb = new StringBuilder();
            if (hint == null) {
                hint = "*";
            }
            sb.append("Send[").append(len).append("][").append(hint).append("]:");
            if (PrintSendFrame) {
                int ct = len > PrintByteCount ? PrintByteCount : len;
                for (int i = 0; i < ct; i++) {
                    byte v = SendBuf[i];
                    int value = GetUnsignedValue(v);
                    sb.append(Integer.toHexString(value + 0x100).substring(1).toUpperCase());
                    sb.append(' ');
                }
                if (len > PrintByteCount) {
                    sb.append("...");
                }
            }
            String str = sb.toString();
            ClrOut.AppendInfo(str);
            sb = null;
        } catch (Exception e) {
            ClrOut.AppendError("PrintSendFrame()->" + e.toString());
        }

    }

    void SaveSectionFile(boolean Append) {
        SaveFunc.SaveYcFile(SwapList_YC, Append);
        SaveFunc.SaveYxFile(SwapList_YX, Append);
    }

    void SaveToDb() {
        String TimeStr = GetCurTimeStr();
        SaveFunc.SaveYcToDb(SwapList_YC, TimeStr);
        SaveFunc.SaveYxToDb(SwapList_YX, TimeStr);
    }

    private void ClearSwap() {
        SwapList_YC.clear();
        SwapList_YX.clear();
    }

    private boolean IntegralityCheck() {
        //APCIhead* ptr = (APCIhead*)RcvBuf;
        int index = 0;
        int offset = 0;
        while (true) {
            if (index > RcvRufLen - 1) {
                return false;
            }

            if (RcvBuf[index] == 0x68) {
                offset = GetUnsignedValue(RcvBuf[index + 1]);
                index += offset + 2;
            } else {
                return false;
            }

            if (index == RcvTotalLen) {
                return true;
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    public static int GetUnsignedValue(byte v) {
        int r = v;
        if (r < 0) {
            r += 0xFF + 1;
        }
        return r;
    }

    public static int GetUnsignedValue(short v) {
        int r = v;
        if (r < 0) {
            r += 0xFFFF + 1;
        }
        return r;
    }

    public static long GetUnsignedValue(int v) {
        long r = v;
        if (r < 0) {
            r += 0xFFFFFFFF + 1;
        }
        return r;
    }
}
