package com.novus.rugu

sealed trait Authentication {
  val username: String
}
case class UsernameAndPassword(username: String, password: String) extends Authentication
case class PrivateKeyFile(username: String, fileLocation: String, keyPassphrase: Option[String] = None) extends Authentication
