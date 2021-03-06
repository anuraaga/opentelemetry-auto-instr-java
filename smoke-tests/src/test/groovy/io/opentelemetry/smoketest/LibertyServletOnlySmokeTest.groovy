/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.smoketest

import spock.lang.IgnoreIf

@AppServer(version = "20.0.0.12", jdk = "8")
@IgnoreIf({ os.windows }) //WindowsTestContainerManager does not support extra resources
class LibertyServletOnlySmokeTest extends LibertySmokeTest {

  @Override
  protected Map<String, String> getExtraResources() {
    return ["liberty-servlet.xml": "/config/server.xml"]
  }

}
