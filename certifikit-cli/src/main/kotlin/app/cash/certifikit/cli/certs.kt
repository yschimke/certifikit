/*
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.certifikit.cli

import app.cash.certifikit.Certificate
import app.cash.certifikit.CertificateAdapters
import app.cash.certifikit.cli.errors.UsageException
import app.cash.certifikit.text.certificatePem
import java.io.File
import java.io.FileNotFoundException
import java.security.cert.X509Certificate
import okio.ByteString.Companion.decodeBase64

internal fun String.parsePemCertificate(fileName: String? = null): Certificate {
  val regex = """-----BEGIN CERTIFICATE-----(.*)-----END CERTIFICATE-----""".toRegex(RegexOption.DOT_MATCHES_ALL)
  val matchResult = regex.find(this) ?: throw UsageException("Invalid format" +
      if (fileName != null) ": $fileName" else "")
    val (pemBody) = matchResult.destructured

  val data = pemBody.decodeBase64()!!

  return CertificateAdapters.certificate.fromDer(data)
}

internal fun File.parsePemCertificate(): Certificate {
  try {
    val pemText = readText()

    return pemText.parsePemCertificate(name)
  } catch (fnfe: FileNotFoundException) {
    throw UsageException("No such file: $this", fnfe)
  }
}

internal fun X509Certificate.writePem(
  output: File
) {
  output.writeText(certificatePem())
}
