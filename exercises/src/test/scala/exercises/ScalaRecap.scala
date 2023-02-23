package exercises

/*
 * Our most used Scala features are:
 * - Case class
 * - Companion Object
 * - Apply function
 * - Pattern match
 * - Trait as interface
 * - Trait as mixin
 */

class ScalaRecap extends munit.FunSuite {

  case class Person(name: String, age: Int) {
    def apply(prefix: String): String =
      prefix + " mi chiamo " + name + "!"

    def makeOlder(amount: Int): Person = copy(age = age + amount)
  }

  object Person {
    def create(details: String): Person = {
      val det = details.split(";", 2)
      Person(name = det(0), age = det(1).toInt)
    }

    def apply(details: String): Person =
      Person.create(details)

    def isFake(p: Person): Boolean = p match {
      case Person("foo", _) => true
      case Person("bar", _) => true
      case Person(_, age) if age < 0 => true
      case _ => false
    }
  }

  trait Fruit {
    def stringify: String
    def eatenBy(name: String): String = s"$name ate $stringify"
  }

  case class Apple() extends Fruit {
    override def stringify: String = "an apple"
  }

  case class Banana() extends Fruit {
    override def stringify: String = "a banana"
  }

  test("define case class") {
    val result = Person("foo", 56)
    assertEquals(result, Person("foo", 56))
  }

  test("define the case class's companion object") {
    val result = Person.create("foo;56")
    assertEquals(result, Person("foo", 56))
  }

  test("case class apply") {
    val result = Person("foo", 56)("Ciao,")
    assertEquals(result, "Ciao, mi chiamo foo!")
  }

  test("companion object apply") {
    val result = Person("foo;56")("Ciao,")
    assertEquals(result, "Ciao, mi chiamo foo!")
  }

  test("update case class state") {
     val p      = Person("foo", 56)
     val result = p.makeOlder(100)
     assertEquals(result.age, 156)
  }

  test("pattern match") {
     import Person._
     assert(isFake(Person("foo", 10)))
     assert(isFake(Person("bar", 10)))
     assert(isFake(Person("baz", -10)))
     assert(!isFake(Person("baz", 10)))
  }

  test("trait as interface (part 1)") {
     assert(Apple().isInstanceOf[Fruit])
     assert(Banana().isInstanceOf[Fruit])
  }

  test("trait as interface (part 2)") {
     assertEquals(Apple().stringify, "an apple")
     assertEquals(Banana().stringify, "a banana")
  }

  test("trait as mixin") {
     assertEquals(Apple().eatenBy("foo"), "foo ate an apple")
     assertEquals(Banana().eatenBy("bar"), "bar ate a banana")
  }
}
