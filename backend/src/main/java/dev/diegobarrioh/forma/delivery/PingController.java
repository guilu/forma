package dev.diegobarrioh.forma.delivery;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal API smoke endpoint (FOR-88). Confirms the versioned API base path is reachable and
 * returns JSON; it carries no product behavior and no user data. Real product endpoints are added
 * by their own stories under {@link ApiPaths#V1}.
 */
@RestController
@RequestMapping(ApiPaths.V1)
public class PingController {

  @GetMapping("/ping")
  public PingResponse ping() {
    return new PingResponse("ok");
  }

  /** Response body for the smoke endpoint. */
  public record PingResponse(String status) {}
}
