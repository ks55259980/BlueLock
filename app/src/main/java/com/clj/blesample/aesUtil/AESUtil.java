package com.clj.blesample.aesUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by dell on 2018-03-23.
 */

public class AESUtil {

    public final static byte[] PRIVATE_AES = new byte[]{0x3A,0x60,0x43,0x2A,0x5C,0x01,0x21,0x1F,
                                                        0x29,0x1E,0x0F,0x4E,0x0C,0x13,0x28,0x25};

    public static byte[] Encrypt(byte[] sSrc, byte[] sKey) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(sSrc);
            return encrypted;
        } catch (Exception ex) {
            return null;
        }
    }

    public static byte[] Decrypt(byte[] sSrc, byte[] sKey) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(sKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] dncrypted = cipher.doFinal(sSrc);
            return dncrypted;
        } catch (Exception ex) {
            return null;
        }
    }

}
