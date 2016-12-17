/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ClrIFace;

/**
 *
 * @author ibm
 */
public interface ClrIFace_YK {
    public ykExeObj GetYkExeObj();
    public void YkExeResult(ykExeObj obj);
    public void LoadYK();
    public void ClearYK();
    public void CancelCurrentExeQueue();
}
