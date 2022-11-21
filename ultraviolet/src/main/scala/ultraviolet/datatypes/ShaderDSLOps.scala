package ultraviolet.datatypes

import scala.annotation.targetName

/** Stub implementations for common built in GLSL operations.
  */
trait ShaderDSLOps extends ShaderDSLTypeExtensions:

  def texture2D(sampler: sampler2D.type, coords: vec2): vec4 =
    vec4(0.0f)
  def textureCube(sampler: samplerCube.type, normal: vec3): vec4 =
    vec4(0.0f)

  def length(genType: Float | vec2 | vec3 | vec4): Float =
    genType match
      case f: Float =>
        Math.sqrt(Math.pow(f, 2.0f)).toFloat

      case vec2(x, y) =>
        Math.sqrt(Math.pow(x, 2.0f) + Math.pow(y, 2.0f)).toFloat

      case vec3(x, y, z) =>
        Math.sqrt(Math.pow(x, 2.0f) + Math.pow(y, 2.0f) + Math.pow(z, 2.0f)).toFloat

      case vec4(x, y, z, w) =>
        Math
          .sqrt(
            Math.pow(x, 2.0f) +
              Math.pow(y, 2.0f) +
              Math.pow(z, 2.0f) +
              Math.pow(w, 2.0f)
          )
          .toFloat

  def step(edge: Float, x: Float): Float =
    if x < edge then 0.0f else 1.0f

  @targetName("fract_float")
  def fract(genType: Float): Float = genType
  @targetName("fract_vec2")
  def fract(genType: vec2): vec2 = genType
  def floor(genType: vec2): vec2 = genType

  def dot(x: Float, y: Float): Float = 0.0f
  def dot(x: vec2, y: vec2): Float   = 0.0f
  def dot(x: vec3, y: vec3): Float   = 0.0f
  def dot(x: vec4, y: vec4): Float   = 0.0f

  def pow(x: Float, y: Float): Float = 0.0f
  def pow(x: vec2, y: vec2): vec2    = vec2(0.0f)
  def pow(x: vec3, y: vec3): vec3    = vec3(0.0f)
  def pow(x: vec4, y: vec4): vec4    = vec4(0.0f)

  def max(x: Float, y: Float): Float = 0.0f
  def max(x: vec2, y: vec2): vec2    = vec2(0.0f)
  def max(x: vec2, y: Float): vec2   = vec2(0.0f)
  def max(x: vec3, y: vec3): vec3    = vec3(0.0f)
  def max(x: vec3, y: Float): vec3   = vec3(0.0f)
  def max(x: vec4, y: vec4): vec4    = vec4(0.0f)
  def max(x: vec4, y: Float): vec4   = vec4(0.0f)

  def min(x: Float, y: Float): Float = 0.0f
  def min(x: vec2, y: vec2): vec2    = vec2(0.0f)
  def min(x: vec2, y: Float): vec2   = vec2(0.0f)
  def min(x: vec3, y: vec3): vec3    = vec3(0.0f)
  def min(x: vec3, y: Float): vec3   = vec3(0.0f)
  def min(x: vec4, y: vec4): vec4    = vec4(0.0f)
  def min(x: vec4, y: Float): vec4   = vec4(0.0f)

  def sin(genType: Float): Float = 0.0f
  def sin(genType: vec2): vec2   = vec2(0.0f)
  def sin(genType: vec3): vec3   = vec3(0.0f)
  def sin(genType: vec4): vec4   = vec4(0.0f)

  def cos(genType: Float): Float = 0.0f
  def cos(genType: vec2): vec2   = vec2(0.0f)
  def cos(genType: vec3): vec3   = vec3(0.0f)
  def cos(genType: vec4): vec4   = vec4(0.0f)

  def tan(genType: Float): Float = 0.0f
  def tan(genType: vec2): vec2   = vec2(0.0f)
  def tan(genType: vec3): vec3   = vec3(0.0f)
  def tan(genType: vec4): vec4   = vec4(0.0f)

  def atan(genType: Float): Float = 0.0f
  def atan(genType: vec2): vec2   = vec2(0.0f)
  def atan(genType: vec3): vec3   = vec3(0.0f)
  def atan(genType: vec4): vec4   = vec4(0.0f)

  def atan2(genType: Float): Float = 0.0f
  def atan2(genType: vec2): vec2   = vec2(0.0f)
  def atan2(genType: vec3): vec3   = vec3(0.0f)
  def atan2(genType: vec4): vec4   = vec4(0.0f)

  def normalize(genType: Float): Float = 0.0f
  def normalize(genType: vec2): vec2   = vec2(0.0f)
  def normalize(genType: vec3): vec3   = vec3(0.0f)
  def normalize(genType: vec4): vec4   = vec4(0.0f)

  def mix(x: Float, y: Float, a: Float): Float = 0.0f

  def reflect(i: Float, n: Float): Float = 0.0f
  def reflect(i: vec2, n: vec2): vec2    = vec2(0.0f)
  def reflect(i: vec3, n: vec3): vec3    = vec3(0.0f)
  def reflect(i: vec4, n: vec4): vec4    = vec4(0.0f)
