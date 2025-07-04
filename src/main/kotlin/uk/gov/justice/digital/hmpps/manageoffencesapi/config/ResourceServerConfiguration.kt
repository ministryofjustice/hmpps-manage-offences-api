package uk.gov.justice.digital.hmpps.manageoffencesapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class ResourceServerConfiguration {

  @Bean
  fun web(http: HttpSecurity): SecurityFilterChain {
    http {
      sessionManagement {
        sessionCreationPolicy = SessionCreationPolicy.STATELESS
      }
      csrf { disable() }
      headers {
        referrerPolicy {
          policy = ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN
        }
      }
      authorizeHttpRequests {
        authorize("/webjars/**", HttpMethod.GET.name(), permitAll)
        authorize("/favicon.ico", HttpMethod.GET.name(), permitAll)
        authorize("/health/**", HttpMethod.GET.name(), permitAll)
        authorize("/info", HttpMethod.GET.name(), permitAll)
        authorize("/swagger-resources/**", HttpMethod.GET.name(), permitAll)
        authorize("/v3/api-docs/**", HttpMethod.GET.name(), permitAll)
        authorize("/swagger-ui/**", HttpMethod.GET.name(), permitAll)
        authorize("/swagger-ui.html", HttpMethod.GET.name(), permitAll)
        authorize("/h2-console/**", HttpMethod.POST.name(), permitAll)
        authorize("/some-url-not-found", HttpMethod.GET.name(), permitAll)
        authorize("/schedule/**", HttpMethod.GET.name(), permitAll)
        authorize(anyRequest, authenticated)
      }
      oauth2ResourceServer {
        jwt {
          jwtAuthenticationConverter = AuthAwareTokenConverter()
        }
      }
    }
    return http.build()
  }
}
