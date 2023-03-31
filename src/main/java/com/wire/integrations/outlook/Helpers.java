package com.wire.integrations.outlook;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Helpers {
    private static final SecureRandom random = new SecureRandom();
    public static final String ZCALENDAR_SID = "zcalendar_SID";

    public static String randomName(int len) {
        final String AB = "0123456789abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(random.nextInt(AB.length())));
        return sb.toString();
    }

    public static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(bytes, 0, bytes.length);
        byte[] hash = md.digest();
        byte[] byteArray = Base64.getEncoder().encode(hash);
        String base64 = new String(byteArray);
        return base64.replace("=", "")
                .replace("/", "_")
                .replace("+", "-");
    }
}
