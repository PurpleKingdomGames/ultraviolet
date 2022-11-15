package ultraviolet.core

import scala.deriving.Mirror

/** A `Shader` is a program that can be run on a graphics card as part of the rendering pipeline.
  */
final case class Shader[In, Out](headers: List[GLSLHeader], ubos: List[UBODef], body: In => Out)
object Shader:
  inline def apply[In, Out](f: In => Out): Shader[In, Out] =
    new Shader[In, Out](Nil, Nil, f)
  inline def apply[In, Out](headers: GLSLHeader*)(f: In => Out): Shader[In, Out] =
    new Shader[In, Out](headers.toList, Nil, f)

  inline def apply[In](f: In => Unit): Shader[In, Unit] =
    new Shader[In, Unit](Nil, Nil, f)
  inline def apply[In](headers: GLSLHeader*)(f: In => Unit): Shader[In, Unit] =
    new Shader[In, Unit](Nil, Nil, f)

  inline def apply(body: => Any): Shader[Unit, Unit] =
    new Shader[Unit, Unit](Nil, Nil, (_: Unit) => body)
  inline def apply(headers: GLSLHeader*)(body: => Any): Shader[Unit, Unit] =
    new Shader[Unit, Unit](headers.toList, Nil, (_: Unit) => body)

  extension (inline ctx: Shader[Unit, Unit])
    inline def toGLSL: String =
      ShaderMacros.toAST(ctx).render

  // extension [In, Out](inline ctx: Shader[In, Out])
  //   inline def toGLSL(using Mirror.ProductOf[In]): String =
  //     ShaderMacros.toAST(ctx).withUBOs(ctx.ubos).toGLSL[In]

  extension [In, Out](inline ctx: Shader[In, Out])
    inline def toGLSL: String =
      ShaderMacros.toAST(ctx).render
