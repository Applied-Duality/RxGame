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

import javafx.geometry._
import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import javafx.scene._
import javafx.scene.image._
import javafx.scene.layout.StackPane
import javafx.stage._
import javafx.application._
import rx.lang.scala.schedulers._
import rx.lang.scala._
import scala.language.postfixOps
import scala.concurrent.duration._

object PlayGame {
  def main(args: Array[String]) {
    javafx.application.Application.launch(classOf[Game])
  }
}

class Game extends Application {

  import utils._

  //val scheduler = NewThreadScheduler()
  val scheduler = TestScheduler()

  val resourceDir = s"file:///${System.getProperty("user.dir")}/resources"

  def start(stage: Stage) {

    val screenWidth = 800
    val screenHeight = 600

    val root = new StackPane()
    //  ^
    //  |
    // -dy
    //  |
    // (0,0) -----dx--->
    root.setAlignment(Pos.BOTTOM_LEFT)
    val scene = new Scene(root)

    // Time
    val clock = Observable.timer(initialDelay = 0 seconds, period = (1/60.0) second, scheduler)
      .observeOn(PlatformScheduler())

    val sky = new Canvas(screenWidth,screenHeight) {
      val context = getGraphicsContext2D
      context.setFill(Color.AZURE)
      context.fillRect(0, 0, screenWidth,screenHeight)
      root.getChildren.add(this)
    }

    val grass = new {
      val tile = new Image(s"$resourceDir/GrassBlock.png")
      def getHeight = tile.getHeight
      val nrTiles = math.ceil(screenWidth/tile.getWidth).asInstanceOf[Int]+1

      val tiles = (0 to nrTiles-1).map(i =>
        new ImageView {
          setImage(tile)
          setTranslateX(i*getImage.getWidth)
          root.getChildren.add(this)
        }).toList

      val v0 = 1

      clock.scan(v0)((dX,_)=>dX).subscribe(dX =>
        tiles.foreach(tile =>
          tile.setTranslateX(
            if(tile.getTranslateX <= -tile.getImage.getWidth) {
              screenWidth-dX
            } else {
              tile.getTranslateX-dX
            })))
    }

    val sun = new ImageView {
      def showHeart(b: Boolean): Unit = {
        setImage(
          if(b)
            new Image(s"$resourceDir/Heart.png")
          else
            new Image(s"$resourceDir/Star.png")
        )
      }

      showHeart(b = false)
      setTranslateY(-(screenHeight-200))
      root.getChildren.add(this)

      val v0 = 3

      clock.scan(v0)((dX,_)=>dX).subscribe(dX =>
        setTranslateX(
          if(getTranslateX <= -getImage.getWidth) {
            screenWidth-dX
          } else {
            getTranslateX-dX
          }))
    }

    val bug = new ImageView {

      val homeY = (-grass.getHeight/2)-5
      val gravity = 0.1

      setImage(new Image(s"$resourceDir/EnemyBug.png"))
      setTranslateY(homeY)
      setTranslateX(screenHeight/2)
      root.getChildren.add(this)

      val jumps: Subject[Double] = Subject()

      jumps.flatMap(v0 => clock.scan(v0)((dY,_)=>dY-gravity).takeUntil(jumps)).subscribe(dy =>
        setTranslateY(
          if(getTranslateY < homeY+dy) {
            getTranslateY-dy
          } else {
            homeY
          }))
    }

    val jumpSpeed = 8
    spaceBars(scene)
      .filter(_ => bug.getTranslateY >= bug.homeY)
      .doOnEach(_ => new javafx.scene.media.AudioClip (s"$resourceDir/smb3_jump.wav").play)
      .subscribe(_ => bug.jumps.onNext(jumpSpeed))

    enterKeys(scene).subscribe(_ => {
      scheduler match {
        case s: TestScheduler => s.advanceTimeBy((10 / 60.0) second)
      }
    })

    val heartBoundingBoxes: Observable[Bounds] = clock.map(_ => sun.localToScene(sun.getLayoutBounds))
    val bugBoundingBoxes: Observable[Bounds] = clock.map(_ => bug.localToScene(bug.getLayoutBounds))

    bugBoundingBoxes.combineLatest(heartBoundingBoxes, (bug: Bounds, heart: Bounds) => bug.intersects(heart))
      .buffer(2,1)
      .filter(hits => hits(0) != hits(1))
      .subscribe(hits =>
      if(!hits(0)) {
        sun.showHeart(true)
        new javafx.scene.media.AudioClip(s"$resourceDir/smb3_coin.wav").play
      } else {
        sun.showHeart(false)
      })

    stage.setOnShown((e: WindowEvent) =>
      new javafx.scene.media.AudioClip(s"$resourceDir/smb3_power-up.wav").play)

    stage.setTitle("A Bugs Life")
    stage.setScene(scene)
    stage.show()
  }
}

