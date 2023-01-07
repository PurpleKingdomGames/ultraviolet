[![MIT License](https://img.shields.io/github/license/PurpleKingdomGames/ultraviolet?color=indigo)](https://github.com/tterb/atomic-design-ui/blob/master/LICENSEs)
[![Latest Tagged Release](https://img.shields.io/badge/dynamic/json?color=purple&label=latest%20release&query=%24%5B0%5D.name&url=https%3A%2F%2Fapi.github.com%2Frepos%2FPurpleKingdomGames%2Fultraviolet%2Ftags)](https://github.com/PurpleKingdomGames/ultraviolet/releases)
[![Discord Chat](https://img.shields.io/discord/716435281208672356?color=blue&label=discord)](https://discord.gg/b5CD47g)
[![CI](https://github.com/PurpleKingdomGames/ultraviolet/actions/workflows/ci.yml/badge.svg)](https://github.com/PurpleKingdomGames/ultraviolet/actions/workflows/ci.yml)

# Ultraviolet

Ultraviolet is a Scala 3 to GLSL transpiler library built on top of Scala 3 inline macros.

## Uses & Examples

Examples can be found in the examples directory of this repo. You can use Ultraviolet to generate GLSL shader code for Indigo, and also for ShaderToy.

## Status: "It works on my machine"

Ultraviolet is in early stage development. It appears to be working well but there will be many, many corner cases that haven't been found yet. Please report bugs and issues!

## Motivation

This project is motivated from two needs:

1. The most pressing need is that GLSL tooling is patchy, and I'd like to have a much better shadering writing experience both for myself and any other Scala devs whether they're writing shaders for [Indigo](https://indigoengine.io/), [ShaderToy](https://www.shadertoy.com/), or some other Scala frontend web framework.
2. Indigo is currently locked into WebGL 2.0, and to move to other platforms or rendering technologies means having some way to abstract away from that. 

## Current Goals

Right now, the goal is an almost a like for like experience of writing GLSL for WebGL in Scala 3, in all it's very specific procedural glory. It will include a few quality of life improvements such as lambdas and function composition, but nothing fancy for now. You will also be able to write unit tests after a fashion.

It is _not_ a goal to be able to write arbirary Scala and have it turned into GLSL. In other words this isn't a 'full' transpiler (like Scala.js), it's a useful cross-over subset of Scala and GLSL. As many GLSL language features as can sensibly be represented, and as much Scala as GLSL can be coerced into expressing.

Ultimately I'd like to be able to write Shaders in FP friendly Scala that can target more than just GLSL 300 - but that is a lot of work and not necessary for a first useful shippable version of Ultraviolet.


# Language feature comparison

## Gotcha's, foot guns, and weird stuff

### UBO's must be in a 'top-level' Shader

This example works perfectly:

```scala
case class UBO1(UV: vec2)

inline def base: Float => Shader[UBO1, vec4] =
  (z: Float) =>
    Shader[UBO1, vec4] { env =>
      vec4(env.UV, z, 1.0f)
    }

inline def toVec2(v4: vec4): Shader[UBO1, vec2] =
  Shader[UBO1, vec2] { env =>
    v4.xy
  }

inline def calc: Shader[UBO1, vec2] =
  for
    a <- base(20.0f)
    b <- toVec2(a)
  yield b + 1.0f

inline def shader: Shader[UBO1, vec2] =
  Shader[UBO1, vec2] { env =>
    ubo[UBO1]
    calc.run(env)
  }
```

But bad things will happen if you try to do this, because the UBO would end up being defined inside a function, and UBO's must be declared at the top level:

```scala
case class UBO1(UV: vec2)

inline def base: Float => Shader[UBO1, vec4] =
  (z: Float) =>
    Shader[UBO1, vec4] { env =>
      ubo[UBO1]
      vec4(env.UV, z, 1.0f)
    }

inline def toVec2(v4: vec4): Shader[UBO1, vec2] =
  Shader[UBO1, vec2] { env =>
    v4.xy
  }

inline def shader: Shader[UBO1, vec2] =
  for
    a <- base(20.0f)
    b <- toVec2(a)
  yield b + 1.0f
```

### Just write a glsl as a String?

This is completely valid but only if it's the only contents of the block:

```scala
Shader {
  "int foo = 10;"
}
```

This is fine anywhere:


```scala
Shader {
  RawGLSL("int foo = 10;")
  raw("int bar = 11;")
}
```

But this will not work:

```scala
Shader {
  raw("int foo = 10;").trim
}
```

Because we can't do string things in GLSL, and trim is a stringy thingy.

### Pattern matching weirdness

A pattern match is converted to a switch statement, and in GLSL you can only switch on an Int. So far that's limiting, but ok.

What is totally unintuitive is that on some graphics hardware, in some implmentations, switch statements will process all branches irrespective of whether they're going to be used or not. Weird but... ok? No.

The problem with that, is that if you declare the same variable name in two branches, the GLSL compiler will fail and tell you that you've redeclared it. Bonkers, but the takeaway is: Don't repeat variable names in pattern match branches...

### Unofficial reserved words

When writing shaders in Scala, Scala reserved words will be checked and errors shown by the compiler.

You shouldn't have too much trouble with GLSL reserved words because many of them have the same status in Scala, but it's worth noting the GLSL is like C and that there will be words to avoid.

One thing to avoid: Do not call a function something like `def xy(v: vec4): ???` because this will likely interfere with the Swizzle mechanisms. Not at the point of definition but at the point of use.

## Comparison table

Only including the differences or note worthy features. If they're the same they are omitted.

| Feature                           | Scala | GLSL | Ultraviolet | Notes                                                                                            |
| --------------------------------- | ----- | ---- | ----------- | ------------------------------------------------------------------------------------------------ |
| Recursion                         | ✅     | ❌    | ❌           |
| A stack!                          | ✅     | ❌    | ❌           |
| `String` and `Char`               | ✅     | ❌    | ❌           |
| `uint` / `uvec`                   | ❌     | ✅    | ❌           |
| `Double` / `dvec`                 | ✅     | ❌    | ❌           |
| `struct`                          | ❌     | ✅    | 💡           | You can define structs by declaring classes.                                                     |
| for loops                         | ❌     | ✅    | 💡           | In Scala, use the `cfor` method provided.                                                        |
| Imports                           | ✅     | ❌    | ✅           | Imported methods and values must be inlined.                                                     |
| Switch statements                 | ❌     | ✅    | 💡           | Scala does not have switch statements, but they can be expressed using pattern matching instead. |
| If statements can return values   | ✅     | ❌    | ✅           |
| Pattern matches can return values | ✅     | ❌    | ✅           |
| `#define`                         | ❌     | ✅    | ✅           | Use the `@define` annotation. (see note below)                                                   |
| `const`                           | ❌     | ✅    | ✅           | `@const`                                                                                         |
| `uniform`                         | ❌     | ✅    | ✅           | `@uniform`                                                                                       |
| `varying`, `in`, `out`            | ❌     | ✅    | ✅           | `@in`, `@out`                                                                                    |
| `%` (modulus op)                  | ✅     | ❌    | ✅           |
| Lambda/Anonymous functions        | ✅     | ❌    | ✅           |
| `compose`                         | ✅     | ❌    | ✅           |
| `andThen`                         | ✅     | ❌    | ✅           |


Other comments:

- Although Ultraviolet is based on GLSL 300, I've kept `texture2D` and `textureCube` from WebGL 1.0 for clarity, and these are rewritten to `texture` for WebGL 2.0. 
- Preprocessor directives largely don't exist, but `#define` supported for special cases where you need to define a global value based on a non-constant value.


## Things to know about inlining

Ultraviolet allows you to share / reuse code, as long as it is inlined following Scala 3's standard inlining rules. However there are things to know about how this will affect your GLSL!

Here, 'external' means 'not inside the body of your shader'.

- You cannot inline external `val`s.
- You can inline external `def`s into your code, but:
  - A def that is essentially a call by reference val such as `inline def x = 1.0f` will have it's value inlined.
  - A def that is a function, laid out like a method e.g. `inline def foo(c: Int): Int = c + 1` will be embedded as a function called `foo` as expected, but the argument `c` will be ignored, and the value passed will be inlined. Bit weird!
  - A def that is a lambda, however, will be embedded with a new name and will work exactly as you'd expect, recommend you do this! `inline def foo: Int => Int = c => c + 1`
