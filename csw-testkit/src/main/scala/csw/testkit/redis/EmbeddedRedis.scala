/*
 * Copyright (c) [year] Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.testkit.redis
import csw.network.utils.SocketUtils.getFreePort
import redis.embedded.{RedisSentinel, RedisServer}

private[testkit] trait EmbeddedRedis {

  def startSentinel(
      sentinelPort: Int = getFreePort,
      serverPort: Int = getFreePort,
      masterId: String,
      keyspaceEvent: Boolean
  ): (RedisSentinel, RedisServer) = {
    val keyspaceEventStr = if (keyspaceEvent) "notify-keyspace-events K$x" else "notify-keyspace-events \"\""
    val redisServer      = RedisServer.builder().port(serverPort).setting(keyspaceEventStr).build()

    val redisSentinel: RedisSentinel = RedisSentinel
      .builder()
      .port(sentinelPort)
      .masterName(masterId)
      .masterPort(serverPort)
      .quorumSize(1)
      .build()

    redisServer.start()
    redisSentinel.start()

    addJvmShutdownHook(stopSentinel(redisSentinel, redisServer))
    (redisSentinel, redisServer)
  }

  def stopSentinel(redisSentinel: RedisSentinel, redisServer: RedisServer): Unit = {
    redisServer.stop()
    redisSentinel.stop()
  }

  private def addJvmShutdownHook[T](hook: => T): Unit =
    Runtime.getRuntime.addShutdownHook(new Thread { override def run(): Unit = hook })
}
