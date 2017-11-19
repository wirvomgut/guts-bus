package services

import javax.inject.Singleton

import akka.actor.ActorRef
import com.google.inject.Inject
import de.wirvomgut.wvgbus.shared.BusProtocol.{BusMessage, FromToMessage, SimpleMessage}
import play.api.{Configuration, Logger}
import play.api.inject.ApplicationLifecycle

import scala.collection.mutable
import scala.concurrent.Future
import scala.sys.process.{Process, ProcessLogger}
import scala.util.matching.Regex

@Singleton
class BusService @Inject() (configuration: Configuration, lifecycle: ApplicationLifecycle) {
  private val busFromToRegex: Regex = """from\s(\d{1,3}\.\d{1,3}\.\d{1,3})\sto\s(\d{1,3}\/\d{1,3}\/\d{1,3})""".r

  val lastMessages:CircularBuffer[BusMessage] = new CircularBuffer(100)
  val actorsToNotify: mutable.ArrayBuffer[ActorRef] = mutable.ArrayBuffer()

  val knxdHost: String = configuration.get[String]("knxd.host")

  val monitorBusProcess: Process = Process(s"knxtool vbusmonitor1 ip:$knxdHost").run(new ProcessLogger {
    Logger.info(s"Starting bus monitor on host $knxdHost")

    override def err(s: => String): Unit = {
      actorsToNotify.foreach(f => f ! SimpleMessage(s))
      println(s)
    }

    override def out(s: => String): Unit = if(s != null) {
      val busMessage = busFromToRegex.findFirstMatchIn(s).map { m =>
        val from = m.group(1)
        val to = m.group(2)

        FromToMessage(SimpleMessage(s), from, to)
      }.getOrElse {
        SimpleMessage(s)
      }

      actorsToNotify.foreach(f => f ! busMessage)
      lastMessages.add(busMessage)
    }

    override def buffer[T](f: => T): T = f
  })

  def knxToolOn(addr: String): Process = {
    knxToolExecutor(s"knxtool on ip:$knxdHost $addr")
  }

  def knxToolOff(addr: String): Process = {
    knxToolExecutor(s"knxtool off ip:$knxdHost $addr")
  }

  def knxToolExecutor(command: String): Process = {
    Process(command).run()
  }

  lifecycle.addStopHook { () =>
    Logger.info("Shutting down bus monitor ...")
    Future.successful(monitorBusProcess.destroy())
  }
}

class CircularBufferIterator[T](buffer:Array[T], start:Int) extends Iterator[T]{
  var idx=0
  override def hasNext: Boolean = idx < buffer.length
  override def next(): T ={
    val i=idx
    idx=idx+1
    buffer(i)
  }
}

class CircularBuffer[T](size:Int)(implicit m:Manifest[T]) extends Seq[T] {
  val buffer = new Array[T](size)
  var bIdx = 0

  override def apply(idx: Int): T = buffer((bIdx + idx) % size)

  override def length: Int = size

  override def iterator = new CircularBufferIterator[T](buffer, bIdx)

  def add(e: T): Unit = {
    buffer(bIdx) = e
    bIdx = (bIdx + 1) % size
  }
}
