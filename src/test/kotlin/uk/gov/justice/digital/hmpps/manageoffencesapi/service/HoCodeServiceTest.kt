package uk.gov.justice.digital.hmpps.manageoffencesapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.Feature
import uk.gov.justice.digital.hmpps.manageoffencesapi.repository.HomeOfficeCodeRepository

class HoCodeServiceTest {

  private val awsS3Service = mock<AwsS3Service>()
  private val homeOfficeCodeRepository = mock<HomeOfficeCodeRepository>()
  private val adminService = mock<AdminService>()
  private val hoCodeService = HoCodeService(awsS3Service, homeOfficeCodeRepository, adminService)

  @Test
  fun `Do not run load if feature toggle is disabled`() {
    whenever(adminService.isFeatureEnabled(Feature.SYNC_HOME_OFFICE_CODES)).thenReturn(false)
    hoCodeService.fullLoadOfHomeOfficeCodes()
    verifyNoInteractions(awsS3Service)
  }
}
