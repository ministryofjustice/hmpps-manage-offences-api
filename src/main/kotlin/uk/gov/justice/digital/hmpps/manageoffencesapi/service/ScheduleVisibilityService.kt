package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class ScheduleVisibilityService {
  fun canViewDrafts(): Boolean = SecurityContextHolder.getContext().authentication
    ?.authorities
    ?.any { it.authority == ADMIN_ROLE } ?: false

  private companion object {
    const val ADMIN_ROLE = "ROLE_MANAGE_OFFENCES_ADMIN"
  }
}
