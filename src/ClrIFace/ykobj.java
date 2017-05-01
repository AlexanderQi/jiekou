/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ClrIFace;
import java.util.Date;
/**
 *
 * @author ibm
 */
public class ykobj {
    /*对应于遥控表里一条遥控命令记录，为抽象的命令意图*/
    //public String Rowid;
    public int ca_id;  //集控站号
    public int id;
    public int sid;     //方案分组编号，无分组时为0
    public int ssid;    //方案分组内序号
    public int czh;
    public int ykh;
    public int ykvalue;
    public int dealtag;   //处理标记，初值为0
    
    public float ytvalue;
    public int ykyttype;   //遥控遥调类型 0为遥控，1为遥调
    public Date yktime;
    public int Tag;

    public enum ActStat{asNew, asSuccess, asFailed, asTimeout};
    public ActStat FirstPrepare;    //第一次预置结果
    public ActStat SecondPrepare;   //第二次预置结果，组合令需判断。
    public ActStat CtrlResult;      //控制结果
}
