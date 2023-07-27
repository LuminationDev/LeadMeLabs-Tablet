package com.lumination.leadmelabs.services;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.sentry.Sentry;

/**
 * A service responsible for the receiving and sending of messages.
 */
public class EncryptionHelper {
    private static final int keySize = 128;
    private static final int derivationIterations = 1000;

    public static String encrypt(String plainText, String passPhrase) {
        try {
            // Generate salt and IV
            byte[] salt = generate128BitsOfRandomEntropy();
            byte[] iv = generate128BitsOfRandomEntropy();

            // Convert the plainText to bytes using Unicode encoding
            byte[] plainTextBytes = plainText.getBytes(StandardCharsets.UTF_16LE); // Unicode (UTF-16LE) encoding in little-endian

            // Derive the key from the passphrase and salt using the same key derivation function and iteration count
            KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, derivationIterations, keySize);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // Encrypt the data using AES in CBC mode with PKCS7 padding
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] encryptedData = cipher.doFinal(plainTextBytes);

            // Concatenate the salt, IV, and encrypted data into a single byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(salt);
            outputStream.write(iv);
            outputStream.write(encryptedData);
            byte[] cipherTextBytes = outputStream.toByteArray();

            return Base64.getEncoder().encodeToString(cipherTextBytes);
        }
        catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
        }

        return "";
    }

    public static String decrypt(String cipherTextBase64, String passPhrase) {
        try {
            byte[] cipherTextBytes = Base64.getDecoder().decode(cipherTextBase64);

            // Extract salt, IV, and encrypted data from the cipherTextBytes
            int saltLength = 16;
            int ivLength = 16;
            byte[] salt = new byte[saltLength];
            byte[] iv = new byte[ivLength];
            byte[] encryptedData = new byte[cipherTextBytes.length - saltLength - ivLength];
            System.arraycopy(cipherTextBytes, 0, salt, 0, saltLength);
            System.arraycopy(cipherTextBytes, saltLength, iv, 0, ivLength);
            System.arraycopy(cipherTextBytes, saltLength + ivLength, encryptedData, 0, encryptedData.length);

            // Derive the key using the same key derivation function and number of iterations as in C# code
            KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, derivationIterations, keySize);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // Decrypt the data using AES in CBC mode with PKCS7 padding
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            byte[] decryptedBytes = cipher.doFinal(encryptedData);

            // Convert the decrypted bytes to a Unicode string
            return new String(decryptedBytes, StandardCharsets.UTF_16LE);
        }
        catch (Exception e) {
            Sentry.captureException(e);
            e.printStackTrace();
        }

        return "";
    }

    private static byte[] generate128BitsOfRandomEntropy()
    {
        byte[] randomBytes = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(randomBytes);
        return randomBytes;
    }
}
