import java.text.DateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class Row {
	public static final String QUESTION = "question";
	public static final String MAIL = "mail";
	public static final String NOTE = "note";
	
	private String _id;
	private String parent;
	private String user;
	private String subject;
	private String content;
	private String type;
	private long noted;
	private Date date = null;
	
	public static class TimeComparator implements Comparator<Row> {
		@Override
		public int compare(Row o1, Row o2) {
			long diff = o1.getTimestamp() - o2.getTimestamp();
			if (diff < 0) return -1;
			else if (diff > 0) return 1;
			return 0;
		}
	}
	
	public String getId() {
		return _id;
	}
	public void setId(String id) {
		this._id = id;
	}
	public String getParent() {
		return parent;
	}
	public void setParent(String parent) {
		this.parent = parent;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public long getTimestamp() {
		return noted;
	}
	public void setTimestamp(long timestamp) {
		this.noted = timestamp;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Date getDate() {
		return date;
	}
	public String getFormattedDate() {
		date = new Date(noted*1000);
		if (date == null) return "null";
		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);
		return df.format(date);
	}
	
	@Override
	public String toString() {
		return "Row [id=" + _id + ", parentId=" + parent + ", userId="
				+ user + ", subject=" + subject + ", content=" + content
				+ ", type=" + type + ", timestamp=" + noted + "]";
	}
}
