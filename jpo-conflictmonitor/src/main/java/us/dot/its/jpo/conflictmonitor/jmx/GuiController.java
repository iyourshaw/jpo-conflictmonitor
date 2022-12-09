package us.dot.its.jpo.conflictmonitor.jmx;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

@Controller
@RequestMapping("/ui")
public class GuiController {

    Logger log = LoggerFactory.getLogger(GuiController.class);
    
     @GetMapping("/beans")
    public String viewAllbeans(Model model) {
        var jmx = new JmxManager();
        var beans = jmx.listAllBeans().stream().sorted((x, y) -> x.getName().compareTo(y.getName())).collect(Collectors.toList());
        model.addAttribute("beans", beans);
        return "view-all-beans";
    }

    @GetMapping("/beans/{domain}")
    public String viewBeans(Model model,
            @PathVariable String domain,
            @RequestParam(name = "key", required = false) String keyFilter,
            @RequestParam(name = "class", required = false) String classNameFilter,
            @RequestParam(name = "attribute", required = false) String[] attributes) {
        log.info("viewBeans domain: {}, keyFilter: {}, classNameFilter: {}, attributes: {}",
            domain, keyFilter, classNameFilter, attributes);
        model.addAttribute("domain", domain);
        model.addAttribute("keyFilter", keyFilter);
        model.addAttribute("classNameFilter", classNameFilter);
        model.addAttribute("attributes", attributes);
        try {
            var jmx = new JmxManager();
            Set<String> attributeSet = null;
            String formattedAttribs = null;
            if (attributes != null) {
                attributeSet = new HashSet<String>(Arrays.asList(attributes));
                formattedAttribs = String.join(",", attributes);
                model.addAttribute("attributes", formattedAttribs);
            }
            var beans = jmx.listBeans(domain, keyFilter, classNameFilter, attributeSet);
            model.addAttribute("beans", beans);
        } catch (Exception ex) {
            var err = new BeanList();
            err.setError(ex.getMessage());
            log.error("Exception in /beans/{domain}", ex);
            model.addAttribute("beans", err);
        }  
        return "view-all-beans";
    }

    @GetMapping("/bean/{name}")
    public String viewBean(
        Model model,
        @PathVariable("name") String name,
        @RequestParam(name = "attribute", required = false) String[] attributes
    ) {
        
        String timestamp = ZonedDateTime.now().toString();
        model.addAttribute("timestamp", timestamp);

        String formattedAttribs = null;
        if (attributes != null && attributes.length > 0) {
            formattedAttribs = String.join(",", attributes);
            model.addAttribute("attributes", formattedAttribs);
        }
        log.info("viewBean name: {}, attributes: {}", name, formattedAttribs);
        var decodedName = UriUtils.decode(name, StandardCharsets.UTF_8);
        log.info("viewBean {}", decodedName);
        model.addAttribute("name", decodedName);
        try {
            var jmx = new JmxManager();
            var bean = jmx.getBean(decodedName, attributes);
            model.addAttribute("bean", bean);
        } catch (Exception ex) {
            log.error("Error getting bean by name", ex);
            var bean = new BaseBean(ex.getMessage());
            model.addAttribute("bean", bean);
        }
        return "view-bean";
    }

   
}
