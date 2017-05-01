/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package JkSave;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ibm
 */
public class MyUdp implements Runnable {

    DatagramSocket sendSocket = null;
    DatagramSocket rcvSocket = null;
    int sendport = 31020;   //对端进程的监听地址
    int rcvport = 31019;    //本进程的监听地址
    InetAddress ip = null;
    InetAddress rcvip = null;
    public static MyUdp Instance = null;
    public MyUdp() {
        try {
            ip = InetAddress.getLocalHost();
            
            sendSocket = new DatagramSocket();  // 创建发送方的套接字，IP默认为本地，端口号随机  
            rcvSocket = new DatagramSocket(rcvport,InetAddress.getByName("127.0.0.1"));
            rcvSocket.setSoTimeout(30*1000);
            Instance = this;
        } catch (Exception e) {
            System.out.print(e.toString());
        }
    }
    //SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void SendMsg() {
        Date now = new Date();
        String dateStr = "hello";//df.format(now);
        //String mes = ip.toString(); //"hello,This's my Ip";  
        byte[] buf = dateStr.getBytes();
        // 创建发送类型的数据报：   
        DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, ip,
                sendport);
        try {
            // 通过套接字发送数据：  
            sendSocket.send(sendPacket);
        } catch (IOException ex) {
            sendSocket.close();
            System.out.print(ex.toString());
        }
    }

    byte[] getBuf = new byte[1024];
    public void run() {
        try {
            while (true) {
                // 创建接受类型的数据报   
                DatagramPacket getPacket = new DatagramPacket(getBuf, getBuf.length);
//            // 通过套接字接受数据
                try{
                    rcvSocket.receive(getPacket);
                }catch(Exception ex)  //超时
                {
                    System.out.print(ex.toString());
                    Runtime runtime = Runtime.getRuntime();
                    runtime.exec("cqjk.bat");
                    
                }
//            // 解析反馈的消息，并打印   
                String backMes = new String(getBuf, 0, getPacket.getLength());
                System.out.println("返回的消息：" + backMes);
            }
            // 关闭套接字   
            //sendSocket.close();            
        } catch (Exception e) {
            //rcvSocket.close();
            System.out.print(e.toString());
        }

    }
}
