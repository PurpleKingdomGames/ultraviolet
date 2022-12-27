import sbt._
import scala.sys.process._

object ShaderDSLGen {

  val tripleQuotes: String = "\"\"\""

  def template(
      name: String,
      contents: String
  ): String =
    s"""package ultraviolet.datatypes
    |
    |import ultraviolet.syntax.*
    |
    |trait ${name} extends ShaderDSLTypes:
    |$contents
    |""".stripMargin

  def makeShaderDSL(sourceManagedDir: File): Seq[File] = {
    println("Generating Indigo Shader DSL...")

    val name = "ShaderDSLTypeExtensions"

    val file: File =
      sourceManagedDir / "ultraviolet" / "datatypes" / (name + ".scala")

    if (!file.exists()) {
      val newContents: String =
        template(name, makeContents())

      IO.write(file, newContents)

      println("Written: " + file.getCanonicalPath)
    } else {
      println("Found, skipping: " + file.getCanonicalPath)
    }

    Seq(file)
  }

  def makeContents(): String = {

    val vec2     = List("x", "y")
    val vec2RGBA = List("r", "g")
    val vec3     = List("x", "y", "z")
    val vec3RGBA = List("r", "g", "b")
    val vec4     = List("x", "y", "z", "w")
    val vec4RGBA = List("r", "g", "b", "a")

    def swizzles1(input: List[String]): List[List[String]] =
      input.map(c => List(c))

    def swizzles2(input: List[String]): List[List[String]] = {
      val res =
        for {
          a <- input
          r <- List.fill(input.length)(a).zip(input).map(p => List(p._1, p._2))
        } yield r

      res
    }

    def swizzles3(input: List[String]): List[List[String]] = {
      val res =
        for {
          a <- input
          b <- List.fill(input.length)(a).zip(input).map(p => List(p._1, p._2))
          r <- List.fill(input.length)(b).zip(input).flatMap(p => List(p._1 :+ p._2))
        } yield r

      res
    }

    def swizzles4(input: List[String]): List[List[String]] = {
      val res =
        for {
          a <- input
          b <- List.fill(input.length)(a).zip(input).map(p => List(p._1, p._2))
          c <- List.fill(input.length)(b).zip(input).flatMap(p => List(p._1 :+ p._2))
          r <- List.fill(input.length)(c).zip(input).flatMap(p => List(p._1 :+ p._2))
        } yield r

      res
    }

    List(
      extensionContent(
        "",
        "Float",
        "vec2",
        swizzles1(vec2) ++
          swizzles2(vec2) ++
          swizzles3(vec2) ++
          swizzles4(vec2) ++
          swizzles1(vec2RGBA) ++
          swizzles2(vec2RGBA) ++
          swizzles3(vec2RGBA) ++
          swizzles4(vec2RGBA),
        Map(
          "x" -> "x",
          "y" -> "y",
          "z" -> "z",
          "w" -> "w",
          "r" -> "r",
          "g" -> "g",
          "b" -> "b",
          "a" -> "a"
        )
      ),
      extensionContent(
        "",
        "Float",
        "vec3",
        swizzles1(vec3) ++
          swizzles2(vec3) ++
          swizzles3(vec3) ++
          swizzles4(vec3) ++
          swizzles1(vec3RGBA) ++
          swizzles2(vec3RGBA) ++
          swizzles3(vec3RGBA) ++
          swizzles4(vec3RGBA),
        Map(
          "x" -> "x",
          "y" -> "y",
          "z" -> "z",
          "w" -> "w",
          "r" -> "r",
          "g" -> "g",
          "b" -> "b",
          "a" -> "a"
        )
      ),
      extensionContent(
        "",
        "Float",
        "vec4",
        swizzles1(vec4) ++
          swizzles2(vec4) ++
          swizzles3(vec4) ++
          swizzles4(vec4) ++
          swizzles1(vec4RGBA) ++
          swizzles2(vec4RGBA) ++
          swizzles3(vec4RGBA) ++
          swizzles4(vec4RGBA),
        Map(
          "x" -> "x",
          "y" -> "y",
          "z" -> "z",
          "w" -> "w",
          "r" -> "r",
          "g" -> "g",
          "b" -> "b",
          "a" -> "a"
        )
      ),
      extensionContent(
        "b",
        "Boolean",
        "bvec2",
        swizzles1(vec2) ++
          swizzles2(vec2) ++
          swizzles3(vec2) ++
          swizzles4(vec2) ++
          swizzles1(vec2RGBA) ++
          swizzles2(vec2RGBA) ++
          swizzles3(vec2RGBA) ++
          swizzles4(vec2RGBA),
        Map(
          "x" -> "x",
          "y" -> "y",
          "z" -> "z",
          "w" -> "w",
          "r" -> "r",
          "g" -> "g",
          "b" -> "b",
          "a" -> "a"
        )
      ),
      extensionContent(
        "b",
        "Boolean",
        "bvec3",
        swizzles1(vec3) ++
          swizzles2(vec3) ++
          swizzles3(vec3) ++
          swizzles4(vec3) ++
          swizzles1(vec3RGBA) ++
          swizzles2(vec3RGBA) ++
          swizzles3(vec3RGBA) ++
          swizzles4(vec3RGBA),
        Map(
          "x" -> "x",
          "y" -> "y",
          "z" -> "z",
          "w" -> "w",
          "r" -> "r",
          "g" -> "g",
          "b" -> "b",
          "a" -> "a"
        )
      ),
      extensionContent(
        "b",
        "Boolean",
        "bvec4",
        swizzles1(vec4) ++
          swizzles2(vec4) ++
          swizzles3(vec4) ++
          swizzles4(vec4) ++
          swizzles1(vec4RGBA) ++
          swizzles2(vec4RGBA) ++
          swizzles3(vec4RGBA) ++
          swizzles4(vec4RGBA),
        Map(
          "x" -> "x",
          "y" -> "y",
          "z" -> "z",
          "w" -> "w",
          "r" -> "r",
          "g" -> "g",
          "b" -> "b",
          "a" -> "a"
        )
      ),
      extensionContent(
        "i",
        "Int",
        "ivec2",
        swizzles1(vec2) ++
          swizzles2(vec2) ++
          swizzles3(vec2) ++
          swizzles4(vec2) ++
          swizzles1(vec2RGBA) ++
          swizzles2(vec2RGBA) ++
          swizzles3(vec2RGBA) ++
          swizzles4(vec2RGBA),
        Map(
          "x" -> "x",
          "y" -> "y",
          "z" -> "z",
          "w" -> "w",
          "r" -> "r",
          "g" -> "g",
          "b" -> "b",
          "a" -> "a"
        )
      ),
      extensionContent(
        "i",
        "Int",
        "ivec3",
        swizzles1(vec3) ++
          swizzles2(vec3) ++
          swizzles3(vec3) ++
          swizzles4(vec3) ++
          swizzles1(vec3RGBA) ++
          swizzles2(vec3RGBA) ++
          swizzles3(vec3RGBA) ++
          swizzles4(vec3RGBA),
        Map(
          "x" -> "x",
          "y" -> "y",
          "z" -> "z",
          "w" -> "w",
          "r" -> "r",
          "g" -> "g",
          "b" -> "b",
          "a" -> "a"
        )
      ),
      extensionContent(
        "i",
        "Int",
        "ivec4",
        swizzles1(vec4) ++
          swizzles2(vec4) ++
          swizzles3(vec4) ++
          swizzles4(vec4) ++
          swizzles1(vec4RGBA) ++
          swizzles2(vec4RGBA) ++
          swizzles3(vec4RGBA) ++
          swizzles4(vec4RGBA),
        Map(
          "x" -> "x",
          "y" -> "y",
          "z" -> "z",
          "w" -> "w",
          "r" -> "r",
          "g" -> "g",
          "b" -> "b",
          "a" -> "a"
        )
      )
    ).mkString("\n")
  }

  def extensionContent(
      prefix: String,
      baseType: String,
      typeName: String,
      swizzles: List[List[String]],
      replace: Map[String, String]
  ): String =
    s"""  extension (inline v: $typeName)
    |${swizzlesToMethods(prefix, baseType, swizzles, replace)}
    |""".stripMargin

  def swizzlesToMethods(
      prefix: String,
      baseType: String,
      swizzles: List[List[String]],
      replace: Map[String, String]
  ): String =
    swizzles
      .map { s =>
        s match {
          case x :: Nil =>
            s"    inline def $x: $baseType = v.${replace(x)}"

          case x :: y :: Nil =>
            s"    inline def $x$y: ${prefix}vec2 = ${prefix}vec2(v.${replace(x)}, v.${replace(y)})"

          case x :: y :: z :: Nil =>
            s"    inline def $x$y$z: ${prefix}vec3 = ${prefix}vec3(v.${replace(x)}, v.${replace(y)}, v.${replace(z)})"

          case x :: y :: z :: w :: Nil =>
            s"    inline def $x$y$z$w: ${prefix}vec4 = ${prefix}vec4(v.${replace(x)}, v.${replace(y)}, v.${replace(z)}, v.${replace(w)})"

          case _ =>
            ""
        }
      }
      .filterNot(_.isEmpty)
      .mkString("\n")

}
