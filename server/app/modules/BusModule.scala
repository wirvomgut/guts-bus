package modules

import com.google.inject.AbstractModule
import services.BusService

class BusModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[BusService]).asEagerSingleton()
  }
}
