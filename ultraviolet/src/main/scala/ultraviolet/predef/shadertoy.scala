package ultraviolet.predef

import ultraviolet.datatypes.ShaderAST
import ultraviolet.datatypes.ShaderValid
import ultraviolet.datatypes.ShaderValidation
import ultraviolet.syntax.*

@SuppressWarnings(Array("scalafix:DisableSyntax.var"))
object shadertoy:

  // Current doesn't support samplerCube types, Ultraviolet does, just not sure how to represent that here.
  final case class ShaderToyEnv(
      iResolution: vec3,                  // viewport resolution (in pixels)
      iTime: Float,                       // shader playback time (in seconds)
      iTimeDelta: Float,                  // render time (in seconds)
      iFrameRate: Float,                  // shader frame rate
      iFrame: Int,                        // shader playback frame
      iChannelTime: array[Float, 4],      // channel playback time (in seconds)
      iChannelResolution: array[vec3, 4], // channel resolution (in pixels)
      iMouse: vec4,                       // mouse pixel coords. xy: current (if MLB down) = null zw: click
      iChannel0: sampler2D.type,          // input channel. XX = 2D/Cube
      iChannel1: sampler2D.type,          // input channel. XX = 2D/Cube
      iChannel2: sampler2D.type,          // input channel. XX = 2D/Cube
      iChannel3: sampler2D.type,          // input channel. XX = 2D/Cube
      iDate: vec4,                        // (year = null month = null day = null time in seconds)
      iSampleRate: Float,                 // sound sample rate (i.e. = null 44100)
      fragCoord: vec2,                    // UV coordinates // Unoffical, from the main function definition
      var fragColor: vec4                 // output variable // Unoffical, from the main function definition
  )
  object ShaderToyEnv:
    def Default: ShaderToyEnv =
      ShaderToyEnv(
        iResolution = vec3(640.0f, 480.0f, 0.0f),
        iTime = 0.0f,
        iTimeDelta = 0.0167,
        iFrameRate = 60,
        iFrame = 0,
        iChannelTime = array[Float, 4](4),      // channel playback time (in seconds)
        iChannelResolution = array[vec3, 4](4), // channel resolution (in pixels)
        iMouse = vec4(0.0f),
        iChannel0 = sampler2D,
        iChannel1 = sampler2D,
        iChannel2 = sampler2D,
        iChannel3 = sampler2D,
        iDate = vec4(0.0f),
        iSampleRate = 44100.0f,
        fragCoord = vec2(0.0),
        fragColor = vec4(0.0f)
      )

  given ShaderValidation with
    def isValid(
        inType: Option[String],
        outType: Option[String],
        headers: List[ShaderAST],
        functions: List[ShaderAST],
        body: ShaderAST
    ): ShaderValid =
      val inTypeValid =
        if inType.contains("ShaderToyEnv") then ShaderValid.Valid
        else
          ShaderValid.Invalid(
            List(
              "ShaderToy Shader instances must be of type Shader[ShaderToyEnv, Unit], environment type was: " +
                inType.getOrElse("<missing>")
            )
          )

      val outTypeValid =
        if outType.contains("Unit") then ShaderValid.Valid
        else
          ShaderValid.Invalid(
            List(
              "ShaderToy Shader instances must be of type Shader[ShaderToyEnv, Unit], return type was: " +
                outType.getOrElse("<missing>")
            )
          )

      inTypeValid |+| outTypeValid
