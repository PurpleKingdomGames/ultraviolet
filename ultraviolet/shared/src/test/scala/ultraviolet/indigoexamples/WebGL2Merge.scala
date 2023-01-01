package ultraviolet.indigoexamples

import ultraviolet.predef.indigo.*
import ultraviolet.syntax.*

object WebGL2Merge:

  case class IndigoMergeData(u_projection: mat4, u_scale: vec2)
  case class IndigoFrameData(TIME: highp[Float], VIEWPORT_SIZE: vec2)

  @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
  case class VertexEnv(var gl_Position: vec4)

  @SuppressWarnings(Array("scalafix:DisableSyntax.var", "scalafix:DisableSyntax.null"))
  object vertex:
    inline def shader =
      Shader[IndigoMergeData & VertexEnv] { env =>
        Version300ES
        PrecisionMediumPFloat

        @layout(0) val a_verticesAndCoords: vec4 = null

        ubo[IndigoMergeData]
        ubo[IndigoFrameData]

        @out var SIZE: vec2 = null
        @out var UV: vec2   = null

        @const val PI: Float    = 3.141592653589793f;
        @const val PI_2: Float  = PI * 0.5f;
        @const val PI_4: Float  = PI * 0.25f;
        @const val TAU: Float   = 2.0f * PI;
        @const val TAU_2: Float = PI;
        @const val TAU_4: Float = PI_2;
        @const val TAU_8: Float = PI_4;

        // format: off
        def translate2d(t: vec2): mat4 =
          mat4(1.0f, 0.0f, 0.0f, 0.0f,
               0.0f, 1.0f, 0.0f, 0.0f,
               0.0f, 0.0f, 1.0f, 0.0f,
               t.x,  t.y,  0.0f, 1.0f
          )

        // format: off
        def scale2d(s: vec2): mat4 =
          mat4(s.x,  0.0f, 0.0f, 0.0f,
               0.0f, s.y,  0.0f, 0.0f,
               0.0f, 0.0f, 1.0f, 0.0f,
               0.0f, 0.0f, 0.0f, 1.0f
          )

        var VERTEX: vec4 = null

        //#vertex_start
        def vertex(): Unit = ()
        //#vertex_end

        def main: Unit =
          UV = a_verticesAndCoords.zw
          SIZE = env.u_scale
          VERTEX = vec4(a_verticesAndCoords.x, a_verticesAndCoords.y, 1.0f, 1.0f)

          vertex()

          val moveToTopLeft: vec2 = SIZE / 2.0f
          val transform: mat4 = translate2d(moveToTopLeft) * scale2d(SIZE)

          env.gl_Position = env.u_projection * transform * VERTEX
      }

    val output = shader.toGLSL[Indigo]

    val expected: String =
      """
      |#version 300 es
      |precision mediump float;
      |layout (location = 0) in vec4 a_verticesAndCoords;
      |layout (std140) uniform IndigoMergeData {
      |  mat4 u_projection;
      |  vec2 u_scale;
      |};
      |layout (std140) uniform IndigoFrameData {
      |  highp float TIME;
      |  vec2 VIEWPORT_SIZE;
      |};
      |out vec2 SIZE;
      |out vec2 UV;
      |const float PI=3.1415927;
      |const float PI_2=PI*0.5;
      |const float PI_4=PI*0.25;
      |const float TAU=2.0*PI;
      |const float TAU_2=PI;
      |const float TAU_4=PI_2;
      |const float TAU_8=PI_4;
      |mat4 translate2d(in vec2 t){
      |  return mat4(1.0,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0,0.0,t.x,t.y,0.0,1.0);
      |}
      |mat4 scale2d(in vec2 s){
      |  return mat4(s.x,0.0,0.0,0.0,0.0,s.y,0.0,0.0,0.0,0.0,1.0,0.0,0.0,0.0,0.0,1.0);
      |}
      |vec4 VERTEX;
      |void vertex(){
      |}
      |void main(){
      |  UV=a_verticesAndCoords.zw;
      |  SIZE=u_scale;
      |  VERTEX=vec4(a_verticesAndCoords.x,a_verticesAndCoords.y,1.0,1.0);
      |  vertex();
      |  vec2 moveToTopLeft=SIZE/2.0;
      |  mat4 transform=translate2d(moveToTopLeft)*scale2d(SIZE);
      |  gl_Position=(u_projection*transform)*VERTEX;
      |}
      |""".stripMargin.trim

  object fragment:
    inline def shader =
      Shader {
        def fragment: Unit = {}
      }

    val output = shader.toGLSL[Indigo]

    val expected: String =
      """
      |x
      |""".stripMargin.trim
