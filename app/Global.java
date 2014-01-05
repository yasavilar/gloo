import play.*;
import play.api.mvc.EssentialFilter;
import play.filters.gzip.GzipFilter;
import play.libs.*;
import scala.concurrent.duration.Duration;

import gloo.PastesManager;

import java.util.concurrent.TimeUnit;

public class Global extends GlobalSettings
{
	@Override
	public void onStart ( Application app )
	{
		if (!Play.isTest ()) {
			long interval = Play.application().configuration().getMilliseconds("cleanning.interval");
			Akka.system ().scheduler ().schedule (
				Duration.create(0, TimeUnit.MILLISECONDS),
				Duration.create(interval, TimeUnit.MILLISECONDS),
				new Runnable() {
					public void run() {
						PastesManager.deleteOld ();
					}
				},
				Akka.system().dispatcher()
			);
		}
	}

	public <T extends EssentialFilter> Class<T>[] filters() {
		return new Class[]{GzipFilter.class};
	}
}