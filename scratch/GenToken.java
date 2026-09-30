import java.security.*;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class GenToken {
    // JWT requires RS256, but implementing RS256 signature natively is a bit complex in raw Java without libraries.
    // However, since Nimbus is on the classpath, I can use Nimbus to generate a token!
}
