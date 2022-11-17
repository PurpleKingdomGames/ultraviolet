package ultraviolet

import ultraviolet.datatypes.ShaderDSLOps
import ultraviolet.macros.UBOReader

import scala.annotation.StaticAnnotation
import scala.deriving.Mirror

object syntax extends ShaderDSLOps:

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

  final class attribute extends StaticAnnotation // WebGL 1.0 // TODO - move to predefs when they exist.
  final class const     extends StaticAnnotation
  final class in        extends StaticAnnotation // WebGL 2.0 // TODO - move to predefs when they exist.
  final class out       extends StaticAnnotation // WebGL 2.0 // TODO - move to predefs when they exist.
  final class uniform   extends StaticAnnotation
  final class varying   extends StaticAnnotation // WebGL 1.0 // TODO - move to predefs when they exist.

  inline def raw(body: String): RawGLSL =
    RawGLSL(body)

  inline def ubo[A](using Mirror.ProductOf[A]) = UBOReader.readUBO[A]

end syntax
