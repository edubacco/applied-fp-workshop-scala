package marsroverkata.answers

object Version6 {

  import marsroverkata.Pacman._
  import Rotation._, Orientation._, Movement._, Command._, ParseError._
  import scala.Console.{ GREEN, RED, RESET }
  import scala.io.Source
  import cats.implicits._
  import cats.effect._

  enum Event {
    case LoadMissionSuccessful(planet: Planet, rover: Rover)
    case LoadMissionFailed(error: Throwable)
    case CommandsReceived(commands: List[Command])
  }

  enum Effect {
    case LoadMission(planetFile: String, roverFile: String)
    case AskCommands
    case ReportObstacleHit(rover: ObstacleDetected)
    case ReportCommandSequenceCompleted(rover: Rover)
    case Ko(error: String)
  }

  enum AppState {
    case Loading
    case Ready(planet: Planet, rover: Rover)
    case Failed
  }

  def init(planetFile: String, roverFile: String): (AppState, Effect) =
    (AppState.Loading, Effect.LoadMission(planetFile, roverFile))

  def update(model: AppState, event: Event): (AppState, Effect) =
    (model, event) match {

      case (AppState.Loading, Event.LoadMissionSuccessful(planet, rover)) =>
        (AppState.Ready(planet, rover), Effect.AskCommands)

      case (AppState.Ready(planet, rover), Event.CommandsReceived(commands)) =>
        executeAll(planet, rover, commands)
          .fold(
            hit => (AppState.Ready(planet, hit), Effect.ReportObstacleHit(hit)),
            complete => (AppState.Ready(planet, complete), Effect.ReportCommandSequenceCompleted(complete))
          )

      case (AppState.Loading, Event.LoadMissionFailed(error)) =>
        (AppState.Failed, Effect.Ko(error.getMessage))

      case _ =>
        (AppState.Failed, Effect.Ko(s"Cannot handle $event event in $model state."))
    }

  def infrastructure(effect: Effect): IO[Option[Event]] =
    effect match {
      case Effect.LoadMission(planetFile, roverFile) =>
        def toFailed(t: Throwable): Event = Event.LoadMissionFailed(t)
        def toSuccessful(planet: Planet, rover: Rover): Event = Event.LoadMissionSuccessful(planet, rover)

        val loadResult =
          for {
            planet <- loadPlanet(planetFile)
            rover <- loadRover(roverFile)
          } yield (planet, rover)

        loadResult.attempt
          .map(_.fold(toFailed, toSuccessful))
          .map(continue)

      case Effect.AskCommands =>
        loadCommands()
          .map(Event.CommandsReceived.apply)
          .map(continue)

      case Effect.ReportObstacleHit(rover) =>
        logInfo(renderObstacle(rover))
          .map(stop)

      case Effect.ReportCommandSequenceCompleted(rover) =>
        logInfo(renderComplete(rover))
          .map(stop)

      case Effect.Ko(error) =>
        logError(error)
          .map(stop)
    }

  def continue(ev: Event): Option[Event] = Some(ev)
  def stop(ignore: Unit): Option[Event] = None

  // INFRASTRUCTURE
  def toException(error: ParseError): Throwable =
    new RuntimeException(renderError(error))

  def eitherToIO[A](value: Either[ParseError, A]): IO[A] =
    IO.fromEither(value.leftMap(toException))

  def loadPlanet(file: String): IO[Planet] =
    loadTuple(file)
      .map(parsePlanet)
      .flatMap(eitherToIO)

  def loadRover(file: String): IO[Rover] =
    loadTuple(file)
      .map(parseRover)
      .flatMap(eitherToIO)

  def loadCommands(): IO[List[Command]] =
    ask("Waiting commands...")
      .map(parseCommands)

  // INFRASTRUCTURE - FILE SYSTEM
  def loadTuple(file: String): IO[(String, String)] =
    loadLines(file).map(lines =>
      lines match {
        case Array(first, second) => (first, second)
        case _                    => throw new RuntimeException(s"Invalid file content: $file")
      }
    )

  def loadLines(file: String): IO[Array[String]] =
    Resource
      .fromAutoCloseable(IO(Source.fromURL(getClass.getClassLoader.getResource(file))))
      .use(source => IO(source.getLines().toArray))

  // INFRASTRUCTURE - CONSOLE
  def puts(message: String): IO[Unit] = IO.println(message)

  def reads(): IO[String] = IO.readLine

  def ask(question: String): IO[String] =
    puts(question).flatMap(_ => reads())

  // INFRASTRUCTURE - LOGGING
  def logInfo(message: String): IO[Unit] =
    puts(green(s"[OK] $message"))

  def logError(message: String): IO[Unit] =
    puts(red(s"[ERROR] ${message}"))

  // PARSING
  def parseCommand(input: Char): Command =
    input.toString.toLowerCase match {
      case "f" => Move(Forward)
      case "b" => Move(Backward)
      case "r" => Turn(OnRight)
      case "l" => Turn(OnLeft)
      case _   => Unknown
    }

  def parseCommands(input: String): List[Command] =
    input.map(parseCommand).toList

  def parseRover(input: (String, String)): Either[ParseError, Rover] = {
    val (inputPosition, inputOrientation) = input
    for {
      position <- parsePosition(inputPosition)
      orientation <- parseOrientation(inputOrientation)
    } yield Rover(position, orientation)
  }

  def parseOrientation(input: String): Either[ParseError, Orientation] =
    input.trim.toLowerCase match {
      case "n" => Right(N)
      case "w" => Right(W)
      case "e" => Right(E)
      case "s" => Right(S)
      case _   => Left(InvalidRover(s"invalid orientation: $input"))
    }

  def parsePosition(input: String): Either[ParseError, Position] =
    parseInts(",", input)
      .map(Position.apply)
      .leftMap(_ => InvalidRover(s"invalid position: $input"))

  def parseSize(input: String): Either[ParseError, Size] =
    parseInts("x", input)
      .map(Size.apply)
      .leftMap(_ => InvalidPlanet(s"invalid size: $input"))

  def parseObstacle(input: String): Either[ParseError, Obstacle] =
    parsePosition(input)
      .map(Obstacle.apply)
      .leftMap(_ => InvalidPlanet(s"invalid obstacle: $input"))

  def parseObstacles(input: String): Either[ParseError, List[Obstacle]] =
    input.split(" ").toList.traverse(parseObstacle)

  def parsePlanet(input: (String, String)): Either[ParseError, Planet] = {
    val (inputSize, inputObstacles) = input
    for {
      size <- parseSize(inputSize)
      obstacles <- parseObstacles(inputObstacles)
    } yield Planet(size, obstacles)
  }

  def parseInts(separator: String, input: String): Either[Throwable, (Int, Int)] =
    Either.catchNonFatal {
      val parts = input.split(separator).toList
      (parts(0).trim.toInt, parts(1).trim.toInt)
    }

  // RENDERING
  def renderError(error: ParseError): String =
    error match {
      case InvalidPlanet(message) => s"Planet parsing: $message"
      case InvalidRover(message)  => s"Rover parsing: $message"
    }

  def green(message: String): String =
    s"$GREEN$message$RESET"

  def red(message: String): String =
    s"$RED$message$RESET"

  def renderComplete(rover: Rover): String =
    s"${rover.position.x}:${rover.position.y}:${rover.orientation}"

  def renderObstacle(hit: ObstacleDetected): String =
    s"O:${hit.position.x}:${hit.position.y}:${hit.orientation}"

  // DOMAIN
  def executeAll(planet: Planet, rover: Rover, commands: List[Command]): Either[ObstacleDetected, Rover] =
    commands.foldLeft(rover.asRight)((prev, cmd) => prev.flatMap(execute(planet, _, cmd)))

  def execute(planet: Planet, rover: Rover, command: Command): Either[ObstacleDetected, Rover] =
    command match {
      case Turn(rotation) => turn(rover, rotation).asRight
      case Move(movement) => move(planet, rover, movement)
      case Unknown        => rover.asRight
    }

  def turn(rover: Rover, turn: Rotation): Rover =
    turn match {
      case OnRight => turnRight(rover)
      case OnLeft  => turnLeft(rover)
    }

  def turnRight(rover: Rover): Rover =
    rover.copy(orientation = rover.orientation match {
      case N => E
      case E => S
      case S => W
      case W => N
    })

  def turnLeft(rover: Rover): Rover =
    rover.copy(orientation = rover.orientation match {
      case N => W
      case W => S
      case S => E
      case E => N
    })

  def move(planet: Planet, rover: Rover, move: Movement): Either[ObstacleDetected, Rover] =
    move match {
      case Forward  => moveForward(planet, rover)
      case Backward => moveBackward(planet, rover)
    }

  def moveForward(planet: Planet, rover: Rover): Either[ObstacleDetected, Rover] =
    next(planet, rover, delta(rover.orientation))
      .map(x => rover.copy(position = x))

  def moveBackward(planet: Planet, rover: Rover): Either[ObstacleDetected, Rover] =
    next(planet, rover, delta(opposite(rover.orientation)))
      .map(x => rover.copy(position = x))

  def opposite(orientation: Orientation): Orientation =
    orientation match {
      case N => S
      case S => N
      case E => W
      case W => E
    }

  def delta(orientation: Orientation): Delta =
    orientation match {
      case N => Delta(0, 1)
      case S => Delta(0, -1)
      case E => Delta(1, 0)
      case W => Delta(-1, 0)
    }

  def next(planet: Planet, rover: Rover, delta: Delta): Either[ObstacleDetected, Position] = {
    val position = rover.position
    val candidate = position.copy(
      x = wrap(position.x, planet.size.width, delta.x),
      y = wrap(position.y, planet.size.height, delta.y)
    )
    val hitObstacle = planet.obstacles.map(_.position).contains(candidate)
    Either.cond(!hitObstacle, candidate, rover)
  }

  // TYPES
  case class Delta(x: Int, y: Int)
  case class Position(x: Int, y: Int)
  case class Size(width: Int, height: Int)
  case class Obstacle(position: Position)
  case class Planet(size: Size, obstacles: List[Obstacle])
  case class Rover(position: Position, orientation: Orientation)

  opaque type ObstacleDetected = Rover

  enum ParseError {
    case InvalidPlanet(message: String)
    case InvalidRover(message: String)
  }

  enum Command {
    case Move(to: Movement)
    case Turn(on: Rotation)
    case Unknown
  }

  enum Movement {
    case Forward, Backward
  }

  enum Rotation {
    case OnRight, OnLeft
  }

  enum Orientation {
    case N, E, W, S
  }
}
