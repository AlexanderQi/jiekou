/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Clr68Hv2;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Qi
 */
public class ClrSocketTest {
    
    public ClrSocketTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }



    

    /**
     * Test of WriteAsdu_YT_FLOAT method, of class ClrSocket.
     */
    @Test
    public void testWriteAsdu_YT_FLOAT() {
        System.out.println("WriteAsdu_YT_FLOAT");
        int TypeId = 0x32;
        int VSQ = 1;
        int Cause = 3;
        int CommonAddr = 1;
        int ykh = 9;
        float Value = 580F;
        String ip = "127.0.0.1";
        
        System.out.println("ini");
        ClrSocket instance = new ClrSocket(ip, ip, 2404);
        
        instance.WriteAsdu_YT_FLOAT(TypeId, VSQ, Cause, CommonAddr, ykh, Value);
        instance.WriteApci(20);
        StringBuilder sb = new StringBuilder();
        // TODO review the generated test code and remove the default call to fail.
        for (int i = 0; i < 20; i++) {
                    byte v = instance.SendBuf[i];
                    int value = instance.GetUnsignedValue(v);
                    sb.append(Integer.toHexString(value + 0x100).substring(1).toUpperCase());
                    sb.append(' ');
                }
        System.out.print(sb.toString());
       // fail("The test case is a prototype."+sb.toString());
        
    }

    
}
