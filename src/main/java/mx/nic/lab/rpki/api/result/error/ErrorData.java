package mx.nic.lab.rpki.api.result.error;

/**
 * Object to represent an error at a response
 *
 */
public class ErrorData {

	/**
	 * Error title, may be an object identifier or something meaningful so that the
	 * client can relate the error to a field/property
	 */
	private String title;

	/**
	 * Error description to provide more details
	 */
	private String description;

	public ErrorData() {
		// Empty
	}

	public ErrorData(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
