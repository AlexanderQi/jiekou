/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ClrIFace;

/**
 *
 * @author ibm
 */
public interface ClrIFace_Out {
    public void AppendInfo(String str);
    public void AppendError(String str);
    public void AppendStr(String str);
    public void AppendDebug(String str);
    public void SetConnectInfo(String str);
    public void StatusBarText(String str);

}
