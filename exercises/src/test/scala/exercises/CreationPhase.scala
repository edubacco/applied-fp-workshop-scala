package exercises

class CreationPhase extends munit.FunSuite {

  case class Item(qty: Int)

  enum OptionalItem {
    case Some(item: Item)
    case None
  }

  import OptionalItem._
  def createItem(qty: String): OptionalItem =
    if (qty.matches("^[0-9]+$")) OptionalItem.Some(Item(qty.toInt))
    else OptionalItem.None // typically return null or throw exception

  test("creation") {
    assertEquals(createItem("100"), OptionalItem.Some(Item(100)))
  }

  test("invalid creation") {
    assertEquals(createItem("asd"), None)
    assertEquals(createItem("1 0 0"), None)
    assertEquals(createItem(""), None)
  }

}
