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

import picocli.CommandLine

class MainTest {
  @org.junit.Test fun version() {
    CommandLine(Main()).execute("-V")
  }

  @org.junit.Test fun certificate() {
    fromArgs("src/test/resources/cert.pem").call()
  }

  @org.junit.Test fun https() {
    fromArgs("--host", "www.google.com").call()
  }

  companion object {
    fun fromArgs(vararg args: kotlin.String?): Main {
      return picocli.CommandLine.populateCommand(Main(), *args)
    }
  }
}