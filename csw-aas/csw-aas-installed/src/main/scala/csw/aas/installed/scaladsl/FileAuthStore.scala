/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.installed.scaladsl

import java.nio.file.{Path, Paths}

import csw.aas.installed.api.AuthStore

import scala.language.implicitConversions

/**
 * File based implementation for [[csw.aas.installed.api.AuthStore]]
 * Stores access token, id token and refresh token in local file system inside provided directory
 */
class FileAuthStore(storePath: Path) extends AuthStore {

  private val idTokenFileName      = "id_token"
  private val refreshTokenFileName = "refresh_token"
  private val accessTokenFileName  = "access_token"

  private val idTokenPath      = toPath(idTokenFileName)
  private val refreshTokenPath = toPath(refreshTokenFileName)
  private val accessTokenPath  = toPath(accessTokenFileName)

  private def toPath(name: String) = Paths.get(s"${storePath.toString}/$name")

  override def getAccessTokenString: Option[String]  = read(accessTokenPath)
  override def getIdTokenString: Option[String]      = read(idTokenPath)
  override def getRefreshTokenString: Option[String] = read(refreshTokenPath)

  override def clearStorage(): Unit = delete(storePath)

  // todo : Will we always have this tokens
  override def saveTokens(idToken: String, accessToken: String, refreshToken: String): Unit = {
    write(idTokenPath, idToken)
    write(accessTokenPath, accessToken)
    write(refreshTokenPath, refreshToken)
  }

  /**
   * **********************************************
   * Internal APIs: Uses os-libs for file handling
   * **********************************************
   */
  private def write(path: Path, content: String): Unit =
    if (os.exists(path)) os.write.over(path, content)
    else os.write(path, content, createFolders = true)

  private def read(path: Path): Option[String] = if (os.exists(path)) Some(os.read(path)) else None

  private def delete(path: Path): Unit = os.remove.all(path)

  implicit private def toOsPath(path: Path): os.Path = os.Path(path)

}
