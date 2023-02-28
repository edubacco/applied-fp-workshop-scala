package application

object Main extends App {

   import application.Version5._
   import cats.effect.unsafe.implicits.global

   val app = createApplication("planet.txt", "rover.txt")
//   val app = createApplication("planet_invalid_content.txt", "rover.txt")
//   val app = createApplication("planet_invalid_data.txt", "rover.txt")
   app.unsafeRunSync()
}
