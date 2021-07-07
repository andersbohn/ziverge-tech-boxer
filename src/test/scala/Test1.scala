import zio.*
import zio.blocking.Blocking
import zio.test.Assertion.*
import zio.test.*

object MiniSpec extends DefaultRunnableSpec {
  override def spec =
    suite("MiniSpec") {
      testM(" check 3==3 ") {
        (for {
          tehCount <- Task.succeed(3)
        } yield
          assert(tehCount)(equalTo(3))
          )
      }//.provideSomeLayer(layers)
    }
}