/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ClrIFace;
import java.util.List;
/**
 *
 * @author ibm
 */
public interface ClrIFace_YCYX {                                        //104协议回调接口，实现以下方法用来保存104协议解码出来的数据。
    public void SaveToRtdb(List<ycyxinfo>YcList, List<ycyxinfo>YxList); //实时库写入接口，YcList 104接口解析出来的YC数据
    public void SaveTodb(List<ycyxinfo>YcList, List<ycyxinfo>YxList); 
    public int SaveYcFile(List<ycyxinfo>YcList, boolean IsChanged);  //YC断面文件写入接口，IsChanged： true表示变位数据，false表示全部数据
    public int SaveYxFile(List<ycyxinfo>YxList, boolean IsChanged);  //YX断面文件写入

    public int SaveYcToDb(List<ycyxinfo>YcList, String RefreshTime);  //YC数据库写入  RefreshTime 数据刷新时间 
    public int SaveYxToDb(List<ycyxinfo>YxList, String RefreshTime);  //YX数据库写入
}
