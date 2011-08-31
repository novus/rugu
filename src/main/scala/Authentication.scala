package com.novus.rugu

sealed trait Authentication {
  val username: String
}
case class UsernameAndPassword(username: String, password: String) extends Authentication
case class PublicKey(username: String, fileLocation: String) extends Authentication
