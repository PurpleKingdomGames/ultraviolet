package com.example.sandbox.scenes

import com.example.sandbox.SandboxGameModel
import com.example.sandbox.SandboxStartupData
import com.example.sandbox.SandboxViewModel
import com.example.sandbox.shaders.ShaderPrograms
import indigo.*
import indigo.ShaderPrimitive.*
import indigo.scenes.*

object ShadersScene extends Scene[SandboxStartupData, SandboxGameModel, SandboxViewModel] {

  type SceneModel     = SandboxGameModel
  type SceneViewModel = SandboxViewModel

  def eventFilters: EventFilters =
    EventFilters.Restricted

  def modelLens: Lens[SandboxGameModel, SandboxGameModel] =
    Lens.keepOriginal

  def viewModelLens: Lens[SandboxViewModel, SandboxViewModel] =
    Lens.keepOriginal

  def name: SceneName =
    SceneName("custom shaders")

  def subSystems: Set[SubSystem[SandboxGameModel]] =
    Set()

  def updateModel(
      context: SceneContext[SandboxStartupData],
      model: SandboxGameModel
  ): GlobalEvent => Outcome[SandboxGameModel] =
    _ => Outcome(model)

  def updateViewModel(
      context: SceneContext[SandboxStartupData],
      model: SandboxGameModel,
      viewModel: SandboxViewModel
  ): GlobalEvent => Outcome[SandboxViewModel] =
    _ => Outcome(viewModel)

  def present(
      context: SceneContext[SandboxStartupData],
      model: SandboxGameModel,
      viewModel: SandboxViewModel
  ): Outcome[SceneUpdateFragment] =
    Outcome(
      SceneUpdateFragment(
        Layer(
          CustomShader(
            20,
            20,
            100,
            100,
            Depth.zero,
            ShaderData(CustomShader.shaderId)
          )
        ).withMagnification(1)
      )
    )

}

final case class CustomShader(x: Int, y: Int, width: Int, height: Int, depth: Depth, shader: ShaderData)
    extends EntityNode[CustomShader]:
  val flip: Flip                    = Flip.default
  val position: Point               = Point(x, y)
  val size: Size                    = Size(width, height)
  val ref: Point                    = Point.zero
  val rotation: Radians             = Radians.zero
  val scale: Vector2                = Vector2.one
  lazy val toShaderData: ShaderData = shader

  def withDepth(newDepth: Depth): CustomShader =
    this.copy(depth = newDepth)

  val eventHandlerEnabled: Boolean                                       = false
  def eventHandler: ((CustomShader, GlobalEvent)) => Option[GlobalEvent] = Function.const(None)

object CustomShader:

  val shaderId: ShaderId =
    ShaderId("custom shader")

  // println(ShaderPrograms.frag2)

  val shader: EntityShader.Source =
    EntityShader
      .Source(shaderId)
      .withFragmentProgram(ShaderPrograms.frag2)
