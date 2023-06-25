import kor.toxicity.cutscenemaker.util.HttpUtil;
import org.junit.jupiter.api.Test;

public class GameProfileTest {
    @Test
    public void testGameProfile() {
        System.out.println(HttpUtil.getPlayerGameProfile("toxicity_210"));
    }
}
