package part2_primer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object BackpressureBasics extends App {

  implicit val system = ActorSystem("BackpressureBasics")
  implicit val materializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] { x =>
    // simulate a long processing
    Thread.sleep(1000)
    println(s"Sink: ${x}")
  }

  fastSource.to(slowSink).run() // why fusing?!
  // this is not backpressure because it's the same actor and there are not compensatory mechanisms

  fastSource.async.to(slowSink).run()
  // backpressure implemented because there are 2 actors and the first has to slow down to keep up with the second one

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming: ${x}")
    x + 1
  }

  fastSource.async
    .via(simpleFlow).async
    .to(slowSink)
    .run()
  // here we see backpressure in action
  // an akka stream can have multiple reactions to implementing backpressure and BUFFER it's one of them, it happens here

  /*
    reactions to brackpressure (in order):
    - try to slow down if possible (1st example it was possible, 2nd one simple flow cannot do that)
    - buffer elements until there's more demand
    - drop down elements from the buffer if ti overflows until there's more demand => we can only intervene here, when it's about to overflow
    - tear down/kill the whole stream (failure)
   */

  val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropBuffer)
  fastSource.async
    .via(bufferedFlow).async // it's the simpleFlow with some additional settings - instead of 16, the limit is 10 and if it overflows => dropHead
    // dropHead - will drop the oldest element in the buffer
    .to(slowSink)
    .run()
  // this prints:
  // - incoming: 1 to 1000
  // - the first 16 numbers in the sink
  // - the last ten numbers from 2 to 1001 in the flow, max flow

  /*
    1-16: nobody is backpressured
    17-26: flow will buffer, the sink doesn't have the change to print, it's too slow, flow will start dropping at the next element
    26-1000: flow will always drop the oldest element
      => 991-1000 => 992-1001 => sink
   */

  /*
    overflow strategies:
    - dropHead = oldest
    - drop tail = newest
    - drop new = exact element to be added = keeps the buffer
    - drop buffer = drops the entire buffer
    - backpressure signal
    - fail
   */

  // throttling - manual trigger backpressure

  import scala.concurrent.duration._
  fastSource.throttle(10, 1 second).runWith(Sink.foreach(println))

}
