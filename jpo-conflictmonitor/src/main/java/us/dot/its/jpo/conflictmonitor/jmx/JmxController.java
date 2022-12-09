package us.dot.its.jpo.conflictmonitor.jmx;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

@RestController
@RequestMapping(produces = "application/json")
public class JmxController {

    Logger log = LoggerFactory.getLogger(JmxController.class);
    
 
    @GetMapping("/beans")
    public BeanList getAllBeans() {
        log.info("getAllBeans");
        var jmx = new JmxManager();
        return jmx.listAllBeans();
    }

    @GetMapping("/beans/{domain}")
    public BeanList getBeans(@PathVariable String domain,
            @RequestParam(name = "key", required = false) String keyFilter,
            @RequestParam(name = "class", required = false) String classNameFilter,
            @RequestParam(name = "attribute", required = false) String[] attributes) {
        log.info("getBeans domain: {}, keyFilter: {}, classNameFilter: {}, attributes: {}", domain, keyFilter, classNameFilter, attributes);
        try {
            var jmx = new JmxManager();
            Set<String> attributeSet = null;
            if (attributes != null) {
                attributeSet = new HashSet<String>(Arrays.asList(attributes));
            }
            return jmx.listBeans(domain, keyFilter, classNameFilter, attributeSet);
        } catch (Exception ex) {
            var err = new BeanList();
            err.setError(ex.getMessage());
            log.error("Exception in /beans/{domain}", ex);
            return err;
        }
    }

    @GetMapping("/bean/{name}")
    public BaseBean getBean(@PathVariable String name) {
        log.info("getBean: {}", name);
        try {
            var decodedName = UriUtils.decode(name, StandardCharsets.UTF_8);
            var jmx = new JmxManager();
            return jmx.getBean(decodedName);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

   
}

