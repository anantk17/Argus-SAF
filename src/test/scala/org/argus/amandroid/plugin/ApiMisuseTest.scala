/*
 * Copyright (c) 2017. Fengguo Wei and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Detailed contributors are listed in the CONTRIBUTOR.md
 */

package org.argus.amandroid.plugin

import org.argus.amandroid.alir.componentSummary.ApkYard
import org.argus.amandroid.core.appInfo.AppInfoCollector
import org.argus.amandroid.core.decompile.{ConverterUtil, DecompileLayout, DecompilerSettings}
import org.argus.amandroid.plugin.apiMisuse.{CryptographicMisuse, HideIcon, SSLTLSMisuse}
import org.argus.jawa.core.{MsgLevel, NoReporter, PrintReporter}
import org.argus.jawa.core.util.FileUtil
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by fgwei on 4/21/17.
  */
class ApiMisuseTest extends FlatSpec with Matchers {
  private final val DEBUG = false

  "Crypto" should "have 2 misuse" in {
    val res = apiMisuse(getClass.getResource("/apks/crypto.apk").getPath, ApiMisuseModules.CRYPTO_MISUSE)
    assert(res.misusedApis.size == 2)
  }

  private def apiMisuse(apkFile: String, module: ApiMisuseModules.Value): ApiMisuseResult = {
    val fileUri = FileUtil.toUri(apkFile)
    val outputUri = FileUtil.toUri(apkFile.substring(0, apkFile.length - 4))
    val reporter =
      if(DEBUG) new PrintReporter(MsgLevel.INFO)
      else new NoReporter
    val yard = new ApkYard(reporter)
    val layout = DecompileLayout(outputUri)
    val settings = DecompilerSettings(debugMode = false, removeSupportGen = true, forceDelete = true, layout)
    val apk = yard.loadApk(fileUri, settings, collectInfo = false)
    val checker = module match {
      case ApiMisuseModules.CRYPTO_MISUSE => new CryptographicMisuse
      case ApiMisuseModules.HIDE_ICON =>
        val man = AppInfoCollector.analyzeManifest(reporter, FileUtil.appendFileName(apk.model.outApkUri, "AndroidManifest.xml"))
        val mainComp = man.getIntentDB.getIntentFmap.find{ case (_, fs) =>
          fs.exists{ f =>
            f.getActions.contains("android.intent.action.MAIN") && f.getCategorys.contains("android.intent.category.LAUNCHER")
          }
        }.map(_._1)
        new HideIcon(mainComp.get)
      case ApiMisuseModules.SSLTLS_MISUSE => new SSLTLSMisuse
    }
    val res = checker.check(apk, None)
    ConverterUtil.cleanDir(outputUri)
    res
  }
}
