package ch.openech.dancer.backend.provider;

import java.time.LocalDate;
import java.util.Optional;

import org.minimalj.repository.query.By;
import org.minimalj.util.CsvReader;

import ch.openech.dancer.backend.DanceEventProvider;
import ch.openech.dancer.backend.EventUpdateCounter;
import ch.openech.dancer.model.DanceEvent;
import ch.openech.dancer.model.EventStatus;
import ch.openech.dancer.model.Location;
import ch.openech.dancer.model.Region;

public class DanceToDanceImport extends DanceEventProvider {
	private static final long serialVersionUID = 1L;

	@Override
	public EventUpdateCounter updateEvents() {
		EventUpdateCounter result = new EventUpdateCounter();

		CsvReader reader = new CsvReader(getClass().getResourceAsStream("/ch/openech/dancer/data/dance_to_dance.csv"));
		for (DanceEvent event : reader.readValues(DanceEvent.class)) {
			Optional<DanceEvent> danceEventOptional = findOne(DanceEvent.class,
					By.field(DanceEvent.$.location, location).and(By.field(DanceEvent.$.date, event.date)));

			if (!danceEventOptional.isPresent() && event.date.isAfter(LocalDate.now())) {
				event.location = this.location;
				event.header = location.name;
				event.status = EventStatus.generated;

				save(event, result);
			}
		}
		return result;
	}

	@Override
	protected Location createLocation() {
		Location location = new Location();
		location.country = "Schweiz";
		location.address = "Gerbergasse 13";
		location.city = "4001 Basel";
		location.region.add(Region.BS);
		location.name = "Dance To Dance";
		location.url = "http://www.dancetodance.ch";
		return location;
	}

}
