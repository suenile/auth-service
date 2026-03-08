package com.authservice.dto;

public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private boolean mfaRequired;

    public TokenResponse() {
    }

    public TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, boolean mfaRequired) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.mfaRequired = mfaRequired;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public static TokenResponseBuilder builder() {
        return new TokenResponseBuilder();
    }

    public static class TokenResponseBuilder {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;
        private boolean mfaRequired;

        public TokenResponseBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public TokenResponseBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public TokenResponseBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public TokenResponseBuilder expiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public TokenResponseBuilder mfaRequired(boolean mfaRequired) {
            this.mfaRequired = mfaRequired;
            return this;
        }

        public TokenResponse build() {
            return new TokenResponse(accessToken, refreshToken, tokenType, expiresIn, mfaRequired);
        }
    }
}
