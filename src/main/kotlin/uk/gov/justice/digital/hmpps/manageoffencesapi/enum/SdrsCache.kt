package uk.gov.justice.digital.hmpps.manageoffencesapi.enum

import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetApplications
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetMojOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetOffence

enum class SdrsCache(val sdrsDataSetName: String, val alphaChar: Char? = null, val messageType: MessageType) {
  OFFENCES_A(sdrsDataSetName = "offence_A", alphaChar = 'A', messageType = GetOffence),
  OFFENCES_B(sdrsDataSetName = "offence_B", alphaChar = 'B', messageType = GetOffence),
  OFFENCES_C(sdrsDataSetName = "offence_C", alphaChar = 'C', messageType = GetOffence),
  OFFENCES_D(sdrsDataSetName = "offence_D", alphaChar = 'D', messageType = GetOffence),
  OFFENCES_E(sdrsDataSetName = "offence_E", alphaChar = 'E', messageType = GetOffence),
  OFFENCES_F(sdrsDataSetName = "offence_F", alphaChar = 'F', messageType = GetOffence),
  OFFENCES_G(sdrsDataSetName = "offence_G", alphaChar = 'G', messageType = GetOffence),
  OFFENCES_H(sdrsDataSetName = "offence_H", alphaChar = 'H', messageType = GetOffence),
  OFFENCES_I(sdrsDataSetName = "offence_I", alphaChar = 'I', messageType = GetOffence),
  OFFENCES_J(sdrsDataSetName = "offence_J", alphaChar = 'J', messageType = GetOffence),
  OFFENCES_K(sdrsDataSetName = "offence_K", alphaChar = 'K', messageType = GetOffence),
  OFFENCES_L(sdrsDataSetName = "offence_L", alphaChar = 'L', messageType = GetOffence),
  OFFENCES_M(sdrsDataSetName = "offence_M", alphaChar = 'M', messageType = GetOffence),
  OFFENCES_N(sdrsDataSetName = "offence_N", alphaChar = 'N', messageType = GetOffence),
  OFFENCES_O(sdrsDataSetName = "offence_O", alphaChar = 'O', messageType = GetOffence),
  OFFENCES_P(sdrsDataSetName = "offence_P", alphaChar = 'P', messageType = GetOffence),
  OFFENCES_Q(sdrsDataSetName = "offence_Q", alphaChar = 'Q', messageType = GetOffence),
  OFFENCES_R(sdrsDataSetName = "offence_R", alphaChar = 'R', messageType = GetOffence),
  OFFENCES_S(sdrsDataSetName = "offence_S", alphaChar = 'S', messageType = GetOffence),
  OFFENCES_T(sdrsDataSetName = "offence_T", alphaChar = 'T', messageType = GetOffence),
  OFFENCES_U(sdrsDataSetName = "offence_U", alphaChar = 'U', messageType = GetOffence),
  OFFENCES_V(sdrsDataSetName = "offence_V", alphaChar = 'V', messageType = GetOffence),
  OFFENCES_W(sdrsDataSetName = "offence_W", alphaChar = 'W', messageType = GetOffence),
  OFFENCES_X(sdrsDataSetName = "offence_X", alphaChar = 'X', messageType = GetOffence),
  OFFENCES_Y(sdrsDataSetName = "offence_Y", alphaChar = 'Y', messageType = GetOffence),
  OFFENCES_Z(sdrsDataSetName = "offence_Z", alphaChar = 'Z', messageType = GetOffence),
  GET_APPLICATIONS(sdrsDataSetName = "getApplications", messageType = GetApplications),
  GET_MOJ_OFFENCE(sdrsDataSetName = "getMojOffence", messageType = GetMojOffence);

  // SDRS caches offence_A to offence_Z are the primary caches
  fun isPrimaryCache(): Boolean = this.alphaChar != null
  companion object {
    fun fromSdrsDataSetName(sdrsDataSetName: String): SdrsCache = values().first { it.sdrsDataSetName == sdrsDataSetName }
  }
}
