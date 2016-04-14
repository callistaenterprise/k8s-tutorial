package se.callista.tutorial.k8s;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Controller
public class PortalController {

	private static final Logger LOG = LoggerFactory.getLogger(PortalController.class);
	
	@Value("${quote.server}")
	private String quoteServer;
	
	@Value("${quote.port}")
	private String quotePort;
	
    RestTemplate restTemplate = new RestTemplate();

    @RequestMapping("/home")
    public String home(Model model, Locale locale) {
    	String language = locale.getLanguage();
    	Quote quote = null;
    	int retries = 0;
    	while (quote == null && retries < 5) {
	        try {
				quote = restTemplate.getForObject("http://"+quoteServer+":"+quotePort+"/quote?language="+language, Quote.class);
			} catch (RestClientException e) {
				LOG.warn("Failed to get quote: " + e.getMessage());
				retries++;
			}
		}
    	if (quote == null) {
    		throw new RuntimeException("Cannot get quote");
    	}
    	model.addAttribute("quote", quote.getQuote());
        return "home";
    }
}