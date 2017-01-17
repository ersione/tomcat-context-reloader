package org.ersione.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class OTPUtil {
    private static final String ALGORITHM = "HmacSHA1";
    private static long DISTANCE = 30000;
    private static String SECRET_KEY = "ersione";
    private static OTPUtil otpUtil = new OTPUtil();

    private OTPUtil() {
    }

    public static OTPUtil getInstance(String key) {
        SECRET_KEY = key;
        return otpUtil;
    }

    public static OTPUtil getInstance(String Key, long distance) {
        SECRET_KEY = Key;
        DISTANCE = distance;
        return otpUtil;
    }

    private long create(long time) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] data = new byte[8];

        long value = time;
        for (int i=8; i--> 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM));

        byte[] hash = mac.doFinal(data);
        int offset = hash[20-1] & 0xF;

        long truncateHash = 0;
        for (int i=0; i<4; ++i) {
            truncateHash <<= 8;
            truncateHash |= hash[offset+i] & 0xFF;
        }

        truncateHash &= 0x7FFFFFFF;
        truncateHash %= 1000000;

        return truncateHash;
    }

    public String create() throws InvalidKeyException, NoSuchAlgorithmException {
        return String.format("%06d", create(new Date().getTime() / DISTANCE));
    }

    public boolean verify(String code) {
        try {
            return create().equals(code);
        } catch (Exception e) {
            return false;
        }
    }
}
