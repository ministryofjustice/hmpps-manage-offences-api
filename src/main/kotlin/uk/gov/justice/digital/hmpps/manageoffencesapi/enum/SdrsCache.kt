package uk.gov.justice.digital.hmpps.manageoffencesapi.enum

import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetApplications
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetMojOffence
import uk.gov.justice.digital.hmpps.manageoffencesapi.enum.MessageType.GetOffence

enum class SdrsCache(
  val sdrsDataSetName: String,
  val alphaChar: Char? = null,
  val messageType: MessageType,
  val isPrimaryCache: Boolean = false,
) {
  OFFENCES_A(sdrsDataSetName = "offence_A", alphaChar = 'A', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_B(sdrsDataSetName = "offence_B", alphaChar = 'B', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_C(sdrsDataSetName = "offence_C", alphaChar = 'C', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_D(sdrsDataSetName = "offence_D", alphaChar = 'D', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_E(sdrsDataSetName = "offence_E", alphaChar = 'E', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_F(sdrsDataSetName = "offence_F", alphaChar = 'F', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_G(sdrsDataSetName = "offence_G", alphaChar = 'G', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_H(sdrsDataSetName = "offence_H", alphaChar = 'H', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_I(sdrsDataSetName = "offence_I", alphaChar = 'I', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_J(sdrsDataSetName = "offence_J", alphaChar = 'J', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_K(sdrsDataSetName = "offence_K", alphaChar = 'K', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_L(sdrsDataSetName = "offence_L", alphaChar = 'L', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_M(sdrsDataSetName = "offence_M", alphaChar = 'M', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_N(sdrsDataSetName = "offence_N", alphaChar = 'N', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_O(sdrsDataSetName = "offence_O", alphaChar = 'O', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_P(sdrsDataSetName = "offence_P", alphaChar = 'P', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_Q(sdrsDataSetName = "offence_Q", alphaChar = 'Q', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_R(sdrsDataSetName = "offence_R", alphaChar = 'R', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_S(sdrsDataSetName = "offence_S", alphaChar = 'S', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_T(sdrsDataSetName = "offence_T", alphaChar = 'T', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_U(sdrsDataSetName = "offence_U", alphaChar = 'U', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_V(sdrsDataSetName = "offence_V", alphaChar = 'V', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_W(sdrsDataSetName = "offence_W", alphaChar = 'W', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_X(sdrsDataSetName = "offence_X", alphaChar = 'X', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_Y(sdrsDataSetName = "offence_Y", alphaChar = 'Y', messageType = GetOffence, isPrimaryCache = true),
  OFFENCES_Z(sdrsDataSetName = "offence_Z", alphaChar = 'Z', messageType = GetOffence, isPrimaryCache = true),
  GET_APPLICATIONS(sdrsDataSetName = "getApplications", messageType = GetApplications),
  GET_MOJ_OFFENCE(sdrsDataSetName = "getMojOffence", messageType = GetMojOffence),
  ;

  companion object {
    fun fromSdrsDataSetName(sdrsDataSetName: String): SdrsCache = values().first { it.sdrsDataSetName == sdrsDataSetName }
  }
}
