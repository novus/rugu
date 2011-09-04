package com.novus.rugu

import scala.util.control.Exception.allCatch

trait Executor {
  def apply[A](command: Command[_, A])(f: java.io.InputStream => A): Either[Throwable, (Option[Int], A, String)]
  def upload(localFile: String, remotePath: String): Either[Throwable, Unit]
  def download(remotePath: String, localFile: String): Either[Throwable, Unit]
}
