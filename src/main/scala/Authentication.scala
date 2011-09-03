package com.novus.rugu

sealed trait Authentication
case class UsernameAndPassword(username: String, password: String) extends Authentication
case class PublicKey(username: String, privateKey: String, password: Option[String] = None) extends Authentication
