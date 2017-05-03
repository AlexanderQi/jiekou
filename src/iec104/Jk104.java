package iec104;

import Clr68Hv2.ClrSocket;
import ClrIFace.ClrIFace_Out;
import ClrIFace.ClrIFace_YK;
import java.io.*;
//import java.util.*;
import java.net.*;
import JkSave.jksave;
import cfgParam.JkParam;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author liqi
 */
public class Jk104 {
    public ClrIFace_Out out_ = null;
    public JkParam jkp =null;
    public ClrSocket clr_instance = null;
    public Thread clrThread = null;
    public jksave SaveFunc = null;
    public ClrIFace_YK ykcmd; //new ykcommand();
    public static boolean IsDebugMode = false;
    public String CurrPath = null;
    public Jk104() {
        CurrPath = System.getProperty("user.dir");
        out_ = JkOut.Instance();
        JkOut myout = (JkOut)out_;
        myout.SetLogDir(CurrPath + "/log4j.properties");
        
        jkp = new JkParam(out_);    
        SaveFunc = jksave.Instance();
        clr_instance = null;
        clrThread = null;
    }

    /**
     * @param args the command line arguments
     */
    private static void checkSingleInstance() {
        try {
            ServerSocket srvSocket = new ServerSocket(52019); //启动一个ServerSocket，用以控制只启动一个实例
        } catch (Exception ex) {
            System.exit(0);
        }
    }

    public static Jk104 Instance = null;

    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-s")) {
                System.out.print("程序以单例模型运行.");
                checkSingleInstance();
            } else if (args[i].equals("-d")) {
                System.out.print("程序以调试模式运行.");
                IsDebugMode = true;
            }
        }

        Instance = new Jk104();
//        if(!Instance.jkp.CheckLicense()){
//           System.out.print("证书错误，请确认程序已被授权.\r\n授权方法，将程序目录下license.txt发给提供者.\r\n");  
//           BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//           String str = br.readLine();
//           return;
//        }

        
        Instance.SaveFunc.cfg_host = Instance.jkp.cfg_host;
        Instance.SaveFunc.cfg_insidePort = Instance.jkp.cfg_insidePort;
        Instance.SaveFunc.CHANNEL = Instance.jkp.cfg_ca;
        Instance.SaveFunc.IsCreateRtdb = Instance.jkp.IsCreateRtdb;
        //Instance.SaveFunc.IniRtdb();  //初始化实时库
        Instance.SaveFunc.iniDb();
        Instance.ykcmd = new ykcommand();
        Instance.ConnectServer();

//        for(int i=0;i<args.length;i++)
//        {
//            
//        }
        System.out.print("104接口启动\r\n");
        //Timer t = new Timer();
        //t.schedule(new clrTimer(), 1000, 1000);  

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new clrTimer(), 1, 1, TimeUnit.SECONDS);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        byte[] buf = new byte[256];
        while (true) {
            System.out.print("\r\n_");
            String str = br.readLine();
            //str = str.trim();
            //System.out.flush();
            //System.out.print("输入字节[" + str.length() + "]: " + str);
//            if (str != null && str.equals("exit")) {
//                //t.cancel();
//                service.shutdown();
//                break;
//            }
        }
    }

    public void ConnectServer() {
        try {
            if (clr_instance == null) {
                clr_instance = new ClrSocket(jkp.cfg_server1, jkp.cfg_server2, jkp.cfg_104Port);
                clr_instance.SetSaveFunc(SaveFunc);
                clr_instance.SetInfoOut(out_);
                clr_instance.SetYkFunc(ykcmd);
                clr_instance.IsSaveToRtdb = true;
                clr_instance.IsSaveSection = (jkp.cfg_file == 1);
                clr_instance.ych_offset = jkp.ych_offset;
                clr_instance.yxh_offset = jkp.yxh_offset;
                clr_instance.ykh_offset = jkp.ykh_offset;
                JkOut myout = (JkOut) Instance.out_;
                clr_instance.PrintRcvFrame = true; //IsDebugMode;
                clr_instance.PrintSendFrame = true; //IsDebugMode;
                myout.log4debug = IsDebugMode;
                //myout.log4debug = IsDebugMode;
                clr_instance.CopyrightTag = jkp.cfg_copyright;
                out_.AppendInfo("RuleTag:" + clr_instance.CopyrightTag);
                SaveFunc.out = Instance.out_;
            }
            if (clr_instance.Connect()) {
                clrThread = new Thread(clr_instance);
                clr_instance.CurThreadName = clrThread.getName();
                out_.AppendInfo("创建线程:" + clr_instance.CurThreadName);
                clrThread.start();
            }
        } catch (Exception e) {
            out_.AppendError("ConnectServer()->" + e.toString());
        }
    }
}
