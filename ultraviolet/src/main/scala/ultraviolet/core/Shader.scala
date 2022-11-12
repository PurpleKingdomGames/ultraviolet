package ultraviolet.core

import scala.deriving.Mirror

/** A `Shader` is a program that can be run on a graphics card as part of the rendering pipeline.
  */
final class Shader[In, Out](body: In => Out)
object Shader:
  inline def apply[In, Out](f: In => Out): Shader[In, Out] = new Shader[In, Out](f)
  inline def apply[In](f: In => Unit): Shader[In, Unit]    = new Shader[In, Unit](f)
  inline def apply(body: => Any): Shader[Unit, Unit]       = new Shader[Unit, Unit]((_: Unit) => body)

  extension (inline ctx: Shader[Unit, Unit])
    inline def toGLSL: String =
      ShaderMacros.toAST(ctx).render

  extension [In, Out](inline ctx: Shader[In, Out])
    inline def toGLSL(using Mirror.ProductOf[In]): String =
      ShaderMacros.toAST(ctx).toGLSL[In]
