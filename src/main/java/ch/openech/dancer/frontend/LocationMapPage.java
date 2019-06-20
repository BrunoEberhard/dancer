package ch.openech.dancer.frontend;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import ch.openech.dancer.model.DanceEvent;
import org.apache.http.client.utils.URIBuilder;
import org.minimalj.backend.Backend;
import org.minimalj.frontend.Frontend;
import org.minimalj.frontend.Frontend.IContent;
import org.minimalj.frontend.impl.json.JsonFrontend;
import org.minimalj.frontend.impl.json.JsonReader;
import org.minimalj.frontend.impl.json.JsonWriter;
import org.minimalj.frontend.page.Page;
import org.minimalj.repository.query.By;

import ch.openech.dancer.DancerWebServer;
import ch.openech.dancer.model.Location;

public class LocationMapPage extends Page {
	private static final Logger LOG = Logger.getLogger(DancerWebServer.class.getName());

	private static String template;

	static {
		template = JsonFrontend.readStream(LocationMapPage.class.getResourceAsStream("/ch/openech/dancer/location_map.html"));
	}

	@Override
	public IContent getContent() {
		List<Location> locations = Backend.find(Location.class, By.all());

		// https://nominatim.openstreetmap.org/search?country=CH&city=Bern&format=jsonv2

		List<Map<String, Object>> locs = new ArrayList<>();
		for (Location location : locations) {


			List<DanceEvent> events = Backend.find(DanceEvent.class, By.field(DanceEvent.$.location, location).order(DanceEvent.$.date));
			Map<String, Object> lMap = new HashMap<>();
			lMap.put("name", location.name);
			lMap.put("address", location.address);
			lMap.put("city", location.city);
			lMap.put("url", location.url);
			if (!events.isEmpty()) {
				DanceEvent upcomingEvent = events.get(0);
				lMap.put("upcoming", createUpcomingEvent(upcomingEvent));
			}

			if (location.latitude == null) {
				location = getPosition(location);
			}
			if (location.latitude != null) {
				lMap.put("latitude", location.latitude);
				lMap.put("longitude", location.longitude);
				locs.add(lMap);
			}
		}

		String json = new JsonWriter().write(locs);

		String html = template.replace("$LOCS", json);
		return Frontend.getInstance().createHtmlContent(html);
	}

	private static DateTimeFormatter shortFormat = DateTimeFormatter.ofPattern("d.M.yyyy");

	private String createUpcomingEvent(DanceEvent event) {
		return String.format("%s %s %s %s %s",shortFormat.format(event.date), event.title, event.description, event.getDayOfWeek(), event.getFromUntil());
	}


	private Location getPosition(Location location) {
		try {
			URIBuilder b = new URIBuilder("https://nominatim.openstreetmap.org");
			b.addParameter("country", location.country);
			b.addParameter("city", location.city.substring(location.city.indexOf(' ') + 1));
			b.addParameter("street", location.address);
			b.addParameter("format", "jsonv2");

			URL url = b.build().toURL();
			try (InputStreamReader isr = new InputStreamReader(url.openStream())) {
				List<?> result = (List<?>) JsonReader.read(isr);
				@SuppressWarnings("unchecked")
				Map<String, Object> values = (Map<String, Object>) result.get(0);
				location.latitude = new BigDecimal((String) values.get("lat"));
				location.longitude = new BigDecimal((String) values.get("lon"));
			}
			return Backend.save(location);
		} catch (Exception x) {
			LOG.warning("Could not retrieve position of " + location.name + " (" + x.getMessage() + ")");
			return location;
		}
	}

}
