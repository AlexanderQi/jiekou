/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package iec104;

/**
 *
 * @author liqi
 */
import ClrIFace.ClrIFace_Out;
//import com.sun.org.apache.bcel.internal.util.Class2HTML;
import org.apache.log4j.*;
public class JkOut implements ClrIFace_Out {
    private static Logger log;
    private static JkOut instance = null;

    public static JkOut Instance() {
        if (instance == null) {
            instance = new JkOut();
        }
        
        
        return instance;
    }
    
    public void SetLogDir(String DirString){
        log = Logger.getLogger("");
        PropertyConfigurator.configure(DirString);
    }

    private JkOut() {
    }
    public boolean log4debug = false;

    @Override
    public void AppendInfo(String str) {  //记入日志
        //System.out.println("Info:" + str);
        log.info("Info:" + str);
    }

    @Override
    public void AppendError(String str) {  //记入日志
        //System.out.println("Err:" + str);
        log.error("Err:" + str);
    }

    @Override
    public void AppendStr(String str) {  //不记入日志
        System.out.println("Msg:" + str);
    }

    @Override
    public void AppendDebug(String str) { //记入日志
        if (log4debug) {
            //System.out.println("Debug:" + str);
            log.debug("Debug:" + str);
        }
    }

    @Override
    public void SetConnectInfo(String str) { //记入日志
        System.out.println("ConnInfo:" + str);
    }

    public void StatusBarText(String str) {  //不记入日志
       // System.out.println("State:" + str);
    }
}
