package ch.openech.dancer.frontend;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.minimalj.backend.Backend;
import org.minimalj.frontend.action.Action;
import org.minimalj.frontend.form.Form;
import org.minimalj.frontend.form.element.ImageFormElement;
import org.minimalj.frontend.form.element.ReferenceFormElement;
import org.minimalj.frontend.form.element.TextFormElement;
import org.minimalj.frontend.page.SimpleTableEditorPage;
import org.minimalj.model.Keys;
import org.minimalj.model.annotation.Size;
import org.minimalj.model.validation.ValidationMessage;
import org.minimalj.repository.query.By;
import org.minimalj.security.Subject;
import org.minimalj.util.StringUtils;

import ch.openech.dancer.model.DanceEvent;
import ch.openech.dancer.model.EventStatus;
import ch.openech.dancer.model.Location;

public class DanceEventTablePage extends SimpleTableEditorPage<DanceEvent> {

	private static final Object[] KEYS = new Object[] { DanceEvent.$.date, DanceEvent.$.getFromUntil(), DanceEvent.$.title, DanceEvent.$.location.name };
	private static final Object[] KEYS_ADMIN = new Object[] { DanceEvent.$.date, DanceEvent.$.title, DanceEvent.$.location.name, DanceEvent.$.status };

	public DanceEventTablePage() {
		super(Subject.currentHasRole("admin") ? KEYS_ADMIN : KEYS);
	}

	private Form<DanceEventTableFilter> filterForm;
	private DanceEventTableFilter filter = new DanceEventTableFilter();

	public static class DanceEventTableFilter {
		public static final DanceEventTableFilter $ = Keys.of(DanceEventTableFilter.class);
		@Size(100)
		public String filter;
	}

//  in next MJ
//	@Override
//	protected FormContent getOverview() {
//		if (filterForm == null) {
//			filterForm = new Form<>();
//			filterForm.line(DanceEventTableFilter.$.filter);
//			filterForm.setChangeListener(form -> refresh());
//			filterForm.setObject(filter);
//		}
//		return filterForm.getContent();
//	}

	@Override
	public List<Action> getTableActions() {
		if (Subject.currentHasRole("admin")) {
			return Arrays.asList(new TableNewObjectEditor(), new TableEditor(), new DeleteDetailAction());
		} else {
			return null;
		}
	}

	@Override
	protected List<DanceEvent> load() {
		if (!StringUtils.isEmpty(filter.filter)) {
			return Backend.find(DanceEvent.class, By.search(filter.filter));
		} else {
			return Backend.find(DanceEvent.class, By.ALL);
		}
	}

	@Override
	protected DanceEvent createObject() {
		DanceEvent event = new DanceEvent();
		event.from = LocalTime.of(20, 00);
		event.until = LocalTime.of(23, 00);
		event.status = EventStatus.published;
		return event;
	}

	@Override
	protected Form<DanceEvent> createForm(boolean editable, boolean newObject) {
		Form<DanceEvent> form = new Form<>(editable, 2);
		form.line(DanceEvent.$.date);
		form.line(DanceEvent.$.from, DanceEvent.$.until);
		form.line(DanceEvent.$.title);
		form.line(DanceEvent.$.description);
		form.line(new ImageFormElement(DanceEvent.$.flyer, 3));
		form.line(editable ? new ReferenceFormElement<>(DanceEvent.$.location, Location.$.name) : new TextFormElement(DanceEvent.$.location));
		// form.line(editable ? new ReferenceFormElement<>(DanceEvent.$.organizer,
		// Organizer.$.name) : new TextFormElement(DanceEvent.$.organizer));
		// form.line(new RecurFormElement(Keys.getProperty(DanceEvent.$.recur),
		// editable));
		form.line(DanceEvent.$.tags);
		if (Subject.currentHasRole("admin")) {
			form.line(DanceEvent.$.status);
		}
		return form;
	}

	@Override
	protected void validate(DanceEvent event, boolean newObject, List<ValidationMessage> validationMessages) {
		if (newObject) {
			if (event.date != null && event.date.isBefore(LocalDate.now())) {
				validationMessages.add(new ValidationMessage(DanceEvent.$.date, "Muss in Zukunft liegen"));
			}
		}
		validateTime(event, validationMessages);
	}

	private void validateTime(DanceEvent event, List<ValidationMessage> validationMessages) {
		if (event.from != null && event.until != null) {
			if (event.from.isAfter(event.until)) {
				validationMessages.add(new ValidationMessage(DanceEvent.$.from, "Von muss zeitlich vor bis sein"));
			}
		}
	}

}