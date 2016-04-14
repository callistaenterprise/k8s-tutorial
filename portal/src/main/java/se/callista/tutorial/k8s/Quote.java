package se.callista.tutorial.k8s;

public class Quote {

	private String quote;
	private String language;

	public Quote() {}

	public Quote(String quote, String language) {
		this.quote = quote;
		this.language = language;
	}
	
	public String getQuote() {
		return quote;
	}
	public void setQuote(String quote) {
		this.quote = quote;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	
}
