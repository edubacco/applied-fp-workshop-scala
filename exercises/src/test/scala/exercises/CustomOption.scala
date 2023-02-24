package exercises

class CustomOption extends munit.FunSuite {

  enum Option[A] {
    case Yes[A](value: A) extends Option[A]
    case Nope[A]() extends Option[A]

    //Implement the map function
    def map[B](f: A => B): Option[B] = this match {
      case Nope() => Nope()
      case Yes(x) => Yes(f(x))
    }

    // Implement the flatMap function
    def flatMap[B](f: A => Option[B]): Option[B] = this match {
      case Nope() => Nope()
      case Yes(x) => f(x)
    }

    // Implement the fold function
    def fold[B](default: => B, to: A => B): B = this match {
      case Nope() => default
      case Yes(x) => to(x)
    }
  }

  object Option {
    // Implement the pure function
    def pure[A](a: A): Option[A] = Yes(a)
  }

  def increment(x: Int): Int =
    x + 1

  def reverseString(x: Int): Option[String] =
    Option.pure(x.toString.reverse)

  test("creation phase") {
    val result = Option
      .pure(10)

    assertEquals(result, Option.Yes(10))
  }

  test("combination phase - normal") {
    val result = Option
      .Yes(10)
      .map(increment)

    assertEquals(result, Option.Yes(11))
  }

  test("combination phase - effectful") {
    val result = Option
      .Yes(10)
      .flatMap(reverseString)

    assertEquals(result, Option.Yes("01"))
  }

  test("removal phase - value") {
    val result = Option
      .Yes(10)
      .fold("nope", _.toString)

    assertEquals(result, "10")
  }

  test("removal phase - alternative value") {
    val result = Option
      .Nope()
      .fold("nope", _.toString)

    assertEquals(result, "nope")
  }
}
