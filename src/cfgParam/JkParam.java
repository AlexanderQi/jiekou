/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cfgParam;

import ClrIFace.ClrIFace_Out;
import java.io.*;
import java.net.InetAddress;

/**
 *
 * @author liqi
 */
public class JkParam {

    public String cfg_host;
    public String cfg_server1, cfg_server2;// cur_server;
    public int cfg_104Port;
    public int cfg_insidePort;
    public int cfg_gcall;
    public int cfg_HotBackup = 0;
    public int cfg_IsYK = 0;
    public int yxh_offset = 1;
    public int ych_offset = 0x4001;
    public int ykh_offset = 0x6001;
    public int yth_offset = 0x6201;
    public int cfg_ca = -1;
    public int cfg_rtdb = 0;
    public int cfg_file = 1;
    public int cfg_copyright = 0;  //0:国标104；1：南瑞104
    public boolean IsCreateRtdb = true;
    //public String UserPath;
    public String LocalHostName;
    public String CurrentPath;
    public int cfg_debug = 0;
    public ClrIFace_Out out = null;
    private static JkParam instance = null;

    public static JkParam Instance(ClrIFace_Out out) {
        if (instance == null) {
            instance = new JkParam(out);
        }
        return instance;
    }

    public JkParam(ClrIFace_Out myout) {
        out = myout;
        ReadEnv();
        ReadCfg();
    }
    public String dir;

    public void ReadEnv() {
        //UserPath = System.getenv("SC_JK");
        CurrentPath = System.getProperty("user.dir");
        LocalHostName = GetHostName();
        dir =
                System.getProperty("os.name") + " "
                + System.getProperty("os.version") + "\njava "
                + System.getProperty("java.version") + "\n"
                + System.getProperty("user.name") + "\nUserPath="
                + CurrentPath + "\n";
        dir = LocalHostName + '\n' + dir;
        out.AppendDebug(dir);
    }

    public String GetHostName() {
        InetAddress address = null;
        String host = "";
        try {
            address = InetAddress.getLocalHost();
            host = address.getHostName();
        } catch (Exception e) {
            out.AppendError(e.toString());
        } finally {
            return host;
        }
    }

    public void ReadCfg() {
        File file = new File(CurrentPath + "/cfg.txt");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String txt;
            while ((txt = reader.readLine()) != null) {
                String[] info = txt.split("=");//分隔读入的行
                if (info.length != 2) {
                    continue;
                }
                if (info[0].equals("host")) {
                    cfg_host = info[1];
                } else if (info[0].equals("srv1")) {
                    cfg_server1 = info[1];
                } else if (info[0].equals("srv2")) {
                    cfg_server2 = info[1];
                } else if (info[0].equals("104port")) {
                    cfg_104Port = Integer.parseInt(info[1]);
                } else if (info[0].equals("iport")) {
                    cfg_insidePort = Integer.parseInt(info[1]);
                } else if (info[0].equals("gcall")) {
                    cfg_gcall = Integer.parseInt(info[1]);
                } else if (info[0].equals("hot")) {
                    cfg_HotBackup = Integer.parseInt(info[1]);
                } else if (info[0].equals("yk")) {
                    cfg_IsYK = Integer.parseInt(info[1]);
                } else if (info[0].equals("yxh_offset")) {
                    yxh_offset = Integer.parseInt(info[1]);
                } else if (info[0].equals("ych_offset")) {
                    ych_offset = Integer.parseInt(info[1]);
                } else if (info[0].equals("ykh_offset")) {
                    ykh_offset = Integer.parseInt(info[1]);
                }  else if (info[0].equals("yth_offset")) {
                    yth_offset = Integer.parseInt(info[1]);
                }else if (info[0].equals("ca")) {
                    cfg_ca = Integer.parseInt(info[1]);
                } else if (info[0].equals("save2rtdb")) {
                    cfg_rtdb = Integer.parseInt(info[1]);
                } else if (info[0].equals("save2file")) {
                    cfg_file = Integer.parseInt(info[1]);
                }else if (info[0].equals("printdebug")) {
                    cfg_debug = (Integer.parseInt(info[1]));
                }else if (info[0].equals("CreateRtdb")) {
                    IsCreateRtdb = (Integer.parseInt(info[1])==1);
                }else if (info[0].equals("ctag")) {
                    cfg_copyright = (Integer.parseInt(info[1]));
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("DBIP:").append(cfg_host).append('\n')
                    .append("104srv1:").append(cfg_server1).append('\n')
                    .append("104srv2:").append(cfg_server2).append('\n')
                    .append("general call(seconds):").append(cfg_gcall)
                    .append('\n').append("IsHotBackup=").append(cfg_HotBackup)
                    .append("\nIsYK=").append(cfg_IsYK).append(" ControlArear=")
                    .append(cfg_ca).append("\nYcOffset=0x").append(Integer.toHexString(ych_offset).toUpperCase())
                    .append(" YxOffset=0x").append(Integer.toHexString(yxh_offset).toUpperCase())
                    .append(" YkOffset=0x").append(Integer.toHexString(ykh_offset).toUpperCase());
            out.SetConnectInfo(sb.toString());

            if (cfg_IsYK == 1) {
                // ykcmd = new ykcommand();
            }
        } catch (Exception e) {
            out.AppendError("ReadCfg()->" + e.toString());
        }
    }

    public boolean CheckLicense() {
        String userinfo = "";
        String keyinfo = "";
        File file = new File(CurrentPath + "/license.txt");
        try {
            if (!file.exists()) {
                FileWriter fw = new FileWriter(CurrentPath + "/license.txt");
                userinfo = dir;
                userinfo = AES_String.encrypt("License", userinfo);
                fw.write("user=" + userinfo);
                fw.flush();
                fw.close();
                return false;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String txt;
            while ((txt = reader.readLine()) != null) {
                String[] info = txt.split("=");//分隔读入的行
                if (info.length != 2) {
                    continue;
                }
                if (info[0].equals("user")) {
                    userinfo = info[1];
                } else if (info[0].equals("key")) {
                    keyinfo = info[1];
                }
            }
            if (keyinfo.equals("")) {
                return false;
            }
            String _key = AES_String.decrypt("Softcore", keyinfo);
            if (!_key.equals(dir)) {
                return false;
            }

        } catch (Exception e) {
            out.AppendError("CheckLicense()->" + e.toString());
            return false;
        }

        return true;
    }
}
