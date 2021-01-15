package org.babelomics.csvs.lib.token;

import io.jsonwebtoken.Claims;

import java.util.Map;

public interface ICSVSToken {

    String NAME = "name";
    String SUBPOPULATIONS = "subpopulations";
    String createJWT(String id, String issuer, String subject, String audience, Map aditionalClaims, long ttlMillis);

    Claims decodeJWT(String jwt);

    boolean isVerified(String jwt);

}
