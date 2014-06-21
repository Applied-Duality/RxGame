/*
 * Copyright 2014 Applied Duality, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aBugsLife

import rx.lang.scala._
import rx.functions.Action0
import javafx.application.Platform
import scala.concurrent.duration._
import rx.lang.scala.JavaConversions.javaSchedulerToScalaScheduler

object PlatformScheduler {

  def apply(): Scheduler = javaSchedulerToScalaScheduler(new rx.Scheduler {

    implicit def actionToRunnable(action: => Unit): Runnable = new Runnable { override def run(): Unit = action }

    override def createWorker(): rx.Scheduler.Worker = new rx.Scheduler.Worker() {

      val subscription = Subscription{}
      override def unsubscribe() = subscription.unsubscribe()
      override def isUnsubscribed = subscription.isUnsubscribed

      override def schedule(action: Action0): rx.Subscription = {
        Platform.runLater{ if(!isUnsubscribed) action.call() }
        this
      }

      override def schedule(action: Action0, delayTime: Long, unit: TimeUnit): rx.Subscription = {
        Platform.runLater {
            Thread.sleep(Duration(delayTime, unit).toMillis)
            if(!isUnsubscribed) action.call()
        }
        this
      }
    }
  })
}
