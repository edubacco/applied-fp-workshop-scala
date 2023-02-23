package application

import application.Version1.Command._
import application.Version1.Orientation.*

class Version1Tests extends munit.FunSuite {

  import application.Version1._

  // TODO: implements tests

  test("turn right command") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 0), N)
    val command = Command.TurnRight
    val result = execute(planet, rover, command)
    assertEquals(result, Rover(Position(0, 0), Orientation.E))
  }

  test("turn left command") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 0), N)
    val command = TurnLeft
    val result = execute(planet, rover, command)
    assertEquals(result, Rover(Position(0, 0), Orientation.W))
  }

  test("move forward command") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 1), N)
    val command = Command.MoveForward

    val result = execute(planet, rover, command)
    assertEquals(result, Rover(Position(0, 2), N))
  }

  test("move forward command, opposite orientation") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 1), S)
    val command = MoveForward

    val result = execute(planet, rover, command)
    assertEquals(result, Rover(Position(0, 0), S))
  }

  test("move backward command") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 1), N)
    val command = MoveBackward

    val result = execute(planet, rover, command)
    assertEquals(result, Rover(Position(0, 0), N))
  }

  test("move backward command, opposite orientation") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 1), S)
    val command = MoveBackward

    val result = execute(planet, rover, command)
    assertEquals(result, Rover(Position(0, 2), S))
  }

  test("wrap on North") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 3), N)
    val command = MoveForward
    val result = execute(planet, rover, command)
    assertEquals(result, Rover(Position(0, 0), N))
  }

  test("pacman effect on y") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 0), N)
    val command = MoveBackward
    val result = execute(planet, rover, command)
    assertEquals(result, Rover(Position(0, 3), N))
  }

  test("pacman effect on y reverse") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 0), N)
    val commands = List(TurnLeft, TurnLeft, MoveForward)
    val result = executeAll(planet, rover, commands)
    assertEquals(result, Rover(Position(0, 3), S))
  }

  test("pacman effect on x") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 0), W)
    val commands = List(MoveForward)
    val result = executeAll(planet, rover, commands)
    assertEquals(result, Rover(Position(4, 0), W))
  }

  test("go to opposite angle") {
    val planet = Planet(Size(5, 4), Set())
    val rover = Rover(Position(0, 0), N)
    val commands: List[Command] = List(TurnLeft, MoveForward, TurnRight, MoveBackward)
    val result = executeAll(planet, rover, commands)
    assertEquals(result, Rover(Position(4, 3), N))
  }
}
