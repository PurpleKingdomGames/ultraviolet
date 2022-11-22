package ultraviolet

import ultraviolet.datatypes.ShaderDSLOps
import ultraviolet.macros.UBOReader

import scala.annotation.StaticAnnotation
import scala.deriving.Mirror

object syntax extends ShaderDSLOps:
  type WebGL1 = ultraviolet.datatypes.ShaderPrinter.WebGL1
  type WebGL2 = ultraviolet.datatypes.ShaderPrinter.WebGL2

  extension (f: Float)
    def +(v: vec2): vec2 = vec2(f + v.x, f + v.y)
    def -(v: vec2): vec2 = vec2(f - v.x, f - v.y)
    def *(v: vec2): vec2 = vec2(f * v.x, f * v.y)
    def /(v: vec2): vec2 = vec2(f / v.x, f / v.y)

    def +(v: vec3): vec3 = vec3(f + v.x, f + v.y, f + v.z)
    def -(v: vec3): vec3 = vec3(f - v.x, f - v.y, f - v.z)
    def *(v: vec3): vec3 = vec3(f * v.x, f * v.y, f * v.z)
    def /(v: vec3): vec3 = vec3(f / v.x, f / v.y, f / v.z)

    def +(v: vec4): vec4 = vec4(f + v.x, f + v.y, f + v.z, f + v.w)
    def -(v: vec4): vec4 = vec4(f - v.x, f - v.y, f - v.z, f - v.w)
    def *(v: vec4): vec4 = vec4(f * v.x, f * v.y, f * v.z, f * v.w)
    def /(v: vec4): vec4 = vec4(f / v.x, f / v.y, f / v.z, f / v.w)

  type highp[A]   = A
  type mediump[A] = A
  type lowp[A]    = A

  type RawGLSL = ultraviolet.datatypes.RawGLSL
  val RawGLSL: ultraviolet.datatypes.RawGLSL.type = ultraviolet.datatypes.RawGLSL

  type Shader[In, Out] = ultraviolet.datatypes.Shader[In, Out]
  val Shader: ultraviolet.datatypes.Shader.type = ultraviolet.datatypes.Shader

  type GLSLHeader[In, Out] = ultraviolet.datatypes.GLSLHeader
  val GLSLHeader: ultraviolet.datatypes.GLSLHeader.type = ultraviolet.datatypes.GLSLHeader

  final class attribute                    extends StaticAnnotation
  final class const                        extends StaticAnnotation
  final class uniform                      extends StaticAnnotation
  final class in                           extends StaticAnnotation
  final class out                          extends StaticAnnotation

  inline def ubo[A](using Mirror.ProductOf[A]) = UBOReader.readUBO[A]

  inline def raw(body: String): RawGLSL =
    RawGLSL(body)

end syntax
