package org.thanthoai.securesms.crypto;

import android.util.Base64;

import org.thanthoai.securesms.utils.Global;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESHelper {

    private static final int ITERATION_COUNT = 1000;
    private static final int KEY_LENGTH = 128;
    private static final String SPLITTER = "%";

    /**
     * Encrypt a given string. <br/>
     *
     * @param password the string to generate password-based key
     * @param plainText the string to encrypt
     * @return the encrypted string in Base64
     */
    public static String encryptToBase64(String password, String plainText) {
        try {
            int saltLength = KEY_LENGTH / 8; // same size as key output
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[saltLength];
            random.nextBytes(salt);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            byte[] iv = new byte[cipher.getBlockSize()];
            random.nextBytes(iv);
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            final Key key = getKey(salt, password);

            cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

            return toBase64(salt) + SPLITTER + toBase64(iv) + SPLITTER + toBase64(cipherBytes);
        } catch (NoSuchAlgorithmException
                | NoSuchPaddingException
                | IllegalBlockSizeException
                | BadPaddingException
                | InvalidAlgorithmParameterException
                | InvalidKeyException
                | InvalidKeySpecException
                | UnsupportedEncodingException e)
        {
            Global.error("AES-exception", e);
        }
        return null;
    }

    /**
     * Decrypt a Base64 encrypted string. <br/>
     *
     * @param password the string to generate password-based key
     * @param cipherText the Base64 string to decrypt
     * @return the decrypted string
     */
    public static String decryptFromBase64(String password, String cipherText) {

        try {
            String[] fields = cipherText.split(SPLITTER);
            byte[] salt = fromBase64(fields[0]);
            byte[] iv = fromBase64(fields[1]);
            byte[] cipherBytes = fromBase64(fields[2]);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            final Key key = deriveKeyPbkdf2(salt, password);
            IvParameterSpec ivParams = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
            byte[] plaintext = cipher.doFinal(cipherBytes);

            return new String(plaintext , "UTF-8");
        } catch (NoSuchAlgorithmException
                | NoSuchPaddingException
                | IllegalBlockSizeException
                | BadPaddingException
                | InvalidAlgorithmParameterException
                | InvalidKeyException
                | InvalidKeySpecException
                | IllegalArgumentException
                | UnsupportedEncodingException e)
        {
            Global.error("AES-exception", e);
        }
        return null;
    }

    /**
     *
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private static Key getKey(byte[] salt, String password) throws NoSuchAlgorithmException,
            InvalidKeySpecException {

        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt,
                ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();

        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     *
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    private static Key deriveKeyPbkdf2(byte[] salt, String password) throws InvalidKeySpecException,
            NoSuchAlgorithmException {

        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt,
                ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();

        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] fromBase64(String src) {
        return Base64.decode(src, Base64.NO_WRAP);
    }

    private static String toBase64(byte[] src) {
        return Base64.encodeToString(src, Base64.NO_WRAP);
    }
}
