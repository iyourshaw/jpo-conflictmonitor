package us.dot.its.jpo.conflictmonitor.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/status")
public class StatusController {

    private static final Logger logger = LoggerFactory.getLogger(StatusController.class);

    private Map

    @GetMapping(value = "/streams", produces = "text/plain")
    public @ResponseBody ResponseEntity<String> streams() {
        
    }
    
}
