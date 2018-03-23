package com.clj.blesample.aesUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by dell on 2018-03-23.
 */

public class AESUtil {

    public final static byte[] PRIVATE_AES = new byte[]{};

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
