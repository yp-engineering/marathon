package mesosphere.marathon.api.v2

import java.util
import javax.ws.rs._
import javax.ws.rs.core.MediaType
import javax.inject.Inject
import mesosphere.marathon.api.{ EndpointsHelper }
import mesosphere.marathon.api.v2.json.EnrichedTask
import mesosphere.marathon.health.HealthCheckManager
import mesosphere.marathon.{ MarathonConf, MarathonSchedulerService }
import mesosphere.marathon.tasks.TaskTracker
import mesosphere.marathon.api.EndpointsHelper
import mesosphere.marathon.api.v2.json.EnrichedTask
import org.apache.log4j.Logger
import com.codahale.metrics.annotation.Timed
import mesosphere.marathon.health.HealthCheckActor.Health
import org.apache.mesos.Protos.TaskState
import scala.concurrent.Await
import scala.collection.JavaConverters._

@Path("v2/tasks")
class TasksResource @Inject() (service: MarathonSchedulerService,
                               taskTracker: TaskTracker,
                               healthCheckManager: HealthCheckManager,
                               config: MarathonConf) {

  val log = Logger.getLogger(getClass.getName)

  @GET
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Timed
  def indexJson(@QueryParam("status") status: String,
                @QueryParam("status[]") statuses: util.List[String]) = {
    if (status != null) statuses.add(status)
    val statusSet = statuses.asScala.flatMap(toTaskState).toSet
    Map(
      "tasks" -> taskTracker.list.flatMap {
        case (appId, setOfTasks) =>
          setOfTasks.tasks.collect {
            case task if statusSet.isEmpty || statusSet(task.getStatus.getState) =>
              EnrichedTask(
                appId,
                task,
                Await.result(healthCheckManager.status(appId, task.getId), config.zkTimeoutDuration)
              )
          }
      }
    )
  }

  @GET
  @Produces(Array(MediaType.TEXT_PLAIN))
  @Timed
  def indexTxt() = EndpointsHelper.appsToEndpointString(
    taskTracker,
    service.listApps().toSeq,
    "\t"
  )

  private def toTaskState(state: String): Option[TaskState] = state.toLowerCase match {
    case "running" => Some(TaskState.TASK_RUNNING)
    case "staging" => Some(TaskState.TASK_STAGING)
    case _         => None
  }
}
