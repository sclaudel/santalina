package org.santalina.diving.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    public static boolean verify(String password, String hash) {
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
        return result.verified;
    }
}
