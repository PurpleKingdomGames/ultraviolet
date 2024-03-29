package ultraviolet.predef

import ultraviolet.datatypes.ShaderAST
import ultraviolet.datatypes.ShaderPrinter
import ultraviolet.datatypes.ShaderValid
import ultraviolet.syntax.*

/** This is an example of what an Indigo shader could look like based on the pre-ultraviolet set up, it's just for
  * testing.
  */
object indigo:

  final case class IndigoVertexEnv(
      TIME: Float,         // Running time
      VIEWPORT_SIZE: vec2, // Size of the viewport in pixels

      // Variables
      ATLAS_SIZE: vec2,
      VERTEX: vec4,
      TEXTURE_SIZE: vec2,
      UV: vec2,
      SIZE: vec2,
      FRAME_SIZE: vec2,
      CHANNEL_0_ATLAS_OFFSET: vec2,
      CHANNEL_1_ATLAS_OFFSET: vec2,
      CHANNEL_2_ATLAS_OFFSET: vec2,
      CHANNEL_3_ATLAS_OFFSET: vec2,
      CHANNEL_0_TEXTURE_COORDS: vec2,
      CHANNEL_1_TEXTURE_COORDS: vec2,
      CHANNEL_2_TEXTURE_COORDS: vec2,
      CHANNEL_3_TEXTURE_COORDS: vec2,
      CHANNEL_0_POSITION: vec2,
      CHANNEL_1_POSITION: vec2,
      CHANNEL_2_POSITION: vec2,
      CHANNEL_3_POSITION: vec2,
      CHANNEL_0_SIZE: vec2,
      POSITION: vec2,
      SCALE: vec2,
      REF: vec2,
      FLIP: vec2,
      ROTATION: Float,
      TEXTURE_COORDS: vec2, // Redundant, equal to UV
      INSTANCE_ID: Int,

      // Constants
      PI: Float,
      PI_2: Float,
      PI_4: Float,
      TAU: Float,
      TAU_2: Float,
      TAU_4: Float,
      TAU_8: Float
  )

  @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
  final case class IndigoFragmentEnv(
      SRC_CHANNEL: sampler2D.type,
      TIME: Float,         // Running time
      VIEWPORT_SIZE: vec2, // Size of the viewport in pixels

      // Variables
      UV: vec2,                       // Unscaled texture coordinates
      SIZE: vec2,                     // Width / height of the objects
      var CHANNEL_0: vec4,            // Pixel value from texture channel 0
      var CHANNEL_1: vec4,            // Pixel value from texture channel 1
      var CHANNEL_2: vec4,            // Pixel value from texture channel 2
      var CHANNEL_3: vec4,            // Pixel value from texture channel 3
      CHANNEL_0_TEXTURE_COORDS: vec2, // Scaled texture coordinates
      CHANNEL_1_TEXTURE_COORDS: vec2, // Scaled texture coordinates
      CHANNEL_2_TEXTURE_COORDS: vec2, // Scaled texture coordinates
      CHANNEL_3_TEXTURE_COORDS: vec2, // Scaled texture coordinates
      CHANNEL_0_POSITION: vec2,       // top left position of this texture on the atlas in UV coords
      CHANNEL_1_POSITION: vec2,       // top left position of this texture on the atlas in UV coords
      CHANNEL_2_POSITION: vec2,       // top left position of this texture on the atlas in UV coords
      CHANNEL_3_POSITION: vec2,       // top left position of this texture on the atlas in UV coords
      CHANNEL_0_SIZE: vec2,           // size of this texture on the atlas in UV coords
      SCREEN_COORDS: vec2,
      ROTATION: Float,
      TEXTURE_SIZE: vec2, // Size of the texture in pixels
      ATLAS_SIZE: vec2,   // Size of the atlas this texture is on, in pixels
      INSTANCE_ID: Int,   // The current instance id

      // Light information
      LIGHT_INDEX: Int,
      LIGHT_COUNT: Int,
      LIGHT_ACTIVE: Int,
      LIGHT_TYPE: Int,
      LIGHT_FAR_CUT_OFF: Int,
      LIGHT_FALLOFF_TYPE: Int,
      LIGHT_COLOR: vec4,
      LIGHT_SPECULAR: vec4,
      LIGHT_POSITION: vec2,
      LIGHT_ROTATION: Float,
      LIGHT_NEAR: Float,
      LIGHT_FAR: Float,
      LIGHT_ANGLE: Float,
      LIGHT_INTENSITY: Float,

      // Constants
      PI: Float,
      PI_2: Float,
      PI_4: Float,
      TAU: Float,
      TAU_2: Float,
      TAU_4: Float,
      TAU_8: Float
  )

  sealed trait Indigo

  given ShaderPrinter[Indigo] = new ShaderPrinter {
    val webGL2Printer = summon[ShaderPrinter[WebGL2]]

    def isValid(
        inType: Option[String],
        outType: Option[String],
        functions: List[ShaderAST],
        body: ShaderAST
    ): ShaderValid =
      webGL2Printer.isValid(inType, outType, functions, body)

    def transformer: PartialFunction[ShaderAST, ShaderAST] =
      val pf: PartialFunction[ShaderAST, ShaderAST] = {
        case ShaderAST.Function(
              "fragment",
              Nil,
              ShaderAST.Block(statements),
              ShaderAST.DataTypes.ident("vec4")
            ) =>
          val nonEmpty = statements
            .filterNot(_.isEmpty)

          val (init, last) =
            if nonEmpty.length > 1 then (nonEmpty.dropRight(1), nonEmpty.takeRight(1))
            else (Nil, nonEmpty)

          ShaderAST.Function(
            "fragment",
            Nil,
            ShaderAST.Block(
              init ++
                List(
                  ShaderAST.Assign(ShaderAST.DataTypes.ident("COLOR"), last.headOption.getOrElse(ShaderAST.Empty()))
                )
            ),
            ShaderAST.unknownType
          )

        case ShaderAST.Function(
              "fragment",
              Nil,
              body,
              ShaderAST.DataTypes.ident("vec4")
            ) =>
          ShaderAST.Function(
            "fragment",
            Nil,
            ShaderAST.Assign(ShaderAST.DataTypes.ident("COLOR"), body),
            ShaderAST.unknownType
          )
      }

      pf.orElse(webGL2Printer.transformer)

    def printer: PartialFunction[ShaderAST, List[String]] = webGL2Printer.printer
  }
