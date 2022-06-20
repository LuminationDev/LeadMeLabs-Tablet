package com.lumination.leadmelabs.services;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

import io.sentry.Sentry;

/**
 * A service responsible for the receiving and sending of messages.
 */
public class EncryptionHelper {
    private static final int keysize = 128;
    private static final int derivationIterations = 1000;

    public static String encrypt(String plainText, String passPhrase) {
        String encrypted = "";
        if (plainText.length() % 32 != 0) {
            int requiredPadding = 32 - (plainText.length() % 32);
            for (int i = 0; i < requiredPadding; i++) {
                plainText += "_";
            }
        }
        for (int i = 0; i < plainText.length(); i += 32)
        {
            int substringLength = 32;
            if (plainText.length() < i + 32)
            {
                substringLength = plainText.length() - i;
            }
            try {
                encrypted += encrypt32(plainText.substring(i, i + substringLength), passPhrase);
            } catch (NoSuchAlgorithmException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            } catch (BadPaddingException e) {
                Sentry.captureException(e);
                e.printStackTrace();
            }
        }

        return encrypted;
    }

    private static String encrypt32(String plainText, String passPhrase) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] saltStringBytes = generate128BitsOfRandomEntropy();
        byte[] ivStringBytes = generate128BitsOfRandomEntropy();
        byte[] plainTextBytes = plainText.getBytes(StandardCharsets.UTF_8);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        PBEKeySpec password = new PBEKeySpec(passPhrase.toCharArray(), saltStringBytes, derivationIterations, keysize);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        SecretKey secretKey = factory.generateSecret(password);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(ivStringBytes));
        byte[] ciphertext = cipher.doFinal(plainTextBytes);
        byte[] message = new byte[ciphertext.length + saltStringBytes.length + ivStringBytes.length];
        int j = 0;
        for (int i = 0; i < saltStringBytes.length; i++, j++) {
            message[j] = saltStringBytes[i];
        }
        for (int i = 0; i < ivStringBytes.length; i++, j++) {
            message[j] = ivStringBytes[i];
        }
        for (int i = 0; i < ciphertext.length; i++, j++) {
            message[j] = ciphertext[i];
        }
        return Base64.getEncoder().encodeToString(message);
    }

    public static String decrypt(String cipherText, String passPhrase) {
        String decrypted = "";
        for (int i = 0; i < cipherText.length(); i += 108)
        {
            int substringLength = 108;
            if (cipherText.length() < i + 108)
            {
                substringLength = cipherText.length() - i;
            }

            try {
                decrypted += EncryptionHelper.decrypt108(cipherText.substring(i, i + substringLength), passPhrase);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
                Sentry.captureException(e);
            }
        }

        return trim(decrypted, '_');
    }

    private static String decrypt108(String cipherText, String passPhrase) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] cipherTextBytesWithSaltAndIv = Base64.getDecoder().decode(cipherText);
        byte[] saltStringBytes = Arrays.copyOfRange(cipherTextBytesWithSaltAndIv, 0, keysize / 8);
        byte[] ivStringBytes = Arrays.copyOfRange(cipherTextBytesWithSaltAndIv, keysize / 8, (keysize / 8) * 2);
        byte[] cipherTextBytes = Arrays.copyOfRange(cipherTextBytesWithSaltAndIv, (keysize / 8) * 2, cipherTextBytesWithSaltAndIv.length);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        PBEKeySpec password = new PBEKeySpec(passPhrase.toCharArray(), saltStringBytes, derivationIterations, keysize);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        SecretKey secretKey = factory.generateSecret(password);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivStringBytes));
        byte[] ciphertext = cipher.doFinal(cipherTextBytes);
        return new String(ciphertext);
    }

    private static byte[] generate128BitsOfRandomEntropy()
    {
        byte[] randomBytes = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    private static String trim(String value, char c) {

        if (c <= 32) return value.trim();

        int len = value.length();
        int st = 0;
        char[] val = value.toCharArray();    /* avoid getfield opcode */

        while ((st < len) && (val[st] == c)) {
            st++;
        }
        while ((st < len) && (val[len - 1] == c)) {
            len--;
        }
        return ((st > 0) || (len < value.length())) ? value.substring(st, len) : value;
    }
}
