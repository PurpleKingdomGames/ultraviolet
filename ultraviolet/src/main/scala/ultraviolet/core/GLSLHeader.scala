package ultraviolet.core

final case class GLSLHeader(glsl: String)
object GLSLHeader:

  inline def Version300ES: GLSLHeader = GLSLHeader("#version 300 es\n")

  inline def PrecisionHighPFloat: GLSLHeader   = GLSLHeader("precision highp float;\n")
  inline def PrecisionMediumPFloat: GLSLHeader = GLSLHeader("precision mediump float;\n")
  inline def PrecisionLowPFloat: GLSLHeader    = GLSLHeader("precision lowp float;\n")
