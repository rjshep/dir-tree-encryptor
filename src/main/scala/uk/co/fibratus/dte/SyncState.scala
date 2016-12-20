package uk.co.fibratus.dte

import java.io.{File, FileWriter}

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions, ConfigValueFactory}

/**
  * Created by rjs on 20/12/2016.
  */
class SyncState(client: String, dest: String) {

  private val state =
    if(syncFile.exists()) ConfigFactory.parseFile(syncFile)
    else ConfigFactory.empty()

  private lazy val renderOpts = ConfigRenderOptions.defaults().setOriginComments(false).setComments(false).setJson(false)

  private lazy val syncFile = new File(s"$dest/.syncstate")

  lazy val timestamp:Option[Long] =
    if(state.hasPath(client)) Some(state.getString(client).toLong)
    else None

  def update():Unit = {
    val newstate = state.withValue(client, ConfigValueFactory.fromAnyRef(System.currentTimeMillis().toString))
    val str = newstate.root().render(renderOpts)
    val fw = new FileWriter(syncFile)
    fw.write(str)
    fw.flush()
    fw.close()
  }
}
