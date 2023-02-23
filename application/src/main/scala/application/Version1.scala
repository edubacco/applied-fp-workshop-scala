package application

import application.Version1.Command.*
import application.Version1.Orientation.*

/*
    ## V1 - Focus on the center (pure domain logic)

    Develop an API (types and functions) that executes commands:

    - Implement all commands logic.
    - Commands are sent in batch and executed sequentially.
    - The planet grid has a wrapping effect from one edge to another (pacman).
    - For now, ignore obstacle detection logic
 */
object Version1 {

  case class Size(width: Int, height: Int)

  case class Planet(size: Size, obstacles: Set[Obstacle])

  case class Obstacle(position: Position)

  enum Command {
    case MoveBackward
    case MoveForward
    case TurnRight
    case TurnLeft
  }

  enum Orientation {
    case N
    case S
    case W
    case E
  }
  case class Position(x: Int, y: Int)

  case class Rover(position: Position, orientation: Orientation)

  def executeAll(planet: Planet, rover: Rover, commands: List[Command]): Rover =
    commands.foldLeft(rover)((updatedRover, command) => execute(planet, updatedRover, command))

  def execute(planet: Planet, rover: Rover, command: Command): Rover =
    command match {
      case MoveBackward => moveBackward(planet, rover)
      case MoveForward  => moveForward(planet, rover)
      case TurnRight    => turnRight(rover)
      case TurnLeft     => turnLeft(rover)
    }

  def turnRight(rover: Rover): Rover =
    rover.copy(orientation = rover.orientation match {
      case N => E
      case S => W
      case W => N
      case E => S
    })

  def turnLeft(rover: Rover): Rover =
    rover.copy(orientation = rover.orientation match {
      case N => W
      case S => E
      case W => S
      case E => N
    })

  def moveForward(planet: Planet, rover: Rover): Rover = {
    val move = rover.orientation match {
      case N => (0, 1)
      case S => (0, -1)
      case W => (-1, 0)
      case E => (1, 0)
    }

    val newPosition = move match {
      case (x, 0) => (wrap(rover.position.x, planet.size.width, x), rover.position.y)
      case (0, y) => (rover.position.x, wrap(rover.position.y, planet.size.height, y))
      case _      => (0, 0)
    }
    rover.copy(position = Position(newPosition._1, newPosition._2))
  }

  def moveBackward(planet: Planet, rover: Rover): Rover = invertOrientation(moveForward(planet, invertOrientation(rover)))

  def invertOrientation(rover: Rover): Rover = rover.copy(orientation = rover.orientation match {
    case N => S
    case S => N
    case W => E
    case E => W
  })

  // NOTE: utility function to get the pacman effect
  def wrap(value: Int, limit: Int, delta: Int): Int =
    (((value + delta) % limit) + limit) % limit
}
