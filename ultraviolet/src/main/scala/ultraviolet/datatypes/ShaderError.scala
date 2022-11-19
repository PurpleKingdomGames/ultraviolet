package ultraviolet.datatypes

import scala.util.control.NoStackTrace

object ShaderError:

  final case class ValidationError(msg: String) extends Exception(msg) with NoStackTrace
