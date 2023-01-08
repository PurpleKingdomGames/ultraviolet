package ultraviolet.macros

import ultraviolet.datatypes.ShaderAST
import ultraviolet.datatypes.ShaderAST.Annotated
import ultraviolet.datatypes.ShaderAST.Assign
import ultraviolet.datatypes.ShaderAST.Block
import ultraviolet.datatypes.ShaderAST.CallFunction
import ultraviolet.datatypes.ShaderAST.Cast
import ultraviolet.datatypes.ShaderAST.DataTypes.*
import ultraviolet.datatypes.ShaderAST.Empty
import ultraviolet.datatypes.ShaderAST.Field
import ultraviolet.datatypes.ShaderAST.For
import ultraviolet.datatypes.ShaderAST.FunctionRef
import ultraviolet.datatypes.ShaderAST.If
import ultraviolet.datatypes.ShaderAST.Infix
import ultraviolet.datatypes.ShaderAST.Neg
import ultraviolet.datatypes.ShaderAST.New
import ultraviolet.datatypes.ShaderAST.RawLiteral
import ultraviolet.datatypes.ShaderAST.ShaderBlock
import ultraviolet.datatypes.ShaderAST.Struct
import ultraviolet.datatypes.ShaderAST.Switch
import ultraviolet.datatypes.ShaderAST.UBO
import ultraviolet.datatypes.ShaderAST.Val
import ultraviolet.datatypes.ShaderAST.While
import ultraviolet.datatypes.*

object ShaderProgramValidation:

  val ErrorMsgNestedFunction: String = "It is not permitted to nest named functions, you can declare nexted anonymous functions, however."

  private def forwardRefMsg(name: String): String =
    s"-${name} is an illegal forward reference."

  @SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
  def validate(knownRefs: List[String]): ShaderAST => ShaderAST = {
    case ast @ Empty() =>
      ast

    case ast @ Block(statements) =>
      // Will need to accumulate references and validate?
      ast

    case ast @ Neg(value) =>
      validate(knownRefs)(ast)

    case ast @ UBO(uboDef) =>
      ast

    case ast @ Struct(name, members) =>
      ast

    case ast @ New(name, args) =>
      ast

    case ShaderBlock(inType, outType, envVarName, statements) =>
      // Will need to accumulate references and validate?
      ShaderBlock(inType, outType, envVarName, statements)

    case ast @ ShaderAST.Function(id, args, body, returnType) =>
      // Should not contain function
      body.find {
        case ShaderAST.Function(_, _, _, _) => true
        case _                              => false
      } match
        case Some(_) =>
          throw ShaderError.Validation(ErrorMsgNestedFunction)

        case None =>
          ()

      // Should not contain forward references
      // Will need to accumulate references and validate?
      // val argNames = args.map(_._2)

      ast

    case ast @ CallFunction(id, args, returnType) =>
      if knownRefs.contains(id) then ast
      else throw ShaderError.Validation(forwardRefMsg(id))

    case ast @ FunctionRef(id, arg, returnType) =>
      if knownRefs.contains(id) then ast
      else throw ShaderError.Validation(forwardRefMsg(id))

    case Cast(value, as) =>
      Cast(validate(knownRefs)(value), as)

    case Infix(op, left, right, returnType) =>
      Infix(op, validate(knownRefs)(left), validate(knownRefs)(right), returnType)

    case Assign(left, right) =>
      Assign(validate(knownRefs)(left), validate(knownRefs)(right))

    case If(condition, thenTerm, elseTerm) =>
      If(validate(knownRefs)(condition), validate(knownRefs)(thenTerm), elseTerm.map(validate(knownRefs)))

    case While(condition, body) =>
      While(validate(knownRefs)(condition), validate(knownRefs)(body))

    case For(initial, condition, next, body) =>
      For(
        validate(knownRefs)(initial),
        validate(knownRefs)(condition),
        validate(knownRefs)(next),
        validate(knownRefs)(body)
      )

    case Switch(on, cases) =>
      Switch(validate(knownRefs)(on), cases.map(c => c._1 -> validate(knownRefs)(c._2)))

    case Val(id, value, typeOf) =>
      // What level are we at? Top level cannot call a function for example...
      Val(id, validate(knownRefs)(value), typeOf)

    case Annotated(name, param, value) =>
      Annotated(name, param, validate(knownRefs)(value))

    case ast @ RawLiteral(value) =>
      ast

    case Field(term, field) =>
      Field(validate(knownRefs)(term), field)

    case ast @ ident(id) =>
      if knownRefs.contains(id) then ast
      else throw ShaderError.Validation(forwardRefMsg(id))

    case ast @ index(id, at) =>
      if knownRefs.contains(id) then ast
      else throw ShaderError.Validation(forwardRefMsg(id))

    case ast @ bool(_) =>
      ast

    case ast @ float(_) =>
      ast

    case ast @ int(_) =>
      ast

    case ast @ vec2(_) =>
      ast

    case ast @ vec3(_) =>
      ast

    case ast @ vec4(_) =>
      ast

    case ast @ bvec2(_) =>
      ast

    case ast @ bvec3(_) =>
      ast

    case ast @ bvec4(_) =>
      ast

    case ast @ ivec2(_) =>
      ast

    case ast @ ivec3(_) =>
      ast

    case ast @ ivec4(_) =>
      ast

    case ast @ mat2(_) =>
      ast

    case ast @ mat3(_) =>
      ast

    case ast @ mat4(_) =>
      ast

    case ast @ array(_, _, _) =>
      ast

    case swizzle(genType, swzl, returnType) =>
      swizzle(validate(knownRefs)(genType), swzl, returnType)

  }
