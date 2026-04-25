package com.boxdispatch.Components;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @NotBlank(message = "JWT secret key is required")
    private String secretKey;

    @NotNull @Positive @Min(1)
    private Integer expirationMinutes = 1440;

    @NotNull @Positive @Min(1)
    private Integer refreshExpirationDays = 7;

    private String  issuer                        = "UDS";
    private String  audience                      = "Authorized UDS USERS";

    @NotNull @Min(0)
    private Integer clockSkewSeconds              = 30;

    private String  algorithm                     = "HS256";
    private String  tokenPrefix                   = "Bearer";
    private String  authorizationHeader           = "Authorization";
    private Boolean enableTokenBlacklist          = true;
    private Long    blacklistCleanupIntervalMinutes = 60L;
    private Long    maxBlacklistSize              = 10000L;
    private String  cookieName                    = "jwt-token";
    private Boolean httpOnlyCookie                = true;
    private Boolean secureCookie                  = true;
    private String  sameSiteCookie                = "Strict";
    private String  corsAllowedOrigins            = "http://localhost:3000,http://localhost:3001,http://localhost:8000";
    private Boolean enableMultiFactorSupport      = false;
    private Integer maxTokensPerUser              = 5;
    private Boolean logTokenIssuance              = false;
    private Boolean enableTokenRotation           = true;
    private Integer rotationGracePeriodMinutes    = 5;

    private AccessToken  accessToken  = new AccessToken();
    private RefreshToken refreshToken = new RefreshToken();

    public boolean isCookieEnabled() {
        return cookieName != null && !cookieName.isBlank();
    }

    public long getExpirationMillis() {
        return expirationMinutes * 60 * 1000L;
    }

    public long getRefreshExpirationMillis() {
        return refreshExpirationDays * 24L * 60 * 60 * 1000L;
    }

    public long getClockSkewMillis() {
        return clockSkewSeconds * 1000L;
    }

    @Getter
    @Setter
    public static class AccessToken {
        private Long expiration = 900000L;
    }

    @Getter
    @Setter
    public static class RefreshToken {
        private Long expiration = 604800000L;
    }
}