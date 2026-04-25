package com.boxdispatch.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
 
@Controller
public class HomeController {
 
    /**
     * GET /  →  redirects to /dashboard
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }
 
    /**
     * GET /dashboard  →  dashboard.html
     * The JWT check is done client-side; if no token exists,
     * the page JS redirects to /login.
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}