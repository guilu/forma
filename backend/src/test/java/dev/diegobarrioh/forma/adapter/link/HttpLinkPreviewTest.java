package dev.diegobarrioh.forma.adapter.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.application.ValidationException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Pulling a product photo out of a page somebody linked to (FOR-200), against a fake fetcher — this
 * suite never opens a socket.
 *
 * <p>The interesting half is what it refuses. The URL comes from a form, and a server that fetches
 * whatever it is handed is a way to read things only the server can reach.
 */
class HttpLinkPreviewTest {

  private final FakeFetcher fetcher = new FakeFetcher();

  /**
   * Names resolve to a public address; literal IPs resolve themselves, which is what the hostile
   * cases below are made of. No test here touches DNS.
   */
  private final HttpLinkPreview preview =
      new HttpLinkPreview(
          fetcher,
          host ->
              host.equals("localhost")
                  ? new InetAddress[] {InetAddress.getByName("127.0.0.1")}
                  : new InetAddress[] {
                    host.matches("[0-9.]+|\\[.*]") ? InetAddress.getByName(host) : PUBLIC
                  });

  private static final InetAddress PUBLIC = publicAddress();

  private static InetAddress publicAddress() {
    try {
      return InetAddress.getByAddress("público", new byte[] {93, (byte) 184, (byte) 216, 34});
    } catch (UnknownHostException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Test
  void takesTheOpenGraphImageWhenThePageHasOne() {
    fetcher.serve(
        "https://tienda.example/producto",
        """
        <html><head>
          <meta property="og:image" content="https://cdn.example/foto.jpg">
        </head></html>
        """);

    assertThat(preview.imageFor("https://tienda.example/producto"))
        .contains("https://cdn.example/foto.jpg");
  }

  /** Attribute order and quoting vary by site; the tag is what matters, not how it was written. */
  @Test
  void readsTheTagHoweverItIsWritten() {
    fetcher.serve(
        "https://tienda.example/p",
        "<meta content='https://cdn.example/x.png' property='og:image' />");

    assertThat(preview.imageFor("https://tienda.example/p")).contains("https://cdn.example/x.png");
  }

  @Test
  void fallsBackToTheTwitterImage() {
    fetcher.serve(
        "https://tienda.example/p",
        "<meta name=\"twitter:image\" content=\"https://cdn.example/t.jpg\">");

    assertThat(preview.imageFor("https://tienda.example/p")).contains("https://cdn.example/t.jpg");
  }

  /**
   * Amazon serves no Open Graph image at all — verified against a real product page, which answers
   * 200 with 2.7 MB of HTML and not one og: tag. Their photo lives in an embedded JSON blob, so
   * this is a deliberate, site-shaped fallback rather than a general rule.
   */
  @Test
  void findsAnAmazonPhotoWhenThereIsNoOpenGraphTag() {
    fetcher.serve(
        "https://www.amazon.es/dp/B07Q31N9D4",
        """
        <html><body><script>
        {"colorImages":{"initial":[{"large":"https://m.media-amazon.com/images/I/31kt192oAzL._AC_.jpg"}]}}
        </script></body></html>
        """);

    assertThat(preview.imageFor("https://www.amazon.es/dp/B07Q31N9D4"))
        .contains("https://m.media-amazon.com/images/I/31kt192oAzL._AC_.jpg");
  }

  @Test
  void findsNothingWhenThePageAdvertisesNoImage() {
    fetcher.serve("https://tienda.example/p", "<html><head><title>Nada</title></head></html>");

    assertThat(preview.imageFor("https://tienda.example/p")).isEmpty();
  }

  /**
   * The URL arrives from a form. A server that fetches whatever it is handed is a way to read what
   * only the server can reach — a cloud metadata endpoint, a database admin page on localhost, a
   * service on the private network. These are refused before a socket is opened.
   */
  @Test
  void refusesAnythingThatIsNotAPublicHttpUrl() {
    assertThat(fetcher.calls()).isEmpty();

    for (String hostile :
        new String[] {
          "http://localhost:8080/admin",
          "http://127.0.0.1/",
          "http://[::1]/",
          "http://169.254.169.254/latest/meta-data/",
          "http://10.0.0.5/interno",
          "http://192.168.1.1/router",
          "file:///etc/passwd",
          "ftp://ejemplo.com/x",
          "javascript:alert(1)",
          "not a url at all"
        }) {
      assertThatThrownBy(() -> preview.imageFor(hostile))
          .describedAs("should refuse %s", hostile)
          .isInstanceOf(ValidationException.class);
    }

    assertThat(fetcher.calls()).describedAs("nothing must be fetched").isEmpty();
  }

  /** An image URL is only useful if a browser will load it: http(s), never a data: or a script. */
  @Test
  void ignoresAnImageThatIsNotAnHttpUrl() {
    fetcher.serve(
        "https://tienda.example/p", "<meta property='og:image' content='javascript:alert(1)'>");

    assertThat(preview.imageFor("https://tienda.example/p")).isEmpty();
  }

  private static final class FakeFetcher implements PageFetcher {
    private final Map<String, String> pages = new LinkedHashMap<>();
    private final java.util.List<String> calls = new java.util.ArrayList<>();

    void serve(String url, String body) {
      pages.put(url, body);
    }

    java.util.List<String> calls() {
      return calls;
    }

    @Override
    public Optional<String> fetch(String url) {
      calls.add(url);
      return Optional.ofNullable(pages.get(url));
    }
  }
}
