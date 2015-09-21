package org.thanthoai.securesms.crypto;

import junit.framework.TestCase;

public class AESHelperTest extends TestCase {

    public void setUp() throws Exception {
        super.setUp();

    }

    public void tearDown() throws Exception {

    }

    public void testAes() throws Exception {
        String plaintText = "có tiếng việt này, chết cha mày chưa";
        String password = "pass là tiếng việt này";
        String cipherText = AESHelper.encryptToBase64(password, plaintText);
        System.out.println("Cipher text: " + cipherText);
        assertEquals(plaintText, AESHelper.decryptFromBase64(password, cipherText));
    }
}