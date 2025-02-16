package sttp.client3.armeria

import _root_.zio._
import sttp.capabilities.Effect
import sttp.capabilities.zio.ZioStreams
import sttp.client3._
import sttp.client3.impl.zio.{ExtendEnv, SttpClientStubbingBase}

package object zio {

  // Forked from async-http-client-backend/zio
  // - Removed WebSocket support

  /** ZIO-environment service definition, which is an SttpBackend. */
  type SttpClient = SttpBackend[Task, ZioStreams]
  type SttpClientStubbing = SttpClientStubbing.SttpClientStubbing

  /** Sends the request. Only requests for which the method & URI are specified can be sent.
    *
    * @return
    *   An effect resulting in a`Response`, containing the body, deserialized as specified by the request (see
    *   `RequestT.response`), if the request was successful (1xx, 2xx, 3xx response codes), or if there was a
    *   protocol-level failure (4xx, 5xx response codes).
    *
    * A failed effect, if an exception occurred when connecting to the target host, writing the request or reading the
    * response.
    *
    * Known exceptions are converted to one of `SttpClientException`. Other exceptions are kept unchanged.
    */
  def send[T](
      request: Request[T, Effect[Task] with ZioStreams]
  ): ZIO[SttpClient, Throwable, Response[T]] =
    ZIO.environmentWithZIO(env => env.get[SttpClient].send(request))

  /** A variant of `send` which allows the effects that are part of the response handling specification (when using
    * websockets or resource-safe streaming) to use an `R` environment.
    */
  def sendR[T, R](
      request: Request[T, Effect[RIO[R, *]] with ZioStreams]
  ): ZIO[SttpClient with R, Throwable, Response[T]] =
    ZIO.environmentWithZIO(env => env.get[SttpClient].extendEnv[R].send(request))

  object SttpClientStubbing extends SttpClientStubbingBase[Any, ZioStreams] {
    override private[sttp] def serviceTag: Tag[SttpClientStubbing.SttpClientStubbing] = implicitly
    override private[sttp] def sttpBackendTag: Tag[SttpClient] = implicitly
  }

  object stubbing {
    import SttpClientStubbing.StubbingWhenRequest

    def whenRequestMatches(p: Request[_, _] => Boolean): StubbingWhenRequest =
      StubbingWhenRequest(p)

    val whenAnyRequest: StubbingWhenRequest =
      StubbingWhenRequest(_ => true)

    def whenRequestMatchesPartial(
        partial: PartialFunction[Request[_, _], Response[_]]
    ): URIO[SttpClientStubbing, Unit] =
      ZIO.environmentWithZIO(_.get.whenRequestMatchesPartial(partial))
  }
}
