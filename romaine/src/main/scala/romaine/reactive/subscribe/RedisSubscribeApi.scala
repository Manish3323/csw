/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.reactive.subscribe

import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.RedisResult

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{blocking, ExecutionContext, Future}

class RedisSubscribeApi[K, V](redisReactiveCommands: RedisPubSubReactiveCommands[K, V])(implicit ec: ExecutionContext)
    extends RedisReactiveApi[K, V] {
  def subscribe(keys: List[K]): Future[Done] = redisReactiveCommands.subscribe(keys: _*).toFuture.toScala.map(_ => Done)
  def observe(overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], NotUsed] =
    Source
      .fromPublisher(redisReactiveCommands.observeChannels(overflowStrategy))
      .map(x => RedisResult(x.getChannel, x.getMessage))
  def unsubscribe(keys: List[K]): Future[Done] = redisReactiveCommands.unsubscribe(keys: _*).toFuture.toScala.map(_ => Done)
  def close(): Future[Unit] =
    Future {
      blocking {
        redisReactiveCommands.getStatefulConnection.close()
      }
    }
}
