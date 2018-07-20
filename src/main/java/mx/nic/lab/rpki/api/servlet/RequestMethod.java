package mx.nic.lab.rpki.api.servlet;

/**
 * Enum representing the supported methods by this API. Each servlet is
 * responsible of indicating which of this methods does support.
 *
 */
public enum RequestMethod {
	GET, POST, PUT, DELETE
}
