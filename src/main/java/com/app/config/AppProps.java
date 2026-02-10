package com.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 애플리케이션 설정 프로퍼티 클래스
 * 
 * application.yml의 app.* 설정을 바인딩합니다.
 */
@Getter @Setter
@ConfigurationProperties(prefix = "app")
public class AppProps {
    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Mail mail = new Mail();
    private Aws aws = new Aws();

    @Getter @Setter
    public static class Cors {
        private String allowedOrigins;
    }

    @Getter @Setter
    public static class Jwt {
        private String issuer;
        private long accessTtlMinutes;
        private long refreshTtlDays;
        private String secret;
    }

    @Getter @Setter
    public static class Mail {
        private String from;
        private long authCodeExpirationMillis;
    }

    @Getter @Setter
    public static class Aws {
        private S3 s3 = new S3();

        @Getter @Setter
        public static class S3 {
            private String bucket;
            private String region;
            private String accessKey;
            private String secretKey;
            private String baseUrl;
        }
    }
}
