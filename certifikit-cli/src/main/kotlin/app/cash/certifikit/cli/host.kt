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

import app.cash.certifikit.cli.errors.UsageException
import app.cash.certifikit.cli.oscp.OcspResponse
import app.cash.certifikit.cli.oscp.ocsp
import app.cash.certifikit.cli.oscp.toCertificate
import app.cash.certifikit.text.certificatePem
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import picocli.CommandLine
import java.io.File
import java.io.IOException

suspend fun Main.queryHost(host: String) {
  coroutineScope {
    val addresses = dnsLookup(host)

    val siteResponses = addresses.map {
      it to async {
        fromHttps(host, it)
      }
    }

    val siteResponse = siteResponses.first().second.await()

    if (siteResponse.peerCertificates.isEmpty()) {
      System.err.println("Warn: ${CommandLine.Help.Ansi.AUTO.string(" @|yellow No trusted certificates|@")}")
    }

    val ocspResponse = ocsp(client, siteResponse)

    val output = output

    siteResponse.peerCertificates.forEachIndexed { i, certificate ->
      if (i > 0) {
        println()
      }

      if (output != null) {
        val outputFile = when {
          output.isDirectory -> File(output, "${certificate.publicKeySha256().hex()}.pem")
          output.path == "-" -> output
          i > 0 -> {
            System.err.println(
              CommandLine.Help.Ansi.AUTO.string(
                "@|yellow Writing host certificate only, skipping (${certificate.subjectX500Principal.name})|@"
              )
            )
            null
          }
          else -> output
        }

        if (outputFile != null) {
          if (outputFile.path == "-") {
            println(certificate.certificatePem())
          } else {
            try {
              certificate.writePem(outputFile)
            } catch (ioe: IOException) {
              throw UsageException("Unable to write to $output", ioe)
            }
          }
        }
      }

      println(certificate.toCertificate().prettyPrintCertificate(trustManager))
    }

    addHostToCompletionFile(host)

    showStrictTransportSecurity(
      siteResponse
    ) // TODO We should add SANs and complete wildcard hosts.

    showOcspResponse(ocspResponse)

    if (siteResponses.size > 1) {
      siteResponses.forEach { siteResponse ->
        println(siteResponse.first)
      }
    }
  }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun Main.dnsLookup(host: String) = withContext(Dispatchers.IO) { client.dns.lookup(host) }

suspend fun showOcspResponse(ocspResponse: Deferred<OcspResponse?>) {
  try {
    val response = ocspResponse.await()

    // null if no url to check.
    if (response != null) {
      println()
      println(response.prettyPrint())
    }
  } catch (e: Exception) {
    System.err.println(
      CommandLine.Help.Ansi.AUTO.string(
        "@|yellow Failed checking OCSP status (${e.message})|@"
      )
    )
  }
}

fun showStrictTransportSecurity(siteResponse: SiteResponse) {
  if (siteResponse.strictTransportSecurity != null) {
    println()
    println("Strict Transport Security: ${siteResponse.strictTransportSecurity}")
  }
}