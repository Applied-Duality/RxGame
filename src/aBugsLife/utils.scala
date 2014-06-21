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

import javafx.scene._
import rx.lang.scala._
import javafx.scene.input._
import javafx.event.EventHandler

package object utils {

  implicit def actionToEventHandler[E <: javafx.event.Event](f: E => Unit): EventHandler[E] =
    new EventHandler[E] { def handle(e: E): Unit  = f(e) }

  def keyPresses (scene: Scene): Observable[KeyEvent] = Observable[KeyEvent](observer => {
    val handler = (e:KeyEvent) => observer.onNext(e)
    scene.addEventHandler(KeyEvent.KEY_PRESSED, handler)
    observer.add {
      scene.removeEventHandler(KeyEvent.KEY_PRESSED, handler)
    }
  })

  def enterKeys(scene: Scene): Observable[KeyEvent] = keyPresses(scene).filter(_.getCode == KeyCode.ENTER)
  def spaceBars(scene: Scene): Observable[KeyEvent] = keyPresses(scene).filter(_.getCode == KeyCode.SPACE)

}
