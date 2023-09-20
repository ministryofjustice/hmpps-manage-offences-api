package uk.gov.justice.digital.hmpps.manageoffencesapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class ResourceServerConfiguration {

  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    val chain = http
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and().headers().frameOptions().sameOrigin()
      .and().csrf().disable()
      .authorizeHttpRequests { auth ->
        auth.requestMatchers(
          "/webjars/**",
          "favicon.ico",
          "/health/**",
          "/info",
          "/swagger-resources/**",
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/h2-console/**",
        ).permitAll().anyRequest().authenticated()
      }
    chain.oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
    return chain.build()
  }
}
