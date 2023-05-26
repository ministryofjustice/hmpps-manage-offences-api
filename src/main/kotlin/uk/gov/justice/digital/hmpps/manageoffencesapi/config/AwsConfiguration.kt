package uk.gov.justice.digital.hmpps.manageoffencesapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import java.time.Duration

@Configuration
class AwsConfiguration(
  @Value("\${aws.region:eu-west-1}") private val awsRegion: String,
) {
  @Bean
  fun s3AsyncClient(
    credentialsProvider: AwsCredentialsProvider,
  ): S3AsyncClient {
    val httpClient: SdkAsyncHttpClient = NettyNioAsyncHttpClient.builder()
      .writeTimeout(Duration.ZERO)
      .maxConcurrency(64)
      .build()
    val serviceConfiguration = S3Configuration.builder()
      .checksumValidationEnabled(false)
      .chunkedEncodingEnabled(true)
      .build()
    return S3AsyncClient.builder().httpClient(httpClient)
      .region(Region.of(awsRegion))
      .credentialsProvider(credentialsProvider)
      .serviceConfiguration(serviceConfiguration)
      .build()
  }
}
