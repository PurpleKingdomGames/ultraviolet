package ultraviolet.core

import ultraviolet.syntax.*

class EnvReaderTests extends munit.FunSuite {

  test("Create a UBO definition from a case class)") {

    case class FragEnv(
        alpha: Float,
        count: Int,
        UV: vec2,
        pos: vec3,
        COLOR: vec4
    )

    val actual =
      EnvReader.readUBO[FragEnv]

    val expected =
      EnvReader.UBODef(
        "FragEnv",
        List(
          EnvReader.UBOField(None, "float", "alpha"),
          EnvReader.UBOField(None, "int", "count"),
          EnvReader.UBOField(None, "vec2", "UV"),
          EnvReader.UBOField(None, "vec3", "pos"),
          EnvReader.UBOField(None, "vec4", "COLOR")
        )
      )

    assertEquals(actual, expected)
  }

  test("Create a UBO definition with precision annotated fields)") {

    case class FragEnv(
        a: highp[Float],
        b: mediump[Float],
        c: lowp[Float],
        d: Float
    )

    val actual =
      EnvReader.readUBO[FragEnv]

    val expected =
      EnvReader.UBODef(
        "FragEnv",
        List(
          EnvReader.UBOField(Option("highp"), "float", "a"),
          EnvReader.UBOField(Option("mediump"), "float", "b"),
          EnvReader.UBOField(Option("lowp"), "float", "c"),
          EnvReader.UBOField(None, "float", "d")
        )
      )

    assertEquals(actual, expected)
  }

  test("UBOField renders correctly") {
    val actual =
      EnvReader.UBOField(Option("highp"), "float", "TIME").render

    val expected =
      "highp float TIME;"

    assertEquals(actual, expected)
  }

  test("UBODef renders correctly") {
    val actual =
      EnvReader.UBODef(
        "MyCustomData",
        List(
          EnvReader.UBOField(Option("highp"), "float", "TIME"),
          EnvReader.UBOField(None, "vec2", "VIEWPORT_SIZE")
        )
      ).render

    val expected =
      s"""
      |layout (std140) uniform MyCustomData {
      |  highp float TIME;
      |  vec2 VIEWPORT_SIZE;
      |};
      |""".stripMargin.trim

    assertEquals(actual, expected)
  }

}
