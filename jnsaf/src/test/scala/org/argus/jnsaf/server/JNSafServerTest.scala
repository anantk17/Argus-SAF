/*
 * Copyright (c) 2018. Fengguo Wei and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Detailed contributors are listed in the CONTRIBUTOR.md
 */

package org.argus.jnsaf.server

import java.io.File

import com.google.common.hash.Hashing
import com.google.common.io.Files
import io.grpc.{Server, ServerBuilder}
import org.argus.jawa.core.io.{MsgLevel, PrintReporter}
import org.argus.jawa.core.util.{FileResourceUri, FileUtil}
import org.argus.jnsaf.client.JNSafClient
import org.argus.jnsaf.server.JNSafServer.{JNSafService, TITLE}
import org.argus.jnsaf.server.jnsaf_grpc.JNSafGrpc
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext

class JNSafServerTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  var server: Server = _
  var client: JNSafClient = _
  var fileUri: FileResourceUri = _
  var loadResponse: Option[String] = _
  override def beforeAll(): Unit = {
    val reporter = new PrintReporter(MsgLevel.INFO)
    val apk_path = "/tmp/apks"
    val apk_uri = FileUtil.toUri(apk_path)
    val ssd = JNSafGrpc.bindService(new JNSafService(apk_uri, reporter), ExecutionContext.global)
    server = ServerBuilder
      .forPort(55001)
      .addService(ssd)
      .build
      .start
    println(s"$TITLE server started.")
    client = new JNSafClient("localhost", 55001, reporter)
    val file_path = getClass.getResource("/apks/icc_explicit1.apk").getPath
    fileUri = FileUtil.toUri(file_path)
    loadResponse = client.loadAPK(fileUri)
  }

  override def afterAll(): Unit = {
    if(server != null) {
      server.shutdown()
      server = null
    }
    if(client != null) {
      client.shutdown()
      client = null
    }
    println(s"$TITLE server stopped.")
    val dir = new File("/tmp/apks")
    dir.delete()
  }

  "LoadApk" should "success" in {
    assert(loadResponse.isDefined)
    val file = FileUtil.toFile(fileUri)
    val hashCode = Files.hash(file, Hashing.sha256())
    assert(loadResponse.get == hashCode.toString)
  }

  "TaintAnalysis" should "success" in {
    assert(client.taintAnalysis(fileUri))
  }

}