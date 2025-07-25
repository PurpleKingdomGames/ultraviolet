package ultraviolet.datatypes

import scala.annotation.nowarn
import scala.quoted.*

final case class ProceduralShader(
    defs: List[ShaderAST],
    ubos: List[ShaderAST.UBO],
    annotationed: List[ShaderAST],
    main: ShaderAST
)

object ProceduralShader:
  given ToExpr[ProceduralShader] with {
    def apply(x: ProceduralShader)(using Quotes): Expr[ProceduralShader] =
      '{ ProceduralShader(${ Expr(x.defs) }, ${ Expr(x.ubos) }, ${ Expr(x.annotationed) }, ${ Expr(x.main) }) }
  }

  // Unused? See below.
  private val pIdentity: PartialFunction[ShaderAST, ShaderAST] = s => s

  extension (p: ProceduralShader)
    @SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
    inline def render[T](headers: List[ShaderHeader])(using printer: ShaderPrinter[T]): ShaderResult.Output =
      import ShaderAST.*

      val inType    = p.main.inType
      val outType   = p.main.outType
      val functions = p.defs
      val body      = p.main

      printer.isValid(inType, outType, functions, body) match
        case ShaderValid.Invalid(reasons) =>
          throw ShaderError.Validation("Shader failed to validate because: " + reasons.mkString("[", ", ", "]"))

        case ShaderValid.Valid =>
          val renderedUBOs = p.ubos.map(u => ShaderPrinter.print(u).mkString("\n"))
          val renderedAnnotations = p.annotationed
            .map(u => ShaderPrinter.print(u).mkString("\n"))
            .map(s => if s.startsWith("#") then s else s + ";")
          val renderedDefs = functions.map(d => ShaderPrinter.print(d).mkString("\n"))
          val renderedBody = ShaderPrinter.print(body)

          // Unused? Leaving in due to a boring binary compatibility problem.
          // Looks pretty suspicious, but is doing no harm.
          @nowarn("msg=unused")
          val transformedBody: ShaderAST =
            body.traverse(printer.transformer.orElse(pIdentity))

          val code =
            (headers.map(_.value) ++ renderedUBOs ++ renderedAnnotations ++ renderedDefs ++ renderedBody)
              .mkString("\n")
              .trim

          val extractedUniforms: List[ShaderField] =
            p.annotationed
              .filter {
                case ShaderAST.Annotated(ShaderAST.DataTypes.ident("uniform"), _, ShaderAST.Val(_, _, _)) => true
                case _                                                                                    => false
              }
              .flatMap {
                case ShaderAST.Annotated(name, param, ShaderAST.Val(id, value, typeOf)) =>
                  List(
                    ShaderField(
                      id,
                      ShaderPrinter
                        .print(typeOf)
                        .headOption
                        .getOrElse(throw ShaderError.Metadata("Uniform declaration missing return type."))
                    )
                  )

                case _ => Nil
              }

          val extractedVaryings: List[ShaderField] =
            p.annotationed
              .filter {
                case ShaderAST.Annotated(ShaderAST.DataTypes.ident("varying"), _, ShaderAST.Val(_, _, _)) => true
                case ShaderAST.Annotated(ShaderAST.DataTypes.ident("in"), _, ShaderAST.Val(_, _, _))      => true
                case ShaderAST.Annotated(ShaderAST.DataTypes.ident("out"), _, ShaderAST.Val(_, _, _))     => true
                case _                                                                                    => false
              }
              .flatMap {
                case ShaderAST.Annotated(name, param, ShaderAST.Val(id, value, typeOf)) =>
                  List(
                    ShaderField(
                      id,
                      ShaderPrinter
                        .print(typeOf)
                        .headOption
                        .getOrElse(throw ShaderError.Metadata("Varying declaration missing return type."))
                    )
                  )

                case _ =>
                  Nil
              }

          ShaderResult.Output(
            code,
            ShaderMetadata(
              extractedUniforms,
              p.ubos.map(_.uboDef),
              extractedVaryings
            )
          )

    def exists(q: ShaderAST): Boolean =
      p.main.exists(q) || p.defs.exists(_.exists(q))

    def find(q: ShaderAST => Boolean): Option[ShaderAST] =
      p.main.find(q).orElse(p.defs.find(_.find(q).isDefined))

    def findAll(q: ShaderAST => Boolean): List[ShaderAST] =
      p.main.findAll(q) ++ p.defs.flatMap(_.findAll(q))
