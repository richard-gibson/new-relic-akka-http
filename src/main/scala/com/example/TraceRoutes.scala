package com.example

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.ActorSystem
import java.util.concurrent.Executors

class TraceRoutes()(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  implicit val ec: ExecutionContext = system.executionContext
  val fixedEc                       = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  val traceRoutes: Route = pathPrefix("dsl") {
    val dslService = new DslService()
    concat(
      path("async-trace") {
        get {
          onSuccess(dslService.asyncTraceOp)(complete(_))
        }
      },
      path("async-trace-nested") {
        get {
          onSuccess(dslService.asyncTraceNested)(complete(_))
        }
      },
      path("async-trace-nested-for") {
        get {
          onSuccess(dslService.asyncTraceNestedFor)(complete(_))
        }
      },
      path("async-trace-nested-chain") {
        get {
          onSuccess(dslService.asyncTraceChain)(complete(_))
        }
      },
      path("slow-trace") {
        get {
          onSuccess(dslService.slowTrace)(complete(_))
        }
      },
      path("failed-slow-trace") {
        get {
          onSuccess(dslService.failedSlowTrace)(complete(_))
        }
      }
    )
  }

  class DslService {
    import com.newrelic.scala.api.TraceOps._
    def syncTrace    = "sync-trace"
    def asyncTraceOp = Future(trace("asyncTrace")("async-trace"))(fixedEc)

    def asyncTraceNestedFor: Future[String] = for {
      topMsg    <- Future(trace("topMsgFor")("async-trace-nested for and then "))
      nestedMsg <- asyncTraceOp
    } yield topMsg + nestedMsg

    def asyncTraceNested: Future[String] =
      Future(trace("topMsgMFM")("async-trace-nested and then "))
        .flatMap(
          asyncTraceFun("nestedMsgMFM")(topMsg =>
            asyncTraceOp
              .map(nestedMsg => topMsg + nestedMsg)
          )
        )

    def asyncTraceChain: Future[String] = {
      Future(trace("topMsg")("async-trace-chain and then "))(fixedEc)
        .map(traceFun("map1")(_ + ", map1"))
        .flatMap(asyncTraceFun("flatmap")(res => Future(res + ", flatmap")))
        .map(traceFun("map2")(_ + ", map2"))
        .filter(traceFun("filter")(_.startsWith("async")))
    }

    def slowTrace: Future[String] =
      Future(trace("start")("start"))
        .flatMap(
          asyncTraceFun("slowTrace")(res => scheduledFuture(1500, res + "slow-trace"))
        )

    def failedSlowTrace: Future[String] =
      Future(trace("start")("start"))
        .flatMap(
          traceFun("failedSlowTrace")(res => scheduledFuture(1500, res + "slow-trace"))
        )

    private def scheduledFuture[T](delay: Long, body: => T)(implicit ec: ExecutionContext): Future[T] = Future {
      Thread.sleep(delay)
      body
    }
  }

}
