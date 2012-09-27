package efrei.refresh.dps;

import java.util.LinkedList;
import java.util.ListIterator;

public class SortedDocsList extends LinkedList<PrintedDoc> {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public boolean add(PrintedDoc item) {
		ListIterator<PrintedDoc> it = listIterator();
		while (it.hasNext()) {
			PrintedDoc element = it.next();
			if (item.getLogin().compareTo(element.getLogin()) < 0) {
				it.previous();
				break;
			}
		}
		it.add(item);
		return true;
	}
	
	public int totalPages(String login) {
		int pages = 0;
		ListIterator<PrintedDoc> it = listIterator();
		while (it.hasNext()) {
			PrintedDoc element = it.next();
			if (login.equals(element.getLogin())) pages += element.getNumPages();
		}
		return pages;
	}
}
