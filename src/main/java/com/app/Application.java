package com.app;

import com.app.config.AppProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Spring Boot 애플리케이션의 시작점입니다.
 *
 * <p>
 * 애플리케이션을 실행하면 내장 톰캣 서버가 구동되고, 컴포넌트 스캔을 통해
 * Controller, Service, Repository(@Mapper) 등의 빈들이 등록됩니다.
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.app")
@ComponentScan(basePackages = {"com.app", "com.app.mapper"})
@EnableConfigurationProperties(AppProps.class)
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    S3Client s3Client(AppProps appProps) {
        AppProps.Aws.S3 s3Props = appProps.getAws().getS3();
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3Props.getRegion()));

        if (StringUtils.hasText(s3Props.getAccessKey()) && StringUtils.hasText(s3Props.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    @Bean
    S3Presigner s3Presigner(AppProps appProps) {
        AppProps.Aws.S3 s3Props = appProps.getAws().getS3();
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(s3Props.getRegion()));

        if (StringUtils.hasText(s3Props.getAccessKey()) && StringUtils.hasText(s3Props.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

}
