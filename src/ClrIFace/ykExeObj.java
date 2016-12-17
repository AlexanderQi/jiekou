/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ClrIFace;

/**
 *
 * @author ibm
 */
public class ykExeObj {
    /*对应于一个抽象的命令意图，为预置，遥控，撤销3种具体的遥控指令 是104协议层面上的遥控指令*/
    public int id;
    public int czh;
    public int ykh;
    public int ykvalue;
    public float ytvalue;
    public int ykyttype;
    public static enum ExeType{Prepare, Contrl, Cancel};
    public ExeType ykType = ExeType.Prepare;
    public boolean IsSuccess = false;
    public boolean IsReturn = false;

    public ykExeObj Clone()
    {
        ykExeObj obj = new ykExeObj();
        obj.id = this.id;
        obj.czh = this.czh;
        obj.ykh = this.ykh;
        obj.ykvalue = this.ykvalue;
        obj.ykType = this.ykType;
        obj.IsReturn = this.IsReturn;
        obj.IsSuccess = this.IsSuccess;
        obj.ykyttype = this.ykyttype;
        obj.ytvalue = this.ytvalue;
       
        return obj;
    }
    public void CloneTo(ykExeObj obj)
    {
        if(obj == null) return;
        obj.id = this.id;
        obj.czh = this.czh;
        obj.ykh = this.ykh;
        obj.ykvalue = this.ykvalue;
        obj.ykType = this.ykType;
        obj.IsReturn = this.IsReturn;
        obj.IsSuccess = this.IsSuccess;
        
        obj.ykyttype = this.ykyttype;
        obj.ytvalue = this.ytvalue;
    }
}
